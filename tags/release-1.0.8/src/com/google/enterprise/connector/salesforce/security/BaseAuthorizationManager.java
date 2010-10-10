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

package com.google.enterprise.connector.salesforce.security;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.enterprise.connector.salesforce.BaseConnector;
import com.google.enterprise.connector.salesforce.BaseConstants;
import com.google.enterprise.connector.salesforce.Util;
import com.google.enterprise.connector.salesforce.modules.salesforce.SFQuery;
import com.google.enterprise.connector.spi.AuthenticationIdentity;
import com.google.enterprise.connector.spi.AuthenticationResponse;
import com.google.enterprise.connector.spi.AuthorizationManager;
import com.google.enterprise.connector.spi.AuthorizationResponse;


/**
 * Class implementing the Authorization Iterface of the connector
 * This class is called by the connector when it needs to authenticate a user
 */
public class BaseAuthorizationManager implements AuthorizationManager {
	private BaseConnector connector;
	private Logger logger;
	
	/**
	 * Initialize the authentication manager and pass in the connector
	 * that is in context
	 * @param connector the Connector object that is in context
	 */	
	public BaseAuthorizationManager(BaseConnector connector) {
		logger = Logger.getLogger(this.getClass().getPackage().getName());
		logger.log(Level.INFO, " SalesForceAuthorizationManager initializing");
		this.connector=connector;
	}
	
	/**
	 * Connector manager sends a collection of documentIDs to the connector 
	 * to authorize for an authenticated context
	 *
	 * @param  col Collection  the docIDs to authorize
	 * @param  id AuthenticationIdentity   the identity to auth
	 * @return Collection of docs that are authorized
	 */	
	
