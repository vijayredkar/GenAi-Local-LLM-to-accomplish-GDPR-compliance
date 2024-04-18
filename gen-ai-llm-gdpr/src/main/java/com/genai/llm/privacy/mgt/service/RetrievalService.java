package com.genai.llm.privacy.mgt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
	
	/*
	 * LLM - RAG orchestration operations
	 */
	public String orchestrate(String text, boolean testMode) //vj2
	{	
		System.out.println("\n---- started LLM - RAG orchestrations");
		String userPrompt = text;
		
		//--step -1  : enhance the user prompt with the context information from the DB 
		String contextFromVectorDb = vectorDataSvc.retrieve(contextType, userPrompt); //vj3
		//String contextFromVectorDb = ""; //vj1
		
		String promptWithFullContext = systemMessage + " " + contextFromVectorDb + "  "+  "\"" + userPrompt + "\"";
		System.out.println("---- constructed RAG promptWithFullContext \n"+promptWithFullContext);		

		//vj4
		//--step -2 : invoke the LLM inferencing engine with the fully constructed prompt
		String response = largeLangModelSvc.generate(promptWithFullContext, testMode);
		//String response = contextFromVectorDb;
		//System.out.println("**** Ollama LLM server de-activated");
		
		System.out.println("---- completed LLM - RAG orchestrations with response : \n"+ response);
		return response;
	}
}
