package com.example.pdf_processing.parser_simple;

import java.util.ArrayList;
import java.util.Arrays;

public class ParagraphManager 
{ 
	private static ParagraphManager single_instance = null; 

	private ParagraphManager() {} 

	public static ParagraphManager getInstance() 
	{ 
		if (single_instance == null) 
			single_instance = new ParagraphManager(); 

		return single_instance; 
	}

	public ArrayList<String> getParagraphs(String text)
	{
		String[] array = text.split("\\n", -1);
		
		ArrayList<String> paragraphs = new ArrayList<String>();

		String paragraph = "";
		String splitWord = ""; // used for words split up

		for (String s: array) 
		{   
			StringBuilder builder = new StringBuilder();

			String[] words = s.split("\\s+"); // words in the current line

			// Check for unfinished words.
			if (splitWord.length() != 0)
			{
				splitWord += words[0];
				words = Arrays.copyOfRange(words, 1, words.length);
				System.out.println("Word: " + splitWord);
				
				builder.append(splitWord + ' ');

				splitWord = "";
			}

			if (s.length() != 0)
			{
				if (splitWord.length() != 0)
					break;

				char lastChar = s.charAt(s.length() - 1);

				if (lastChar == '-')
				{
					splitWord += words[words.length - 2];
					words = Arrays.copyOf(words, words.length - 2);
				}
			}

			// Check for paragraph endings.
			String st = s;
			st.replaceAll("\\s+", "");
			
			for (String w : words) 
			{
			    builder.append(w);
			    builder.append(' ');
			}			

			String str = builder.toString();
			paragraph += str;
			
			if (st.length() > 1)
			{
				st = st.substring(st.length() - 2, st.length() - 1);
				
				if (st.equals(".")  || st.equals("?") || st.equals("!"))
				{
					paragraphs.add(paragraph);
					paragraph = "";
				}
			}
			
			//System.out.println("Line: " + str);
		}

		return paragraphs;
	}
}
