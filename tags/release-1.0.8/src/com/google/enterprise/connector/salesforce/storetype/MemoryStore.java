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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.NumberFormat;

import com.google.enterprise.connector.salesforce.BaseConnector;

/**
 * Implementation of a memory storetype for the connector
 * <p>
 * This store type is entirely in memory and should really delete the store entry after the file is read.
 * TODO: implement a delete method
 */
public class MemoryStore implements IStoreType{
	
	HashMap  hm_docs = new HashMap();

    private Logger logger;
    private BaseConnector connector;
    private int push_counter =0;
    private int pop_counter =0;
    
	public MemoryStore(BaseConnector connector){
		logger = Logger.getLogger(this.getClass().getPackage().getName());
		logger.log(Level.INFO,"Initialize Memory Store  ");
		this.connector = connector;
	}
	
	
	public synchronized DocListEntry getDocsImmediatelyAfter(String checkpoint){
		DocListEntry dret = null;	
		String selectedkey= "";
		logger.log(Level.FINE, "Requsting docs afer  "+ checkpoint );

		Object[] key = hm_docs.keySet().toArray();
		Arrays.sort(key);
		
		if (key.length>0) {
			dret = new DocListEntry((String)key[0],(String)hm_docs.get(key[0]));
			//delete the entry from the hashmap
			pop_counter++;
			logger.log(Level.INFO, "Returning firstentry  "+ (String)key[0]  + " Mem QueueLength " + key.length + "  push/pop_counter " + push_counter +  "/"  + pop_counter);
			hm_docs.remove((String)key[0]);
		}

		return dret;
		}


	public synchronized void setDocList(String checkpoint, String str_store_entry){ 

		Iterator itr = hm_docs.keySet().iterator();
		
		
		//BigDecimal  offset = new BigDecimal(".0000");
		//while (itr.hasNext()){
		//	String key = (String)itr.next();
		//	if (key.startsWith(checkpoint)) {
		//		offset = offset.add(new BigDecimal(".0001"));
		//	}
		//}
		//create a unique numeric key for this entry
		//must always increase
		//String filename = checkpoint+offset; 
		
		HashMap map = new LinkedHashMap();
		List keys = new ArrayList(hm_docs.keySet());
		Collections.sort(keys);
		
		push_counter++;
		BigDecimal  offset = new BigDecimal(".0000" + push_counter);
		checkpoint = checkpoint + offset;		
		hm_docs.put(checkpoint,str_store_entry);
		
		Runtime runtime = Runtime.getRuntime();
		NumberFormat format = NumberFormat.getInstance();

		long freeMemory = runtime.freeMemory();

		logger.log(Level.INFO,"Runtime.freeMemory(): " + format.format((freeMemory) / 1024));
		
		logger.log(Level.INFO,"Adding to MemoryStore:   INDEX VALUE: " + checkpoint + "  push/pop counter: " + push_counter + "/" + pop_counter);
	}
	

}
