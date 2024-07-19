package com.genai.llm.privacy.mgt.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import dev.langchain4j.data.embedding.Embedding;
import jakarta.annotation.PostConstruct;

@Service
public class RetrievalService 
{
	@Value("${vector.context.type:assistant}")
	//@Value("${vector.context.type:assistant to help detect}")
	//@Value("${vector.context.type:detect}")
	private String contextType;
	
	@Value("${llm.system.message:You are a helpful}")
	private String systemMessage;
	
	@Autowired
	private VectorDataStoreService vectorDataSvc;
	
	@Autowired
	private LargeLanguageModelService largeLangModelSvc;
	
	@Autowired
	private IntegrationService intgrSvc;
	
	/* 
	 * LLM - RAG orchestration operations  - new with batches
	 */
	public String orchestrate(String userPrompt, String customSystemMessage, String category, String llmModel, 
							  boolean testMode, String temperature, String embeddingsMinScore, String retrievalLimitMax) 
							  throws Exception
	{	
		System.out.println("\n---- started LLM - RAG orchestrations");
		
		// step -1  : prepare prompt
		String promptWithFullContext = constructFullPrompt(userPrompt, customSystemMessage, category, embeddingsMinScore, retrievalLimitMax);		
		System.out.println("---- constructed RAG promptWithFullContext \n"+promptWithFullContext);
		
		// step -2  : invoke the LLM inferencing engine		
		//List<String> llmPromptInBatches = splitPromptInBatches(promptWithFullContext, customSystemMessage,  category);
		//List<String> llmPromptInBatches = splitIntoBatches(promptWithFullContext, 1000);
		List<String> llmPromptInBatches = splitIntoBatches(promptWithFullContext, category);
		String response = invokeLLMServer(llmPromptInBatches, category, llmModel, testMode, temperature);		
		 
	    System.out.println("---- completed LLM - RAG orchestrations with response : \n"+ response);
		return response;
	}
	
	/* vj24
	 * LLM - Image analyze 
	 */
	public String orchestrateImageAnalyze(String userPrompt, String customSystemMessage, String category, String llmModel, 
										  boolean testMode, String temperature, String embeddingsMinScore, String retrievalLimitMax) 
										  throws Exception
	{	
		System.out.println("\n---- started LLM - orchestrateImageAnalyze");
		String response = invokeLLMServerImageAnalyze(userPrompt, category, llmModel, testMode, temperature);		
		 
	    System.out.println("---- completed LLM - orchestrateImageAnalyze : \n"+ response);
		return response;
	}
	
	/*
	 * enhance the user prompt with the context information from the system/DB/File
	 */	
	private String constructFullPrompt(String userPrompt, String customSystemMessage, String category, String embeddingsMinScore, String retrievalLimitMax) throws IOException 
	{
		StringBuilder fullPromptBldr = new StringBuilder();
		
		fullPromptBldr.append(constructSystemMessage(customSystemMessage, category));
		fullPromptBldr.append(" ");
		fullPromptBldr.append(extractContext(userPrompt, customSystemMessage,  category, embeddingsMinScore, retrievalLimitMax));
		fullPromptBldr.append(" ");
		fullPromptBldr.append("\"" + userPrompt + "\"");
		
		return fullPromptBldr.toString();
	}

	private String constructSystemMessage(String customSystemMessage, String category)
	{	
		if("knowledgebase".equals(category))
		{	
			String[] portions = customSystemMessage.split("Z@@"); //constants.customSystemMessageKnwBase+"Z@@"+customUserQuestion+"Z@@"+documentTitle
			customSystemMessage = portions[0];
		}
		
		if(customSystemMessage != null || "".equals(customSystemMessage.trim()))
		{
			systemMessage = customSystemMessage;
		}
		
		return systemMessage;
	}
	
