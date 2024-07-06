package com.genai.llm.privacy.mgt.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory. annotation. Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework. stereotype.Service;

import ai.djl.repository.zoo.ModelNotFoundException;
import dev. langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
//import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;

@Service
public class ContextLoadService
{
	@Autowired	
	private FileUtilsService fileUtilsSvc;
	
	//vj18
	@Value("${vector.chromadb.url.local}")
	String vectorDbUrlLocalMc;
	
	@Value("${vector.chromadb.url.pgocp.internal}")
	String vectorDbUrlPgOcpInternal;
	
	@Value("${vector.chromadb.url.pgocp.external}")
	String vectorDbUrlPgOcpExternal;
	
	@Value("${vector.chromadb.url.pgvm.internal}")
	String vectorDbUrlPgVMInternal;
	
	@Value("${vector.chromadb.url.pgvm.external}")
	String vectorDbUrlPgVMExternal;
	
	
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
	
	public EmbeddingStore<TextSegment> getEmbeddingStoreForTests(String vectorDbCollection)
	{
			try
			{	
				//vj18
				String vectorDbUrl = handleVectorDBConnection();
				
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
			
			System.out.println("---- completed connect to VectorDB for tests. Got embeddingStore " +embeddingStore);
			return embeddingStore;
	}

	//vj18
	private String handleVectorDBConnection()
	{	
		String vectorDbUrl = null;		
		String env = System.getProperty("deployment_env");
		
		if(env.contains("Bawaba-PG-OCP_internal"))
		{  
			vectorDbUrl = vectorDbUrlPgOcpInternal;
		}
		else if(env.contains("Bawaba-PG-OCP_external"))
		{  
			vectorDbUrl = vectorDbUrlPgOcpExternal;
		}
		else if(env.contains("Bawaba-PG-VM_internal"))
		{  
			vectorDbUrl = vectorDbUrlPgVMInternal;
		}
		else if(env.contains("Bawaba-PG-VM_external"))
		{  
			vectorDbUrl = vectorDbUrlPgVMExternal;
		}
		else
		{
			System.out.println("**** error - invalid Vector DB env connection");					
		}
		return vectorDbUrl;
	}
}
