package com.genai.llm.privacy.mgt.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.awaitility.Durations;

import com.genai.llm.privacy.mgt.utils.Constants;

import dev.langchain4j.data.message.AiMessage;
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
	
	/*
	 * Local LLM server : Ollama operations	
	 */
	public String generate(String text, boolean testMode, String llmModel) throws Exception //vj7
	{
		
		Map<String, String> severConfigMap  = gatherConfig(testMode, llmModel); //vj7	
		
		String modelName       = severConfigMap.get("modelName");	//default model
		
		//vj7
		if(llmModel !=null && Constants.getValidModelsMap().containsKey(llmModel.trim()))
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
				
	    //vj4
	    /*
             -- on the fly launch. Does not work in PG env
	    GenericContainer<?> ollama = startLLMServer(modelName, llmServerPort); 	    
	    ChatLanguageModel model = buildLLMResponseModel(ollama, modelName, llmResponseTemp);
	    stopLLMServer(ollama); // stop LLM server on the fly.
	    */	
		
	    //vj7
	    //--standalone server invocation
	    //String llmServerUrl = "http://127.0.0.1:11434"; //vijay hardcoded local machine
	    //String llmServerUrl = "http://ollama.bawabaai-gpt.svc.cluster.local:11434"; //vijay hardcoded
        //String llmServerUrl = "http://ollama-llama3.bawabaai-gpt.svc.cluster.local:11434"; //vijay hardcoded access from with PG POD only
		//String llmServerUrl = "https://ollama-llama3-bawabaai-gpt.pgocp.uat.emiratesnbd.com"; //vijay hardcoded access from local machine
		
		//vj7	    
		String llmServerUrl = Constants.getValidModelsMap().get(modelName);
		String llmResponse = "";
		
		
		//vj8
	     System.out.println("\n---- Started local LLM invocation for user input : "+ text);
	     System.out.println("---- Generating with modelName : "+ modelName);
		
	     //vj8  regular o/p
	    //ChatLanguageModel model = buildLLMResponseModelStandAloneServer(llmServerUrl, modelName, llmResponseTemp);		    
	    //llmResponse = model.generate(text);	  
	     
	     
	     
	     //vj8 streaming o/p  start
	     StreamingChatLanguageModel modelStreaming = buildLLMResponseModelStandAloneServerStreaming(llmServerUrl, modelName, llmResponseTemp);		    
	     CompletableFuture<Response<AiMessage>> futureResponse = new CompletableFuture<>();
	        modelStreaming.generate(text, new StreamingResponseHandler<AiMessage>() {

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
	            }
	        });
	     Response<AiMessage> aiMsg = futureResponse.join();
	     llmResponse  = aiMsg.content().text();	     
	     
	   //vj8 streaming o/p  stop
	     
	     
	     System.out.println("\n---- Got local LLM response : "+ llmResponse);	    	    
	    
	    return llmResponse;
	 }

	/*
	 * get server config
	 */
	private Map<String, String> gatherConfig(boolean testMode, String llmModel) //vj7
	{
		Map<String, String> llmServerConfig = new HashMap<String, String>();
		
		String currentDir = System.getProperty("user.dir");
		String resoucePath = currentDir + "\\"+ "\\src\\main\\resources\\application.properties";
		
		//vj5
		String modelName       = llmModel; //vj7	
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

	//vj4
	 /*
	  * standalone LLM server instance.
	  * be sure to have Ollama server running 
	  */
	 private ChatLanguageModel buildLLMResponseModelStandAloneServer(String llmServerUrl, String modelName, double llmResponseTemp) 
	 {
			ChatLanguageModel model = OllamaChatModel.builder()
								        			   //.baseUrl("http://127.0.0.1:11434") //server running on localhost
												   .baseUrl(llmServerUrl)
								        			   .modelName(modelName)
								        			   .temperature(llmResponseTemp)
								        			   .timeout(Durations.TEN_MINUTES) //best is to NOT change this
								        			   .build();
			return model;
	}
	 
	 //vj8
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
