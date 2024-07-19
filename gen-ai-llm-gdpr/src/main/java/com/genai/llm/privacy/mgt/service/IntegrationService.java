package com.genai.llm.privacy.mgt.service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


import jakarta.annotation.PostConstruct;

import org.apache.hc.client5.http.utils.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//vj25
@Service
public class IntegrationService
{
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	Environment env;
	
	private static final Logger LOGGER = LogManager.getLogger();
	//vj24B	
	public String getTokenizationAuth()
	{
		String url = "https://rhsso-uat.clouduat.emiratesnbd.com/auth/realms/enbd/protocol/openid-connect/token";  //move to config/vault
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		String authForKibana = null;
		
		try 
		{
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, prepareAuthBody(), String.class);
			LOGGER.info("---- getTokenizationAuth status: "+response.getStatusCode());
						
			String authTokenBody = response.getBody();
			String authTokenPortion = authTokenBody.split(":")[1];
			authForKibana = authTokenPortion.split(",")[0];
			authForKibana = authForKibana.replaceAll("\"","");			
		}
		catch(Exception e)
		{
			LOGGER.error("error: "+e); //throw custom error
		}		
		
		return authForKibana;
	} 
	
	private HttpEntity<MultiValueMap<String, String>> prepareAuthBody() 
	{
		MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
				
		requestParams.add("client_id", env.getProperty("client_id_kibana_uat"));
		requestParams.add("client_secret", env.getProperty("client_secret_kibana_uat"));		
		requestParams.add("grant_type", "client_credentials");		
	
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestParams, headers);
		return request;
	}
	
	public String getKibanaLogsByUrc(LogExtractRequest logExtractReq, String authToken)
	{
		LOGGER.info("---- getKibanaLogsByUrc input "+logExtractReq.toString());
		String url = "https://bawabauat.clouduat.emiratesnbd.com/engagement/kibana-log-search/v1/search";  //move to config/vault
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		String logs = null;
		
		try 
		{
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, prepareRequestBody(logExtractReq, authToken), String.class);
			LOGGER.info("---- content: "+response.getBody());
			LOGGER.info("---- status: "+response.getStatusCode());
						
			logs = response.getBody();
			LOGGER.info("\n\n---- extacted logs "+logs); 
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			LOGGER.error("error: "+e); //throw custom error
		}		
		
		return logs;
	} 

	private HttpEntity<TokenizationData> prepareRequestBody(LogExtractRequest logExtractReq, String authToken) //24C
	{
		TokenizationData tokenizationData = new TokenizationData();
		tokenizationData.setUrc(logExtractReq.getUrc());
		
		String projectName =  logExtractReq.getProjectName();
		String filter =  logExtractReq.getFilter(); //["error,info"]
		Integer maxDuration =  logExtractReq.getMaxPastDaysFromNow();
		
		tokenizationData.setProjectName(handleInput(projectName, tokenizationData.getProjectName()));
		tokenizationData.setFilter(handleFilter(filter, tokenizationData.getFilter()));
		
		maxDuration = Integer.parseInt(handleInput(maxDuration, tokenizationData.getMaxPastDaysFromNow().toString()));		
		tokenizationData.setEndTime(handleDateTime(LocalDateTime.now())); //2024-07-14T05:40:38.006Z
		tokenizationData.setStartTime(handleDateTime(LocalDateTime.now().minusDays(maxDuration)));
		
		System.out.println("---- request body \n "+tokenizationData.toString());
				
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/json");
		headers.set("Accept", "application/json");
		headers.set("Authorization", "Bearer "+authToken);
		headers.set("Channel-Id", "BOT");
		headers.set("Financial-Id", "EBI");
		headers.set("Client-Timestamp", "1514764800000");
		headers.set("Client-Ip", "1.1.1.1");		
		headers.set("Unique-Reference-Code", "Logs-"+UUID.randomUUID().getMostSignificantBits());
		HttpEntity<TokenizationData> request = new HttpEntity<TokenizationData>(tokenizationData,headers);
		return request; // ["error"]   ["error"]
	}

	private List<String> handleFilter(String value, List<String> defaultValue) 
	{
		if(value == null)
		{
			return defaultValue; //default set to error
		}
		
		//user specified 
		List<String> filterList = new ArrayList<String>();
		String[] portions = value.split(",");   // error,info,warn
		Arrays.asList(portions)
			  .forEach(p -> filterList.add(p));	
		
		return filterList;
	}

	private String handleDateTime(LocalDateTime dateTime) 
	{
		return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")); 
	}

	private String handleInput(Object value, String defaultValue)
	{
		if(value == null)
		{
			return defaultValue;
		}
		return value.toString();
	}
}
