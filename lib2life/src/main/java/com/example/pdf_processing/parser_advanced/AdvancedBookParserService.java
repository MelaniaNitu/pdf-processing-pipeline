package com.example.pdf_processing.parser_advanced;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import com.cedarsoftware.util.io.JsonWriter;

@Service
public class AdvancedBookParserService {
	public String chapterTitle = "";
	
	public JSONObject parse (PDDocument document) throws IOException
	{
		
		final String fontType = "";
		float fontSize = 0;
		
		final List<LineMetadata> lines = new ArrayList<>();

		PDFTextStripper stripper = new PDFTextStripper() {
		    String prevBaseFont = "";

		    protected void writeString(String text, List<TextPosition> textPositions) throws IOException{
		    	LineMetadata line = new LineMetadata();
		    	
		    	line.text = text;
		    	
		    	Map<FontInfo, Integer> fontInfoCounts = new HashMap<>();
		    	
		    	line.startPosition = textPositions.size() > 0 ? textPositions.get(0) : null;
		    	line.endPosition = textPositions.size() > 0 ? textPositions.get(textPositions.size() - 1) : null;
		    	
		    	for (TextPosition position : textPositions) {
					String baseFont = position.getFont().getName();
		            float fontSize = position.getFontSizeInPt();
		            
		            Optional<Entry<FontInfo,Integer>> fontInfoEntry = fontInfoCounts.entrySet().stream()
		            		.filter(entry -> entry.getKey().fontName.equals(baseFont) 
		            				&& entry.getKey().fontSize == fontSize)
		            		.findAny();
		            
		            if(fontInfoEntry.isPresent()) {
		            	int currentCount = fontInfoEntry.get().getValue();
		            	fontInfoEntry.get().setValue(currentCount + 1);
		            } else {
		            	fontInfoCounts.put(new FontInfo(baseFont, fontSize), 1);
		            }
		    	}
		    	
		    	Optional<Entry<FontInfo,Integer>> commonFontInfoEntry = fontInfoCounts.entrySet().stream()
		    			.max(Comparator.comparing(entry -> entry.getValue()));
		    	
		    	if(commonFontInfoEntry.isPresent()) {
		    		line.fontInfo = commonFontInfoEntry.get().getKey();
		    	}
		    	
		    	lines.add(line);
		    	/*StringBuilder builder = new StringBuilder();
				String title = "";

		        for (TextPosition position : textPositions){
		            String baseFont = position.getFont().getName();
		            float fontSize = position.getFontSizeInPt();

//		            System.out.println(fontSize);
//		            System.out.println(baseFont);
//		            System.out.println(baseFont + " " + fontSize);

					String t = "";

//		            System.out.println(fontSize);
		            if (baseFont.equals(fontType) && fontSize == 11.0f) {
		            	title += position.getUnicode();
		            	t += position.getUnicode();
//		            	System.out.println("t " + t);
		            	chapterTitle += t ;
//		            	System.out.println(position.getUnicode());
		            }else{
//			            if (baseFont != null && !baseFont.equals(prevBaseFont))
//			            {
//			                builder.append('[').append(baseFont).append(']');
//			                prevBaseFont = baseFont;
//			            }
			            
			            builder.append(position.getUnicode());
		            }
		        }
		        
	        
//		        System.out.println("TITLE: " + chapterTitle);
		        writeString(builder.toString());
//				System.out.println(prevBaseFont);
		        */
		    }
		};
		
		String text = stripper.getText(document);
			
		//Optional<Entry<FontInfo,Integer>> commonFontInfoEntry = fontInfoCounts.entrySet().stream()
		//.max(Comparator.comparing(entry -> entry.getValue()));
		
		/*Map<FontInfo, Long> textFonts = lines.stream()
				.collect(Collectors.groupingBy(line -> line.fontInfo, Collectors.counting()));
		
		textFonts.entrySet().forEach(entry -> System.out.println(entry.getKey().fontName + " " + entry.getKey().fontSize + ": " + entry.getValue()));
		
		//Most common font = content font		
		Optional<Entry<FontInfo, Long>> contentFontEntry = textFonts.entrySet().stream()
				.max(Comparator.comparing(group -> group.getValue()));
		
		FontInfo contentFont = contentFontEntry.get().getKey();*/
		
		Map<String, Long> textFonts = lines.stream()
				.collect(Collectors.groupingBy(line -> line.fontInfo.fontName, Collectors.counting()));
		
		textFonts.entrySet().forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));
		
		//Most common font = content font		
		Optional<Entry<String, Long>> contentFontEntry = textFonts.entrySet().stream()
				.max(Comparator.comparing(group -> group.getValue()));
		
		String contentFont = contentFontEntry.get().getKey();
		
		/*
		 * Cream un tag json initial pentru fiecare tip
		 * Parcurgem fiecare linie in ordine
		 * O introducem in tag-ul potrivit (titlu carte, titlu capitol, continut)
		 * Cand se schimba tipul in chapter_title din altceva, cream un obiect chapter nou
		 */
		
		JSONObject json = new JSONObject();
			