	private String invokeLLMServer( List<String> llmPromptInBatches, String category, String llmModel, boolean testMode, String temperature) throws Exception
	{
		 System.out.println("\n\n---- Total batch count: "+llmPromptInBatches.size());
		 StringBuilder responseBldr = new StringBuilder();
	     int count = 1;
	     Iterator<String> batchItr  = llmPromptInBatches.iterator();
	     while(batchItr.hasNext())
	     {	 
	    	 String batch = batchItr.next();
	    	 System.out.println("\n\n ---- LLM prompt length "+batch.length());
	    	 System.out.println("\n\n---- processing batch#: "+count);
	    	 System.out.println("\n\n---- processing batch with prompt: \n\n"+batch);
	    	 System.out.println("\n\n");
	    	 
	    	 
	    	 
	    	 String response = largeLangModelSvc.generate(batch, testMode, llmModel, category, temperature);
	    	 
	    	 responseBldr.append(response);
	    	 count++;
	     }
	     
		return responseBldr.toString();
	}
	
	//vj24
	private String invokeLLMServerImageAnalyze( String userPrompt, String category, String llmModel, boolean testMode, String temperature) throws Exception
	{  	 
	  	String response = largeLangModelSvc.generateImageAnalyze(userPrompt, testMode, llmModel, category, temperature);
		return response;
	}

	private String extractContext(String userPrompt, String customSystemMessage, String category, String embeddingsMinScore, String retrievalLimitMax) throws IOException {
		String contextFromPreparesDataSrc;
		
		if("generic".equals(category)) 
		{   
		  contextFromPreparesDataSrc = userPrompt; //no additional context. Pass incoming prompt as is
		}
		else if("explainflow".equals(category)) //Take from file. embedding scores for bytecodes are very close/indistinguishable. hence VectorDB semantic fetch does not work accurately
		{   //vj hardcoded
			contextFromPreparesDataSrc = vectorDataSvc.retrieveFromFileByCategory(category, contextType, userPrompt+"-examineflow.txt");  //post-debit-cards-details-v1-examineflow.txt
		}
		else
		{
			if("knowledgebase".equals(category))
			{	
				String[] portions = customSystemMessage.split("Z@@"); //constants.customSystemMessageKnwBase+"Z@@"+customUserQuestion+"Z@@"+documentTitle
				userPrompt = portions[2];//documentTitle
			}
			contextFromPreparesDataSrc = vectorDataSvc.retrieveFromVectorDBByCategory(category, contextType, userPrompt, embeddingsMinScore, retrievalLimitMax); //Take from DB
		}
		return contextFromPreparesDataSrc;
	}
	
