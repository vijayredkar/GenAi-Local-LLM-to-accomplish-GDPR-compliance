package com.genai.llm.privacy.mgt.service;

public class TokenizationResponse {
	private static final long serialVersionUID = 1L;
	
	private String token;	
	private String status;
	private String reason;
	//private Status[] status;
	
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	
	/*
	public Status[] getStatus() {
		return status;
	}
	public void setStatus(Status[] status) {
		this.status = status;
	}
	 */
}
