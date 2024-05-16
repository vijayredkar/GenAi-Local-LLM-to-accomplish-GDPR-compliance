package com.genai.llm.privacy.mgt.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constants {

	//private static String[] validModelsArr = {"llama2","llama3", "llama3:70b", "phi3:mini"};
	private static String[] validModelsArr = {"llama2","llama3", "phi3:mini"};
	private final List<String> validModelsList = new ArrayList<String>();	
	private static Map<String, String> validLlmModels = new HashMap<String,String>();
	
	public static void addNewModelsToList()
	 {
		Arrays.asList(validModelsArr);
	 }
	
	public static void addModelsToMap()
	 {
		Arrays.asList(validModelsArr)
		       //.forEach(m -> validLlmModels.put(m, m));
				.forEach(m -> createValidLlmModelsMap(m));
	 }	
	
	private static void createValidLlmModelsMap(String modelName) 
	{
		if("llama3".equals(modelName))
		{  
			//vj7
			//validLlmModels.put(modelName, "http://127.0.0.1:11434");  //Locally running instance
			validLlmModels.put(modelName, "http://ollama.bawabaai-gpt.svc.cluster.local:11434");  //PG POD access
			//validLlmModels.put(modelName, "https://ollama-llama3-bawabaai-gpt.pgocp.uat.emiratesnbd.com"); //Local mc access to PG
		}
		else if("llama2".equals(modelName))
		{	
			//vj7
			//validLlmModels.put(modelName, "http://127.0.0.1:11434");  //Locally running instance
			validLlmModels.put(modelName, "http://ollama.bawabaai-gpt.svc.cluster.local:11434");  //PG POD access
			//validLlmModels.put(modelName, "https://ollama-bawabaai-gpt.pgocp.uat.emiratesnbd.com"); //Local mc access to PG
		}
		else if("phi3:mini".equals(modelName))
		{			
			//vj7
			//validLlmModels.put(modelName, "http://127.0.0.1:11434");  //Locally running instance
			validLlmModels.put(modelName, "http://ollama.bawabaai-gpt.svc.cluster.local:11434");  //PG POD access
			//validLlmModels.put(modelName, "https://ollama-bawabaai-gpt.pgocp.uat.emiratesnbd.com"); //Local mc access to PG
		}
	}

	public static Map<String, String> getValidModelsMap()
	 {
		return validLlmModels;
	 }
}
