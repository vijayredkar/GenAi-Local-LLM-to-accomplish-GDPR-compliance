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
	
	/* vj14
	 * LLM - RAG orchestration operations  - new with batches
	 */
	public String orchestrate(String userPrompt, String customSystemMessage, String category, String llmModel, boolean testMode) throws Exception
	{	
		System.out.println("\n---- started LLM - RAG orchestrations");
		
		// step -1  : prepare prompt
		String promptWithFullContext = constructFullPrompt(userPrompt, customSystemMessage, category);		
		System.out.println("---- constructed RAG promptWithFullContext \n"+promptWithFullContext);
		
		// step -2  : invoke the LLM inferencing engine		
		List<String> llmPromptInBatches = splitPromptInBatches(promptWithFullContext, customSystemMessage,  category);		 
		String response = invokeLLMServer(llmPromptInBatches, category, llmModel, testMode);		
		 
	    System.out.println("---- completed LLM - RAG orchestrations with response : \n"+ response);
		return response;
	}
	
	/*
	 * enhance the user prompt with the context information from the system/DB/File
	 */	
	private String constructFullPrompt(String userPrompt, String customSystemMessage, String category) throws IOException 
	{
		StringBuilder fullPromptBldr = new StringBuilder();
		
		fullPromptBldr.append(constructSystemMessage(customSystemMessage));
		fullPromptBldr.append(" ");
		fullPromptBldr.append(extractContext(userPrompt, category));
		fullPromptBldr.append(" ");
		fullPromptBldr.append("\"" + userPrompt + "\"");
		
		return fullPromptBldr.toString();
	}

	private String constructSystemMessage(String customSystemMessage) 
	{
		if(customSystemMessage != null || "".equals(customSystemMessage.trim()))
		{
			systemMessage = customSystemMessage;
		}
		
		return systemMessage;
	}
	
	private String invokeLLMServer( List<String> llmPromptInBatches, String category, String llmModel, boolean testMode) throws Exception //vj15
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
	    	 String response = largeLangModelSvc.generate(batch, testMode, llmModel, category);
	    	 
	    	 responseBldr.append(response);
	    	 count++;
	     }
	     
		return responseBldr.toString();
	}

	private String extractContext(String userPrompt, String category) throws IOException {
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
			contextFromPreparesDataSrc = vectorDataSvc.retrieveFromVectorDBByCategory(category, contextType, userPrompt); //Take from DB
		}
		return contextFromPreparesDataSrc;
	}
	
	/* vj14
	 * LLM - RAG orchestration operations
	 */
	public String orchestrateWithRetry(String userPrompt, String category, boolean testMode, String llmModel, List<String> buffer) throws Exception
	{	
		System.out.println("\n---- started LLM - RAG orchestrations");
		//String userPrompt = text;
		
		//--step -1  : enhance the user prompt with the context information from the DB 
		String contextFromVectorDb = vectorDataSvc.retrieveFromVectorDBByCategory(category, contextType, userPrompt);//vj14
		
		String promptWithFullContext = systemMessage + " " + contextFromVectorDb + "  "+  "\"" + userPrompt + "\"";
		System.out.println("---- constructed RAG promptWithFullContext \n"+promptWithFullContext);		

		//--step -2 : invoke the LLM inferencing engine with the fully constructed prompt
		String response = largeLangModelSvc.generateWithRetry(promptWithFullContext, testMode, llmModel, "explainflow", buffer);
		
		System.out.println("---- completed LLM - RAG orchestrations with response : \n"+ response);
		return response;
	}

	
	/*
	 * LLM - RAG orchestration operations
	 */
	public String orchestrateFlowTrain(String text, boolean testMode, String llmModel, String category) throws Exception //vj14
	{	
		System.out.println("\n---- started LLM - RAG orchestrations");
		String userPrompt = text;
		
		//--step -1  : enhance the user prompt with the context information from the DB 
		String contextFromVectorDb = vectorDataSvc.retrieveFromVectorDBByCategory(category, contextType, userPrompt);//vj14
		//String contextFromVectorDb = ""; 
		
		String promptWithFullContext = systemMessage + " " + contextFromVectorDb + "  "+  "\"" + userPrompt + "\"";
		System.out.println("---- constructed RAG promptWithFullContext \n"+promptWithFullContext);		

		//--step -2 : invoke the LLM inferencing engine with the fully constructed prompt
		String response = largeLangModelSvc.generate(promptWithFullContext, testMode, llmModel, category);//vj14
		//String response = contextFromVectorDb;
		//System.out.println("**** Ollama LLM server de-activated");
		
		System.out.println("---- completed LLM - RAG orchestrations with response : \n"+ response);
		return response;
	}

	//vj14
	/*
	 * LLM - RAG orchestration operations
	 */
	public String orchestrateApiInfo(String text, boolean testMode, String llmModel, String category) throws Exception
	{	
		System.out.println("\n---- started LLM - RAG orchestrations");
		String userPrompt = text;
		
		//--step -1  : enhance the user prompt with the context information from the DB 
		String contextFromVectorDb = vectorDataSvc.retrieveFromVectorDBByCategory(category, contextType, userPrompt);//vj14
		
		String promptWithFullContext = systemMessage + " " + contextFromVectorDb + "  "+  "\"" + userPrompt + "\"";
		System.out.println("---- constructed RAG promptWithFullContext \n"+promptWithFullContext);		

		//--step -2 : invoke the LLM inferencing engine with the fully constructed prompt
		String response = largeLangModelSvc.generate(promptWithFullContext, testMode, llmModel, category);//vj14
		
		System.out.println("---- completed LLM - RAG orchestrations with response : \n"+ response);
		return response;
	}	
	
	//vj6
	public String orchestrateVectorDBOnly(String text, boolean testMode) 
	{	
		return orchestrateVectorDBOnly("city", text, testMode);
	}
	
	/*
	 * invoke Vector DB
	 */
	public String orchestrateVectorDBOnly(String category, String text, boolean testMode) //vj6
	{	
		System.out.println("\n---- started orchestrateVectorDBOnly");
		String userPrompt = text;
		
		//--step -1  : enhance the user prompt with the context information from the DB 
		String contextFromVectorDb = vectorDataSvc.retrieve(category, contextType, userPrompt); //vj12
		System.out.println("**** Ollama LLM server de-activated");
		String response = contextFromVectorDb;
			
		System.out.println("---- completed orchestrateVectorDBOnly response : \n"+ response);
		return response;
	}	
	
	//vj5
	/*
	 * invoke LLM engine
	 */
	public String orchestrateLLMServerOnly(String text, boolean testMode, String llmModel, String category) throws Exception //vj14
	{	
		System.out.println("\n---- started orchestrateLLMServerOnly");
		String userPrompt = text;
		
		//--step -1  : enhance the user prompt with the context information from the DB 
		String contextFromVectorDb = ""; //vj1
		System.out.println("**** VectorDB de-activated");
		String promptWithFullContext = systemMessage + " " + contextFromVectorDb + "  "+  userPrompt;
		System.out.println("---- constructed RAG promptWithFullContext \n"+promptWithFullContext);		
		
		//vj3
		//--step -2 : invoke the LLM inferencing engine with the fully constructed prompt
		String response = largeLangModelSvc.generate(promptWithFullContext, testMode, llmModel, category);//vj14
		System.out.println("---- completed orchestrateLLMServerOnly response : \n"+ response);
		return response;
	}
	
	//vj5
		/*
		 * invoke LLM engine
		 */
		public String orchestrateLLMServerOnlyWithBigPayload_DISCARD(String text, boolean testMode, String context, String llmModel, String category) throws Exception //vj14
		{	
			System.out.println("\n---- started orchestrateLLMServerOnlyWithBigPayload");
			String userPrompt = text;
			
			//--step -1  : enhance the user prompt with the context information from the DB 
			String contextFromVectorDb = ""; //vj1
			System.out.println("**** VectorDB de-activated");
			
			String systemMessage1 = context;
			String contextFromVectorDb1 = "";			
			String promptWithFullContext = systemMessage1 + " " + contextFromVectorDb1 + " \n "+  userPrompt;
			System.out.println("---- constructed RAG promptWithFullContext \n"+promptWithFullContext);	
			
			//vj3
			//--step -2 : invoke the LLM inferencing engine with the fully constructed prompt
			String response = largeLangModelSvc.generate(promptWithFullContext, testMode, llmModel, category);//vj14
			
			System.out.println("---- completed orchestrateLLMServerOnlyWithBigPayload response : \n"+ response);
			return response;
		}
		
		/*
		 * Huge prompts to LLM cause timeouts/hallucination - eg .dynamically generated bytecodes
		 * split the prompt then send sequentially to Ollama +  collect responses sequentially.
		 */
		private List<String> splitPromptInBatches_DISCARD(String textFullVolume, String category) //vj14
		{
			int batchSize = 2000;
			if("explainflow".equals(category))  // explainflow bytecode fetched from VectorDB is dynamic and may be huge
			{
			 //String roleAndTask = "You are a helpful assistant. The following bytecode is generated by the Javaassist tool. Please explain the business execution logic in simple language. Do not mention assembly language words in your response. Mention the Java classes, methods and subsystems involved.\n";
			 //String roleAndTask = "You are a helpful assistant. Please summarize the business execution logic from the following Java bytecode in simple language in 2 points only. Do not mention assembly language words in your response. Mention the Java classes, methods that fall under the subpackages of com.enbd.microservices only.\n";
			 String roleAndTask = "You are a helpful assistant. Your task is to summarize the business execution logic in simple language. You will be provided the execution flow in Java bytecode. Specify the Java classes, methods that fall under the subpackages of com.enbd.microservices only. Provide your response in points format and restrict to 100 words only.\n";	
			 /*
			 String[] batches = textFullVolume.split("Execution logic of Java"); //split the userprompt in to smaller chunks
			 List<String> batchesRaw = Arrays.asList(batches);
			 //batchesRaw.remove(0); // Generic statement "You are a helpful assistant". Remove for now
			 
			 
			 List<String> batchesPerExecutionMethod = batchesRaw.stream()
					 									.filter(b -> !b.startsWith("You are a helpful assistant."))// Generic statement "You are a helpful assistant". Remove for now
					 									.map(b -> roleAndTask.concat(" ") //for every batch add the role and task at the start so that AI can understand the context.
					 														 .concat(b))
					 									.toList(); 
			 */
			 
			 /*batchesPerExecutionMethod may still be bigger than what Ollama can process in 1 shot.			 
			   Hence split these batches further in to acceptable chunk size 
			 */
			 List<String> batchesToProcess = new ArrayList<String>();
			 /*
			 for(String batch : batchesPerExecutionMethod)
			 {*/
				 
				 if(textFullVolume.length() < batchSize)
				 {
					 batchesToProcess.add(roleAndTask.concat("\n").concat(textFullVolume)); //prompt size is under tolerance limits	 
				 }
				 else
				 {
					 while(textFullVolume.length() > batchSize)  //prompt size is too big
					 {
						 String subBatch = textFullVolume.substring(0, batchSize);
						 
						 if(!subBatch.startsWith(roleAndTask))
						 {	subBatch.replace("You are a helpful assistant.", "");
							 subBatch = roleAndTask.concat("\n").concat(subBatch);  //add the role+task context to every batch
						 }
						 batchesToProcess.add(subBatch);
						 textFullVolume = textFullVolume.substring(batchSize);     //remove the 2000 size that is already subbatched. process the remaining
						 
						 if(textFullVolume.length() < batchSize)
						 {
							textFullVolume =  roleAndTask.concat("\n").concat(textFullVolume);  
							batchesToProcess.add(textFullVolume);
							break;  //stop while loop
						 }						 
						 
					 }//end while
				 } //end else	 			 
			/* } */ //end for
			 
			 
			 return batchesToProcess;
			 
			 
			 //hardcoded for tests		
			 /*
			 List<String> batchesToProcessTemp = new ArrayList<String>();
			 batchesToProcessTemp.clear();
			 batchesToProcessTemp.add("You are a helpful assistant. The following bytecode is generated by the Javaassist tool. Please explain the business execution logic in simple language. Mention the Java classes, methods and subsystems involved.\r\n" + 
								 		"  class DebitCardsController and method getDebitCardsLimits\r\n" + 
								 		"ldc #41 = \"debitCardNumber\"\r\n" + 
								 		"astore_1\r\n" + 
								 		"aload_0\r\n" + 
								 		"getfield #9 = Field com.enbd.microservices.cards.debit.controller.DebitCardsController.responseGenerationUtility(Lcom/enbd/interceptor/utils/ResponseGenerationUtility;)\r\n" + 
								 		"aload_0\r\n" + 
								 		"");
			 return batchesToProcessTemp;
			 */
			//hardcoded for tests		 
			}
			
			return Arrays.asList(textFullVolume);
		}
		
		/*
		 * Huge prompts to LLM cause timeouts/hallucination - eg .dynamically generated bytecodes
		 * split the prompt then send sequentially to Ollama +  collect responses sequentially.
		 */
		private List<String> splitPromptInBatches(String textFullVolume, String customSystemMessage, String category) //vj14
		{
			int batchSize = 2000;
			
			if("explainflow".equals(category))  // explainflow bytecode fetched from VectorDB is dynamic and may be huge
			{
				customSystemMessage = "You are a helpful assistant. Your task is to summarize the business execution logic in simple language. You will be provided the execution flow in Java bytecode. Specify the Java classes, methods that fall under the subpackages of com.enbd.microservices or bawaba.services only. Provide your response in points format and restrict to 100 words only.\n";
				//customSystemMessage = "You are a helpful assistant. Your task is to summarize the business execution logic in simple language. You will be provided the execution flow in Java bytecode. Specify the Java classes, methods that fall under the subpackages of com.enbd.microservices only. Provide your response in points format and restrict to 100 words only.\n";
				//customSystemMessage = "You are a helpful assistant. Your task is to summarize the business execution logic in simple language. You will be provided the execution flow in Java bytecode. Specify the Java classes, methods that fall under the subpackages of bawaba.services only. Provide your response in points format and restrict to 100 words only.\n";
				//customSystemMessage = "You are a helpful assistant. Your task is to provide a detailed explanation of the business execution logic in simple language. You will be provided the execution flow in Java bytecode. Specify the Java classes, methods, validations performed, database operations involved that fall under the subpackages of bawaba.services only. Provide your response in points format. This explanation will be utilized by the production support team to troubleshoot an issue with this flow .\n";
				//customSystemMessage = "You are a helpful assistant. Your task is to provide a detailed explanation of the business execution logic in simple language. This explanation will be utilized by the production support team to troubleshoot an issue with this flow. You will be provided the execution flow in Java bytecode. Specify the Java classes, methods, validations performed, transation inserted, updated or retrieved involved that fall under the subpackages of bawaba.services only. Provide your response in points format.\n";
			}
			  
			List<String> batchesToProcess = new ArrayList<String>();			 
			if(textFullVolume.length() < batchSize)
			 {
				 batchesToProcess.add(customSystemMessage.concat("\n").concat(textFullVolume)); //prompt size is under tolerance limits	 
			 }
			else
			 {
				 while(textFullVolume.length() > batchSize)  //prompt size is too big
				 {
					 String subBatch = textFullVolume.substring(0, batchSize);
					 
					 if(!subBatch.startsWith(customSystemMessage))
					 {	subBatch.replace("You are a helpful assistant.", "");
						 subBatch = customSystemMessage.concat("\n").concat(subBatch);  //add the role+task context to every batch
					 }
					 batchesToProcess.add(subBatch);
					 textFullVolume = textFullVolume.substring(batchSize);     //remove the 2000 size that is already subbatched. process the remaining
					 
					 if(textFullVolume.length() < batchSize)
					 {
						textFullVolume =  customSystemMessage.concat("\n").concat(textFullVolume);  
						batchesToProcess.add(textFullVolume);
						break;  //stop while loop
					 }						 
					 
				 }//end while
			 } //end else	 			 
			 
			 return batchesToProcess;
		}
}
