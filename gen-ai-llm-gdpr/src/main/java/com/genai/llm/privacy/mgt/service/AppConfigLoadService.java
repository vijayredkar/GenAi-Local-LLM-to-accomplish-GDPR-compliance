package com.genai.llm.privacy.mgt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype. Service;

import com.genai.llm.privacy.mgt.utils.Constants;

import jakarta.annotation. PostConstruct;

@Service
public class AppConfigLoadService
{
	@Autowired //vj1
	private VectorDataStoreService vectorDataSvc;
	
	
	@Autowired
	private Constants constants;
	/*
	* On application startup load default context to VectorDB
	*/	
	@PostConstruct
	public void loadonStartup ()
	{
		boolean testMode = true; //vj1	
		//boolean testMode" false; //vj1
		
		System.out.println("\n started initial context load on startup");
		
		String resourcePath = "";//vj1	
		if(!testMode)
		{
			String currentDir = System.getProperty("user.dir");	
			resourcePath = currentDir + "\\" + "training-docs\\sensitive-fields-training.txt"; //vj1		
			System.out.println("resoucePath" + resourcePath);
			
			String resourcePath1 = currentDir +"/"+ "training-docs/sensitive fields-training.txt"; //vj1		
			System.out.println("resourcePath1" + resourcePath1);
			
			String resourcePath2 = getClass().getClassLoader ().getResource("application.properties").getFile();		
			System.out.println("resourcePath2: "+resourcePath2);
		}
			
		vectorDataSvc.load(resourcePath, true ); //vj1		
		//String filePath = getClass().getClassLoader ().getResource (filetiame].getPile ();	
		
		
		constants.createModelsToEnvMap();//vj15
		System.out.println(" createModelsToEnvMap done");	
		
		System.out.println(" completed initial context load on startup");
		}
}
