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
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.enterprise.connector.salesforce.exception.BaseException;

/**
 * Class that logs into salesforce and provides the sessionID and server this login
 * should use for the query
 * <p>
 * If there is a login failure, the getResponseXML() method will return the response
 * </p>
 */
public class SalesForceLogin {

	   private boolean is_logged_in=false;
	    private String str_sessionID = null;
	    private String login_server;
	    private String endpoint_server;
	    private String login_response;
	    Logger logger;
	    
	    /**
	     * Constructor does the login automatically
	     * <p>
	     * </p>
	     * @throws BaseException
	     * @param userName   
	     * @param pwd
	     * @param login_server  to use
	     */	    
		public SalesForceLogin ( String userName, String pwd, String login_server)throws BaseException
		{		    
			logger =  Logger.getLogger(this.getClass().getName());
			StringWriter sw = new StringWriter();
			try {

				this.login_server = login_server;
				
		        //Create the transformer for displaying request/response xml
		        TransformerFactory transformerFactory = TransformerFactory.newInstance();
		        Transformer transformer =  transformerFactory.newTransformer();
		        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

		        StreamResult result = new StreamResult(sw);
				
		         SOAPConnectionFactory soapConnFactory =  SOAPConnectionFactory.newInstance();
	             SOAPConnection connection =  soapConnFactory.createConnection();
	             MessageFactory messageFactory = MessageFactory.newInstance();
	             SOAPMessage message = messageFactory.createMessage(); 
	             
	             Hashtable userHeaderTable = 
	            	 (Hashtable) message.getProperty(MessageContext.HTTP_REQUEST_HEADERS);

	             if(userHeaderTable == null){
	            	 userHeaderTable = 	 new Hashtable();}            
	             userHeaderTable.put("Accept-Encoding", "gzip");
			     
	             message.setProperty(MessageContext.HTTP_REQUEST_HEADERS, userHeaderTable);
	             
	             SOAPPart soapPart =     message.getSOAPPart();
	             SOAPEnvelope envelope = soapPart.getEnvelope();
	             SOAPBody body =         envelope.getBody();

	             SOAPElement bodyElement = 
	                 body.addChildElement(envelope.createName("login","","urn:enterprise.soap.sforce.com"));
	             		bodyElement.addChildElement("username").addTextNode(userName);
	             		bodyElement.addChildElement("password").addTextNode(pwd);

	       message.saveChanges(); 

	       ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
	       message.writeTo(bos);
	       logger.log(Level.FINEST, "Login Request XML \n" + bos.toString("UTF-8"));
	       logger.log(Level.INFO, "Login Server URL: " + this.login_server);
	       String destination = this.login_server;
	       
	       
	       SOAPMessage reply = connection.call(message, destination);
	       
	        connection.close();         
	        
	        Source sourceContent = reply.getSOAPPart().getContent();

	        transformer.transform(sourceContent, result);

	        logger.log(Level.INFO, "Login Response XML \n" + sw.toString());
	        
	        this.login_response = sw.toString();

	        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	        DocumentBuilder db = dbf.newDocumentBuilder();
	        StringBuffer sb1 = new StringBuffer(sw.toString());
	        ByteArrayInputStream bis = new ByteArrayInputStream(sb1.toString().getBytes("UTF-8"));

	        Document doc = db.parse(bis);
	        doc.getDocumentElement().normalize();

	        NodeList nlresult = doc.getElementsByTagName("result");

	        if (nlresult!=null){
	        	if (nlresult.getLength()>0) {
	        		Node nresult = nlresult.item(0);
				 	for (int i=0;i<nresult.getChildNodes().getLength(); i++)
				 	{
				 		Node n = nresult.getChildNodes().item(i);
			            if (n.getNodeType() == Node.ELEMENT_NODE) {  
			            	if (n.getNodeName().equalsIgnoreCase("sessionId")) {
			            		this.str_sessionID = n.getFirstChild().getNodeValue();
			            	}
			            	if (n.getNodeName().equalsIgnoreCase("serverUrl")) {
			            		this.endpoint_server=  n.getFirstChild().getNodeValue();	   
			            	}
			            }			 		
				 	}
	        	}
	        }
	        
		 	logger.log(Level.INFO, "SESSIONID: " + this.str_sessionID);
		 	logger.log(Level.INFO, "ServerURL: " + this.endpoint_server);
		 	
			 	if (str_sessionID==null) {
			 		this.is_logged_in=false;
			 		logger.log(Level.SEVERE, "SessionID Is null ");
			 		//throw new BaseException("SessionID is null: " + sw.toString());
			 	} 
			 	else{
			 		this.is_logged_in = true;
			 	}
			}
			catch (Exception e) {
				logger.log(Level.SEVERE, "Login error " + e.getMessage() + "  " +  sw);
				this.is_logged_in= false;
				throw new BaseException("Login Error : " + e.getMessage()+ "  " +  sw.toString());
			}
		}

		public boolean isLoggedIn() {
			return is_logged_in;
		}

		public String getSessionID(){
			return str_sessionID;
		}
		
		public String getEndPointServer() {
			return this.endpoint_server;
		}

	    
	    /**
	     * Returns the XMl format of the login response
	     * Mostly used to parse/identify why the login failed
	     * <p>
	     * </p>
	     * @return the login response in XML format
	     */	    
		public String getLoginResponseXML() {
			return this.login_response;
		}
}
