package com.genai.llm.privacy.mgt.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

//import org.apache.poi.xddf.usermodel.text.TextContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.awaitility.Durations;

import com.genai.llm.privacy.mgt.utils.Constants;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ImageContent; 
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel; 
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.output.Response;

@Service
public class LargeLanguageModelService 
{
	@Autowired
	private FileUtilsService fileUtilsSvc;	
	
	 
	 @Autowired
	 private Constants constants;
	
	StreamingChatLanguageModel modelStreaming = null;
	/*
	 * Local LLM server : Ollama operations	
	 */
	public String generate_in_batches(String text, boolean testMode, String llmModel, String category, String temperature) throws Exception //vj19
	{
		
		Map<String, String> severConfigMap  = gatherConfig(testMode, llmModel); 	
		
		String modelName       = severConfigMap.get("modelName");	//default model
		
		
		//if(llmModel !=null && Constants.getValidModelsMap().containsKey(llmModel.trim()))
		if(constants.isModelValid(modelName, System.getProperty("deployment_env")))
		{
			modelName = llmModel; //user specified model
		}
		else
		{
			System.out.println("**** Invalid LLM Model requested by user. Exiting");
			throw new Exception("**** Invalid LLM Model requested by user. Exiting");
		}
		System.out.println("---- LLM Model set is "+modelName);
		
		Integer llmServerPort  = Integer.parseInt(severConfigMap.get("llmServerPort"));
		Double llmResponseTemp = Double.parseDouble(severConfigMap.get("llmResponseTemp"));
				
	    
	    /*
             -- on the fly launch. Does not work in PG env
	    GenericContainer<?> ollama = startLLMServer(modelName, llmServerPort); 	    
	    ChatLanguageModel model = buildLLMResponseModel(ollama, modelName, llmResponseTemp);
	    stopLLMServer(ollama); // stop LLM server on the fly.
	    */	
		
	    
	    //--standalone server invocation
	    //String llmServerUrl = "http://127.0.0.1:11434"; //vijay hardcoded local machine
	    //String llmServerUrl = "http://ollama.bawabaai-gpt.svc.cluster.local:11434"; //vijay hardcoded
        //String llmServerUrl = "http://ollama-llama3.bawabaai-gpt.svc.cluster.local:11434"; //vijay hardcoded access from with PG POD only
		//String llmServerUrl = "https://ollama-llama3-bawabaai-gpt.pgocp.uat.emiratesnbd.com"; //vijay hardcoded access from local machine
		
			    
		String llmServerUrl = constants.getResourceByModelName(modelName, System.getProperty("deployment_env"));
		String llmResponse = "";
		
		
		
	     System.out.println("\n---- Started local LLM invocation for user input : "+ text);
	     //System.out.println("---- Generating with modelName : "+ modelName + "\n llmServerUrl : "+ llmServerUrl);
		
	     //  regular o/p
	    //ChatLanguageModel model = buildLLMResponseModelStandAloneServer(llmServerUrl, modelName, llmResponseTemp);		    
	    //llmResponse = model.generate(text);	  
	     	     
	     if(modelStreaming == null)
	     {
	      llmResponseTemp =  "".equals(temperature) ? llmResponseTemp : Double.parseDouble(temperature);
	      modelStreaming = buildLLMResponseModelStandAloneServerStreaming(llmServerUrl, modelName, llmResponseTemp);
	     }
	     
	     CompletableFuture<Response<AiMessage>> futureResponse = new CompletableFuture<>();
	     
	     
	     List<String> llmPromptInBatches = manageLlmPromptVolume(text, category);
	     
	     /*
	     List<Boolean> processingInProgress = new ArrayList<Boolean>();	     
	     processingInProgress.add(false); //at the start nothing is in progress
	     */
	     Boolean[] processingInProgress = {false};         //at the start nothing is in progress
	     
	     int count = 1;
	     Iterator<String> batchItr  = llmPromptInBatches.iterator();
	     //for(String batch : llmPromptInBatches)
	     while(batchItr.hasNext())
	     {	 
		     while(!processingInProgress[0])	 //only if currently nothing is processingInProgress then start next batch 
		     {
		    	 System.out.println("\n\n######### Start processing the batch: "+count);
		    	 
		    	 
		    	 //processingInProgress.clear();
		    	 //processingInProgress.add(true);     //curr batch processing has started. Next batch cannot start
		    	 processingInProgress[0]=true;		   //curr batch processing has started. Next batch cannot start
		    	 
		    	 //streaming o/p  start for 1 batch
		    	 String batch = batchItr.next();
		    	 modelStreaming.generate(batch, new StreamingResponseHandler<AiMessage>() 
		    	 {
			            @Override
			            public void onNext(String token) {
			                System.out.print(token);
			            }
	
			            @Override
			            public void onComplete(Response<AiMessage> response) {
			                futureResponse.complete(response); //curr batch processing has completed in success. Next batch can start
			                /*
			                processingInProgress.clear();
			                processingInProgress.add(false); 
			                */
			                processingInProgress[0]=false;    //curr batch processing has completed in success. Next batch can start 
			            }
	
			            @Override
			            public void onError(Throwable error) {
			                futureResponse.completeExceptionally(error); //curr batch processing has complete in error. Next batch can start
			                processingInProgress[0]=false;               //curr batch processing has complete in error. Next batch can start 
			            }
			        });
		    	 
			     Response<AiMessage> aiMsg = futureResponse.join(); //collate streaming o/p of 1 batch
			     llmResponse  = aiMsg.content().text();	            //collate streaming o/p of 1 batch
			     
			     processingInProgress[0]=false;               //curr batch processing has complete in error. Next batch can start
			     count++;
		       }//end while
		     
		        //batchItr.next();    //next batch up for processing
		     	System.out.println("%%%%%%%% Next batch ready for processing ");
		     	
		     	
	     }//end for
	          
	     
	     System.out.println("\n---- Got local LLM response : "+ llmResponse);	    	    
	    
	    return llmResponse;
	 }