	/* 
	 * LLM - RAG orchestration operations
	 */
	public String orchestrateWithRetry(String userPrompt, String category, boolean testMode, String llmModel, List<String> buffer, String embeddingsMinScore, String retrievalLimitMax, String temperature) throws Exception
	{	
		System.out.println("\n---- started LLM - RAG orchestrations");
		//String userPrompt = text;
		
		//--step -1  : enhance the user prompt with the context information from the DB 
		String contextFromVectorDb = vectorDataSvc.retrieveFromVectorDBByCategory(category, contextType, userPrompt, embeddingsMinScore, retrievalLimitMax);
		
		String promptWithFullContext = systemMessage + " " + contextFromVectorDb + "  "+  "\"" + userPrompt + "\"";
		System.out.println("---- constructed RAG promptWithFullContext \n"+promptWithFullContext);		

		//--step -2 : invoke the LLM inferencing engine with the fully constructed prompt
		String response = largeLangModelSvc.generateWithRetry(promptWithFullContext, testMode, llmModel, "explainflow", buffer, temperature);
		
		System.out.println("---- completed LLM - RAG orchestrations with response : \n"+ response);
		return response;
	}

	
	/*
	 * LLM - RAG orchestration operations
	 */
	public String orchestrateFlowTrain(String text, boolean testMode, String llmModel, String category, String embeddingsMinScore, String retrievalLimitMax, String temperature) throws Exception
	{	
		System.out.println("\n---- started LLM - RAG orchestrations");
		String userPrompt = text;
		
		//--step -1  : enhance the user prompt with the context information from the DB 
		String contextFromVectorDb = vectorDataSvc.retrieveFromVectorDBByCategory(category, contextType, userPrompt, embeddingsMinScore, retrievalLimitMax);
		//String contextFromVectorDb = ""; 
		
		String promptWithFullContext = systemMessage + " " + contextFromVectorDb + "  "+  "\"" + userPrompt + "\"";
		System.out.println("---- constructed RAG promptWithFullContext \n"+promptWithFullContext);		

		//--step -2 : invoke the LLM inferencing engine with the fully constructed prompt
		String response = largeLangModelSvc.generate(promptWithFullContext, testMode, llmModel, category, temperature);
		//String response = contextFromVectorDb;
		//System.out.println("**** Ollama LLM server de-activated");
		
		System.out.println("---- completed LLM - RAG orchestrations with response : \n"+ response);
		return response;
	}

	
	/*
	 * LLM - RAG orchestration operations
	 */
	public String orchestrateApiInfo(String text, boolean testMode, String llmModel, String category, String embeddingsMinScore, String retrievalLimitMax, String temperature) throws Exception 
	{	
		System.out.println("\n---- started LLM - RAG orchestrations");
		String userPrompt = text;
		
		//--step -1  : enhance the user prompt with the context information from the DB 
		String contextFromVectorDb = vectorDataSvc.retrieveFromVectorDBByCategory(category, contextType, userPrompt, embeddingsMinScore, retrievalLimitMax);
		
		String promptWithFullContext = systemMessage + " " + contextFromVectorDb + "  "+  "\"" + userPrompt + "\"";
		System.out.println("---- constructed RAG promptWithFullContext \n"+promptWithFullContext);		

		//--step -2 : invoke the LLM inferencing engine with the fully constructed prompt
		String response = largeLangModelSvc.generate(promptWithFullContext, testMode, llmModel, category, temperature);
		
		System.out.println("---- completed LLM - RAG orchestrations with response : \n"+ response);
		return response;
	}	
	
	
	public String orchestrateVectorDBOnly(String text, boolean testMode, String embeddingsMinScore, String retrievalLimitMax)
	{	
		return orchestrateVectorDBOnly("city", text, testMode, embeddingsMinScore, retrievalLimitMax);
	}
	
	/*
	 * invoke Vector DB
	 */
	public String orchestrateVectorDBOnly(String category, String text, boolean testMode, String embeddingsMinScore, String retrievalLimitMax) 
	{	
		System.out.println("\n---- started orchestrateVectorDBOnly");
		String userPrompt = text;
		
		//--step -1  : enhance the user prompt with the context information from the DB 
		String contextFromVectorDb = vectorDataSvc.retrieve(category, contextType, userPrompt, embeddingsMinScore, retrievalLimitMax); 
		System.out.println("**** Ollama LLM server de-activated");
		String response = contextFromVectorDb;
			
		System.out.println("---- completed orchestrateVectorDBOnly response : \n"+ response);
		return response;
	}	
	
	/*  vj24A
	 * embeddings operations
	 */
	public Embedding manageEmbeddings(String category, String text, boolean testMode, String embeddingsMinScore, String retrievalLimitMax) 
	{	
		return vectorDataSvc.generateEmbeddings(category, text, null, embeddingsMinScore, retrievalLimitMax);
	}
	
