package com.genai.llm.privacy.mgt.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class Constants {
	
	@Value("${valid.llm.models.env.pgocp}")
	private String validLlmModelsListEnvPgOcp;
	
	@Value("${external.access.llm.models.pgocp.1}")
	private String externalAccessLlmModelsPgOcp1;
	
	@Value("${internal.access.llm.models.pgocp.2}")
	private String internalAccessLlmModelsPgOcp2;
	
	@Value("${external.access.llm.models.pgocp.3}")
	private String externalAccessLlmModelsPgOcp3;
	
	@Value("${internal.access.llm.models.pgocp.4}")
	private String internalAccessLlmModelsPgOcp4;
	
	@Value("${external.access.llm.models.pgocp.5}")
	private String externalAccessLlmModelsPgOcp5;
	
	@Value("${internal.access.llm.models.pgocp.6}")
	private String internalAccessLlmModelsPgOcp6;


	@Value("${valid.llm.models.env.pgvm}")
	private String validLlmModelsListEnvPgVm;
	
	@Value("${external.access.llm.models.vm.1}")
	private String externalAccessLlmModelsVm1;
	
	@Value("${internal.access.llm.models.vm.2}")
	private String internalAccessLlmModelsVm2;
			
	
	@Value("${vector.db.index.examineflow}")
	private String vectorDbIndexExamineflow;
	
	@Value("${vector.db.index.knowledgebase}")
	private String vectorDbIndexKnowledgeBase;
	
	@Value("${vector.db.index.logsextract}") //vj24B
	private String vectorDbIndexLogsExtract;
	
	
	
	public String customSystemMessageKnwBase = 
			" You are a helpful assistant. You will be provided documentation on standard procedures to be followed by an employee.\r\n" + 
			" This vast documentation is tedious for the employee to comprehend. "
			+ " Your task is to extract information specific to the question asked by the user. "
			//+ " If the documentation is less than 95% relevant to the question asked then simple respond with \"NOT RELEVANT\" ."
			+ " If the documentation is less than 95% relevant to the question asked then simple respond with a blank statement."
			+ " If the documentation is more than 94% relevant then extract only the specific information that the user has asked for. "
			+ " Provide your response in points format within 100 words only.\r\n" + 
			" Here is the company documentation:\n";
	//vj24B
	public String customSystemMessageLogsRca = "You are a helpful assistant. You are a helpful assistant. You will be provided application logs in CSV format. "
												+ "Your role is production support analyst who is an expert in analyzing logs. "
												+ "Your task is to extract information specific to the question asked by the user. "
												+ "If the logs segment is less than 90% relevant to the question asked then simply respond with a blank statement. "
												+ "If the documentation is more than 90% relevant then extract only the specific information that the user has asked for. "
												+ "Provide concise and to the point response in 3 lines only. Here are the application logs:";
	 
	public String customUserQuestionLogsRca = "What is root causeof the error and which system did it originate from";
	
	//externalAccessLlmModelsPgOcp1
	// external.access.llm.models.pgocp.3=https://ollama-big-bawabaai-gpt.pgocp.uat.emiratesnbd.com#llama3:70b
	private static Map<String, String> modelEnvPgOcpMap = new HashMap<String,String>();
	private static Map<String, String> modelEnvPgVmMap = new HashMap<String,String>();
	private static Map<String, String> categoryVectorDbMap = new HashMap<String,String>();
	
	public void createModelsToEnvMap()
	 {
		prepareEnvConnInternal();
		prepareEnvConnExternal();
		
		System.out.println("models env map: \n" );
		System.out.println("PG-OCP: "+ modelEnvPgOcpMap.toString()+"\n");
		System.out.println("PG-VM:  " +modelEnvPgVmMap.toString()+"\n");
	 }	
	
	
	private void prepareEnvConnInternal() 
	{
		if(System.getProperty("deployment_env").contains("internal"))
		{
			String[] elements = null;		
			String[] models = null;
						
			elements = internalAccessLlmModelsPgOcp2.split("#");  
			final String envResourceUrl2 = elements[0];                           
			models = elements[1].split(",");                      
			
			Arrays.asList(models)
					.forEach(m -> modelEnvPgOcpMap.put(m, envResourceUrl2)); 
			
			
			
			elements = internalAccessLlmModelsPgOcp4.split("#");  
			final String envResourceUrl4 = elements[0];                           
			models = elements[1].split(",");                      
			
			Arrays.asList(models)
					.forEach(m -> modelEnvPgOcpMap.put(m, envResourceUrl4));
			
			
			
			elements = internalAccessLlmModelsPgOcp6.split("#");  
			final String envResourceUrl6 = elements[0];                           
			models = elements[1].split(",");                      
			
			Arrays.asList(models)
					.forEach(m -> modelEnvPgOcpMap.put(m, envResourceUrl6)); 
			
			
			elements = internalAccessLlmModelsVm2.split("#");  
			final String envResourceUrl8 = elements[0];                           
			models = elements[1].split(",");                      
			
			Arrays.asList(models)
					.forEach(m -> modelEnvPgVmMap.put(m, envResourceUrl8));
		}		
	}

	private void prepareEnvConnExternal() 
	{	
		if(System.getProperty("deployment_env").contains("external"))
		{
			String[] elements = null;		
			String[] models = null;
			
			elements = externalAccessLlmModelsPgOcp1.split("#");  //https://ollama-big-bawabaai-gpt.pgocp.uat.emiratesnbd.com   llama3:70b,llama10
			final String envResourceUrl1 = elements[0];           //https://ollama-big-bawabaai-gpt.pgocp.uat.emiratesnbd.com
			models = elements[1].split(",");                      //llama3:70b   llama10
			
			Arrays.asList(models)
					.forEach(m -> modelEnvPgOcpMap.put(m, envResourceUrl1));  // llama3:70b -> https://ollama-big-bawabaai-gpt.pgocp.uat.emiratesnbd.com
						
			
			elements = externalAccessLlmModelsPgOcp3.split("#");  
			final String envResourceUrl3 = elements[0];                           
			models = elements[1].split(",");                      
			
			Arrays.asList(models)
					.forEach(m -> modelEnvPgOcpMap.put(m, envResourceUrl3)); 
			
			
			elements = externalAccessLlmModelsPgOcp5.split("#");  
			final String envResourceUrl5 = elements[0];                           
			models = elements[1].split(",");                      
			
			Arrays.asList(models)
					.forEach(m -> modelEnvPgOcpMap.put(m, envResourceUrl5)); 
			
			
			elements = externalAccessLlmModelsVm1.split("#");  
			final String envResourceUrl7 = elements[0];                           
			models = elements[1].split(",");                      
			
			Arrays.asList(models)
					.forEach(m -> modelEnvPgVmMap.put(m, envResourceUrl7));
		}	
	}

	
	public boolean isModelValid(String modelName, String env)
	 {
		boolean result = false;
		
		if(modelName == null)
		{
			return false;
		}
				
		
		if(env.contains("Bawaba-PG-OCP"))
		{
			result = validLlmModelsListEnvPgOcp.contains(modelName.trim());
		}
		else if (env.contains("Bawaba-PG-VM"))
		{
			result = validLlmModelsListEnvPgVm.contains(modelName.trim());	
		}
		
		return result;
	 }
	
	
	public String getResourceByModelName(String modelName, String env) throws Exception
	 {
		System.out.println("---- getResourceByModelName modelName and env: "+modelName + " - "+ env);
		String result = null;
		
		if(modelName == null)
		{
			throw new Exception("**** LLM Model name is not specified");
		}
		//vj23
		if("llama3:70b".equals(modelName.trim())) //special case: massive 40GB llama3:70b model enabled in the PG-VM internal only
		{
			System.out.println("---- Got modelName: "+ modelName +  " \nCurrent env: " + env + " \nRouting to Bawaba-PG-VM");
			result = internalAccessLlmModelsVm2.split("#")[0]; //default PG-VM internal
			if(env.contains("external"))  //from ENBD mc
			{
				result = externalAccessLlmModelsVm1.split("#")[0]; //for local mc testing -> PG-VM external
			}
			
			System.out.println("---- Explicitly connecting resource: "+ result);		
			System.out.println("---- getResourceByModelName got valid match");
		}
		else if(env.contains("Bawaba-PG-OCP"))
		{ 
			result = modelEnvPgOcpMap.get(modelName.trim());
			System.out.println("---- getResourceByModelName got valid match");
		}
		else if(env.contains("Bawaba-PG-VM"))
		{
			result = modelEnvPgVmMap.get(modelName.trim());	
			System.out.println("---- getResourceByModelName got valid match");
		}
		
		return result;
	 }
	
	
	public void createCategoryVectorDbMap()
	 {
		categoryVectorDbMap.put("examineFlow", vectorDbIndexExamineflow);
		categoryVectorDbMap.put("knowledgebase", vectorDbIndexKnowledgeBase);
		categoryVectorDbMap.put("logsextract", vectorDbIndexLogsExtract); //vj24B
	 }
	
	
	public Map<String, String> getCategoryVectorDbMap()
	 {
		return categoryVectorDbMap;
	 }
}
