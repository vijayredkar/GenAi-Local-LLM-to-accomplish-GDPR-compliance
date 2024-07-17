package com.genai.llm.privacy.mgt.service;

import java.util.List;

public class TokenizationRequest {
	private static final long serialVersionUID = 1L;
	
	private List<TokenizationData> tokenizationData;

	public List<TokenizationData> getTokenizationData() {
		return tokenizationData;
	}

	public void setTokenizationData(List<TokenizationData> tokenizationData) {
		this.tokenizationData = tokenizationData;
	}

	@Override
	public String toString() {
		return "TokenizationRequest [tokenizationData=" + tokenizationData + "]";
	}
}
