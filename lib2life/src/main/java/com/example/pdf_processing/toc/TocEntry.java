package com.example.pdf_processing.toc;

public class TocEntry {
	private String chapterTitle;
	private int page;
	
	public TocEntry(String chapterTitle, int page) {
		super();
		this.chapterTitle = chapterTitle;
		this.page = page;
	}
	
	public String getChapterTitle() {
		return chapterTitle;
	}
	public void setChapterTitle(String chapterTitle) {
		this.chapterTitle = chapterTitle;
	}
	
	public int getPage() {
		return page;
	}
	public void setPage(int page) {
		this.page = page;
	}
}
