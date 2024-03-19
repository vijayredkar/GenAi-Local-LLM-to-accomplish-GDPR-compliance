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

@SpringBootApplication
public class DataPrivacyMgtApplication 
{	
  public static void main(String[] args)
   {
	System.out.println("\n******************** Please ensure that your Vector DB istance is running and reachable ********************");
	System.out.println("\n******************** Please ensure that your Vector DB istance is running and reachable ********************");
	System.out.println("\n******************** Please ensure that your Vector DB istance is running and reachable ********************");
	SpringApplication.run(DataPrivacyMgtApplication.class, args);
   }  
}