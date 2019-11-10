package com.example.pdf_processing.toc;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class TocBasedSpliService {
	public JSONArray splitContents(PDDocument doc, List<TocEntry> tocEntries, int firstPagePosition) throws IOException {
		JSONArray result = new JSONArray();
		
		for(int i = 0; i < tocEntries.size(); i++) {
			//Detect chapter page range
			TocEntry tocEntry = tocEntries.get(i);
			int startPage = tocEntry.getPage();
			int endPage;
			if(i+1 < tocEntries.size()) {
				endPage = tocEntries.get(i+1).getPage();
			} else {
				endPage = doc.getNumberOfPages();
			}
			
			//Apply offset
			startPage += firstPagePosition - 2;
			endPage += firstPagePosition - 2;
			
			//Check bounds
			startPage = Math.max(startPage, 0);
			endPage = Math.min(endPage, doc.getNumberOfPages());
			
			String chapterText = getRangeContents(doc, startPage, endPage);
			
			JSONObject chapter = new JSONObject();
			chapter.put("chapterTitle", tocEntry.getChapterTitle());
			chapter.put("paragraphs", chapterText);
			result.add(chapter);
		}
		
		return result;
	}
	
	//Checks if each chapter has text from the previous one (same page) and moves it to the correct chapter
	public JSONArray splitCorrected(PDDocument doc, List<TocEntry> tocEntries, int firstPagePosition) throws IOException {
		JSONArray chapterSplit = splitContents(doc, tocEntries, firstPagePosition);
		
		for(int i = 1; i < chapterSplit.size(); i++) {
			JSONObject chapter = (JSONObject) chapterSplit.get(i);
			String chapterTitle = (String) chapter.get("chapterTitle");
			String chapterText = (String) chapter.get("paragraphs");
			
			String[] chapterTextParts = chapterText.split("(?i)" + Pattern.quote(chapterTitle), 2);
			String previousChapterFragment = "";
			String currentChapterText;
			if(chapterTextParts.length > 1) {
				previousChapterFragment = chapterTextParts[0];
				currentChapterText = chapterTextParts[1];
			} else {
				previousChapterFragment = "";
				currentChapterText = chapterText;
			}
			
			chapter.put("paragraphs", currentChapterText);
			
			JSONObject previousChapter = (JSONObject) chapterSplit.get(i-1);
			String previousChapterText = (String) previousChapter.get("paragraphs");
			previousChapter.put("paragraphs", previousChapterText + "\n" + previousChapterFragment);
		}
		return chapterSplit;
	}
	
	public String getRangeContents(PDDocument fullDoc, int startPage, int endPage) throws IOException {
		try(PDDocument rangeDoc = new PDDocument()) {
			for(int i = startPage; i < endPage; i++) {
				rangeDoc.addPage(fullDoc.getPage(i));
			}
			
			PDFTextStripper textStripper = new PDFTextStripper();
			return textStripper.getText(rangeDoc);
		}
	}
}
