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
// See the License for the specific language 


package com.google.enterprise.connector.salesforce;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import com.google.enterprise.connector.salesforce.exception.BaseException;
import com.google.enterprise.connector.salesforce.modules.salesforce.SFQuery;
import com.google.enterprise.connector.salesforce.modules.salesforce.SalesForceLogin;



/**
 * Test utility used to login to salesforce and run a sample query
 * optionally, the user can also specify an XSLT to transform the SOAP response from
 * The log file is called TestUtility.log.<br/>
 * Usage:   
 * -->  To test a SalesForce Query: <br/>
 *         edit init.properties and run <br/>
 *         <li/><i>java -cp .:salesforceconnector.jar com.google.enterprise.connector.salesforce.TestUtility init.properties</i><br/>
 * -->  To test a SalesForce Query and a response XSLT:<br/>
 *         <li/><i>java -cp .:salesforceconnector.jar com.google.enterprise.connector.salesforce.TestUtility init.properties XLSTFile.xsl </i><br/>
 */

public class TestUtility {
	private Logger logger;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TestUtility sfr = new TestUtility(args);
	}
	
	
	public  TestUtility(String[] args){			
			        try
			        {
			        	
			        	//initialize the logging mechanisms
					    logger = Logger.getLogger(this.getClass().getPackage().getName());
						ConsoleHandler console = new ConsoleHandler();
						FileHandler filelogger = new FileHandler("TestUtility.log");
						
						//set the default logger to the INFO (this is a test utility afterall)
						logger.setLevel(Level.INFO);
						console.setFormatter(new SimpleFormatter());
						filelogger.setFormatter(new SimpleFormatter());		
						console.setLevel(Level.FINE);
						//logger.addHandler(console);
						logger.addHandler(filelogger);
						
						if (args.length<1){
							logger.log(Level.INFO,"Usage:  com.google.enterprise.connector.salesforce.TestUtility <init.properties>  (optional)<queryXSLT>");
							logger.log(Level.INFO,"where configfile.txt is: ");
							logger.log(Level.INFO,"   username=: ");
							logger.log(Level.INFO,"   password=: ");
							logger.log(Level.INFO,"   loginserver=: ");
							logger.log(Level.INFO,"   query=: ");
							System.exit(0);
						}
						
						//info about a goog base64 converter (no line breaks!)
						logger.log(Level.FINE,"Base64 Encocer: http://base64-encoder-online.waraxe.us/");

						int cursor_counter = 1;
						
						Properties props = new Properties();
						props.load(new FileInputStream(args[0]));
						
						String username = props.getProperty("username");
						String password = props.getProperty("password");
						String loginserver = props.getProperty("loginserver");
						String query = props.getProperty("query");

						boolean do_transform = false;
						
						if (args.length>1)
							do_transform = true;
						
					    Calendar cal = Calendar.getInstance();
					    SimpleDateFormat sdf = new SimpleDateFormat("hh_mm_ss");
					    String strDirectory = sdf.format(cal.getTime());
					    
					    boolean success = (new File(strDirectory)).mkdir();
					    if (success) {
					    	logger.log(Level.INFO,("Directory: " + strDirectory + " created"));
					    }
					    else {
					    	logger.log(Level.SEVERE,("Unable to create Directory: " + strDirectory + " ...exiting"));
					    	System.exit(0);
					    }
						
						//do a salesforce login given the props
						SalesForceLogin sfl = new SalesForceLogin(username,password,loginserver);
						
						if (sfl.isLoggedIn()){
							//if logged in, run a query

							SFQuery sfq = new SFQuery();
							sfq.submitStatement(query, BaseConstants.QUERY_TYPE, sfl.getEndPointServer(), sfl.getSessionID());
							Document soap_response_doc = sfq.submitStatement(query, BaseConstants.QUERY_TYPE, sfl.getEndPointServer(), sfl.getSessionID());
							String xml_response = Util.XMLDoctoString(soap_response_doc);
							
					    	//do we have a cursor in the response
						    String q_locator = sfq.getQueryLocator(soap_response_doc);
						    						    						    						    
						    String filename = strDirectory + System.getProperty("file.separator") + "response_" + cursor_counter  + ".xml";						    
						    logger.log(Level.INFO, "Writing response to " + filename);
						    FileWriter fstream = new FileWriter(filename);
					        BufferedWriter out = new BufferedWriter(fstream);
					        out.write(xml_response);
					        out.close();
					    							
							if (do_transform) {
							    String tx_resp = this.TransformDoc(args[1],soap_response_doc);
							    filename = strDirectory + System.getProperty("file.separator") + "tx_" + cursor_counter + ".xml";						    
							    logger.log(Level.INFO, "Writing transform to " + filename);
							    fstream = new FileWriter(filename);
						        out = new BufferedWriter(fstream);
						        out.write(tx_resp);
						        out.close();
							}
							
						    //if we do, then query again
						    while (q_locator != null) {
							    logger.log(Level.FINE, "Query locator cursor found " + q_locator);
							    cursor_counter++;
							     soap_response_doc =sfq.submitStatement(q_locator, BaseConstants.QUERYMORE_TYPE,sfl.getEndPointServer(), sfl.getSessionID()) ;							     
								 if (soap_response_doc==null)
									   	throw new BaseException("QUERY RESPONSE document is null");
								xml_response = Util.XMLDoctoString(soap_response_doc);	
								logger.log(Level.INFO, "Query Response XML " + xml_response);
								
							    filename = strDirectory + System.getProperty("file.separator") + "response_" + cursor_counter  + ".xml";						    
							    logger.log(Level.INFO, "Writing response to " + filename);
							    fstream = new FileWriter(filename);
						        out = new BufferedWriter(fstream);
						        out.write(xml_response);
						        out.close();
								
							    q_locator = sfq.getQueryLocator(soap_response_doc);	
								if (do_transform) {
								    String tx_resp = this.TransformDoc(args[1],soap_response_doc);
								    filename = strDirectory + System.getProperty("file.separator") + "tx_" + cursor_counter + ".xml";						    
								    logger.log(Level.INFO, "Writing transform to " + filename);
								    fstream = new FileWriter(filename);
							        out = new BufferedWriter(fstream);
							        out.write(tx_resp);
							        out.close();
								}

						    } 
						}
						
			        }
			        catch (Exception ex) {
			        	logger.log(Level.SEVERE, "Error running tests: " + ex);
			        }					      
	}
	
	private String TransformDoc(String xsltfile,Document soap_response_doc){
		
		try{
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer(new StreamSource(new FileInputStream(xsltfile)));
			 
			    StringBuilder xsltcontents = new StringBuilder();
			    BufferedReader input =  new BufferedReader(new FileReader(xsltfile));

			    String line = null; 
			        while (( line = input.readLine()) != null){
			        	xsltcontents.append(line);
			        	xsltcontents.append(System.getProperty("line.separator"));
			        }
			        
			 Document XSLTDoc = Util.XMLStringtoDoc(xsltcontents.toString());		 							 
			 Document doc_transformed = Util.TransformDoctoDoc(soap_response_doc, XSLTDoc);
			 
			 return Util.XMLDoctoString(doc_transformed);
			 //logger.log(Level.INFO,"Transformed Response " + Util.XMLDoctoString(doc_transformed));
		}
	catch (Exception ex){
		logger.log(Level.SEVERE, "Error Transforming tests: " + ex);
		return null;
	}
	}
}
