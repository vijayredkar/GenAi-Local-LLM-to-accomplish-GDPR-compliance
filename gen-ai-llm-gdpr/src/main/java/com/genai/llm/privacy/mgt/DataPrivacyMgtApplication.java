package com.genai.llm.privacy.mgt;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.Timeout;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;






import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.cert.X509Certificate;
import java.util.concurrent.ThreadPoolExecutor;

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

	@Bean(name = "restTemplate")//vj24B
    public RestTemplate restTemplateForCallingOauthService() {
        RestTemplate restTemplate = new RestTemplate(buildClientHttpRequestFactory(6000, 6000, 6000));
		//RestTemplate restTemplate = new RestTemplate();
		return restTemplate;
    }
	
	private ClientHttpRequestFactory buildClientHttpRequestFactory(int connectTimeout, int connectionRequestTimeout,
            															int readTimeout) 
	{
		HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		clientHttpRequestFactory.setConnectTimeout(connectTimeout);
		clientHttpRequestFactory.setConnectionRequestTimeout(connectionRequestTimeout);
		//clientHttpRequestFactory.setReadTimeout(readTimeout);
		clientHttpRequestFactory.setHttpClient(prepareHttpClient());
		return new BufferingClientHttpRequestFactory(clientHttpRequestFactory);
	}

	private HttpClient prepareHttpClient() {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                //.setDefaultCredentialsProvider(credentialsProvider())
                .setDefaultRequestConfig(prepareHttpRequestConfig()).setConnectionManager(prepareClientConnManager());
        return httpClientBuilder.build();
    }

    private HttpClientConnectionManager prepareClientConnManager() {
        PoolingHttpClientConnectionManager httpClientConnectionManager = prepareConnectionManager();
        return httpClientConnectionManager;
    }

    private PoolingHttpClientConnectionManager prepareConnectionManager() {

        return new PoolingHttpClientConnectionManager(prepareSslDisabledRegistry());
    }

    private RequestConfig prepareHttpRequestConfig() {
        return RequestConfig.custom().setConnectTimeout(Timeout.ofSeconds(30000))
                .setConnectionRequestTimeout(Timeout.ofSeconds(30000))
                .build();
    }

    private Registry<ConnectionSocketFactory> prepareSslDisabledRegistry() {
        try {
            TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE).register("https", csf).build();

            return socketFactoryRegistry;
        } catch (Exception e) {
            System.out.println("Exception during DebitCardsApplication.prepareSslDisabledRegistry");
            return null;
        }
    }	
}