    public Collection authorizeDocids(Collection col, AuthenticationIdentity id) {
      logger.log(Level.FINER, " SalesForceAuthorizationManager. authorizeDocids called for " + id.getUsername());

      //first see if we have a callable authorization module to try
      
      String callable_az_module = System.getProperty(BaseConstants.CALLABLE_AZ + "_" + connector.getInstanceName());
      
      if (callable_az_module != null){
	      logger.log(Level.FINE, "Using Loadable Authorization Module : " + callable_az_module);  
			try{
			  	  Class cls = Class.forName(callable_az_module);
				  java.lang.reflect.Constructor co = cls.getConstructor();
				  IAuthorizationModule icau = (IAuthorizationModule)co.newInstance();
				  
				  Collection auth_col =  icau.authorizeDocids(col, id.getUsername());
				  
				  Collection ret_col = new ArrayList();
				  
				  for (Iterator i = auth_col.iterator(); i.hasNext(); ) {
					    String did = (String) i.next();
 		    		    AuthorizationResponse ap = new AuthorizationResponse(true,did);
 		    		    ret_col.add(ap);
				  }
		    		 
				  return ret_col;
				}
			catch (Exception ex) {
				logger.log(Level.SEVERE, "Unable to load Authorization Module " + callable_az_module); 
			}	      
      }
      else {
	      logger.log(Level.FINER, "Using Default Authorization Module");  
      }      
      
      
      Iterator itr = col.iterator();
      logger.log(Level.FINER, " AUTHORIZING  BATCH OF : " + col.size() + " documents");

      
      //vector to hold the list of docs that will eventually get authorized
      Vector v_docIDs = new Vector();
      
      //create a string of 'docid1','docid2','docid3'  to send into the AZ query
      String doc_wildcard = "";
      while(itr.hasNext()){
    	  String docID = (String)itr.next();
    	  v_docIDs.add(docID);
    	  doc_wildcard=doc_wildcard + "'" + docID + "'";
    	  if (itr.hasNext())
    		  doc_wildcard=doc_wildcard + ",";
      }
      
      //initialize the collection for the response
      Collection col_resp = new ArrayList();
	  String query = connector.getAzquery();
	  
	  //substitute the doc IDs into the AZ query
      String modified_az_query= query.replace("$DOCIDS", doc_wildcard);
      modified_az_query= modified_az_query.replace("$USERID", id.getUsername());
      
      logger.log(Level.FINER,"Attempting Authorizing DocList " + modified_az_query);

      	//get ready to submit the query
		      SFQuery sfq = new SFQuery();
		 //get the user's sessionID, login server thats in context
		 //this step maynot be necessary if we use the connector's login context
		 //instead of the users...
		 //TODO: figure out which way is better later on
	    	     Properties session_props = connector.getUserSession(id.getUsername());
	     //not that it matters, how did the user authenticate..
	     //if its strong (i.e, we got a session ID, we can submit a full AZ query)	     	    	    
	    	      String auth_strength = (String)session_props.get(BaseConstants.AUTHENTICATION_TYPE);

	    	      if (auth_strength.equals(BaseConstants.STRONG_AUTHENTICATION)){
	    	    	  logger.log(Level.FINER, "Using Strong Authentication" );
		      
		      try{

		    	  //following section is used if we want to AZ using the connectors authenticated super context
		    	  //its commented out for now but we'll touch on this later
		    	   // if (connector.getSessionID().equalsIgnoreCase("")){
		    	   // 	 SalesForceLogin sfl = new SalesForceLogin(connector.getUsername(),connector.getPassword(),connector.getLoginsrv());
		    	   // 	 if (sfl.isLoggedIn()){
		    	   // 		 connector.setSessionID(sfl.getSessionID());
		    	   // 		 connector.setEndPointServer(sfl.getEndPointServer());
		    	   // 	 }
		    	   //  }
		    	     
		    	     
	    	     //for connector-managed sessions
	    	     //todo figure out someway to purge the older sessions
		    	     
				    	  logger.log(Level.INFO,"Submitting  [" + (String)session_props.getProperty(BaseConstants.LOGIN_SERVER) + "]  [" + (String)session_props.getProperty(BaseConstants.SESSIONID) + "]");
				    	 org.w3c.dom.Document az_resp =  sfq.submitStatement(modified_az_query, BaseConstants.QUERY_TYPE, (String)session_props.getProperty(BaseConstants.LOGIN_SERVER) , (String)session_props.getProperty(BaseConstants.SESSIONID));
		    	     
				    	 
				    	 //if  using system session to check AZ
			    	 //org.w3c.dom.Document az_resp =  sfq.submitStatement(modified_az_query, BaseConstants.QUERY_TYPE,connector.getEndPointServer() , connector.getSessionID());	    	  
			    	 
				    //now transform the AZ SOAP response into the canonical form using
				    //the AZ XLST provided.
							String encodedXSLT =connector.getAzxslt();
							byte[] decode = org.apache.commons.codec.binary.Base64.decodeBase64(encodedXSLT.getBytes());
				    	 			    				    	 
							org.w3c.dom.Document az_xsl = Util.XMLStringtoDoc(new String(decode));

			    	 logger.log(Level.FINER,"AZ Query Response " + Util.XMLDoctoString(az_resp));
			    	 Document tx_xml = Util.TransformDoctoDoc(az_resp,az_xsl);
			    	 tx_xml.getDocumentElement().normalize(); 
				     logger.log(Level.FINER,"AZ transform result for "+ id.getUsername() + "  " + Util.XMLDoctoString(tx_xml));
				     
				     //TODO...figure out why I can use tx_xml as a document by itself
				     //have to resort to convert tx_xml  to string and then back to Document 
				     //for some reason
				       DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				       DocumentBuilder db = dbf.newDocumentBuilder();
				       StringBuffer sb1 = new StringBuffer( Util.XMLDoctoString(tx_xml));
				       ByteArrayInputStream bis = new ByteArrayInputStream(sb1.toString().getBytes("UTF-8"));		        				        
				       Document doc = db.parse(bis);		      
				       doc.getDocumentElement().normalize();
				    				   	   
				        //now that the soap response is transformed, extract the documents that were
				       //authorized from the canonical XML AZ form
				        NodeList nl_documents = doc.getElementsByTagName("azdecisions");
				        //get the NodeList under <document>
				        HashMap hm_azdecisions = new HashMap();;
				        Node n_documents = nl_documents.item(0);
					 	for (int i=0;i<n_documents.getChildNodes().getLength(); i++)
					 	{						 						 		
					 		Node n_doc = n_documents.getChildNodes().item(i);
					 		if (n_doc.getNodeType() == Node.ELEMENT_NODE) {					 								 			
					 		      TransformerFactory transfac = TransformerFactory.newInstance();
					 		      Transformer trans = transfac.newTransformer();
					 		      trans.setOutputProperty(OutputKeys.INDENT, "yes");

					 			if (n_doc.getNodeName().equalsIgnoreCase("docID")){
					 				//ok...so this doc ID was returned so we'll allow/permit this
					 				hm_azdecisions.put(n_doc.getFirstChild().getNodeValue(),"PERMIT");
					 			}
					 		}
					 		}
					 //for each doc ID we got in, iterate and authorize the docs that we got back..
					 //TODO, ofcourse we could just forego this loop
					 	//and simply iterate the hm_azdecisions hashmap to create col_resp
				     for (int i=0; i< v_docIDs.size();i++){
				    	 //a doc id we got to test
				    	 String in_docID = (String)v_docIDs.get(i);
				         //if the doc exists in the set we authorized
				    	 //the more i write this the more i want to just iterate the hm_azdecisions
				    	 //and get it over with...i'll work on that next week
				    	 if (hm_azdecisions.containsKey(in_docID)){
				    		 AuthorizationResponse ap = new AuthorizationResponse(true,in_docID);
				    		 col_resp.add(ap);
				    	 }				    	 
				     }			

		      }
		      catch (Exception bex){
		    	  logger.log(Level.SEVERE, " ERROR SUBMITTING AZ Query " + bex);
		      }
	    	      }
	    	      //if the user was just authenticated
	    	      //we don't have the sessionid so we'lll authorize all docs.
	    	      
	    	      //WEAK_AUTH flag should never get set since
	    	      //we've failed the AU attempt in the BaseAuthenticationManager already
		      else if (auth_strength.equals(BaseConstants.WEAK_AUTHENTICATION)) {
		    	  logger.log(Level.FINER, "Using Weak Authentication" );
				      if (connector.allowWeakAuth()){
				    	  
				    	  col_resp = new ArrayList();
					      for (int i=0; i<v_docIDs.size(); i++){
					    	  String docID = (String)v_docIDs.get(i);
					    	  logger.log(Level.FINER,"Authorizing " + docID);
					    	  AuthorizationResponse ap = new AuthorizationResponse(true,docID);
					      	col_resp.add(ap);
					      }
				      }
		      }
      return col_resp;
    }
  }