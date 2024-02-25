package com.genai.llm.privacy.mgt.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

@Service
public class ContextLoadService 
{	
	@Autowired
	private FileUtilsService fileUtilsSvc;
	
	private EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore();
	
	public EmbeddingStore<TextSegment> getEmbeddingStore() 
	{
		System.out.println("---- started connect to Chroma");
		
		Map<String, String> vectorDbConfig = new HashMap<String, String>();		
		String currentDir = System.getProperty("user.dir");
		String resoucePath = currentDir + "\\"+ "\\src\\main\\resources\\application.properties";		
		String vectorDbUrl        = new FileUtilsService().extractFields("vector.db.url", resoucePath);
		String vectorDbCollection = new FileUtilsService().extractFields("vector.db.collection", resoucePath);		
		
		if (embeddingStore == null) 
		{
			
			embeddingStore = ChromaEmbeddingStore.builder()
												 .baseUrl(vectorDbUrl)
												 .collectionName(vectorDbCollection)
												 .build();
		}
		
		System.out.println("---- completed connect to Chroma. Got embeddingStore "+embeddingStore);
		return embeddingStore;
	}	
}