/*		String content = "";
		for(LineMetadata line : lines) {
			content += line.text;
		}
		json.put("content", content);
		
*/		
		JSONArray chapters = new JSONArray();
		json.put("chapters", chapters);
		
		String chapterTitle = "";
		String paragraphs = "";
		
		String lastType = null;
		
		for(LineMetadata line : lines) {
			String newType;
			if(line.fontInfo.fontName.equals(contentFont)) {
				newType = "paragraphs";
			} else {
				newType = "chapter_title";
			}
			
			if(!newType.equals(lastType) && lastType != null && newType.equals("chapter_title")) {
				//if last title was just a number(superscript), add the paragraph to the previous chapter
				if(isInteger(chapterTitle)) {
					JSONObject chapter = (JSONObject) chapters.get(chapters.size() - 1);
					String chapterParagraphs = (String) chapter.get("paragraphs");
					chapterParagraphs += paragraphs;
					chapter.put("paragraphs", chapterParagraphs);
				} else {
					JSONObject chapter = new JSONObject();
					chapters.add(chapter);
					chapter.put("chapter_title", chapterTitle);
					chapter.put("paragraphs", paragraphs);
				}
				chapterTitle = "";
				paragraphs = "";
			}
			
			switch(newType) {
				case "chapter_title":
					chapterTitle += line.text;
					break;
				case "paragraphs":
					paragraphs += line.text;
					break;
			}
			
			lastType = newType;
		}
		
		//At the end, add remaining text, if any
		JSONObject chapter = new JSONObject();
		if(chapterTitle.length() > 0) {
			chapter.put("chapter_title", chapterTitle);
		}
		if(paragraphs.length() > 0) {
			chapter.put("paragraphs", paragraphs);
		}
		if(!chapter.isEmpty()) {
			chapters.add(chapter);
		}
		
//		System.out.println(text);
//		System.out.println(chapterTitle);
		
		
		/*ParagraphManager paragraphManager = ParagraphManager.getInstance();
		ArrayList<String> paragraphs = paragraphManager.getParagraphs(text);
		
		JSONObject json = new JSONObject();
		JSONArray list = new JSONArray();
		
		Iterator<String> paragraphsIterator = paragraphs.iterator();
		
		while (paragraphsIterator.hasNext()) {
	        list.add(paragraphsIterator.next());
//	        System.out.println(paragraphsIterator.next());
		}
		
		json.put("title", chapterTitle);
		json.put("paragraphs", list);

//		fstream = new OutputStreamWriter(new FileOutputStream(mergedFile), StandardCharsets.UTF_8);
		
		try (OutputStreamWriter jsonFile = new OutputStreamWriter(new FileOutputStream("pdf.json"), StandardCharsets.UTF_16)) {
			String formattedJson = JsonWriter.formatJson(json.toJSONString());
			jsonFile.write(formattedJson);
			System.out.println("Successfully Copied JSON Object to File...");
		}
		*/
		
		//String formattedJson = JsonWriter.formatJson(json.toJSONString());
		
		return json;
	}
	
	private static boolean isInteger(String str) {
		return str.matches("^\\d+$");
	}
}