	 /*
	 * invoke LLM engine
	 */
	public String orchestrateLLMServerOnly(String text, boolean testMode, String llmModel, String category, String temperature) throws Exception 
	{	
		System.out.println("\n---- started orchestrateLLMServerOnly");
		String userPrompt = text;
		
		//--step -1  : enhance the user prompt with the context information from the DB 
		String contextFromVectorDb = ""; 
		System.out.println("**** VectorDB de-activated");
		String promptWithFullContext = systemMessage + " " + contextFromVectorDb + "  "+  userPrompt;
		System.out.println("---- constructed RAG promptWithFullContext \n"+promptWithFullContext);		
		
		
		//--step -2 : invoke the LLM inferencing engine with the fully constructed prompt
		
		System.out.println("---- Final generation: promptWithFullContext \n\n"+promptWithFullContext);
		System.out.println("\n\n");
		
		String response = largeLangModelSvc.generate(promptWithFullContext, testMode, llmModel, category, temperature);
		System.out.println("---- completed orchestrateLLMServerOnly response : \n"+ response);
		return response;
	}
	
	
		/*
		 * invoke LLM engine
		 */
		public String orchestrateLLMServerOnlyWithBigPayload_DISCARD(String text, boolean testMode, String context, String llmModel, String category, String temperature) throws Exception 
		{	
			System.out.println("\n---- started orchestrateLLMServerOnlyWithBigPayload");
			String userPrompt = text;
			
			//--step -1  : enhance the user prompt with the context information from the DB 
			String contextFromVectorDb = ""; 
			System.out.println("**** VectorDB de-activated");
			
			String systemMessage1 = context;
			String contextFromVectorDb1 = "";			
			String promptWithFullContext = systemMessage1 + " " + contextFromVectorDb1 + " \n "+  userPrompt;
			System.out.println("---- constructed RAG promptWithFullContext \n"+promptWithFullContext);	
			
			
			//--step -2 : invoke the LLM inferencing engine with the fully constructed prompt
			String response = largeLangModelSvc.generate(promptWithFullContext, testMode, llmModel, category, temperature);
			
			System.out.println("---- completed orchestrateLLMServerOnlyWithBigPayload response : \n"+ response);
			return response;
		}
				
		
		/*
		 * Huge prompts to LLM cause timeouts/hallucination - eg .dynamically generated bytecodes
		 * split the prompt then send sequentially to Ollama +  collect responses sequentially.
		 */
		/*
		private List<String> splitPromptInBatches(String textFullVolume, String customSystemMessage, String category)
		{
			int batchSize = 2000;
			String userQuestionForKnwBase = null;
			//vj18
			String[] portions = null;
			if("knowledgebase".equals(category))
			{	
				portions = customSystemMessage.split("Z@@"); //constants.customSystemMessageKnwBase+"Z@@"+customUserQuestion+"Z@@"+documentTitle
				userQuestionForKnwBase = " Here is the user's question:\n"+portions[1];
			}			
			
			if("explainflow".equals(category))  // explainflow bytecode fetched from VectorDB is dynamic and may be huge
			{	 
				customSystemMessage = "You are a helpful assistant. Your task is to summarize the business execution logic in simple language. You will be provided the execution flow in Java bytecode. Specify the Java classes, methods that fall under the subpackages of com.enbd.microservices or bawaba.services only. Provide your response in points format and restrict to 100 words only.\n";
			}
			//vj18
			else if("knowledgebase".equals(category))  // documentation text from confluence provided by the user fetched may be huge
			{					
				batchSize = 500;
				customSystemMessage = portions[0] + "\n";
			}
			  
			List<String> batchesToProcess = new ArrayList<String>();			 
			if(textFullVolume.length() < batchSize)
			 {
				
				//vj18
				if("knowledgebase".equals(category))  // temporary adjustment - hardcode user's question here
				{	 
					customSystemMessage.concat("\n").concat(textFullVolume).concat("\n").concat(userQuestionForKnwBase);
				}
				else
				{
				 batchesToProcess.add(customSystemMessage.concat("\n").concat(textFullVolume)); //prompt size is under tolerance limits	 
				}
			 }
			else
			 {
				 while(textFullVolume.length() > batchSize)  //prompt size is too big
				 {
					 String subBatch = textFullVolume.substring(0, batchSize);
					 
					 //if(!subBatch.startsWith(customSystemMessage))
					 if(!subBatch.contains(customSystemMessage))//vj18
					 {	//subBatch.replace("You are a helpful assistant.", "");
						 subBatch = subBatch.replace(customSystemMessage, "");
						 subBatch = customSystemMessage.concat("\n").concat(subBatch).concat("\n").concat(userQuestionForKnwBase);  //add the role+task context to every batch
					 }
					 batchesToProcess.add(subBatch);
					 textFullVolume = textFullVolume.substring(batchSize);     //remove the 2000 size that is already subbatched. process the remaining
					 
					 if(textFullVolume.length() < batchSize)
					 { 
						//vj18
						if("knowledgebase".equals(category))  // temporary adjustment - hardcode user's question here
						{	 
							textFullVolume =  customSystemMessage.concat("\n").concat(textFullVolume).concat("\n").concat(userQuestionForKnwBase);
						}
						else
						{
							textFullVolume =  customSystemMessage.concat("\n").concat(textFullVolume);	
						}
						
						batchesToProcess.add(textFullVolume);
						break;  //stop while loop
					 }						 
					 
				 }//end while
			 } //end else	 			 
			 
			 return batchesToProcess;
		}
		*/
		
