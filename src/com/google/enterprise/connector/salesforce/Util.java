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


package com.google.enterprise.connector.salesforce;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

/**
 * Utility class with some static methods to conver Dates, XML ,etc
 */
public class Util {

	public Logger logger;
	
	public Util() {
		logger = Logger.getLogger(this.getClass().getPackage().getName());
	}
	
	
	  /**
	   * Returns the instance name from the springworking directory string
	   * the working directory ends with /WEB-INF/connectors/salesforce-connector/<instancename>
	   * so this routine returns the <instancename> portion
	   * @return the instance name alone
	   */	
	  public static String getInstanceNameFromWorkingDir(String working_dir) {
		  String[] sarr = null;		  
			if (System.getProperty("file.separator").equals("\\"))
				sarr = working_dir.split("\\" + System.getProperty("file.separator"));
			
			if (System.getProperty("file.separator").equals("/"))
				sarr = working_dir.split(System.getProperty("file.separator"));		

			String rst= sarr[sarr.length-1];
	    return rst;
	  }	
	  
	  /**
	   * Converts a string format from "yyyyMMddHHmmss" into a java Date object thats
	   * @return java date object
	   */
	  public static Date getNumericDate_from_String(String in_date) {
		  try{
	            DateFormat df = new SimpleDateFormat(BaseConstants.CHECKPOINT_DATE_FORMAT);
	            Date dt = df.parse(in_date);
	            return dt;       
		  }
		  catch (Exception ex){
			  Logger.getLogger(Util.class.getName()).log(Level.SEVERE,"Error getting Formatted Date " + ex);
		  }
	    return null;
	  }	
	  
	  /**
	   * Converts a Date object into a String object thats formated as yyyyMMddHHmmss
	   * @return String java date represented as yyyyMMddHHmmss
	   */	  
	  public static String getNumericString_from_Date(Date in_date){
		  try{
			  SimpleDateFormat df = new SimpleDateFormat(BaseConstants.CHECKPOINT_DATE_FORMAT);
		        StringBuilder conv = new StringBuilder( df.format( in_date ) );
	          return conv.toString();   
		  }
		  catch (Exception ex){
			  Logger.getLogger(Util.class.getName()).log(Level.SEVERE,"Error getting Formatted Date " + ex);
		  }
	    return null;		  
	  }

	  /**
	   * Converts Salesforce Date format (yyyy-MM-dd'T'HH:mm:ss'Z') into local  time
	   * 
	   * @return String java date long format
	   */
		public static String convertSF_GMTToLocalTime(String olddate) {

	        try {
		
	        	if (olddate.indexOf(".")>0)
	        		olddate = olddate.substring(0, olddate.indexOf("."));
	        	
	        	if (olddate.indexOf("Z")>0)
	        		olddate = olddate.substring(0, olddate.indexOf("Z"));
	        	
	        	
	     	   DateFormat dt_from = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");          	         		
	     	   dt_from.setTimeZone(TimeZone.getTimeZone("GMT"));

	            Date date2 = dt_from.parse (olddate);
	            
	            return date2.toString();

	        } catch (Exception e) {
	        	Logger.getLogger(Util.class.getName()).log(Level.SEVERE,"Error during Date Transform "+ e);
	        }
	        return "";
		}

