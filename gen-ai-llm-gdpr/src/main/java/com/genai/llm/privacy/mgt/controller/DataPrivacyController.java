package com.genai.llm.privacy.mgt.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.genai.llm.privacy.mgt.utils.Constants;//vj18

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

@RestController
@RequestMapping(value="/gen-ai/v1/llm")
public class DataPrivacyController 
{
	@Autowired
	private RetrievalService retrievalSvc;
	
	@Autowired //vj4
	private VectorDataStoreService vectorDataSvc;
	
	@Autowired //vj18
	private Constants constants;
	

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
										   @RequestParam(value = "llmModel", required = false, defaultValue = "llama3:70b") String llmModel) 
										   throws Exception  //vj18
	{
		boolean testMode= true; 
		System.out.println("\n---- started retrieve flow - mode : "+testMode);
		String response = retrievalSvc.orchestrate(userPrompt, customSystemMessage, category, llmModel, testMode);//vj14
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	
	
	
	//vj18
	/*
	*  extract specific knowledge from vast documentation/Confluence 
	*/
	@PostMapping("/knowledgebase")	
	public ResponseEntity<String> knowledgeExtract(@RequestBody String documentContent,
												   @RequestParam(value = "documentTitle", required = false, defaultValue = "") String documentTitle,
												   @RequestParam(value = "customUserQuestion", required = false, defaultValue = "") String customUserQuestion,
												   @RequestParam(value = "customSystemMessage", required = false, defaultValue = "") String customSystemMessage,
												   @RequestParam(value = "documentRepoName", required = false, defaultValue = "") String documentRepoName,
												   @RequestParam(value = "llmModel", required = false, defaultValue = "llama3:70b") String llmModel)
											  throws Exception
	{	
		boolean testMode= true;
		System.out.println("\n---- started explainflow - mode : "+testMode);
		String response = null;		
		
		//stage-1: data prepare - load to VectorDB - confluence text i/p is vast. Split in to segments and save in to vectorDB per document title
		if("".equals(documentRepoName.trim())) // implies doc has been saved to vector DB previously. Do not store again
		{
			vectorDataSvc.loadData(documentContent, "knowledgebase", true, documentTitle);
		}
		
		//stage-2: narrow down to relevant text segments only - fetch from vectorDB per document title only those segments relevant to user's question
		List<EmbeddingMatch<TextSegment>> matchingRecords = vectorDataSvc.fetchRecords("knowledgebase", customUserQuestion, documentTitle);
		
		//stage 3:  1st level of LLM refinement - retain only those segments that are > 95% relevant
		String userQuestionForKnwBase = " Here is the user's question:\n" + customUserQuestion +"\n";
		StringBuilder responseBldr = new StringBuilder();
		matchingRecords.forEach(m -> { 
									
									
									System.out.println("---- m \n\n"+m.embedded().text());
									String enhancedPrompt = constants.customSystemMessageKnwBase +" \n "+ m.embedded().text() +" \n "+userQuestionForKnwBase;		
									try {
											String response1 = retrievalSvc.orchestrateLLMServerOnly(enhancedPrompt, testMode, llmModel, "generic");
											responseBldr.append(response1).append("\n");
											System.out.println("---- response1: \n "+response1);
									} catch (Exception e) 
									{
										e.printStackTrace();
									}     
		} );
		
		response =  responseBldr.toString();
		
		//stage 4: 2nd level of LLM refinement - run user's question on the consolidated LLM 1st level response 
		String consolidatedEnhancedPrompt = constants.customSystemMessageKnwBase +" \n "+ response +" \n "+userQuestionForKnwBase;
		try 
		{
			String response2 = retrievalSvc.orchestrateLLMServerOnly(consolidatedEnhancedPrompt, testMode, llmModel, "generic");
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
	
	
	
	
	
	
	
	
	
	

	/* vj14
	 * Devathon prototype - external txfrs/IBAN/NRE flow explanations 
	 */
	@GetMapping("/retrieve-flowtrain")	
	public ResponseEntity<String> retrieveFlowTrain (@RequestParam("text") String text, @RequestParam(value = "llmModel", required = false, defaultValue = "llama3:70b") String llmModel) throws Exception  //vj14/13
	{	
		boolean testMode= true;
		System.out.println("\n---- started retrieveFlowTrain flow - mode : "+testMode);
		String response = retrievalSvc.orchestrateFlowTrain(text, testMode, llmModel, "flowtrain");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	//vj11
	/*
	* retrieve SAHAB spec info
	*/
	@GetMapping("/retrieve-apiinfo")	
	public ResponseEntity<String> retrieveApiInfo (@RequestParam("text") String text, @RequestParam(value = "llmModel", required = false, defaultValue = "llama3:70b") String llmModel) throws Exception  //vj14/13
	{	
		boolean testMode= true;
		System.out.println("\n---- started retrieveApiInfo flow - mode : "+testMode);
		String response = retrievalSvc.orchestrateApiInfo(text, testMode, llmModel, "apiinfo");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/*
	 * endpoint to fetch response directly from the VectorDB only
	 */
	@GetMapping("/testVectorDBInvocationOnly")
	public ResponseEntity<String> invokeVectorDBOnly(@RequestParam("text") String text) 
	{
		boolean testMode= true; 
		System.out.println("\n---- started invokeVectorDBOnly - mode : "+testMode);
		String response = retrievalSvc.orchestrateVectorDBOnly(text, testMode);
		//String response = ""; //vj1
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/* vj14
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
	
	//vj14
	/*
	*  explain business logic executed in an application flow - fetch debit cards status, limits fetch, create debit card
	*/
	@GetMapping("/explainflow")	
	public ResponseEntity<String> explainflow(@RequestParam(value = "userPrompt", required = true) String userPrompt,
											  @RequestParam(value = "customSystemMessage", required = false, defaultValue = "") String customSystemMessage,
											  @RequestParam(value = "llmModel", required = false, defaultValue = "llama3:70b") String llmModel) 
											  throws Exception  //vj14/13
									
	{	
		boolean testMode= true;
		System.out.println("\n---- started explainflow - mode : "+testMode);
		String response = null;		
		
		//stage-1 response: very detailed based off the interpreted bytecode instructions from the file  
		response = retrievalSvc.orchestrate(userPrompt, customSystemMessage,  "explainflow", llmModel, testMode);
		
		//stage-2 response: summarized to make it readable for generic audience
		System.out.println("\n\n ---- Final summmary of the execution flow ----"); //vj14
		String instruction = "You are a helpful assistant. Please summarize the below execution flow in simple language. "
							  + "Specify the Java classes, methods and subsystems involved. "
							  + "Display your response in 3 sections under the headers Execution Flow, Exception Handling and Return Result"
							  //+ "Restrict your response to 5 lines only. "
							  ;		
		String enhancedPrompt = instruction+" \n "+response;
		response = retrievalSvc.orchestrateLLMServerOnly(enhancedPrompt, testMode, llmModel, "generic");//vj14 summarize the o/p further
		
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/*
	 * endpoint to fetch response directly from the VectorDB only - city, employer names semantic match
	 */
	@GetMapping("/closest-semantic-match")   //city employer
	public ResponseEntity<String> match(@RequestParam(required=false, defaultValue="city") String category, @RequestParam("text") String text) 
	{
		boolean testMode= true; 
		System.out.println("\n---- started match - mode : "+testMode);
		String response = retrievalSvc.orchestrateVectorDBOnly(category, text, testMode);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/*
	 * endpoint to fetch response from the local LLM inference engine only - small payload generic prompts
	 */
	@GetMapping("/testLLMServerInvocationOnly")
	public ResponseEntity<String> invokeLLMServerOnlyWithSmallPayload(@RequestParam(value = "userPrompt", required = true) String userPrompt, 
																	  @RequestParam(value = "customSystemMessage", required = false, defaultValue = "") String customSystemMessage,
																	  @RequestParam(value = "llmModel", required = false, defaultValue = "llama3:70b") String llmModel)
																	  throws Exception  //vj14/13
	{
		boolean testMode= true; 
		System.out.println("\n---- started invokeLLMServerOnlyWithSmallPayload - mode : "+testMode);		
		String response = retrievalSvc.orchestrate(userPrompt, customSystemMessage, "generic", llmModel, testMode);//vj14
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/* vj14
	 * endpoint to fetch response from the local LLM inference engine only
	 * big payloads generic prompts - huge YML to JSON/ generate Cucumber test cases/ dataMapper
	 */
	@PostMapping("/testLLMServerInvocationOnly")
	public ResponseEntity<String> invokeLLMServerOnlyWithBigPayload(@RequestBody String userPrompt,
																	@RequestParam(value = "customSystemMessage", required = false, defaultValue = "") String customSystemMessage,
																	@RequestParam(value = "llmModel", required = false, defaultValue = "llama3:70b") String llmModel) 
																	throws Exception  //vj14/13
	{
		validate(userPrompt);
		
		boolean testMode= true;
		System.out.println("\n---- started invokeLLMServerOnlyWithBigPayload - mode : "+testMode + " llmModel : "+llmModel);
		String response = retrievalSvc.orchestrate(userPrompt, customSystemMessage, "generic", llmModel, testMode);//vj14
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	private void validate(String userPrompt) throws Exception {
		if(userPrompt == null || "".equals(userPrompt.trim()))
		{
			throw new Exception("User prompt is empty");
		}
	}	
}
