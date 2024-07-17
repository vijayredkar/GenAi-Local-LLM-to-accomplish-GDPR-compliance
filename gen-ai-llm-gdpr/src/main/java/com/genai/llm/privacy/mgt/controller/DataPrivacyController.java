package com.genai.llm.privacy.mgt.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation. PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation. RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind. annotation. RestController;

import com.genai.llm.privacy.mgt.service.RetrievalService;
import com.genai.llm.privacy.mgt.service.VectorDataStoreService;
import com.genai.llm.privacy.mgt.utils.Constants;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

@RestController
@RequestMapping(value="/gen-ai/v1/llm")
public class DataPrivacyController
{
	@Autowired
	private RetrievalService retrievalSvc;
	
	@Autowired 
	private VectorDataStoreService vectorDataSvc;
	
	@Autowired 
	private Constants constants;
	
	@Value("${vector.db.index.logsextract}") //vj24B
	private String vectorDbIndexLogsExtract;
	
	/*
	endpoint to load newer contexts provided by the user
	*/
	@PostMapping("context")
	public ResponseEntity<String> loadContext(@RequestParam String fileNameWithFullPath)
	{
		String response = null;
		if (fileNameWithFullPath == null || "".equals(fileNameWithFullPath.trim()))
		{
			response = "File is empty. Nothing to load";
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}
		
		response = "Vector DB new context loaded";	
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	/*
	* generic retrieve endpoint to get a response from the local inference engine - ask anything
	*/	
	@GetMapping("/retrieve")	
	public ResponseEntity<String> retrieve(@RequestParam(value = "userPrompt", required = true) String userPrompt,
										   @RequestParam(value = "customSystemMessage", required = false, defaultValue = "") String customSystemMessage,
										   @RequestParam(value = "category", required = false, defaultValue = "generic") String category,
										   @RequestParam(value = "llmModel", required = false, defaultValue = "llama3") String llmModel,
										   @RequestParam(value = "embeddingsMinScore", required = false, defaultValue = "0.5") String embeddingsMinScore,
										   @RequestParam(value = "temperature", required = false, defaultValue = "0.5") String temperature,
										   @RequestParam(value = "retrievalLimitMax", required = false, defaultValue = "5") String retrievalLimitMax,
										   @RequestParam(value = "htmlOutput", required = false, defaultValue = "") String htmlOutput)
										   throws Exception  
	{
		boolean testMode= true; 
		System.out.println("\n---- started retrieve flow - mode : "+testMode);
		String response = retrievalSvc.orchestrate(userPrompt, customSystemMessage, category, llmModel, testMode,temperature, embeddingsMinScore, retrievalLimitMax);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/*
	*  extract specific knowledge from vast documentation/Confluence 
	*/
	@PostMapping("/knowledgebase")	
	public ResponseEntity<String> knowledgeExtract(@RequestBody String documentContent,
												   @RequestParam(value = "customUserQuestion", required = true, defaultValue = "") String customUserQuestion,
												   @RequestParam(value = "documentTitle", required = true, defaultValue = "") String documentTitle,
												   @RequestParam(value = "customSystemMessage", required = false, defaultValue = "") String customSystemMessage,
												   @RequestParam(value = "documentRepoName", required = false, defaultValue = "") String documentRepoName,												   
												   @RequestParam(value = "embeddingsMinScore", required = false, defaultValue = "0.6") String embeddingsMinScore,
												   @RequestParam(value = "temperature", required = false, defaultValue = "0") String temperature,
												   @RequestParam(value = "retrievalLimitMax", required = false, defaultValue = "4") String retrievalLimitMax,
												   @RequestParam(value = "htmlOutput", required = false, defaultValue = "") String htmlOutput,
												   @RequestParam(value = "llmModel", required = false, defaultValue = "llama3") String llmModel)
											  throws Exception
	{	
		boolean testMode= true;		
		System.out.println("\n---- started knowledgeExtract - mode : "+testMode);		
		String response = null;		
				
		
		//stage-1: data prepare - load to VectorDB - confluence text i/p is vast. Split in to segments and save in to vectorDB per document title
		if("".equals(documentRepoName.trim())) // implies doc has been saved to vector DB previously. Do not store again
		{
			vectorDataSvc.loadData(documentContent, "knowledgebase", true, documentTitle);
		}
		
		//stage-2: narrow down to relevant text segments only - fetch from vectorDB per document title only those segments relevant to user's question
		List<EmbeddingMatch<TextSegment>> matchingRecords = vectorDataSvc.fetchRecords("knowledgebase", customUserQuestion, documentTitle, embeddingsMinScore, retrievalLimitMax);
		
		//stage 3:  1st level of LLM refinement - retain only those segments that are > 95% relevant
		String userQuestionForKnwBase = " Here is the user's question:\n" + customUserQuestion +"\n";
		String systemMsgKnwBase = "".equals(customSystemMessage) ? constants.customSystemMessageKnwBase : customSystemMessage;		
		StringBuilder responseBldr = new StringBuilder();
		
		matchingRecords.forEach(m -> {									
										System.out.println("---- semantically matching record from VectorDB: \n\n"+m.embedded().text());										
										String enhancedPrompt = systemMsgKnwBase +" \n "+ m.embedded().text() +" \n "+userQuestionForKnwBase;										
										
										try 
										{
												String response1 = retrievalSvc.orchestrateLLMServerOnly(enhancedPrompt, testMode, llmModel, "generic", temperature);
												responseBldr.append(response1).append("\n");
												System.out.println("---- response1: \n "+response1);
										} catch (Exception e) 
										{
											e.printStackTrace();
										}     
									} );
		
		response =  responseBldr.toString();
		
		//stage 4: 2nd level of LLM refinement - run user's question on the consolidated LLM 1st level response 
		String consolidatedEnhancedPrompt = systemMsgKnwBase +" \n "+ response +" \n "+userQuestionForKnwBase;
		try 
		{
			String response2 = retrievalSvc.orchestrateLLMServerOnly(consolidatedEnhancedPrompt, testMode, llmModel, "generic", temperature);
			responseBldr.append(response2).append("\n");
			System.out.println("---- final response2: \n "+response2);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}   
		
		/*
		//stage-1 response: very detailed based off the confluence vast text
		response = retrievalSvc.orchestrate(documentContent, constants.customSystemMessageKnwBase+"Z@@"+customUserQuestion+"Z@@"+documentTitle,  "knowledgebase", llmModel, testMode);
		
		//stage-2 response: summarized to make it readable for generic audience
		System.out.println("\n\n ---- Final summmary of the execution flow ----"); 
		
		String userQuestionForKnwBase = " Here is the user's question:\n" + customUserQuestion +"\n";		
		String enhancedPrompt = constants.customSystemMessageKnwBase +" \n "+response+" \n "+userQuestionForKnwBase; //ensure this size is less than 2000 words
		response = retrievalSvc.orchestrateLLMServerOnly(enhancedPrompt, testMode, llmModel, "generic");
		*/
		
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/* vj24B
	*  extract RCA from vast volumes of logs 
	*/
	@PostMapping("/logsextract")	
	public ResponseEntity<String> logsExtract(@RequestBody String documentContent,
												   @RequestParam(value = "customUserQuestion", required = true, defaultValue = "") String customUserQuestion,
												   @RequestParam(value = "documentTitle", required = true, defaultValue = "") String documentTitle,
												   @RequestParam(value = "customSystemMessage", required = false, defaultValue = "") String customSystemMessage,
												   @RequestParam(value = "documentRepoName", required = false, defaultValue = "") String documentRepoName,												   
												   @RequestParam(value = "embeddingsMinScore", required = false, defaultValue = "0.5") String embeddingsMinScore,
												   @RequestParam(value = "temperature", required = false, defaultValue = "0") String temperature,
												   @RequestParam(value = "retrievalLimitMax", required = false, defaultValue = "4") String retrievalLimitMax,
												   @RequestParam(value = "htmlOutput", required = false, defaultValue = "") String htmlOutput,
												   @RequestParam(value = "llmModel", required = false, defaultValue = "llama3") String llmModel)
											  throws Exception
	{	
		boolean testMode= true;		
		System.out.println("\n---- started logsExtract - mode : "+testMode);		
		String response = null;

		String userQuestionForKnwBase = " Here is the user's question:\n" + constants.customUserQuestionLogsRca +"\n";
		String systemMsgKnwBase = "".equals(customSystemMessage) ? constants.customSystemMessageLogsRca : customSystemMessage;	
		
		//stage-0: fetch logs by URC from Kibana APIs
		documentContent = retrievalSvc.fetchLogsByUrc(parseInput(documentContent));
		documentTitle =  new Long(Math.abs(UUID.randomUUID().getMostSignificantBits())).toString(); //only positive unique long randoms //col-logsextract-1-8477365280683177256
		
		//stage-1: data prepare - load to VectorDB - logs text i/p is vast. Split in to segments and save in to vectorDB per document title
		if("".equals(documentRepoName.trim())) // save the i/p to DB. If already saved do not store again. 
		{	
			vectorDataSvc.loadData(documentContent, "logsextract", true, documentTitle);
		}
		
		//stage-2: narrow down to relevant text segments only - fetch from vectorDB per document title only those segments relevant to user's question
		List<EmbeddingMatch<TextSegment>> matchingRecords = vectorDataSvc.fetchRecords("logsextract", constants.customUserQuestionLogsRca, documentTitle, embeddingsMinScore, retrievalLimitMax);
		
		//stage 3:  1st level of LLM refinement - retain only those segments that are > 95% relevant	
		StringBuilder responseBldr = new StringBuilder();
		matchingRecords.forEach(m -> {									
										System.out.println("---- semantically matching record from VectorDB: \n\n"+m.embedded().text());										
										String enhancedPrompt = systemMsgKnwBase +" \n "+ m.embedded().text() +" \n "+userQuestionForKnwBase;										
										
										try 
										{
												String response1 = retrievalSvc.orchestrateLLMServerOnly(enhancedPrompt, testMode, llmModel, "generic", temperature);
												responseBldr.append(response1).append("\n");
												System.out.println("---- response1: \n "+response1);
										} catch (Exception e) 
										{
											e.printStackTrace();
										}     
									} );
		
		response =  responseBldr.toString();
		
		//stage 4: 2nd level of LLM refinement - run user's question on the consolidated LLM 1st level response 
		String consolidatedEnhancedPrompt = systemMsgKnwBase +" \n "+ response +" \n "+userQuestionForKnwBase;
		String response2 = null;
		try 
		{
			response2 = retrievalSvc.orchestrateLLMServerOnly(consolidatedEnhancedPrompt, testMode, llmModel, "generic", temperature);
			responseBldr.append(response2).append("\n");
			System.out.println("---- final response2: \n "+response2);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}   
		
		//prepare final response 
		String finalResponse = "";
		finalResponse = finalResponse.concat(responseBldr.toString())										
									 .concat("\n\n")
									 .concat("For Internal use: tracking number for future: ")
									 .concat(vectorDbIndexLogsExtract.concat("-").concat(documentTitle)); //col-logsextract-1-8477365280683177256

		return new ResponseEntity<>(finalResponse, HttpStatus.OK);
	}
	
	
	
	private String parseInput(String input) //vj24B
	{ /*
		{
		    "urc":"94b79904-e410-41be-9e43-85a6aceaba65"
		}
		*/
		input = input.replace("{", "")
					 .replace("}", "");
		input = input.trim()
		     		.split(":")[1]
		     		.replaceAll("\"", "");
		return input;
	}

	/* 
	 * Devathon prototype - external txfrs/IBAN/NRE flow explanations 
	 */
	@GetMapping("/retrieve-flowtrain")	
	public ResponseEntity<String> retrieveFlowTrain (@RequestParam("text") String text, 
													 @RequestParam(value = "llmModel", required = false, defaultValue = "llama3") String llmModel,
												     @RequestParam(value = "embeddingsMinScore", required = false, defaultValue = "0.5") String embeddingsMinScore,
												     @RequestParam(value = "temperature", required = false, defaultValue = "0.5") String temperature,
												     @RequestParam(value = "retrievalLimitMax", required = false, defaultValue = "5") String retrievalLimitMax,
												     @RequestParam(value = "htmlOutput", required = false, defaultValue = "") String htmlOutput)
													 throws Exception  
	{	
		boolean testMode= true;
		System.out.println("\n---- started retrieveFlowTrain flow - mode : "+testMode);
		String response = retrievalSvc.orchestrateFlowTrain(text, testMode, llmModel, "flowtrain", embeddingsMinScore, retrievalLimitMax, temperature);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
		
	/*
	* retrieve SAHAB spec info
	*/
	@GetMapping("/retrieve-apiinfo")	
	public ResponseEntity<String> retrieveApiInfo (@RequestParam("text") String text, 
													@RequestParam(value = "llmModel", required = false, defaultValue = "llama3") String llmModel,
													@RequestParam(value = "embeddingsMinScore", required = false, defaultValue = "0.5") String embeddingsMinScore,
												    @RequestParam(value = "temperature", required = false, defaultValue = "0.5") String temperature,
												    @RequestParam(value = "retrievalLimitMax", required = false, defaultValue = "5") String retrievalLimitMax,
												    @RequestParam(value = "htmlOutput", required = false, defaultValue = "") String htmlOutput)
													throws Exception  
	{	
		boolean testMode= true;
		System.out.println("\n---- started retrieveApiInfo flow - mode : "+testMode);
		String response = retrievalSvc.orchestrateApiInfo(text, testMode, llmModel, "apiinfo", embeddingsMinScore, retrievalLimitMax, temperature);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/*
	 * endpoint to fetch response directly from the VectorDB only
	 */
	@GetMapping("/testVectorDBInvocationOnly")
	public ResponseEntity<String> invokeVectorDBOnly(@RequestParam("text") String text,
													 @RequestParam(value = "embeddingsMinScore", required = false, defaultValue = "0.5") String embeddingsMinScore,
												     @RequestParam(value = "temperature", required = false, defaultValue = "0.5") String temperature,
												     @RequestParam(value = "retrievalLimitMax", required = false, defaultValue = "5") String retrievalLimitMax,
												     @RequestParam(value = "htmlOutput", required = false, defaultValue = "") String htmlOutput)
	{
		boolean testMode= true; 
		System.out.println("\n---- started invokeVectorDBOnly - mode : "+testMode);
		String response = retrievalSvc.orchestrateVectorDBOnly(text, testMode, embeddingsMinScore, retrievalLimitMax);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/* 
	 * endpoint to load data directly in to the VectorDB only
	 */
	@PostMapping("/dataToVectorStore")
	public ResponseEntity<String> invokeVectorDBOnlyWithPayload(@RequestBody String text, @RequestParam(value = "category", required = true) String category) throws Exception
	{
		
		 // valid category is mandatory to determine the vectorDBName
		boolean testMode= true; 
		System.out.println("\n---- started invokeVectorDBOnlyWithPayload - mode : "+testMode);
		
		String response = vectorDataSvc.loadData(text, category, true, null);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	
	/*
	*  explain business logic executed in an application flow - fetch debit cards status, limits fetch, create debit card
	*/
	@GetMapping("/explainflow")	
	public ResponseEntity<String> explainflow(@RequestParam(value = "userPrompt", required = true) String userPrompt,
											  @RequestParam(value = "customSystemMessage", required = false, defaultValue = "") String customSystemMessage,
											  @RequestParam(value = "llmModel", required = false, defaultValue = "llama3") String llmModel,
											  @RequestParam(value = "embeddingsMinScore", required = false, defaultValue = "0.5") String embeddingsMinScore,
										      @RequestParam(value = "temperature", required = false, defaultValue = "0.5") String temperature,
										      @RequestParam(value = "retrievalLimitMax", required = false, defaultValue = "5") String retrievalLimitMax,
										      @RequestParam(value = "htmlOutput", required = false, defaultValue = "") String htmlOutput,
										      @RequestParam(value = "visual", required = false, defaultValue = "") String visual)										      
											  throws Exception  
									
	{	
		//curlExec.execute(); 
		
		boolean testMode= true;
		System.out.println("\n---- started explainflow - mode : "+testMode);
		String response = null;		
		
		String seededInstruction = " You are a helpful assistant. You will be provided an execution flow generated by JavaAssist. Please summarize this execution flow in simple language. "
								  + " Specify the Java classes, methods and subsystems involved. Do not include the assembly, byte codes in your response. "
								  + " Provide your response within 100 words. ";
								  //+ "Display your response in 3 sections under the headers Execution Flow, Exception Handling and Return Result ";
		
		//String instruction = seededInstruction;
		if("".equals(customSystemMessage.trim())) 
		{
			customSystemMessage = seededInstruction;
		}				
		
		//stage-1 response: very detailed based off the interpreted bytecode instructions from the file  
		response = retrievalSvc.orchestrate(userPrompt, customSystemMessage,  "explainflow", llmModel, testMode, temperature, embeddingsMinScore, retrievalLimitMax);
		
		//stage-2 response: summarized to make it readable for generic audience
		System.out.println("\n\n ---- Final summmary of the execution flow ----"); 
		
							  //+ "Restrict your response to 5 lines only. "
							  ;		
		
	    response = response.replace(seededInstruction, "");
	    
	    
	    //stage-3:  final user friendly o/p generation
	    String finalSystemMessage = " You are a helpful assistant. ";
	    String visualOutputPrompt = " Please provide a simple flow chart diagram with the Java classes, methods and subsystems for this execution flow. ";
	    String textOutputPrompt   = " Please provide a summarized explanation in simple langauge with the Java classes, methods and subsystems for this execution flow. "
	    						   +" Display your response in 3 sections under the headers Execution Flow, Exception Handling and Return Result ";
	    String finalUserPrompt = " Here is the user's request: ";
	    
	    
	    if(!"".equals(customSystemMessage))
	    {
	    	finalSystemMessage =  customSystemMessage;	
	    }
	    
	    
	    if("Y".equals(visual))
	    {
	    	finalUserPrompt =  finalUserPrompt + "\n" +visualOutputPrompt;
	    }
	    else
	    {
	    	finalUserPrompt =  finalUserPrompt + "\n" +textOutputPrompt;
	    }
	    
		String enhancedPrompt = finalSystemMessage+" \n "+response+" \n " + finalUserPrompt;
		response = retrievalSvc.orchestrateLLMServerOnly(enhancedPrompt, testMode, llmModel, "generic", temperature); //summarize the o/p further
		
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/*
	 * endpoint to fetch response directly from the VectorDB only - city, employer names semantic match
	 */
	@GetMapping("/closest-semantic-match")
	public ResponseEntity<String> match(@RequestParam("text") String text,
										@RequestParam(required=false, defaultValue="city") String category,										
										@RequestParam(value = "embeddingsMinScore", required = false, defaultValue = "0.5") String embeddingsMinScore,
									    @RequestParam(value = "temperature", required = false, defaultValue = "0.5") String temperature,
									    @RequestParam(value = "retrievalLimitMax", required = false, defaultValue = "5") String retrievalLimitMax,
									    @RequestParam(value = "htmlOutput", required = false, defaultValue = "") String htmlOutput)
	{
		boolean testMode= true; 
		System.out.println("\n---- started match - mode : "+testMode);
		String response = retrievalSvc.orchestrateVectorDBOnly(category, text, testMode, embeddingsMinScore, retrievalLimitMax);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/* vj24A
	 * endpoint to fetch response directly from the VectorDB only - city, employer names semantic match
	 */
	@PostMapping("/embeddings")
	public ResponseEntity<String> generateEmbeddings(@RequestBody(required=true) String text,
													@RequestParam(required=false, defaultValue="") String category,										
													@RequestParam(value = "embeddingsMinScore", required = false, defaultValue = "0.5") String embeddingsMinScore,
												    @RequestParam(value = "temperature", required = false, defaultValue = "0.5") String temperature,
												    @RequestParam(value = "retrievalLimitMax", required = false, defaultValue = "5") String retrievalLimitMax,
												    @RequestParam(value = "htmlOutput", required = false, defaultValue = "") String htmlOutput)
	{
		boolean testMode= true; 
		System.out.println("\n---- started generateEmbeddings - mode : "+testMode);
		Embedding embeddings = retrievalSvc.manageEmbeddings("generic", text, testMode, embeddingsMinScore, retrievalLimitMax);
		return new ResponseEntity<>(embeddings.toString(), HttpStatus.OK);
	}
	
	/*
	 * endpoint to fetch response from the local LLM inference engine only - small payload generic prompts
	 */
	@GetMapping("/testLLMServerInvocationOnly")
	public ResponseEntity<String> invokeLLMServerOnlyWithSmallPayload(@RequestParam(value = "userPrompt", required = true) String userPrompt, 
																	  @RequestParam(value = "customSystemMessage", required = false, defaultValue = "") String customSystemMessage,
																	  @RequestParam(value = "llmModel", required = false, defaultValue = "llama3") String llmModel,
																	  @RequestParam(value = "embeddingsMinScore", required = false, defaultValue = "0.5") String embeddingsMinScore,
																	  @RequestParam(value = "temperature", required = false, defaultValue = "0.5") String temperature,
																	  @RequestParam(value = "retrievalLimitMax", required = false, defaultValue = "5") String retrievalLimitMax,
																	  @RequestParam(value = "htmlOutput", required = false, defaultValue = "") String htmlOutput)
																	  throws Exception  
	{
		boolean testMode= true; 
		System.out.println("\n---- started invokeLLMServerOnlyWithSmallPayload - mode : "+testMode);		
		String response = retrievalSvc.orchestrate(userPrompt, customSystemMessage, "generic", llmModel, testMode, temperature, embeddingsMinScore, retrievalLimitMax);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/* 
	 * endpoint to fetch response from the local LLM inference engine only
	 * big payloads generic prompts - huge YML to JSON/ generate Cucumber test cases/ dataMapper
	 */
	@PostMapping("/testLLMServerInvocationOnly")
	public ResponseEntity<String> invokeLLMServerOnlyWithBigPayload(@RequestBody String userPrompt,
																	@RequestParam(value = "customSystemMessage", required = false, defaultValue = "") String customSystemMessage,
																	@RequestParam(value = "llmModel", required = false, defaultValue = "llama3") String llmModel, 
																	@RequestParam(value = "embeddingsMinScore", required = false, defaultValue = "0.5") String embeddingsMinScore,
																    @RequestParam(value = "temperature", required = false, defaultValue = "0.5") String temperature,
																    @RequestParam(value = "retrievalLimitMax", required = false, defaultValue = "5") String retrievalLimitMax,
																    @RequestParam(value = "htmlOutput", required = false, defaultValue = "") String htmlOutput)
																	throws Exception  
	{
		validate(userPrompt);
		
		boolean testMode= true;
		System.out.println("\n---- started invokeLLMServerOnlyWithBigPayload - mode : "+testMode + " llmModel : "+llmModel);
		String response = retrievalSvc.orchestrate(userPrompt, customSystemMessage, "generic", llmModel, testMode, temperature, embeddingsMinScore, retrievalLimitMax);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	private void validate(String userPrompt) throws Exception {
		if(userPrompt == null || "".equals(userPrompt.trim()))
		{
			throw new Exception("User prompt is empty");
		}
	}
}
