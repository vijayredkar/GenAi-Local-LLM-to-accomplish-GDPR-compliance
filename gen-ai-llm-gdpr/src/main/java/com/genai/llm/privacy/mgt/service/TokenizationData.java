package com.genai.llm.privacy.mgt.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//24C
public class TokenizationData 
{
	    private static final long serialVersionUID = 1L;
		
		String urc; 
		String projectName = "project-uat-*,enbd-tibco-*";
	    Integer maxPastDaysFromNow = 14;
	    String startTime ;
	    String endTime =  LocalDateTime.now().toString(); 
	   
	    
	    //String filter = "[\"error\"]";   // "[\"error,info\"]";	 
	    List<String> filter = new ArrayList<String>(); //"[\"error\"]";   // "[\"error,info\"]";	 
	    
	    
	    public String getStartTime() {
			return startTime;
		}
		public void setStartTime(String startTime) {
			this.startTime = startTime;
		}
		public String getEndTime() {
			return endTime;
		}
		
		public void setEndTime(String endTime) {
			this.endTime = endTime;
		}
				
		public String getUrc() {
			return urc;
		}
		public void setUrc(String urc) {
			this.urc = urc;
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
		public List<String> getFilter() 
		{
			filter.add("error");
			//filter.add("info");
			return filter;
		}
		public void setFilter(List<String> filter) {
			this.filter = filter;
		}
		
		@Override
		public String toString() {
			return "TokenizationData [urc=" + urc + ", projectName=" + projectName + ", maxPastDaysFromNow="
					+ maxPastDaysFromNow + ", startTime=" + startTime + ", endTime=" + endTime + ", filter=" + filter + "]";
		}	
	}
