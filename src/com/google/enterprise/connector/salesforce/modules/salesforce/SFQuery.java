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

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.enterprise.connector.salesforce.BaseConstants;
import com.google.enterprise.connector.salesforce.exception.BaseException;

/**
 * Class which queries salesforce
 * <p>
 * </p>
 */
public class SFQuery {

	private Logger logger;
	private int records_processed;
	private int total_records;
	
    public SFQuery (){
    	logger = Logger.getLogger(this.getClass().getPackage().getName());
    }
    
    
	 /**
	   * Submits a query 
	   * @param qvar the query to submit
	   * @param  query_type (QUERY_TYPE or QUERYMORE_TYPE from the BaseConstants class)
	   * @param system_server the  system server to use
	   * @param sessionID the session number for the connector
	   * 
	   */    
	public Document submitStatement(String qvar, int query_type, String system_server, String sessionID) throws BaseException{

				try {
			         SOAPConnectionFactory soapConnFactory =  SOAPConnectionFactory.newInstance();
		             SOAPConnection connection =  soapConnFactory.createConnection();
		             MessageFactory messageFactory = MessageFactory.newInstance();
		             SOAPMessage message = messageFactory.createMessage(); 

		             SOAPPart soapPart =     message.getSOAPPart();		             
		             SOAPEnvelope envelope = soapPart.getEnvelope();

		             Hashtable userHeaderTable = 
		            	 (Hashtable) message.getProperty(MessageContext.HTTP_REQUEST_HEADERS);

		             if(userHeaderTable == null){
		            	 userHeaderTable = 	 new Hashtable();}            
		             userHeaderTable.put("Accept-Encoding", "gzip");
				     
		             message.setProperty(MessageContext.HTTP_REQUEST_HEADERS, userHeaderTable);		             
		             
		             
		             //add the sessionID
		             SOAPHeader header = envelope.getHeader();
		             SOAPElement headerElement = header.addChildElement(envelope.createName("SessionHeader","","urn:enterprise.soap.sforce.com"));		             
		             headerElement.addChildElement("sessionId").addTextNode(sessionID); 
		             
		             SOAPBody body =         envelope.getBody();
		             SOAPElement bodyElement;
		             
		             
		             //specify the type of query to run
		       if (query_type == BaseConstants.QUERY_TYPE) {
		              bodyElement = body.addChildElement(envelope.createName("query","","urn:enterprise.soap.sforce.com"));
		              bodyElement.addChildElement("queryString").addTextNode(qvar);
		       }
		       
		       if (query_type == BaseConstants.QUERYMORE_TYPE) {		    	      	   
		    	   bodyElement =  body.addChildElement(envelope.createName("queryMore","","urn:enterprise.soap.sforce.com"));
			       bodyElement.addChildElement("queryLocator").addTextNode(qvar);
		       }
		       message.saveChanges(); 		       
		       
		       ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
		       message.writeTo(bos);
		       logger.log(Level.FINER, "Query Request XML \n" + bos.toString("UTF-8"));
		       logger.log(Level.FINER,"Sending Query Request");
		       String destination =system_server;

		       SOAPMessage reply = connection.call(message, destination);
		        connection.close();         

		        //Create the transformer
		        TransformerFactory transformerFactory = TransformerFactory.newInstance();
		        Transformer transformer =  transformerFactory.newTransformer();
		        
		        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		        
		        		        
		        //get the SOAP reply
		        Source sourceContent = reply.getSOAPPart().getContent();

		        //convert it to text
		        Level level = logger.getLevel();
		        while (level == null && logger.getParent() != null) {
		            logger = logger.getParent();
		            level = logger.getLevel();
		        }

		        if (level == Level.FINE || level == Level.FINEST ) {
			        StringWriter xsw = new StringWriter();
			        StreamResult xstrresult = new StreamResult(xsw);
			        transformer.transform(sourceContent, xstrresult);	
				    String xstrXML = xsw.toString();			    
				    logger.log(Level.FINEST, "Query Response XML : " + xstrXML);
		        }
		        
			    logger.log(Level.INFO, "Received Query Response." );
			    //or convert the soap reply into XML Document
		        DOMResult xmlresult = new DOMResult();
		        transformer.transform(sourceContent, xmlresult); 
			    Document soap_response_doc = (Document)xmlresult.getNode();  
			    
			    //is the response a soapfault
			    NodeList nlfault = reply.getSOAPBody().getElementsByTagName("soapenv:Fault");
			    
			    //return the error string as an exception
			    if (nlfault.getLength() >0){
			        StringWriter sw = new StringWriter();
			        StreamResult strresult = new StreamResult(sw);
			        transformer.transform(sourceContent, strresult);	
				    String strXML = sw.toString();	
			    	throw new BaseException("Query Error: " + strXML);
			    }
			    logger.log(Level.FINEST,"Transformed Response: " + com.google.enterprise.connector.salesforce.Util.XMLDoctoString(soap_response_doc));
			    return  soap_response_doc;

		   }
		   catch (Exception ex) {					
					//logger.log(Level.ERROR, "Submit Query Statement Error " + ex);
					throw new BaseException("Submit Statement Error " + ex.getMessage());
				}
	  }   
	    
	
	
