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

package com.google.enterprise.connector.salesforce.modules.salesforce;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import com.google.enterprise.connector.salesforce.BaseConnector;
import com.google.enterprise.connector.salesforce.BaseConstants;
import com.google.enterprise.connector.salesforce.BaseTraversalManager;
import com.google.enterprise.connector.salesforce.Util;
import com.google.enterprise.connector.salesforce.exception.BaseException;
import com.google.enterprise.connector.salesforce.storetype.IStoreType;

/**
 * Class invoked with the Quartz scheduler to query salesforce
 * <p>
 * </p>
 */
public class SalesForceModule  {
	
	private BaseConnector connector;
	private Logger logger;
	private SalesForceLogin sfl;
	private BaseTraversalManager btm;
	

	 /**
	   * Constructor which sets the BaseTraversalManager
	   * @param btm BaseTraversalManager storing the username/session info
	   */	
	public SalesForceModule(BaseTraversalManager btm){
		logger = Logger.getLogger(this.getClass().getPackage().getName());
		this.btm = btm;
		this.connector = btm.getConnector();	

	}

	public boolean login(){
		try {
			 sfl = new SalesForceLogin(connector.getUsername(),connector.getPassword(), connector.getLoginsrv());	 
		}
		catch (BaseException be) {
			logger.log(Level.SEVERE, "Unable to login: " + be);
			return false;
		}
		return true;
	}
	
	public String getSessionID(){
		return sfl.getSessionID();
	}
	
	public String getEndPointServer(){
		return sfl.getEndPointServer();
	}
	
	

	 /**
	   * Populate the store with data we get from salesforce
	   * @param store IStoreType  the store type to populate
	   */		
	public void populateStore(IStoreType store) {
		    
		logger.log(Level.FINER, "Enter populateStore");
		    Date now =  new java.util.Date();
			StringBuffer sb = new StringBuffer();

			logger.log(Level.INFO, "Submitting query");
			
			String query = connector.getQuery();
			String ckpt_dt = connector.getLastsync();
			logger.log(Level.FINER," QUERY: " + connector.getQuery());
			logger.log(Level.FINER," checkpoint : " + connector.getLastsync());
			
			//$LAST_SYNC_DATE is a wildcard we substitute into the query
			String last_sync_date = Util.convertCheckPoint_Date_to_SF_Date_(ckpt_dt);
        	String modified_sf_query= query.replace("$LAST_SYNC_DATE", last_sync_date);
			
        	logger.log(Level.FINER," modified : " + modified_sf_query);
			   
			   try {			    

				   modified_sf_query = java.net.URLDecoder.decode(modified_sf_query,"UTF-8");
				
				   //get ready to query 
				   SFQuery sfq = new SFQuery();
				   //submit the query and get the XML Document response
				   Document soap_response_doc = sfq.submitStatement(modified_sf_query,BaseConstants.QUERY_TYPE, sfl.getEndPointServer(), sfl.getSessionID());
   
				   if (soap_response_doc==null)
					   	throw new BaseException("QUERY RESPONSE document is null");
				   
				   ///TODO:
				   // DTD validate soap_response_doc here

				   //send the raw soap response into the store
			    	this.pushBatch(store, now, soap_response_doc);
		
			    	//do we have a cursor in the response
				    String q_locator = sfq.getQueryLocator(soap_response_doc);
    
				    //if we do, then query again
				    while (q_locator != null) {
					    logger.log(Level.INFO, "Query locator cursor found " + q_locator);
					     soap_response_doc =sfq.submitStatement(q_locator, BaseConstants.QUERYMORE_TYPE,sfl.getEndPointServer(), sfl.getSessionID()) ;
					     
						 if (soap_response_doc==null)
							   	throw new BaseException("QUERY RESPONSE document is null");
						 
						   ///TODO:
						   // DTD validate soap_response_doc here
						 
						 //send the nth response from the cursor into the store
				    		this.pushBatch(store, now,  soap_response_doc); 
						   
					     q_locator = sfq.getQueryLocator(soap_response_doc);				     
					     

				    }

				    logger.log(Level.INFO, "Query Complete");
				   }
				catch (BaseException e) {
		           logger.log(Level.SEVERE,"SFException  to continue Processing Records " + e.getMessage());
				}
				catch (Exception e) {
			           logger.log(Level.SEVERE,"Exception  to continue Processing Records " + e.getMessage());
					}

	}
	
	 /**
	   * Push the XML response into the store
	   * @param store IStoreType  the store type to populate
	   * @param crawl_date the time Salesforce was queried
	   * @param docXML the raw SOAP response from the query
	   */
  private void pushBatch(IStoreType store, Date crawl_date,  Document docXML){
		try {			
				logger.log(Level.FINER, "Got XML DOC batch from SalesForce");							
				String str_xml = Util.XMLDoctoString(docXML);	
				
		        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		        DocumentBuilder db = dbf.newDocumentBuilder();
		        StringBuffer sb1 = new StringBuffer(str_xml.toString());
		        ByteArrayInputStream bis = new ByteArrayInputStream(sb1.toString().getBytes("UTF-8"));		        
		        Document doc = db.parse(bis);		      
		        doc.getDocumentElement().normalize();
		        
				  Transformer transformer = TransformerFactory.newInstance().newTransformer();
			      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			      
				  StreamResult result = new StreamResult(new StringWriter());
				  DOMSource source = new DOMSource(doc);
				  transformer.transform(source, result);
				  	        
			 	String str_chkpt = Util.getNumericString_from_Date(crawl_date);
			 	//save the document
				store.setDocList(str_chkpt,result.getWriter().toString());				
				logger.log(Level.FINER, "Exit populateStore");
		    }
		    catch (Exception ex){
		    	logger.log(Level.SEVERE," ERROR populateStore " + ex);
		    }
  }

	

    
    

}
