package com.genai.llm.privacy.mgt.controller;

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

@RestController
@RequestMapping(value="/gen-ai/v1/llm")
public class DataPrivacyController 
{
	@Autowired
	private RetrievalService retrievalSvc;

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
	endpoint to get a response from the local inference engine
	*/	
	@GetMapping("/retrieve")	
	public ResponseEntity<String> retrieve (@RequestParam("text") String text, @RequestParam(value = "llmModel", required = false, defaultValue = "llama3") String llmModel) throws Exception
	{	
		boolean testMode= true; 
		System.out.println("\n---- started retrieve flow - mode : "+testMode);
		String response = retrievalSvc.orchestrate(text, testMode, llmModel);	
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/retrieve-flowtrain")	
	public ResponseEntity<String> retrieveFlowTrain (@RequestParam("text") String text, @RequestParam(value = "llmModel", required = false, defaultValue = "llama3") String llmModel) throws Exception
	{	
		boolean testMode= true;
		System.out.println("\n---- started retrieveFlowTrain flow - mode : "+testMode);
		String response = retrievalSvc.orchestrateFlowTrain(text, testMode, llmModel);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	//vj11
	/*
	 SAHAB spec info
	*/
	@GetMapping("/retrieve-apiinfo")	
	public ResponseEntity<String> retrieveApiInfo (@RequestParam("text") String text, @RequestParam(value = "llmModel", required = false, defaultValue = "llama3") String llmModel) throws Exception
	{	
		boolean testMode= true;
		System.out.println("\n---- started retrieveApiInfo flow - mode : "+testMode);
		String response = retrievalSvc.orchestrateApiInfo(text, testMode, llmModel);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/*
	 * endpoint to get a response from the VectorDB 
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
	
	/*
	 * endpoint to get a response from the VectorDB 
	 */
	@GetMapping("/closest-semantic-match")   //city employer
	public ResponseEntity<String> match(@RequestParam(required=false, defaultValue="city") String category, @RequestParam("text") String text) 
	{
		boolean testMode= true; 
		System.out.println("\n---- started match - mode : "+testMode);
		String response = retrievalSvc.orchestrateVectorDBOnly(category, text, testMode);
		//String response = ""; //vj1
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/*
	 * endpoint to get a response from the local LLM inference engine 
	 */
	@GetMapping("/testLLMServerInvocationOnly")
	public ResponseEntity<String> invokeLLMServerOnlyWithSmallPayload(@RequestParam("text") String text, @RequestParam(value = "llmModel", required = false, defaultValue = "llama3") String llmModel) throws Exception
	{
		boolean testMode= true; 
		System.out.println("\n---- started invokeLLMServerOnlyWithSmallPayload - mode : "+testMode);
		String response = retrievalSvc.orchestrateLLMServerOnly(text, testMode, llmModel);
		//String response = ""; //vj1
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/*
	 * endpoint to get a response from the local LLM inference engine 
	 */
	@PostMapping("/testLLMServerInvocationOnly")
	public ResponseEntity<String> invokeLLMServerOnlyWithBigPayload(@RequestBody String text, @RequestParam(value = "context", required = false) String context, @RequestParam(value = "llmModel", required = false, defaultValue = "llama3") String llmModel) throws Exception
	{
		boolean testMode= true;
		System.out.println("\n---- started invokeLLMServerOnlyWithBigPayload - mode : "+testMode + " llmModel : "+llmModel);
		String response = retrievalSvc.orchestrateLLMServerOnlyWithBigPayload(text, testMode, context, llmModel);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
