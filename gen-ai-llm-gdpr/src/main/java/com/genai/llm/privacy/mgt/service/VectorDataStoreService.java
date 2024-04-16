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
import dev. langchain4j.data.embedding. Embedding; 
import dev.langchain4j.data.segment.TextSegment; 
import dev.langchain4j.model.embedding. EmbeddingModel; 
import dev.langchain4j.store.embedding.EmbeddingMatch; 
import dev.langchain4j.store.embedding.EmbeddingStore;

@Service //vjl
public class VectorDataStoreService
{
	@Value("${retrieval.max.limit:1}")	
	int maxResultsToRetrieve;
	
	@Value("${embeddings.min.score:0.5}")	
	double minScoreRelevanceScore;
	
	@Autowired	
	private ModelService modelSvc;
	
	@Autowired //vjl	
	private ContextLoadService contextLoadSvc;
	
	@Autowired	
	private FileUtilsService fileUtilsSvc;
	
	/* 
	 * VectorDB operations to fetch records
	 */
	
	public String retrieve(String text, String userPrompt)//vj3
	{
		System.out.println("\n--- started VectorDB operations");		
		//vj3
		System.out.println("---- embeddingModel : "+ modelSvc.getEmbeddingModel() +" \n "+"text : "+ text +" \n "+"userPrompt : "+ userPrompt  +" \n "+"minScore : "+ minScoreRelevanceScore +" \n "+"maxResults : "+ maxResultsToRetrieve);
		
		//vj3
		//List<EmbeddingMatch<TextSegment>> result = fetchRecords(text);
		List<EmbeddingMatch<TextSegment>> result = fetchRecords(userPrompt);
		
		StringBuilder responseBldr = new StringBuilder(); StringBuilder tempResponseBldr = new StringBuilder();
		
		for (EmbeddingMatch<TextSegment> segment: result)		
		{
			responseBldr.append(segment.embedded().text());		
			
			tempResponseBldr.append(segment.embedded().text());
			tempResponseBldr.append("- with embedding score : ");		
			tempResponseBldr.append(segment.score());		
			tempResponseBldr.append("\n");
		}		
		
		System.out.println("--- Got most relevant record from VectorDB : \n"+tempResponseBldr.toString()); 
		System.out.println("--- completed VectorDB operations");		
		return responseBldr.toString();
	}
	
	/*
	* fetches records from VectorDB based on semantic similarities	
	*/	
	public List<EmbeddingMatch<TextSegment>> fetchRecords (String query)
	{	
		EmbeddingModel embdgModel= modelSvc.getEmbeddingModel();	
		//EmbeddingStore<TextSegment> embdgStore = contextLoadSvc.getEmbeddingStore(); //vjl 
		EmbeddingStore<TextSegment> embdgStore = contextLoadSvc.getEmbeddingStoreForTests();	
		Embedding queryEmbedding = embdgModel.embed (query).content(); 
		//vj3
	        //return  embdgStore.findRelevant(queryEmbedding, maxResultsToRetrieve, minScoreRelevanceScore);
	        System.out.println("**** VectorDB invocation de-activated");
	        return new ArrayList<EmbeddingMatch<TextSegment>>(); // "dummySemanticResult"
	}
	
	/*
	* loads context to VectorDB
	*/	
	public void load (String fileNameWithFullPath, boolean testMode)
	{
		System.out.println("\n---- started loading context to Vector DB ");	
		
		List<String> lines = new ArrayList<String>();
		
		if(testMode)
		{	
		String testContext = "This instruction is to detect. Given a text, you will respond with fields that indicate person identifiable information. Your response should contain only these specific fields separted by commas. Be very concise in your response. The text is";	
		lines.add(testContext);
		
		testContext = "Nicobar";	
		lines.add(testContext);	
		testContext = "North Middle Andaman";	
		lines.add(testContext);	
		testContext = "South Andaman";	
		lines.add(testContext);	
		testContext = "Anantapur";	
		lines.add(testContext);	
		testContext = "Chittoor";	
		lines.add(testContext);	
		testContext = "East Godavari";	
		lines.add(testContext);	
		testContext = "Alluri Sitarama Raju";	
		lines.add(testContext);	
		testContext = "Anakapalli";	
		lines.add(testContext);	
		testContext = "Annamaya";	
		lines.add(testContext); 
		testContext = "Bapatla"; 
		lines.add(testContext); 
		testContext = "Eluru"; 
		lines.add(testContext); 
		testContext = "Guntur"; 
		lines.add(testContext); 
		testContext = "Kadapa"; 
		lines.add(testContext);
		}
		else
		{
			lines = fileUtilsSvc.readFile (fileNameWithFullPath);	
		}
		
		insertVectorData (modelSvc.getEmbeddingModel(), lines, testMode);
		
		System.out.println("---- completed loading context to VectorDB");
	}


	/*	
	* inserts to VectorDB	
	*/	
	private void insertVectorData (EmbeddingModel embeddingModel, List<String> lines, boolean testMode) 
	{
		//vj1
		for (String text: lines)			
		{
			TextSegment segment1 = TextSegment.from(text, new Metadata()); 
			Embedding embedding1 = embeddingModel.embed (segment1).content();
			
			if (testMode)			
			{
				System.out.println("---- VectorDB testMode "+testMode);			
				EmbeddingStore<TextSegment> embdStore = contextLoadSvc.getEmbeddingStoreForTests();			
				if (embdStore!=null) //if VectorDB is running on local
				{
					System.out.println("---- VectorDB connection is good ");			
					embdStore.add(embedding1, segment1);			
					System.out.println("---- loaded to VectorDB context : "+text);
				}
			}			
			else			
			{			
				System.out.println("---- VectorDB testMode) " +testMode);
				contextLoadSvc.getEmbeddingStore().add(embedding1, segment1); 
				System.out.println("---- loaded to VectorDB - context : "+ text);
			}
		} //end for
		
		System.out.println("---- insertVectorData executed");
	}	
}
