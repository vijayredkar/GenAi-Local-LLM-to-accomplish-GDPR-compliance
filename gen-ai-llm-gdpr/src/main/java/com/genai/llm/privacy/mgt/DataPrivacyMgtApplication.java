package com.genai.llm.privacy.mgt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.BufferedReader;
import java.io. InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.security. KeyManagementException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class DataPrivacyMgtApplication 
{	
public static void main(String[] args) 
	{
		System.out.println("\n******************** Please ensure that your Vector DB istance is running and reachable ********************");
		System.out.println("\n******************** Please ensure that your Vector DB istance is running and reachable ********************");
		System.out.println("\n******************** Please ensure that your Vector DB istance is running and reachable ********************");
	      //vj12
		/*
		String deploymentEnvPgOcp = "Bawaba-PG-OCP";
		String deploymentEnvPgVM = "Bawaba-PG-VM";
		*/
		//vj15
		List<String> validDeploymentEnvs = new ArrayList<String>();
		validDeploymentEnvs.add("Bawaba-PG-OCP_internal");//connect to PG-OCP with internal OCP svc only exposed endpoints
		validDeploymentEnvs.add("Bawaba-PG-OCP_external");//connect to PG-OCP with external ENBD machine exposed endpoints
		validDeploymentEnvs.add("Bawaba-PG-VM_internal"); //connect to PG-VM  with internal container only exposed endpoints
		validDeploymentEnvs.add("Bawaba-PG-VM_external"); //connect to PG-VM  with external ENBD machine exposed endpoints
		
		String deploymentEnv = "Bawaba-PG-VM_internal";               //default	 
		if(args.length > 0)
		{
			if(!"".equals(args[0].trim()))
			{
			 System.out.println("---- args[0] "+args[0] + "   args[1] "+args[1]);
			 deploymentEnv = args[0] + "_"+args[1]; //Bawaba-PG-VM_external     Bawaba-PG-VM_internal
			}
			
			if(!validDeploymentEnvs.contains(deploymentEnv))
			{
				System.out.println("**** Error - invalid deployment env. Exiting ....");
				System.exit(-1);
			}
		}	
		System.out.println("---- deploymentEnv is set to: "+deploymentEnv);
		System.setProperty("deployment_env", deploymentEnv);		
		System.out.println("---- System.getProperty(deployment_env): "+System.getProperty("deployment_env"));
				
		SpringApplication.run(DataPrivacyMgtApplication.class, args);		
	}
}
