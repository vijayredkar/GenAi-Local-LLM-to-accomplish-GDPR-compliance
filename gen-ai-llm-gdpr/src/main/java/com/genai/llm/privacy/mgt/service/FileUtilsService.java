package com.genai.llm.privacy.mgt.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class FileUtilsService 
{			
	/*
	 * general file operations
	 */
	public List<String> readFile(String fileNameWithFullPath) 
	{
		List<String> lines = new ArrayList<String>();
		Path path = Paths.get(fileNameWithFullPath);
		
		try 
		{
			 lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return lines;
	}
	
	/*
	 * expects fieldNamePair to be in the format key=value
	 * llm.response.temperature=0.8
	 */
	
	public String extractFields(String fieldNamePair, String resourcePath) 
	{	
		List<String> lines = readFile(resourcePath);
		
		//llm.response.temperature=0.8
		String keyValuePair = lines.stream()
									 .filter(line -> line.contains(fieldNamePair))
									 .findFirst()
									 .orElse("");
		
		String[] tokens = keyValuePair.split("=");   // llm.response.temperature     0.8
		return tokens[1]; // 0.8
	}
}