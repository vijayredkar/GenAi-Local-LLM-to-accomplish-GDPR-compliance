package com.genai.llm.privacy.mgt.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory. annotation. Autowired;
import org.springframework. stereotype.Service;
import dev. langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
//import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;

@Service
public class ContextLoadService
{
	@Autowired	
	private FileUtilsService fileUtilsSvc;
	
	//private EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore(); //vj1	
	private EmbeddingStore<TextSegment> embeddingStore = null; //vj1
	
	public EmbeddingStore<TextSegment> getEmbeddingStore()
	{
		System.out.println("started connect to VectorDB");
		
		//http://stackoverflow.com/questions/60003109/reading-files-from-ja nosuchfileexception		
		//String filePath getClass().getClassLoader().getResource("application.properties").getFile();		
		//System.out.println("filePath +filePath);
		
		Map<String, String> vectorDbConfig = new HashMap<String, String>();
		String currentDir = System.getProperty("user.dir");
		String resourcePath = currentDir + "\\" + "\\src\\main\\resources\\application.properties";		
		System.out.println("resoucePath" + resourcePath); 
		
		//vj1		
		String resourcePath1 = currentDir +"/"+ "src/main/resources/application.properties";		
		System.out.println("resourcePath1: " +resourcePath1);		
		String resourcePath2 = getClass().getClassLoader ().getResource ("application.properties").getFile();
		System.out.println("resourcePath2 " +resourcePath2);
		
		String vectorDbUrl 		  = new FileUtilsService().extractFields("vector.db.url", resourcePath);
		String vectorDbCollection = new FileUtilsService().extractFields("vector.db.collection", resourcePath);
		
		if (embeddingStore == null)
		{
			embeddingStore = ChromaEmbeddingStore.builder()
												.baseUrl(vectorDbUrl)
												.collectionName(vectorDbCollection)
												.build();
		}
		
		System.out.println("completed connect to Chroma. Got embeddingStore "+embeddingStore);
		return embeddingStore;
	}
	
	public EmbeddingStore<TextSegment> getEmbeddingStoreForTests() //vj1
	{
		if (embeddingStore == null)
		{
			try  //try connecting to VectorDB from local machine launch
			{
				//--  Chroma
				/*
				// local machine
				String vectorDbUrl        = "http://127.0.0.1:8000";
				String vectorDbCollection = "collection-gdpr-1";	
				*/
				// PG env with Docker  
				String vectorDbUrl        = "http://chroma.bawabaai-gpt.svc.cluster.local:8000";
				String vectorDbCollection = "collection-gdpr-1";	
				
				System.out.println("---- started connect to VectorDB for Tests " + " vectorDbUrl " + vectorDbUrl + " vectorDbCollection: " + vectorDbCollection);
				embeddingStore = ChromaEmbeddingStore.builder()
													.baseUrl (vectorDbUrl)
													.collectionName (vectorDbCollection)
													.build();
				//-- ElasticSearch
				/*
				String vectorDburl = "http://10.119.7.235:9200";		
				String userName = "openai-vector";
				String password = "openai-vector";		
				String vectorDbCollection = "notApplicableForElastic";
				
				System.out.println("started connect to Vectors for Tests vectorDbUrl: " + vectorDbUrl + " vectorDbCollection: "+vectorDbCollection);
				embeddingStore = ElasticsearchEmbebeddingStore.builder()		
														.serverUrl(vectorDbUrl)												
														.userisme(userName)												
														.password(password)												
														//.dimension (1) //0.27.1												
														//.indexName ("collection-fraud-detect-1") //v0.27.1												
														.build();
				*/
			}
			catch (Exception e)
			{
				System.out.println("Error Cannot connect to VectorDB: " +e);// scenario local machine does not have running instance of vectors.
			}
		}
			System.out.println("---- completed connect to Vectorte for tests. Got embeddingStore " +embeddingStore);
			return embeddingStore;
	}
}
