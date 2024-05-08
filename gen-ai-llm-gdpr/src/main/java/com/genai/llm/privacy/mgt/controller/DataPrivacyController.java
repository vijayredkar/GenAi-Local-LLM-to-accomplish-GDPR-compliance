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
import com.genai.llm.privacy.mgt.service.VectorDataStoreService;

@RestController
@RequestMapping(value="/gen-ai/v1/llm")
public class DataPrivacyController 
{
	@Autowired //vj
	private RetrievalService retrievalSvc;

	/* vj1
	@Autowired
	private VectorDataStoreService vectorDataSvc;
	*/
	
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
		
		//vectorDataSvc.load(fileNameWithFullPath); vj1
		
		response = "Vector DB new context loaded";	
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	/*
	endpoint to get a response from the local inference engine
	*/
	
	@GetMapping("/retrieve")	
	public ResponseEntity<String> retrieve (@RequestParam("text") String text)
	{	
		boolean testMode= true; //vj2
		System.out.println("\n---- started retrieve flow - mode : "+testMode);
		String response = retrievalSvc.orchestrate(text, testMode);	
		//string response; //vj1
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	//vj5
	/*
	 * endpoint to get a response from the VectorDB 
	 */
	@GetMapping("/testVectorDBInvocationOnly")
	public ResponseEntity<String> invokeVectorDBOnly(@RequestParam("text") String text) 
	{
		boolean testMode= true; //vj2
		System.out.println("\n---- started invokeVectorDBOnly - mode : "+testMode);
		String response = retrievalSvc.orchestrateVectorDBOnly(text, testMode);//vj6
		//String response = ""; //vj1
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	
	//vj6
		/*
		 * endpoint to get a response from the VectorDB 
		 */
		@GetMapping("/closest-semantic-match")   //city employer
		public ResponseEntity<String> match(@RequestParam(required=false, defaultValue="city") String category, @RequestParam("text") String text) 
		{
			boolean testMode= true; //vj2
			System.out.println("\n---- started match - mode : "+testMode);
			String response = retrievalSvc.orchestrateVectorDBOnly(category, text, testMode);
			//String response = ""; //vj1
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	
	
	//vj5
	/*
	 * endpoint to get a response from the local LLM inference engine 
	 */
	@GetMapping("/testLLMServerInvocationOnly")
	public ResponseEntity<String> invokeLLMServerOnlyWithSmallPayload(@RequestParam("text") String text)
	{
		boolean testMode= true; //vj2
		System.out.println("\n---- started invokeLLMServerOnlyWithSmallPayload - mode : "+testMode);
		String response = retrievalSvc.orchestrateLLMServerOnly(text, testMode);
		//String response = ""; //vj1
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	
	
	//vj5
		/*
		 * endpoint to get a response from the local LLM inference engine 
		 */
		@PostMapping("/testLLMServerInvocationOnly")
		public ResponseEntity<String> invokeLLMServerOnlyWithBigPayload(@RequestBody String text, @RequestParam(value = "context", required = false) String context)
		{
			boolean testMode= true; //vj2
			System.out.println("\n---- started invokeLLMServerOnlyWithBigPayload - mode : "+testMode);
			String response = retrievalSvc.orchestrateLLMServerOnlyWithBigPayload(text, testMode, context);
			//String response = ""; //vj1
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
}
