package com.genai.llm.privacy.mgt.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;

@Service
public class VectorDataStoreService 
{			
	@Value("${retrieval.max.limit : 1}")
	int maxResultsToRetrieve; 
	
	@Value("${embeddings.min.score : 0.5}")
	double minScoreRelevanceScore;	 
	
	@Autowired
	private ModelService modelSvc;
	
	@Autowired
	private ContextLoadService contextLoadSvc;
	
	@Autowired
	private FileUtilsService fileUtilsSvc;
	
	/*
	 * Chroma operations to fetch records
	 */
	public String retrieve(String text) 
	{	
		System.out.println("\n--- started Chroma operations");
		System.out.println("---- embeddingModel : "+ modelSvc.getEmbeddingModel() +" \n "+"text : "+ text +" \n "+"minScore : "+ minScoreRelevanceScore +" \n "+"maxResults : "+ maxResultsToRetrieve);
		
		List<EmbeddingMatch<TextSegment>> result = fetchRecords(text);	
		
		StringBuilder responseBldr = new StringBuilder();
		StringBuilder tempResponseBldr = new StringBuilder();
		
		for(EmbeddingMatch<TextSegment> segment : result)
		{
			responseBldr.append(segment.embedded().text());
			
			tempResponseBldr.append(segment.embedded().text());
			tempResponseBldr.append("- with embedding score : ");
			tempResponseBldr.append(segment.score());
			tempResponseBldr.append("\n");
		}
		
		System.out.println("--- Got most relevant record from Chroma : \n"+tempResponseBldr.toString());
		System.out.println("--- completed Chroma operations");
		return responseBldr.toString();
	}
	
	/*
	 * fetches records from Chroma based on semantic similarities
	 */
	public List<EmbeddingMatch<TextSegment>> fetchRecords(String query) 
	{
		EmbeddingModel embdgModel= modelSvc.getEmbeddingModel();
		EmbeddingStore<TextSegment> embdgStore = contextLoadSvc.getEmbeddingStore();  
		
        Embedding queryEmbedding = embdgModel.embed(query).content();
        return  embdgStore.findRelevant(queryEmbedding, maxResultsToRetrieve, minScoreRelevanceScore);
    }
	
	/*
	 * loads context to Chroma
	 */
	public void load(String fileNameWithFullPath) 
	{
		System.out.println("\n---- started loading context to Chroma ");		
				
		List<String> lines = fileUtilsSvc.readFile(fileNameWithFullPath);		
		insertVectorData(modelSvc.getEmbeddingModel(), lines);        
		
		System.out.println("---- completed loading context to Chroma");
    }

	/*
	 * inserts to Chroma
	 */
	private void insertVectorData(EmbeddingModel embeddingModel, List<String> lines) 
	{
		for(String text : lines)
		{
			TextSegment segment1 = TextSegment.from(text, new Metadata());
	        Embedding embedding1 = embeddingModel.embed(segment1).content();
	        contextLoadSvc.getEmbeddingStore().add(embedding1, segment1);	        
	        System.out.println("---- loading to Chroma : " +text);
		}
	}
}