		/* vj18
		 * Huge prompts to LLM cause timeouts/hallucination - eg .dynamically generated bytecodes
		 * split the prompt in batches of smaller text
		 */
		//public List<String> splitIntoBatches(String textFullVolume, int batchMaxSize)
		public List<String> splitIntoBatches(String textFullVolume, String category)
		{							
			List<String> batchesToProcess = new ArrayList<String>();			
			int batchMaxSize = manageBatchSize(category); //vj24
			
			if(textFullVolume.length() < batchMaxSize) // i/p size is under tolerance limits	
			 {
				 batchesToProcess.add(textFullVolume); 
			 }
			else
			 {
				 while(textFullVolume.length() > batchMaxSize)  //prompt size is too big
				 {
					 String subBatch = textFullVolume.substring(0, batchMaxSize);
					 batchesToProcess.add(subBatch);
					 textFullVolume = textFullVolume.substring(batchMaxSize);     //remove the 2000 size that is already subbatched. process the remaining
					 
					 if(textFullVolume.length() < batchMaxSize)
					 { 
						batchesToProcess.add(textFullVolume);
						break;  //stop while loop
					 }					 
				 }//end while
			 } //end else	 			 
			 
			 return batchesToProcess;
		}

		//vj24
		private int manageBatchSize(String category) 
		{
			int batchMaxSize = 2000;
			if("knowledgebase".equals(category))
			{
				batchMaxSize = 1000;
			}
			
			return batchMaxSize;
		}

		public String fetchLogsByUrc(String urc) //vj24C
		{	
			LogExtractRequest logExtractRequest = new LogExtractRequest();
			logExtractRequest.setUrc(urc);
			
			return fetchLogsByUrc(logExtractRequest);
		}
		
		public String fetchLogsByUrc(LogExtractRequest logExtractReq) //vj24C
		{			
			String authToken = intgrSvc.getTokenizationAuth();
			String logs = intgrSvc.getKibanaLogsByUrc(logExtractReq, authToken);			
			return logs;
		}

		public boolean validateLogExtractReq(LogExtractRequest logExtractReq) //vj24C
		{
			boolean result = true;
			if(logExtractReq.getUrc() == null || "".equals(logExtractReq.getUrc()))
			{
			 System.out.println("**** URC cannot be empty "+ logExtractReq.toString());
			 result = false;
			}
			
			 return result;
		}
		
		public boolean validateLogExtractReq(String input) //vj24C
		{
			boolean result = true;
			if(input == null || "".equals(input))
			{
			 System.out.println("**** URC cannot be empty ");
			 result = false;
			}
			
			 return result;
		}
}