		  /**
		   * Converts Salesforce Date format (yyyy-MM-dd'T'HH:mm:ss'Z') into a string numeric represtnted
		   * date/time like yyyyMMddHHmmss
		   * 
		   * @return String represented time like yyyyMMddHHmmss
		   */		
		public static String convertSF_Date_to_CheckPoint_Date(String olddate) {

	        try {
		
	        	if (olddate.indexOf(".")>0)
	        		olddate = olddate.substring(0, olddate.indexOf("."));
	        	
	        	if (olddate.indexOf("Z")>0)
	        		olddate = olddate.substring(0, olddate.indexOf("Z"));
	        	
	        	
	     	   DateFormat dt_from = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");          	         		
	     	   dt_from.setTimeZone(TimeZone.getTimeZone("GMT"));

	            Date date2 = dt_from.parse (olddate);
	            
				  SimpleDateFormat df = new SimpleDateFormat(BaseConstants.CHECKPOINT_DATE_FORMAT);
			        StringBuilder conv = new StringBuilder( df.format(date2 ) );
	            return conv.toString();

	        } catch (Exception e) {
	        	Logger.getLogger(Util.class.getName()).log(Level.SEVERE,"Error during Date Transform "+ e);
	        }
	        return "";
		}
		
		
		  /**
		   * Converts Checkpoint date/time (yyyyMMddHHmmss) into a the Salesforce time format
		   *  (yyyy-MM-dd'T'HH:mm:ss'Z')
		   * 
		   * @return String represented time like yyyy-MM-dd'T'HH:mm:ss'Z'
		   */			
		public static String convertCheckPoint_Date_to_SF_Date_(String olddate) {

	        try {
		       	
	        	
	     	   DateFormat dt_from = new SimpleDateFormat(BaseConstants.CHECKPOINT_DATE_FORMAT);          	         		
	     	   dt_from.setTimeZone(TimeZone.getTimeZone(System.getProperty("user.timezone")));

	            Date date2 = dt_from.parse (olddate);
	            
				  SimpleDateFormat df = new SimpleDateFormat(BaseConstants.SF_DATE_FORMAT);
				  df.setTimeZone(TimeZone.getTimeZone("GMT"));
			        StringBuilder conv = new StringBuilder( df.format(date2 ) );
	            return conv.toString();

	        } catch (Exception e) {
	        	Logger.getLogger(Util.class.getName()).log(Level.SEVERE,"Error during Date Transform "+ e);
	        }
	        return "";
		}		
		
				
		  /**
		   * Converts Salesforce date time  date/time (yyyy-MM-dd'T'HH:mm:ss'Z') 
		   * into the the yyyy-MM-dd (for indexing on the GSA)
		   * @return String represented time like yyyy-MM-dd
		   */			
		public static String formatlmDate(String olddate) {

	        try {

	        	if (olddate.indexOf(".")>0)
	        		olddate = olddate.substring(0, olddate.indexOf("."));
	        	
	        	if (olddate.indexOf("Z")>0)
	        		olddate = olddate.substring(0, olddate.indexOf("Z"));
	        	
	        	
	     	   DateFormat dt_from = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");          	     
	    	   dt_from.setTimeZone(TimeZone.getTimeZone("GMT"));
	            Date from_date = dt_from.parse (olddate);
	            DateFormat to_format = new SimpleDateFormat("yyyy-MM-dd"); 
	            to_format.setTimeZone(TimeZone.getTimeZone(System.getProperty("user.timezone")));
	            
	            return to_format.format(from_date);


	        } catch (Exception e) {
	        	Logger.getLogger(Util.class.getName()).log(Level.SEVERE,"Error during transform  "+ e);
		        return "";
	        }
		}
		
		  /**
		   * Converts a java XML document to its string representation 
		   * @param doc Document XML doucument to process
		   * @return the string form of that doc
		   */			
		public static String XMLDoctoString(Document doc){
			try {			
				  Transformer transformer = TransformerFactory.newInstance().newTransformer();
			      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			      //transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			        
				  StreamResult result = new StreamResult(new StringWriter());
				  DOMSource source = new DOMSource(doc);
				  transformer.transform(source, result);
				  return result.getWriter().toString();
			}catch (Exception ex){
				Logger.getLogger(Util.class.getName()).log(Level.SEVERE,"Error "+ ex);
				return "";
			}
		}

		  /**
		   * Converts a string into a Java Document
		   * @param strXML Stirng XML string to process
		   * @return the Document form of that string
		   */
		public static Document XMLStringtoDoc(String strXML){
			try{
	        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	        dbf.setNamespaceAware(true);
	        DocumentBuilder db = dbf.newDocumentBuilder();        
	        StringBuffer sb1 = new StringBuffer(strXML);
	        ByteArrayInputStream bis = new ByteArrayInputStream(sb1.toString().getBytes("UTF-8"));		        
	        Document doc = db.parse(bis);		      
	        doc.getDocumentElement().normalize();
			return doc;
			}
			catch (Exception ex){
				Logger.getLogger(Util.class.getName()).log(Level.SEVERE,"Error " +ex);
				Logger.getLogger(Util.class.getName()).log(Level.SEVERE,"XML String exception " + strXML);
				return null;
			}
		}
		
		 public static Document TransformDoctoDoc(Document in_doc, Document in_transform)
		{
			  try {			  
				  DOMSource dsource = new DOMSource(in_doc);
				  DOMSource dxsltsource = new DOMSource(in_transform);
				  DOMResult dresult = new DOMResult();

				  TransformerFactory tFactory = TransformerFactory.newInstance();
				  Transformer transformer = tFactory.newTransformer (dxsltsource);	
				  transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				  transformer.transform(dsource, dresult);
				  Document doc = (Document)dresult.getNode();
				  doc.getDocumentElement().normalize();
				  return doc;
				    }
				  catch (Exception e) {
					  Logger.getLogger(Util.class.getName()).log(Level.SEVERE,"Err " + e);
				    return null;
				    }
		}
		    
			  
}
