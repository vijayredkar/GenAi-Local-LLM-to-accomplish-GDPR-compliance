package com.genai.llm.privacy.mgt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation. PostMapping;
import org.springframework.web.bind.annotation. RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind. annotation. RestController;
import com.genai.llm.privacy.mgt.service.RetrievalService;
import com.genai.llm.privacy.mgt.service.VectorDataStoreService;

@RestController
@RequestMapping(value="/gen-ai/v1/lla")
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
}
