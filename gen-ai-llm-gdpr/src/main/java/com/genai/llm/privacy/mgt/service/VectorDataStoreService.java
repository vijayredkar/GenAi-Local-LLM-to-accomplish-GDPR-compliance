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
	
	
	//vj6
	public String retrieve(String contextType, String userPrompt)
	{
		return retrieve("city", contextType, userPrompt);
	}
	
	/* 
	 * VectorDB operations to fetch records
	 */
	
	public String retrieve(String category, String contextType, String userPrompt)//vj6
	{
		System.out.println("\n--- started VectorDB operations");		
		//vj3
		System.out.println("---- embeddingModel : "+ modelSvc.getEmbeddingModel() +" \n "+"contextType : "+ contextType +" \n "+"userPrompt : "+ userPrompt +" \n "+"category : "+ category  +" \n "+"minScore : "+ minScoreRelevanceScore +" \n "+"maxResults : "+ maxResultsToRetrieve);
		
		//vj3
		//List<EmbeddingMatch<TextSegment>> result = fetchRecords(text);
		List<EmbeddingMatch<TextSegment>> result = fetchRecords(category, userPrompt);//vj6
		
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
	public List<EmbeddingMatch<TextSegment>> fetchRecords(String category, String query)//vj6
	{	
		EmbeddingModel embdgModel= modelSvc.getEmbeddingModel();	
		//EmbeddingStore<TextSegment> embdgStore = contextLoadSvc.getEmbeddingStore(); //vjl 
		
		//vj6
		String vectorDbCollection = "collection-gdpr-1";
		if("employer".equals(category))
		{
		  vectorDbCollection = "collection-employer-1";		
		}
		
		EmbeddingStore<TextSegment> embdgStore = contextLoadSvc.getEmbeddingStoreForTests(vectorDbCollection);	//vj6
		
		
		Embedding queryEmbedding = embdgModel.embed (query).content(); 
		//vj3
	        return  embdgStore.findRelevant(queryEmbedding, maxResultsToRetrieve, minScoreRelevanceScore);
	        //System.out.println("**** VectorDB invocation de-activated");
	        //return new ArrayList<EmbeddingMatch<TextSegment>>(); // "dummySemanticResult"
	}
	
	/*
	* loads context to VectorDB
	*/	
	public void load (String fileNameWithFullPath, boolean testMode)
	{
		System.out.println("\n---- started loading context to Vector DB ");	
		
		//vj6
		List<String> lines = new ArrayList<String>();		
		lines = storeCityData(fileNameWithFullPath, testMode, lines);		
		insertVectorData (modelSvc.getEmbeddingModel(), lines, testMode, "collection-gdpr-1");
		
		//vj6
		List<String> employerLines = new ArrayList<String>();
		employerLines = storeEmployerData(fileNameWithFullPath, testMode, employerLines);		
		insertVectorData (modelSvc.getEmbeddingModel(), employerLines, testMode, "collection-employer-1");
		
		System.out.println("---- completed loading context to VectorDB");
	}
	
	//vj6
    private List<String> storeCityData(String fileNameWithFullPath, boolean testMode, List<String> lines) {
		if(testMode)
		{			
		String testContext = "Nicobar";	
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
		testContext = "Coimbatore"; //vj3
		lines.add(testContext);	
		}
		else
		{
			lines = fileUtilsSvc.readFile (fileNameWithFullPath);	
		}
		return lines;
	}

    //vj6
	private List<String> storeEmployerData(String fileNameWithFullPath, boolean testMode, List<String> lines) {
			if(testMode)
			{
			String testContext = "Emirates Airlines";	
			lines.add(testContext);	
			testContext = "Damac Real Estates";	
			lines.add(testContext);	
			testContext = "Emaar Real Estates";	
			lines.add(testContext);	
			testContext = "Al Ansari Finance";	
			lines.add(testContext);	
			testContext = "Dnata Air Cargo";	
			lines.add(testContext);	
			testContext = "Aramex Courier";	
			lines.add(testContext);	
			testContext = "Etisalat Telecom";	
			lines.add(testContext);	
			testContext = "Du Telecom";	
			lines.add(testContext);	
			testContext = "ADNOC Oil Corporation";	
			lines.add(testContext); 
			testContext = "Carrefour Supermarket"; 
			lines.add(testContext); 
			testContext = "Hertz Car Rental"; 
			lines.add(testContext); 
			testContext = "DEWA"; 
			lines.add(testContext); 
			testContext = "Romano Water Supply"; 
			lines.add(testContext);
			testContext = "Centerpoint"; //vj3
			lines.add(testContext);	
			}
			else
			{
				lines = fileUtilsSvc.readFile (fileNameWithFullPath);	
			}
			return lines;
		}

	
	
	
	/*	
	* inserts to VectorDB	
	*/	
	private void insertVectorData (EmbeddingModel embeddingModel, List<String> lines, boolean testMode, String vectorDbCollection) 
	{
		//vj1
		for (String text: lines)			
		{
			TextSegment segment1 = TextSegment.from(text, new Metadata()); 
			Embedding embedding1 = embeddingModel.embed (segment1).content();
			
			if (testMode)			
			{
				System.out.println("---- VectorDB testMode "+testMode);			
				EmbeddingStore<TextSegment> embdStore = contextLoadSvc.getEmbeddingStoreForTests(vectorDbCollection);
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