	public String generateWithRetry(String text, boolean testMode, String llmModel, String category, List<String> buffer, String temperature) throws Exception //vj19
	{
		
		//hardcoded for tests
		//String roleAndTask = "You are a helpful assistant. The following bytecode is generated by the Javaassist tool. Please explain the business execution logic in simple language. Do not mention assembly language words in your response. Mention the Java classes, methods and subsystems involved.\n";
		String roleAndTask = "You are a helpful assistant. The following bytecode is generated by the Javaassist tool. "
								+ "Please explain the business execution logic in simple language. "
								+ "Do not mention assembly language words in your response. "
								+ "Mention the Java classes, methods and subsystems involved."
								+ "Restrict your response to the Java classes that are fall under the subpackages of com.enbd  \n";
		//com.enbd.microservices
		String newText = null;
		text = text.replace("You are a helpful assistant.", roleAndTask);
		//newText = text.substring(0, 1500); //works with phi3 llama2 - works
		newText = text.substring(0, 2500);
		//newText = text.substring(0, 4000);
		
		
		
		//text = text.substring(0, 200);    // chck with llama3:70b
		System.out.println("\n----modified prompt for tests \n"+text);
		//hardcoded for tests		
		
		Map<String, String> severConfigMap  = gatherConfig(testMode, llmModel); 
		
		String modelName       = severConfigMap.get("modelName");	//default model
		
		
		if(constants.isModelValid(modelName, System.getProperty("deployment_env")))
		{
			modelName = llmModel; //user specified model
		}
		else
		{
			System.out.println("**** Invalid LLM Model requested by user. Exiting");
			//throw new Exception("**** Invalid LLM Model requested by user. Exiting");
		}
		System.out.println("---- LLM Model set is "+modelName);
		
		Integer llmServerPort  = Integer.parseInt(severConfigMap.get("llmServerPort"));
		Double llmResponseTemp = Double.parseDouble(severConfigMap.get("llmResponseTemp"));
				
	    
	    /*
             -- on the fly launch. Does not work in PG env
	    GenericContainer<?> ollama = startLLMServer(modelName, llmServerPort); 	    
	    ChatLanguageModel model = buildLLMResponseModel(ollama, modelName, llmResponseTemp);
	    stopLLMServer(ollama); // stop LLM server on the fly.
	    */	
		
	    
	    //--standalone server invocation
	    //String llmServerUrl = "http://127.0.0.1:11434"; //vijay hardcoded local machine
	    //String llmServerUrl = "http://ollama.bawabaai-gpt.svc.cluster.local:11434"; //vijay hardcoded
        //String llmServerUrl = "http://ollama-llama3.bawabaai-gpt.svc.cluster.local:11434"; //vijay hardcoded access from with PG POD only
		//String llmServerUrl = "https://ollama-llama3-bawabaai-gpt.pgocp.uat.emiratesnbd.com"; //vijay hardcoded access from local machine
		
				
		String llmServerUrl = constants.getResourceByModelName(llmModel, System.getProperty("deployment_env"));
		String llmResponse = "";
		
		
		
	     System.out.println("\n---- Started local LLM invocation for user input : "+ text);
	     System.out.println("---- Generating with modelName : "+ modelName);
		
	    /*
	      regular o/p
	    ChatLanguageModel model = buildLLMResponseModelStandAloneServer(llmServerUrl, modelName, llmResponseTemp);
	    llmResponse = model.generate(newText);	  
	    System.out.println("\n\n---- buffer populated"); 
	    buffer.add(llmResponse);
	    */
		
	    
	    
	     
	     //vj19 streaming o/p  start
	     llmResponseTemp =  "".equals(temperature) ? llmResponseTemp : Double.parseDouble(temperature);
	     StreamingChatLanguageModel modelStreaming = buildLLMResponseModelStandAloneServerStreaming(llmServerUrl, modelName, llmResponseTemp);	     
	     CompletableFuture<Response<AiMessage>> futureResponse = new CompletableFuture<>();
	     try 
	     {
	        modelStreaming.generate(text, new StreamingResponseHandler<AiMessage>() 
	        {	        	
	        	@Override
	            public void onNext(String token) {
	                System.out.print(token);
	            }

	            @Override
	            public void onComplete(Response<AiMessage> response) {
	                futureResponse.complete(response);
	            }

	            @Override
	            public void onError(Throwable error) {
	                futureResponse.completeExceptionally(error);
	                System.out.println("**** error "+error);
	            }
	        });
	        
	     Response<AiMessage> aiMsg = futureResponse.join();
	     llmResponse  = aiMsg.content().text();	     
	     System.out.println("\n---- Got local LLM response : "+ llmResponse);	
	     }
	     catch(Exception e)
	     {
	    	 System.out.println("**** error "+e);
	     }
	    
	    System.out.println("\n\n---- buffer populated"); 
	    buffer.add(llmResponse);
	    
	    return llmResponse;
	    
	 }
	
