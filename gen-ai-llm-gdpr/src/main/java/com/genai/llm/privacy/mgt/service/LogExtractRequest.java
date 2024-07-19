package com.genai.llm.privacy.mgt.service;

//24C
public class LogExtractRequest 
{	
	private static final long serialVersionUID = 1L;
	
	String urc;
    String filter;
	String projectName;
	Integer maxPastDaysFromNow;
	
	public String getUrc() {
		return urc;
	}
	public void setUrc(String urc) {
		this.urc = urc;
	}
	public String getFilter() {
		return filter;
	}
	public void setFilter(String filter) {
		this.filter = filter;
	}
	public String getProjectName() {
		return projectName;
	}
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public Integer getMaxPastDaysFromNow() {
		return maxPastDaysFromNow;
	}
	public void setMaxPastDaysFromNow(Integer maxPastDaysFromNow) {
		this.maxPastDaysFromNow = maxPastDaysFromNow;
	}
	
	@Override
	public String toString() {
		return "LogExtractRequest [urc=" + urc + ", filter=" + filter + ", projectName=" + projectName
				+ ", maxPastDaysFromNow=" + maxPastDaysFromNow + "]";
	}
}
