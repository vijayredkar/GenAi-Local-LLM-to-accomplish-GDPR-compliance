package com.genai.llm.privacy.mgt.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constants {

	private static String[] validModelsArr = {"llama2","llama3", "phi3:mini", "custom-model-1", "model-leap-1", "model-leap-controller-1", "model-leap-handler-1", "model-leap-repository-1", "llama3-70b-model-1", "llama3:70b", "llama3-financial-categorize"};//vj14/13
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
		if("llama3:70b".equals(modelName) || "custom-model-1".equals(modelName) || "llama3-financial-categorize".equals(modelName))//vj14/13   PG-OCP has only big llama3 now. small llama3 was removed due to space constraint
		{  
			//vj7
			//validLlmModels.put(modelName, "http://127.0.0.1:11434");  //Locally running instance
			validLlmModels.put(modelName, "http://ollama-llama3.bawabaai-gpt.svc.cluster.local:11434");  //PG POD access
			//validLlmModels.put(modelName, "https://ollama-llama3-bawabaai-gpt.pgocp.uat.emiratesnbd.com"); //Local mc access to PG
		}
		else if("llama3-70b-model-1".equals(modelName))//vj12
		{	
			//validLlmModels.put(modelName, "http://127.0.0.1:11434");  //Locally running instance
			validLlmModels.put(modelName, "http://ollama-llama3:11434");  //VM POD access
			//validLlmModels.put(modelName, "http://lventibapp501u.uat.emiratesnbd.com:11434"); //Local mc access to VM
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
		//vj9
		else if("model-leap-1".equals(modelName))
		{			
			//vj7
			//validLlmModels.put(modelName, "http://127.0.0.1:11434");  //Locally running instance
			validLlmModels.put(modelName, "http://ollama.bawabaai-gpt.svc.cluster.local:11434");  //PG POD access
			//validLlmModels.put(modelName, "https://ollama-bawabaai-gpt.pgocp.uat.emiratesnbd.com"); //Local mc access to PG
		}
		else if("model-leap-controller-1".equals(modelName))
		{			
			//vj7
			//validLlmModels.put(modelName, "http://127.0.0.1:11434");  //Locally running instance
			validLlmModels.put(modelName, "http://ollama.bawabaai-gpt.svc.cluster.local:11434");  //PG POD access
			//validLlmModels.put(modelName, "https://ollama-bawabaai-gpt.pgocp.uat.emiratesnbd.com"); //Local mc access to PG
		}
		else if("model-leap-handler-1".equals(modelName))
		{			
			//vj7
			//validLlmModels.put(modelName, "http://127.0.0.1:11434");  //Locally running instance
			validLlmModels.put(modelName, "http://ollama.bawabaai-gpt.svc.cluster.local:11434");  //PG POD access
			//validLlmModels.put(modelName, "https://ollama-bawabaai-gpt.pgocp.uat.emiratesnbd.com"); //Local mc access to PG
		}
		else if("model-leap-repository-1".equals(modelName))
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