	public String generate(String text, boolean testMode, String llmModel, String category, String temperature) throws Exception  //vj19
	{
		/*
		//hardcoded for tests
		//String roleAndTask = "You are a helpful assistant. The following bytecode is generated by the Javaassist tool. Please explain the business execution logic in simple language. Do not mention assembly language words in your response. Mention the Java classes, methods and subsystems involved.\n";
		String roleAndTask = "You are a helpful assistant. The following bytecode is generated by the Javaassist tool. "
								+ "Please explain the business execution logic in simple language. "
								+ "Do not mention assembly language words in your response. "
								+ "Mention the Java classes, methods and subsystems involved."
								+ "Restrict your response to the Java classes that are fall under the subpackages of com.enbd  \n";
		//com.enbd.microservices
		String newText = null;
		text = text.replace("You are a helpful assistant.", roleAndTask);
		//newText = text.substring(0, 1500); //works with phi3 llama2 - works
		newText = text.substring(0, 2500);
		//text = text.substring(0, 4000);
				
		//text = text.substring(0, 200);    // chck with llama3:70b
		System.out.println("\n----modified prompt for tests \n"+text);
		//hardcoded for tests		
		*/
		
		Map<String, String> severConfigMap  = gatherConfig(testMode, llmModel); 
		
		String modelName       = severConfigMap.get("modelName");	//default model
		
		
		if(constants.isModelValid(modelName, System.getProperty("deployment_env")))
		{
			modelName = llmModel; //user specified model
		}
		else
		{
			System.out.println("**** Invalid LLM Model requested by user. Exiting");
			//throw new Exception("**** Invalid LLM Model requested by user. Exiting");
		}
		//System.out.println("---- LLM Model set is "+modelName);
		
		Integer llmServerPort  = Integer.parseInt(severConfigMap.get("llmServerPort"));
		Double llmResponseTemp = Double.parseDouble(severConfigMap.get("llmResponseTemp"));
				
	    
	    /*
             -- on the fly launch. Does not work in PG env
	    GenericContainer<?> ollama = startLLMServer(modelName, llmServerPort); 	    
	    ChatLanguageModel model = buildLLMResponseModel(ollama, modelName, llmResponseTemp);
	    stopLLMServer(ollama); // stop LLM server on the fly.
	    */	
		
	    
	    //--standalone server invocation
	    //String llmServerUrl = "http://127.0.0.1:11434"; //vijay hardcoded local machine
	    //String llmServerUrl = "http://ollama.bawabaai-gpt.svc.cluster.local:11434"; //vijay hardcoded
        //String llmServerUrl = "http://ollama-llama3.bawabaai-gpt.svc.cluster.local:11434"; //vijay hardcoded access from with PG POD only
		//String llmServerUrl = "https://ollama-llama3-bawabaai-gpt.pgocp.uat.emiratesnbd.com"; //vijay hardcoded access from local machine
		
		
		String llmServerUrl = constants.getResourceByModelName(modelName, System.getProperty("deployment_env"));
		String llmResponse = "";
		
		
		
	     //System.out.println("\n---- Started local LLM invocation for user input : "+ text);
	     System.out.println("---- Generating with modelName: "+ modelName + " on LLM server: "+ llmServerUrl);
		
	     /*
	       regular o/p
	    ChatLanguageModel model = buildLLMResponseModelStandAloneServer(llmServerUrl, modelName, llmResponseTemp);	
	    try
	    {
	    	llmResponse = model.generate(text);	  
		}
	    catch(Exception e)
	    {
	   	 //System.out.println("**** error "+e.getCause());
	   	 System.out.println("---- retrying ...");
	   	 generate(text, testMode, llmModel, category);
	   	 
	    } 
	     */
	     
	     //vj19
	     StreamingChatLanguageModel modelStreaming = buildLLMResponseModelStandAloneServerStreaming(llmServerUrl, modelName, llmResponseTemp);	     
	     CompletableFuture<Response<AiMessage>> futureResponse = new CompletableFuture<>();
	     try 
	     {
	    	 
	    	//System.out.println("\n\n---- before generate text size: "+text.length()); 
	        modelStreaming.generate(text, new StreamingResponseHandler<AiMessage>() 
	        {	        	
	        	@Override
	            public void onNext(String token) {
	        		System.out.print(token);
	            }

	            @Override
	            public void onComplete(Response<AiMessage> response) {
	                futureResponse.complete(response);
	            }

	            @Override
	            public void onError(Throwable error) {
	                futureResponse.completeExceptionally(error);
	                System.out.println("**** error "+error);
	            }
	        });
	        
	     Response<AiMessage> aiMsg = futureResponse.join();
	     llmResponse  = aiMsg.content().text();	     
	     //System.out.println("\n---- Got local LLM response : "+ llmResponse);	
	     }
	     catch(Exception e)
	     {
	    	 System.out.println("**** error "+e);
	     }
	    
	    return llmResponse;
	 }
	
