package com.genai.llm.privacy.mgt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class AppConfigLoadService 
{	
	@Autowired
	private VectorDataStoreService vectorDataSvc;
	
	/*
	 * On application startup - load default context to Chroma
	 */
	@PostConstruct
	public void loadOnStartup() 
	{	
		System.out.println("\n---- started initial context load on startup");
		
		String currentDir = System.getProperty("user.dir");
		String resoucePath = currentDir + "\\"+ "training-docs\\sensitive-fields-training.txt";
		
		vectorDataSvc.load(resoucePath);		
		System.out.println("---- completed initial context load on startup");
	}		
}