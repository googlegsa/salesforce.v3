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
 * Interface the different store types must implement
 * <p>
 * </p>
 */
public interface IStoreType {

	 /** Get the VERY next document set after this checkpoint date
	   * @param checkpoint get the very NEXT document set from the store after htis checkpoint date
	   */  
	public  DocListEntry getDocsImmediatelyAfter(String checkpoint);
	
	 /** 
	  * Saves the document entry into the store and associate it with this checkpoint
	  */  	
	public  void setDocList(String checkpoint, String str_store_entry);
}