	//vj24
	public String generateImageAnalyze(String text, boolean testMode, String llmModel, String category, String temperature) throws Exception
	{	
		Map<String, String> severConfigMap  = gatherConfig(testMode, llmModel); 
		String modelName       = severConfigMap.get("modelName");	//default model
		if(constants.isModelValid(modelName, System.getProperty("deployment_env")))
		{
			modelName = llmModel; //user specified model
		}
		else
		{
			System.out.println("**** Invalid LLM Model requested by user. Exiting");
			//throw new Exception("**** Invalid LLM Model requested by user. Exiting");
		}
		//System.out.println("---- LLM Model set is "+modelName);
		
		Integer llmServerPort  = Integer.parseInt(severConfigMap.get("llmServerPort"));
		Double llmResponseTemp = Double.parseDouble(severConfigMap.get("llmResponseTemp"));
		
		String llmServerUrl = constants.getResourceByModelName(modelName, System.getProperty("deployment_env"));
		String llmResponse = "";
		System.out.println("---- Generating with modelName: "+ modelName + " on LLM server: "+ llmServerUrl);
		
	    
		
		
	    //   regular o/p
	    ChatLanguageModel model = buildLLMResponseModelStandAloneServer(llmServerUrl, modelName, llmResponseTemp);	
	    try
	    {
	    	/*
	    	Response<AiMessage> llmResponse1 = model.generate(UserMessage.userMessage(TextContent.from("Provide 1 word title for this picture"), 
	    					ImageContent.from("http://localhost:8888/test-1.JPG")));	
	    	*/
	    	
	    	
	    	String[] portions = text.split("~");
	    	Response<AiMessage> llmResponse1 = model.generate(UserMessage.userMessage(TextContent.from(portions[0]), 
	    													  ImageContent.from("http://localhost:8888/"+portions[1])));	
					 										 						    
	    	
	    	llmResponse = llmResponse1.content().text();
		}
	    catch(Exception e)
	    {
	   	 System.out.println("---- error ");
	   	 e.printStackTrace();
	    }
	    	    
	    
	    
	    
	    
	    return llmResponse;
	 }

	
	
	