	 /**
	   * Tries to find the querylocator in the response
	   */	
	public String getQueryLocator(Document soap_response_doc) {
			    //check to see if this query is done or if we need to utilize a cursor
			   
			   boolean has_more =false;
			   String salesforce_query_cursor = null;
		 		try{
				NodeList nlresponse = soap_response_doc.getElementsByTagName("queryResponse");
				if (nlresponse.getLength()==0)
					nlresponse = soap_response_doc.getElementsByTagName("queryMoreResponse");
			 	Node qrespnode  = nlresponse.item(0);
			 	NodeList nlresult = qrespnode.getChildNodes();	
			 	Node nresult = nlresult.item(0);

			 	
			 	for (int i=0;i<nresult.getChildNodes().getLength(); i++)
			 	{			 	

			 		Node n = nresult.getChildNodes().item(i);
		            if (n.getNodeType() == Node.ELEMENT_NODE) {  
		            	if (n.getNodeName().equalsIgnoreCase("done")) {
		            		if (n.getFirstChild().getNodeValue().equalsIgnoreCase("false"))
		            				has_more = true;
		            	}
		            	if (n.getNodeName().equalsIgnoreCase("queryLocator")) {
		            		//check to see if the nodevalue is null or not
		            		if (n.hasChildNodes())
		            			salesforce_query_cursor = n.getFirstChild().getNodeValue();		   
		            	}
		            	if (n.getNodeName().equalsIgnoreCase("size")) {
		            		//check to see if the nodevalue is null or not
		            		if (n.hasChildNodes()) {
		            			String str_size = n.getFirstChild().getNodeValue();
		            			total_records =  new Integer(str_size).intValue();
		            			logger.log(Level.INFO, "Total Records found matching query " + total_records);
		            		}
		            	}	            	
		            	if (n.getNodeName().equalsIgnoreCase("records")) {
		            		//check to see if the nodevalue is null or not
		            		if (n.hasChildNodes()) {
		            			int isizeze = n.getFirstChild().getChildNodes().getLength();
		            			this.records_processed = records_processed  + isizeze;
		            			
		            		}
		            	}	            	

		            }			 		
			 	}
			 	logger.log(Level.INFO, "Processing " + this.records_processed + " of " + this.total_records   + "  queryCursor:[" + salesforce_query_cursor + "]");

			 	
			}
			catch (Exception e) {
	          logger.log(Level.FINEST, "Error getting Cases " + e);
	          System.out.println("\n\n");
			}
		    	

			   return salesforce_query_cursor;
		   }
		   
	
}
