package com.genai.llm.privacy.mgt.service;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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

import com.genai.llm.privacy.mgt.utils.Constants;

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
	
	@Value("${vector.db.index.flowtrain:collection-flowtrain-1}")	
	String vectorDbIndexFlowtrain;
	
	@Value("${vector.db.index.apiinfo:collection-apiinfo-1}")//vj11
	String vectorDbIndexApiinfo;	
	
	@Value("${vector.db.load.flowtrain:Y}")	
	String vectorDbLoadFlowtrain;
	
	@Value("${vector.db.load.apiinfo:Y}")//vj11
	String vectorDbLoadApiinfo;
	
	
	@Value("${vector.db.load.city:Y}")//vj12
	String vectorDbLoadCity;
	
	@Value("${vector.db.load.employer:Y}")//vj12
	String vectorDbLoadEmployer;
	
	@Value("${vector.db.index.examineflow:collection-examineflow-1}")	//vj14
	String vectorDbIndexExamineflow;
	
	@Value("${vector.db.load.examineflow:Y}")//vj14
	String vectorDbLoadExamineflow;
	
	@Value("${vector.db.index.knowledgebase:collection-knowledgebase-1}")	//vj18
	String vectorDbIndexKnowledgebase;
	
	@Value("${vector.db.load.knowledgebase:Y}")//vj18
	String vectorDbLoadKnowledgebase;
		
	@Value("${default.llm.model:llama3:70b}")//vj18
	String defaultLlmModel;
	 
	@Value("${llm.system.message}")//vj18
	String systemMessage;
	
	
	
	
	
	@Autowired	
	private ModelService modelSvc;
	
	@Autowired //vjl	
	private ContextLoadService contextLoadSvc;
	
	@Autowired	
	private FileUtilsService fileUtilsSvc;
	
	@Autowired  //vj18
	Constants constants;
	
	@Autowired  //vj18
	private LargeLanguageModelService largeLangModelSvc;
	
	@Autowired  //vj18
	private RetrievalService retrievalSvc;
		
	//vj6
	public String retrieve(String contextType, String userPrompt)
	{
		return retrieve("city", contextType, userPrompt);//vj12
	}
	
	//vj14
	public String retrieveFromVectorDBByCategory(String category, String contextType, String userPrompt)
	{
		return retrieve(category, contextType, userPrompt);
	}
	
	/* 
	 * VectorDB operations to fetch records
	 */
	public String retrieve(String category, String contextType, String userPrompt)//vj12
	{
		System.out.println("\n--- started VectorDB operations");		
		//vj3
		System.out.println("---- embeddingModel : "+ modelSvc.getEmbeddingModel() +" \n "+"contextType : "+ contextType +" \n "+"userPrompt : "+ userPrompt +" \n "+"category : "+ category  +" \n "+"minScore : "+ minScoreRelevanceScore +" \n "+"maxResults : "+ maxResultsToRetrieve);
		
		//vj3
		//List<EmbeddingMatch<TextSegment>> result = fetchRecords(text);
		List<EmbeddingMatch<TextSegment>> result = fetchRecords(category, userPrompt, null);//vj18
		
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
	public List<EmbeddingMatch<TextSegment>> fetchRecords(String category, String query, String suffix)//vj18
	{	
		EmbeddingModel embdgModel= modelSvc.getEmbeddingModel();	
		//EmbeddingStore<TextSegment> embdgStore = contextLoadSvc.getEmbeddingStore(); //vjl 
		
		//vj6
		String vectorDbCollection = "collection-gdpr-1";
		if("employer".equals(category))
		{
		  vectorDbCollection = "collection-employer-1";		
		}
		else if("flowtrain".equals(category)) //vj10
		{
		  vectorDbCollection = vectorDbIndexFlowtrain; // "collection-flowtrain-1";
		}
		else if("apiinfo".equals(category)) //vj11
		{
		  vectorDbCollection = vectorDbIndexApiinfo; // "collection-apiinfo-1";
		}
		else if("explainflow".equals(category)) //vj14
		{
		  vectorDbCollection = vectorDbIndexExamineflow; // "collection-examineflow-1";
		}
		else if("knowledgebase".equals(category)) //vj18
		{
		  suffix = suffix.replaceAll("\\s", "_");	
		  vectorDbCollection = vectorDbIndexKnowledgebase+"-"+suffix; // collection-knowledgebase-2-Bawaba_Automation_13_:_AI_Scenario_&_Strategies
		  //vectorDbCollection = vectorDbIndexKnowledgebase+"-"+"xyz";
		}
		EmbeddingStore<TextSegment> embdgStore = contextLoadSvc.getEmbeddingStoreForTests(vectorDbCollection);	//vj12
		
		Embedding queryEmbedding = embdgModel.embed (query).content(); 
		//vj3
	        return  embdgStore.findRelevant(queryEmbedding, maxResultsToRetrieve, minScoreRelevanceScore);
	        //System.out.println("**** VectorDB invocation de-activated");
	        //return new ArrayList<EmbeddingMatch<TextSegment>>(); // "dummySemanticResult"
	}
	
	/*  vj14
	* loads context to VectorDB. Usually from preset file/on startup
	*/	
	public String loadData (String text, String category, boolean testMode, String suffix)
	{
		System.out.println("\n---- started loading data to Vector DB ");
		String result = "Failure : Vector DB load operation failed";
		String vectorDbName = null;
		
		//vj18
		/*
		if("examineFlow".equals(category))
		{
			vectorDbName = vectorDbIndexExamineflow;
		}
		*/
		vectorDbName = constants.getCategoryVectorDbMap().get(category);
		
		try
		{
			if("Y".equals(vectorDbLoadExamineflow))
			{
				insertVectorData (modelSvc.getEmbeddingModel(), text, testMode, vectorDbName);
				result = "Success: Vector DB load operation succeeded";
			}
			if("Y".equals(vectorDbLoadKnowledgebase))//vj18
			{
				List<String> batches = retrievalSvc.splitIntoBatches(text, 1000);
				suffix = suffix.replaceAll("\\s", "_");
				final String vectorDbNameKnwBase = vectorDbName + "-"+suffix; // collection-knowledgebase-2-Bawaba_Automation_13_:_AI_Scenario_&_Strategies
				//final String vectorDbNameKnwBase = vectorDbName + "-"+"xyz"; 
				batches.forEach(b -> insertVectorData (modelSvc.getEmbeddingModel(), b, testMode, vectorDbNameKnwBase));
				result = "Success: Vector DB load operation succeeded";
			}
		}
		catch(Exception e )
		{
			System.out.println("**** error occurred loadData VectorDB");
			e.printStackTrace();
			return result;
		}
		
		return result;
	}
	
	//vj18
	/*
	private String prepareVectorData(String text, boolean testMode, String category) 
	{
		String promptWithFullContext =  systemMessage
										+ " Your will be provided raw HTML source code. "
										+ " Your task is to extract only the text";
		String response = largeLangModelSvc.generate(promptWithFullContext, testMode, defaultLlmModel, category);
		return null;
	}
	*/

	/*
	* loads context to VectorDB. Usually from preset file/on startup
	*/	
	public void load (String fileNameWithFullPath, boolean testMode)
	{
		System.out.println("\n---- started loading context to Vector DB ");	
		
		//vj12
		if("Y".equals(vectorDbLoadCity))
		{
		List<String> lines = new ArrayList<String>();		
		lines = storeCityData(fileNameWithFullPath, testMode, lines);		
		insertVectorData (modelSvc.getEmbeddingModel(), lines, testMode, "collection-gdpr-1");
		}
		
		//vj12
		if("Y".equals(vectorDbLoadEmployer))
		{
		List<String> employerLines = new ArrayList<String>();
		employerLines = storeEmployerData(fileNameWithFullPath, testMode, employerLines);		
		insertVectorData (modelSvc.getEmbeddingModel(), employerLines, testMode, "collection-employer-1");
		}
		
		//vj10
		if("Y".equals(vectorDbLoadFlowtrain))
		{
			List<String> flowTrainLines = new ArrayList<String>();
			flowTrainLines = storeFlowTrainData(fileNameWithFullPath, testMode, flowTrainLines);		
			insertVectorData (modelSvc.getEmbeddingModel(), flowTrainLines, testMode, vectorDbIndexFlowtrain); //"collection-flowtrain-1"
		}
		
		//vj11
		if("Y".equals(vectorDbLoadApiinfo))
		{
			List<String> apiInfoLines = new ArrayList<String>();
			apiInfoLines = storeApiInfoData(fileNameWithFullPath, testMode, apiInfoLines);		
			insertVectorData (modelSvc.getEmbeddingModel(), apiInfoLines, testMode, vectorDbIndexApiinfo);
		}
				
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

	//vj10
	private List<String> storeFlowTrainData(String fileNameWithFullPath, boolean testMode, List<String> lines) {
		if(testMode)
		{
		String testContext = "NRE international fund transfer technical flow is invoked with the REST endpoint POST /payment/v1/transfers/external-transfers\r\n" + 
				"The request flow is then processed by the Java classes and methods in the following sequence:\r\n" + 
				"class ExternalTransferController and method confirmTransfer.\r\n" + 
				"class ConfirmExternalTransferHandler and method handle.\r\n" + 
				"class ValidateExternalTransferCommand and method setPaymentConfirm.\r\n" + 
				"class EnableCommandToEvent publishes the transaction data to Kafka topic  external-transfer. \r\n" + 
				"A consumer listening to this topic consumes this message and further processes this fund transfer.\r\n" + 
				"class UpdatePaymentRequestLogHandler and method handle saves this transaction with the status Processing to the Mongo DB for auditing.\r\n" + 
				"An HTTP response with a status code of 202 Accepted is sent to the consumer and the flow completes.";	
		lines.add(testContext);	
		
		testContext = "IBAN local fund transfer technical flow is invoked with the REST endpoint POST /payment/v1/transfers/external-transfers\r\n" + 
				"The request flow is then processed by the Java classes and methods in the following sequence:\r\n" + 
				"class ExternalTransferController and method confirmTransfer.\r\n" + 
				"class ConfirmExternalTransferHandler and method handle.\r\n" + 
				"class InstitutionsViewQueryHandler and method handle checks if the sender and receiver institution information is valid.\r\n" + 
				"class InstitutionsPHQueryHandler and method handle checks if the institution information is valid in PaymentHub record keeping system.\r\n" + 
				"class ConfirmExternalTransferWithoutOrderIdHandler and method handle.\r\n" + 
				"class ValidateExternalTransferCommand and method setPaymentConfirm.\r\n" + 
				"class EnableCommandToEvent publishes the transaction data to Kafka topic  external-transfer. \r\n" + 
				"A consumer listening to this topic consumes this message and further processes this fund transfer.\r\n" + 
				"class UpdatePaymentRequestLogHandler and method handle saves this transaction with the status Processing to the Mongo DB for auditing.\r\n" + 
				"An HTTP response with a status code of 202 Accepted is sent to the consumer and the flow completes.";	
		lines.add(testContext);
		}
		else
		{
			lines = fileUtilsSvc.readFile (fileNameWithFullPath);	
		}
		return lines;
	}
	
	//vj11
	private List<String> storeApiInfoData(String fileNameWithFullPath, boolean testMode, List<String> lines) {
		if(testMode)
		{
			String testContext = "API to insert customer data with version 1 is POST https://bawabauat.clouduat.emiratesnbd.com/banking/customers/v1/customer. Request body JSON is {\"id\": \"CUE-12345\", \"cif\": \"7786543\", \"salutation\": \"MR\", \"firstName\": \"John\", \"firstNameInArabic\": \"Alam\", \"middleName\": \"Smith\", \"middleNameInArabic\": \"Jahan\"}. API Response on success is HttpStatusCode 201 Created.";
			lines.add(testContext);	
			
			testContext = "API to insert customer data with version 2 is POST https://bawabauat.clouduat.emiratesnbd.com/banking/customers/v2/customer.  Request body JSON is {\"id\": \"CUE-12345\", \"cif\": \"7786543\",\"salutation\": \"MR\", \"firstName\": \"John\", \"firstNameInArabic\": \"Alam\", \"middleName\": \"Smith\", \"middleNameInArabic\": \"Jahan\", \"email\": \"jahan@gmail.com\", \"phone\": \"97177788899\", \"address\": \"21 Baker St. Al Raffa, Dubai\"}. API Response on success is HttpStatusCode 201 Created.";
			lines.add(testContext);
			
			testContext = "API to fetch customer data with version 1 is GET https://bawabauat.clouduat.emiratesnbd.com/banking/customers/v1/customer/CUE-LNoQAK80IN.  Response body on success is {\"id\": \"CUE-12345\", \"cif\": \"7786543\",\"salutation\": \"MR\", \"firstName\": \"John\", \"firstNameInArabic\": \"Alam\", \"middleName\": \"Smith\", \"middleNameInArabic\": \"Jahan\"}.";
			lines.add(testContext);
			
			testContext = "API to fetch customer data with version 2 is GET https://bawabauat.clouduat.emiratesnbd.com/banking/customers/v2/customer/CUE-LNoQAK80IN?type=corporate.  Response body on success is {\"id\": \"CUE-12345\", \"cif\": \"7786543\",\"salutation\": \"MR\", \"firstName\": \"John\", \"firstNameInArabic\": \"Alam\", \"middleName\": \"Smith\", \"middleNameInArabic\": \"Jahan\", \"email\": \"jahan@gmail.com\", \"phone\": \"97177788899\", \"address\": \"21 Baker St. Al Raffa, Dubai\", \"emiratesId\": \"33322244455\", \"citizenship\": \"EGP\", \"occupation\": \"engineer\"}.";
			lines.add(testContext);
			
			testContext = "API to update customer data with version 1 is PUT https://bawabauat.clouduat.emiratesnbd.com/banking/customers/v1/customer/CUE-LNoQAK80IN. API Request body JSON is {\"id\": \"CUE-12345\", \"cif\": \"7786543\", \"salutation\": \"MR\", \"firstName\": \"John\", \"firstNameInArabic\": \"Alam\", \"middleName\": \"Smith\", \"middleNameInArabic\": \"Jahan\"}. API Response on success is HttpStatusCode 202 Accepted.";
			lines.add(testContext);
			
			testContext = "API to insert loan data with version 1 is POST https://bawabauat.clouduat.emiratesnbd.com/banking/loans/v1/loan/LNA-LNoQAK80IN. API Request body JSON is {\"id\": \"LNA-12345\",\"amount\": \"20000\",\"dueDate\": \"20-12-2025\", \"cif\": \"7786543\", \"salutation\": \"MR\", \"firstName\": \"John\", \"firstNameInArabic\": \"Alam\", \"middleName\": \"Smith\", \"middleNameInArabic\": \"Jahan\"}. API Response on success is HttpStatusCode 201 Created.";
			lines.add(testContext);
			
			testContext = "API to update loan data with version 1 is PUT https://bawabauat.clouduat.emiratesnbd.com/banking/loans/v1/loan/LNA-LNoQAK80IN. API Request body JSON is {\"id\": \"LNA-12345\",\"amount\": \"20000\",\"dueDate\": \"20-12-2025\", \"cif\": \"7786543\", \"salutation\": \"MR\", \"firstName\": \"John\", \"firstNameInArabic\": \"Alam\", \"middleName\": \"Smith\", \"middleNameInArabic\": \"Jahan\"}. API Response on success is HttpStatusCode 202 Accepted.";
			lines.add(testContext);
			
			testContext = "API to fetch loan data with version 1 is GET https://bawabauat.clouduat.emiratesnbd.com/banking/loans/v1/loan/LNA-LNoQAK80IN.  Response body on success is {\"id\": \"LNA-12345\",\"amount\": \"20000\",\"dueDate\": \"20-12-2025\", \"cif\": \"7786543\",\"salutation\": \"MR\", \"firstName\": \"John\", \"firstNameInArabic\": \"Alam\", \"middleName\": \"Smith\", \"middleNameInArabic\": \"Jahan\", \"email\": \"jahan@gmail.com\", \"phone\": \"97177788899\", \"address\": \"21 Baker St. Al Raffa, Dubai\", \"emiratesId\": \"33322244455\", \"citizenship\": \"EGP\", \"occupation\": \"engineer\"}.";
			lines.add(testContext);
			
			testContext = "API to fetch loan data with version 2 is GET https://bawabauat.clouduat.emiratesnbd.com/banking/loans/v2/loan/LNA-LNoQAK80IN.  Response body on success is {\"id\": \"LNA-12345\",\"amount\": \"20000\",\"dueDate\": \"20-12-2025\", \"cif\": \"7786543\",\"salutation\": \"MR\", \"firstName\": \"John\", \"firstNameInArabic\": \"Alam\", \"middleName\": \"Smith\", \"middleNameInArabic\": \"Jahan\", \"email\": \"jahan@gmail.com\", \"phone\": \"97177788899\", \"address\": \"21 Baker St. Al Raffa, Dubai\", \"emiratesId\": \"33322244455\", \"citizenship\": \"EGP\", \"occupation\": \"engineer\"}.";
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
				EmbeddingStore<TextSegment> embdStore = contextLoadSvc.getEmbeddingStoreForTests(vectorDbCollection);//vj12
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
		
		System.out.println("---- insertVectorData context executed");
	}	
	
	/*	vj14
	* inserts to VectorDB	
	*/	
	private void insertVectorData (EmbeddingModel embeddingModel, String text, boolean testMode, String vectorDbCollection) 
	{
			TextSegment segment1 = TextSegment.from(text, new Metadata()); 
			Embedding embedding1 = embeddingModel.embed (segment1).content();
			
			if (testMode)			
			{
				System.out.println("---- VectorDB testMode "+testMode);			
				EmbeddingStore<TextSegment> embdStore = contextLoadSvc.getEmbeddingStoreForTests(vectorDbCollection);//vj12
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
		System.out.println("---- insertVectorData data executed");
		
		
		/*
		//--tests only vj18
		EmbeddingModel embdgModel= modelSvc.getEmbeddingModel();	
		Embedding queryEmbedding = embdgModel.embed (text).content(); 
		//Embedding queryEmbedding = embdgModel.embed ("Bawaba Automation 13 : AI Scenario & Strategies").content(); 
		List<EmbeddingMatch<TextSegment>> txtSg = contextLoadSvc.getEmbeddingStore().findRelevant(queryEmbedding, 1);
		System.out.println("---- "+txtSg);
		*/
		//--
	}

	//vj14
	public String retrieveFromFileByCategory(String category, String contextType, String fileName) throws IOException
	{
		Path path = Paths.get("C:\\git-workspace\\BawabaFlowPoints\\"+fileName);
		return Files.readString(path);
	}
}