	public String generate_1(String text, boolean testMode, String llmModel, String category) throws Exception 
	{
		 
		//hardcoded for tests
		//String roleAndTask = "You are a helpful assistant. The following bytecode is generated by the Javaassist tool. Please explain the business execution logic in simple language. Do not mention assembly language words in your response. Mention the Java classes, methods and subsystems involved.\n";
		String roleAndTask = "You are a helpful assistant. The following bytecode is generated by the Javaassist tool. "
								+ "Please explain the business execution logic in simple language. "
								+ "Do not mention assembly language words in your response. "
								+ "Mention the Java classes, methods and subsystems involved."
								+ "Restrict your response to the Java classes that are fall under the subpackages of com.enbd  \n";
		//com.enbd.microservices
		String newText = null;
		text = text.replace("You are a helpful assistant.", roleAndTask);
		//newText = text.substring(0, 1500); //works with phi3 llama2 - works
		newText = text.substring(0, 2500);
		//text = text.substring(0, 4000);
		
		
		
		//text = text.substring(0, 200);    // chck with llama3:70b
		//System.out.println("\n----modified prompt for tests \n"+text);
		//hardcoded for tests		
		
		Map<String, String> severConfigMap  = gatherConfig(testMode, llmModel); 
		
		String modelName       = severConfigMap.get("modelName");	//default model
		
		
		if(constants.isModelValid(modelName, System.getProperty("deployment_env")))
		{
			modelName = llmModel; //user specified model
		}
		else
		{
			System.out.println("**** Invalid LLM Model requested by user. Exiting");
			//throw new Exception("**** Invalid LLM Model requested by user. Exiting");
		}
		System.out.println("---- LLM Model set is "+modelName);
		
		Integer llmServerPort  = Integer.parseInt(severConfigMap.get("llmServerPort"));
		Double llmResponseTemp = Double.parseDouble(severConfigMap.get("llmResponseTemp"));
				
	    
	    /*
             -- on the fly launch. Does not work in PG env
	    GenericContainer<?> ollama = startLLMServer(modelName, llmServerPort); 	    
	    ChatLanguageModel model = buildLLMResponseModel(ollama, modelName, llmResponseTemp);
	    stopLLMServer(ollama); // stop LLM server on the fly.
	    */	
		
	    
	    //--standalone server invocation
	    //String llmServerUrl = "http://127.0.0.1:11434"; //vijay hardcoded local machine
	    //String llmServerUrl = "http://ollama.bawabaai-gpt.svc.cluster.local:11434"; //vijay hardcoded
        //String llmServerUrl = "http://ollama-llama3.bawabaai-gpt.svc.cluster.local:11434"; //vijay hardcoded access from with PG POD only
		//String llmServerUrl = "https://ollama-llama3-bawabaai-gpt.pgocp.uat.emiratesnbd.com"; //vijay hardcoded access from local machine
		
		
		String llmServerUrl = constants.getResourceByModelName(modelName, System.getProperty("deployment_env"));
		String llmResponse = "";
		
		
		
	     System.out.println("\n---- Started local LLM invocation for user input : "+ text);
	     //System.out.println("---- Generating with modelName : "+ modelName);
		
	     /*
	       regular o/p
	    ChatLanguageModel model = buildLLMResponseModelStandAloneServer(llmServerUrl, modelName, llmResponseTemp);	
	    try
	    {
	    	llmResponse = model.generate(text);	  
		}
	    catch(Exception e)
	    {
	   	 //System.out.println("**** error "+e.getCause());
	   	 System.out.println("---- retrying ...");
	   	 generate(text, testMode, llmModel, category);
	   	 
	    } 
	     */
	     
	     // streaming o/p  start
	     StreamingChatLanguageModel modelStreaming = buildLLMResponseModelStandAloneServerStreaming(llmServerUrl, modelName, llmResponseTemp);	     
	     CompletableFuture<Response<AiMessage>> futureResponse = new CompletableFuture<>();
	     try 
	     {
	        modelStreaming.generate(newText, new StreamingResponseHandler<AiMessage>()  
	        {	        	
	        	@Override
	            public void onNext(String token) {
	                System.out.print(token);
	            }

	            @Override
	            public void onComplete(Response<AiMessage> response) {
	                futureResponse.complete(response);
	            }

	            @Override
	            public void onError(Throwable error) {
	                futureResponse.completeExceptionally(error);
	                System.out.println("**** error "+error);
	            }
	        });
	        
	     Response<AiMessage> aiMsg = futureResponse.join();
	     llmResponse  = aiMsg.content().text();	     
	     System.out.println("\n---- Got local LLM response : "+ llmResponse);	
	     }
	     catch(Exception e)
	     {
	    	 System.out.println("**** error "+e);
	     }
	    
	    return llmResponse;
	 }

