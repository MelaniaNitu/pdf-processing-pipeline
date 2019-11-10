package com.example.pdf_processing.parser_simple;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import com.cedarsoftware.util.io.JsonWriter;

//https://stackoverflow.com/questions/21705961/get-font-of-each-line-using-pdfbox
@Service
public class SimpleBookParserService
{
	public String chapterTitle = "";
	
	public String parse (InputStream file) throws IOException 
	{
		//File file = new File("C:/Users/a-menitu/Desktop/UPB/Phd/SRI/project/proj-java/bcub_ok/Viata-in-Evul-Mediu.pdf"); 
		PDDocument document = PDDocument.load(file);
		
		PDPage page = document.getPage(17);
		PDDocument document1 = new PDDocument();
		document1.addPage(page);
		
		String fontType = "CAAAAA+TimesNewRomanPS-BoldMT";
		float fontSize = 11f;

		PDFTextStripper stripper = new PDFTextStripper() 
		{
		    //String prevBaseFont = "";

		    protected void writeString(String text, List<TextPosition> textPositions) throws IOException
		    {
		        StringBuilder builder = new StringBuilder();
				//String title = "";

		        if(textPositions.size() > 0) {
		            System.out.println(text
		            		+ " "+ textPositions.get(0).getFont().getName()
		            		+ " "+ textPositions.get(0).getFontSizeInPt());
		        }
		        
		        for (TextPosition position : textPositions)
		        {
		            String baseFont = position.getFont().getName();
		            float textFontSize = position.getFontSizeInPt();
		            //System.out.println(baseFont);
		            //System.out.println(fontSize);
					String t = "";

//		            System.out.println(fontSize);
		            if (baseFont.equals(fontType) && textFontSize == fontSize)
		            {
		            	//title += position.getUnicode();
		            	t += position.getUnicode();
//		            	System.out.println("t " + t);
		            	chapterTitle += t;
//		            	System.out.println(position.getUnicode());
		            }
		            else
		            {
//			            if (baseFont != null && !baseFont.equals(prevBaseFont))
//			            {
//			                builder.append('[').append(baseFont).append(']');
//			                prevBaseFont = baseFont;
//			            }
			            
			            builder.append(position.getUnicode());
		            }
		        }
		        
//		        System.out.println("TITLE: " + title);
		        writeString(builder.toString());
//				System.out.println(prevBaseFont);
		    }
		};
		
		String text = stripper.getText(document);
//		System.out.println(text);
//		System.out.println(chapterTitle);
		
		
		ParagraphManager paragraphManager = ParagraphManager.getInstance();
		ArrayList<String> paragraphs = paragraphManager.getParagraphs(text);
		
		JSONObject json = new JSONObject();
		JSONArray list = new JSONArray();

		Iterator<String> paragraphsIterator = paragraphs.iterator();
		
		while (paragraphsIterator.hasNext()) 
		{
	        list.add(paragraphsIterator.next());
		}
		
		json.put("title", chapterTitle);
		json.put("paragraphs", list);

		/*try (FileWriter jsonFile = new FileWriter("pdf.json")) 
		{
			String formattedJson = JsonWriter.formatJson(json.toJSONString());
			jsonFile.write(formattedJson);
			System.out.println("Successfully Copied JSON Object to File...");
		}*/
		

		String formattedJson = JsonWriter.formatJson(json.toJSONString());
		
		return formattedJson;
	}
}
