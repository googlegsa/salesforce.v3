// Copyright 2007-2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.connector.salesforce.storetype;

/**
 * A single doc list entry which stores the XML response and the time it was 
 * retrieved from salesforce
 * <p>
 * </p>
 */
public class DocListEntry {

	private String checkpoint;
	private String str_response;
	public DocListEntry(String checkpoint, String response_XML){
		this.checkpoint = checkpoint;
		this.str_response = response_XML;
	}
	
	public String getCheckpoint(){
		return this.checkpoint;
	}
	
	public String getResponseXML(){
		return this.str_response;
	}
	
}