	private List<String> manageLlmPromptVolume(String textFullVolume, String category) 
	{
		
		if("explainflow".equals(category))  // explainflow bytecode fetched from VectorDB is dynamic and may be huge
		{
		 String roleAndTask = "You are a helpful assistant. The following bytecode is generated by the Javaassist tool. Please explain the business execution logic in simple language. Do not mention assembly language words in your response. Mention the Java classes, methods and subsystems involved.\n";
		 String[] batches = textFullVolume.split("Execution logic of Java"); //split the userprompt in to smaller chunks
		 List<String> batchesRaw = Arrays.asList(batches);
		 //batchesRaw.remove(0); // Generic statement "You are a helpful assistant". Remove for now
		 
		 
		 List<String> batchesToProcess = batchesRaw.stream()
				 									.filter(b -> !b.startsWith("You are a helpful assistant."))// Generic statement "You are a helpful assistant". Remove for now
				 									.map(b -> roleAndTask.concat(" ") //for every batch add the role and task at the start so that AI can understand the context.
				 														 .concat(b))
				 									.toList(); 
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
	 * get server config
	 */
	private Map<String, String> gatherConfig(boolean testMode, String llmModel) 
	{
		Map<String, String> llmServerConfig = new HashMap<String, String>();
		
		String currentDir = System.getProperty("user.dir");
		String resoucePath = currentDir + "\\"+ "\\src\\main\\resources\\application.properties";
		
		//vj5
		String modelName       = llmModel; 	
		String llmServerPort   = "11434";
		String llmResponseTemp = "0.9";
		
		if(!testMode)
		{
			modelName       = fileUtilsSvc.extractFields("llm.model.name", resoucePath);		
			llmServerPort    = fileUtilsSvc.extractFields("llm.server.port", resoucePath);
			llmResponseTemp = fileUtilsSvc.extractFields("llm.response.temperature", resoucePath);
		}		
		
		llmServerConfig.put("modelName", modelName);
		llmServerConfig.put("llmServerPort", llmServerPort);
		llmServerConfig.put("llmResponseTemp", llmResponseTemp);
		
		return llmServerConfig;
	}

	/*
	 * create and start Ollama server on the fly
	 */
	private GenericContainer<?> startLLMServer(String modelName, Integer llmServerPort) 
	{
		System.out.println("\n---- starting LLM server with : "+ "langchain4j/ollama-" + modelName + ":latest");
		
		//-- be patient - this docker pull model will require time to complete on its 1st run
		System.out.println("**********  LLM server launch in progress. This docker pull may take time. Please be patient **********");
	    GenericContainer<?> ollama = new GenericContainer<>("langchain4j/ollama-" + modelName + ":latest") 
	            							.withExposedPorts(llmServerPort);
	    ollama.start();
	    System.out.println("---- started LLM server");
		return ollama;
	}
	
	/*
	 * stop Ollama server
	 */
	private void stopLLMServer(GenericContainer<?> ollama) 
	{
		System.out.println("---- stopping LLM server");
		ollama.stop();
		System.out.println("---- stopped LLM server");
	}

	/*
	 * build Ollama server host URL
	 */
	 private String baseUrl(GenericContainer<?> ollama) 
	 {
	    return String.format("http://%s:%d", ollama.getHost(), ollama.getFirstMappedPort());
	 }
	 
	 /*
	  * build LLM response model 
	  */
	 private ChatLanguageModel buildLLMResponseModel(GenericContainer<?> ollama, String modelName, double llmResponseTemp) 
	 {
			ChatLanguageModel model = OllamaChatModel.builder()
								        			   .baseUrl(baseUrl(ollama))
								        			   .modelName(modelName)
								        			   .temperature(llmResponseTemp)
								        			   .timeout(Durations.TEN_MINUTES) //best is to NOT change this
								        			   .build();
			return model;
	}

	
	 /*
	  * standalone LLM server instance.
	  * be sure to have Ollama server running 
	  */
	 //   vj25
	 private ChatLanguageModel buildLLMResponseModelStandAloneServer(String llmServerUrl, String modelName, double llmResponseTemp) 
	 {
			ChatLanguageModel model = OllamaChatModel.builder()
								        			   //.baseUrl("http://127.0.0.1:11434") //server running on localhost
												   .baseUrl(llmServerUrl)
								        			   .modelName(modelName)
								        			   .temperature(llmResponseTemp)
								        			   .timeout(Durations.TEN_MINUTES) //best is to NOT change this
								        			   .maxRetries(100)//vj25
								        			   .build();
			return model;
	}
	
	 
	 /*
	  * standalone LLM server instance.
	  * be sure to have Ollama server running 
	  */
	 private StreamingChatLanguageModel buildLLMResponseModelStandAloneServerStreaming(String llmServerUrl, String modelName, double llmResponseTemp) 
	 {
		 StreamingChatLanguageModel model = OllamaStreamingChatModel.builder()
													  			   //.baseUrl("http://127.0.0.1:11434") //server running on localhost
																   .baseUrl(llmServerUrl)
													  			   .modelName(modelName)
													  			   .temperature(llmResponseTemp)
													  			   .timeout(Durations.TEN_MINUTES) //best is to NOT change this
													  			   .build();
		 
		 return model;
	}
}
