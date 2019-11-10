package com.example.pdf_processing.toc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class TableOfContentsParserService {
	private Pattern tocPageNumberPattern = Pattern.compile("\\d+\\s*$", Pattern.MULTILINE);
	private Pattern tocEntryPattern = Pattern.compile("(.+?)[^\\w\\d]+(\\d+)\\s*\\r?\\n");
	private int maxTocPages = 10;
	
	public List<TocEntry> parse(PDDocument doc) throws IOException {
		PDPageTree pages = doc.getPages();
		
		//Find TOC start
		Integer tocStartPageNo = getTocStartPageNo(pages);
		
		//Get a few pages of text from TOC start
		PDDocument tocSearchDoc = new PDDocument();
		for(int i = tocStartPageNo; i < Math.min(tocStartPageNo + maxTocPages, pages.getCount()); i++) {
			tocSearchDoc.addPage(pages.get(i));
		}
		
		PDFTextStripper textStripper = new PDFTextStripper();
		String tocSearchText = textStripper.getText(tocSearchDoc);
		
		//Parse TOC entries
		List<TocEntry> tocEntries = new ArrayList<>();
		
		Matcher m = tocEntryPattern.matcher(tocSearchText);
		while(m.find() ) {
			tocEntries.add(new TocEntry(m.group(1), Integer.valueOf(m.group(2))));
		}
		
		return tocEntries;
	}
	
	public Integer getTocStartPageNo(PDPageTree pages) throws IOException {
		for(int i=0; i < pages.getCount(); i++) {
			if(pageIsTocStart(pages.get(i))) {
				return i;
			}
		}
		
		return null;
	}
	
	private boolean pageIsTocStart(PDPage page) throws IOException {
		String pageText = getPageText(page);
		
		//Check for TOC title
		if(pageText.toLowerCase().contains("cuprins") || pageText.toLowerCase().contains("tabla de materii")) {
			//check if it has multiple lines ending with numbers
			int pageNubmerMatches = countPattern(pageText, tocPageNumberPattern);
			if(pageNubmerMatches >= 3) {
				return true;
			}
		}
		
		return false;
	}
	
	private String getPageText(PDPage page) throws IOException {
		PDFTextStripper textStripper = new PDFTextStripper();
		
		PDDocument tempDoc = new PDDocument();
		tempDoc.addPage(page);
		
		return textStripper.getText(tempDoc);
	}
	
	//Count no. of regex pattern matches
	public static int countPattern(String references, Pattern referencePattern) {
	    Matcher matcher = referencePattern.matcher(references);
	    return Stream.iterate(0, i -> i + 1)
	            .filter(i -> !matcher.find())
	            .findFirst()
	            .get();
	}
}
