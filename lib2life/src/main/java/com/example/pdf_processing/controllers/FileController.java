package com.example.pdf_processing.controllers;


import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cedarsoftware.util.io.JsonWriter;
import com.example.pdf_processing.parser_advanced.AdvancedBookParserService;
import com.example.pdf_processing.parser_simple.ParsePdfImages;
import com.example.pdf_processing.parser_simple.SimpleBookParserService;
import com.example.pdf_processing.toc.TableOfContentsParserService;
import com.example.pdf_processing.toc.TocBasedSpliService;
import com.example.pdf_processing.toc.TocEntry;

@RestController
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private AdvancedBookParserService bookParserService;
    @Autowired
    private TableOfContentsParserService tableOfContentsParserService;
    @Autowired
    private TocBasedSpliService tocBasedSpliService;
    @Autowired
    private ParsePdfImages parsePdfImages;
    
    
    @PostMapping("/uploadFile")
    public String uploadFile(
    	@RequestParam("file") MultipartFile file,
    	@RequestParam("bookTitle") String bookTitle,
    	//@RequestParam("author") String author,
    	//@RequestParam("publishingYear") int publishingYear,
    	//@RequestParam("language") String language,
    	@RequestParam("pageone") int pageone,
    	@RequestParam("images") boolean images
    ) throws IOException {
    	InputStream fileStream = file.getInputStream();
    	try(PDDocument doc = PDDocument.load(fileStream)) {
	    	
	  	  	//JSONObject json = bookParserService.parse(doc);
	    	JSONObject json = new JSONObject();
	
	    	/*
			json.put("bookTitle", bookTitle);
			json.put("author", author);
			json.put("publishingYear", publishingYear);
			json.put("language", language);
			*/
	    	
	    	List<TocEntry> tocEntries = tableOfContentsParserService.parse(doc);
	    	
	    	//Adaugare cuprins in json
	    	/*JSONArray tcoEntriesJson = new JSONArray();
	    	for(TocEntry tocEntry : tocEntries) {
	    		JSONObject tocEntryJson = new JSONObject();
	    		tocEntryJson.put("title", tocEntry.getChapterTitle());
	    		tocEntryJson.put("page", tocEntry.getPage());
	    		tcoEntriesJson.add(tocEntryJson);
	    	}
	    	json.put("tableOfContents", tcoEntriesJson);*/

	    	//Spargere per capitole
	    	//JSONArray bookContent = tocBasedSpliService.splitCorrected(doc, tocEntries, pageone);
	    	
	    	//Spargere combinanta
	    	JSONArray bookContent = tocBasedSpliService.splitCorrected(doc, tocEntries, pageone);
	    	
	    	//json.put("content", bookContent);
	    	
	    	if(images) {
	    		parsePdfImages.parse(doc, bookTitle);
	    	}
	
	  	    return JsonWriter.formatJson(bookContent.toJSONString());
    	}
    }
    
    @PostMapping("/saveJson")
    public void saveJson(
    	@RequestParam("json") String json,
    	@RequestParam("bookTitle") String bookTitle,
    	@RequestParam("author") String author,
    	@RequestParam("publishingYear") int publishingYear,
    	@RequestParam("language") String language
    ) throws IOException, ParseException {
    	
    	//Add metadata and create final json
    	JSONObject jsonObject = new JSONObject();
    	JSONParser parser = new JSONParser();
    	JSONArray bookContent = (JSONArray) parser.parse(json);
    	jsonObject.put("content", bookContent);
    	
    	jsonObject.put("bookTitle", bookTitle);
    	jsonObject.put("bookTitle", bookTitle);
    	jsonObject.put("author", author);
		jsonObject.put("publishingYear", publishingYear);
		jsonObject.put("language", language);
		json = jsonObject.toJSONString();
    	
		//Save to elastic
    	Settings settings = Settings.builder().put("cluster.name", "elasticsearch").build();
    	try(TransportClient client = new PreBuiltTransportClient(settings)) {
    		client.addTransportAddress(new TransportAddress(InetAddress.getLocalHost(), 9300));

    		ClusterHealthResponse clusterResponse = client
    			    .admin()
    			    .cluster()
    			    .prepareHealth()
    			    .setWaitForGreenStatus()
    			    .setTimeout( TimeValue.timeValueSeconds( 5 ) )
    			    .execute()
    			    .actionGet();

    		if ( clusterResponse.isTimedOut() ) {
    			logger.warn( "The cluster is unhealthy: " + clusterResponse.getStatus() );
    			//return;
    		}
    		
    		IndexResponse indexResponse = client.prepareIndex("books", "book")
    	        .setSource(json, XContentType.JSON)
    	        .get();
    	
    		System.out.println(indexResponse);
    	}
    }
    
    //search in ES index    
    @PostMapping("/search")
    public List<String> searchInIndex(@RequestParam("searchBook") String searchBook) 
  		  throws IOException {
    	List<String> results = new ArrayList<>();	
    	
    	Settings settings = Settings.builder().put("cluster.name", "elasticsearch").build();
    	try(TransportClient client = new PreBuiltTransportClient(settings)) {
    		client.addTransportAddress(new TransportAddress(InetAddress.getLocalHost(), 9300));

  			SearchRequest searchRequest = new SearchRequest("books"); 
  			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder(); 
  			
  			HighlightBuilder highlightBuilder = new HighlightBuilder();
  			highlightBuilder.highlighterType("unified");
  			highlightBuilder.field(new HighlightBuilder.Field("bookTitle"));
  			highlightBuilder.field(new HighlightBuilder.Field("chapterTitle"));
  			highlightBuilder.field(new HighlightBuilder.Field("paragraphs"));
  			searchSourceBuilder.highlighter(highlightBuilder);
  			
  			searchSourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
  			
  			searchSourceBuilder.query(QueryBuilders.queryStringQuery(searchBook)); 
  			
  			searchRequest.source(searchSourceBuilder);
  			
  			SearchResponse searchResponse = client.search(searchRequest).actionGet();

  			
  			//Search Response
  			RestStatus status = searchResponse.status();
  			TimeValue took = searchResponse.getTook();
  			Boolean terminatedEarly = searchResponse.isTerminatedEarly();
  			boolean timedOut = searchResponse.isTimedOut();
  			
  			SearchHits hits = searchResponse.getHits();
  			
  			SearchHit[] searchHits = hits.getHits();
  			for (SearchHit hit : searchHits) {
  			    				
  				String id = hit.getId();
  				System.out.println("doc id: "+ id);
  				
  				Map<String, Object> sourceAsMap = hit.getSourceAsMap();
  				String bookTitle = (String) sourceAsMap.get("book_title");
  				System.out.println("title: "+ bookTitle);
  				results.add(bookTitle);
  				  				
  				Map<String, HighlightField> highlightFields = hit.getHighlightFields();
  				for(Map.Entry<String, HighlightField> entry : highlightFields.entrySet()) {
	  			    HighlightField highlight = entry.getValue(); 
	  			    Text[] fragments = highlight.fragments();  
	  			    String fragmentString = fragments[0].string();
	  				
	  				System.out.println("fragment: "+ fragmentString);
  				}

  			}
  			
    	}
    	return results;
   	}
         
}