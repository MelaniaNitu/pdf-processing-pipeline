package com.example.pdf_processing.parser_simple;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import com.cedarsoftware.util.io.JsonWriter;

@Service
public class ParsePdfImages 
{
	public static void parse (PDDocument document, String bookTitle) throws IOException 
	{
		//Make book title valid for path use
		bookTitle = bookTitle.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
		String imagesPath = "extractedImages/" + bookTitle;
		Files.createDirectories(Paths.get(imagesPath));
		
		System.out.println("PDF loaded"); 

		int pages = document.getNumberOfPages();
		System.out.println("Pages: " + pages);

		PDPage page = document.getPage(8);
		float height = page.getMediaBox().getHeight();
		float width = page.getMediaBox().getWidth();
		Rectangle2D header = new Rectangle2D.Float(0, 0, page.getTrimBox().getWidth(), 15);
		String regionName = "header";
		
		System.out.println(height);
		System.out.println(width);
		PDPage page2 = document.getPage(8);
		
		System.out.println(page2.getMediaBox().getLowerLeftX());
		System.out.println(page.getTrimBox().getHeight());
		System.out.println(page2.getAnnotations());
		PDFTextStripperByArea stripper;
		stripper = new PDFTextStripperByArea();
		stripper.addRegion(regionName, header);
		stripper.extractRegions(page);
		String text = stripper.getTextForRegion(regionName);
		
		ParagraphManager paragraphManager = ParagraphManager.getInstance();
		ArrayList<String> paragraphs = paragraphManager.getParagraphs(text);
		
		//extrect images
		PDResources pdResources = page.getResources();
		int i = 1;
		for (COSName name : pdResources.getXObjectNames()) 
		{
			PDXObject o = pdResources.getXObject(name);
			if (o instanceof PDImageXObject) 
			{
				PDImageXObject image = (PDImageXObject)o;
				String filename = imagesPath + "/extracted-image-" + i + ".png";
				ImageIO.write(image.getImage(), "png", new File(filename));
				System.out.println("Image saved.");
				i++;
			}
		}

		//Closing the document
		document.close();

		// Writing to JSON
		JSONObject json = new JSONObject();
 
		JSONArray list = new JSONArray();

		Iterator<String> paragraphsIterator = paragraphs.iterator();
		
		while (paragraphsIterator.hasNext()) 
		{
	        list.add(paragraphsIterator.next());
		}
		
		json.put("paragraphs", list);

		try (FileWriter jsonFile = new FileWriter("pdf.json")) 
		{
			String formattedJson = JsonWriter.formatJson(json.toJSONString());
			jsonFile.write(formattedJson);
			System.out.println("Successfully Copied JSON Object to File...");
		}
	}
}