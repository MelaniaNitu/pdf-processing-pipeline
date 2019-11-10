package com.example.pdf_processing.parser_advanced;

public class FontInfo {
	public String fontName;
	public float fontSize;
	
	public FontInfo(String fontName, float fontSize) {
		this.fontName = fontName;
		this.fontSize = fontSize;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj != null && obj.getClass() == getClass()) {
			FontInfo fontInfo = (FontInfo)obj;
			return fontInfo.fontName.equals(fontName) && fontInfo.fontSize == fontSize;
		}
		
		return false;
	}

	@Override
	public int hashCode() {
		return fontName.hashCode() + (int)fontSize;
	}
	
	
}
