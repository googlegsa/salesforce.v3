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

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.enterprise.connector.spi.ConfigureResponse;
import com.google.enterprise.connector.spi.ConnectorFactory;
import com.google.enterprise.connector.spi.ConnectorType;


/**
 * Class implementing the actual ConnectorType from the connector 
 * This class is the springframework UI component thats displayed on
 * the GSA connector screen.  It HTML form the connector shows and persists
 * the data to the spring properties/config files
 * <p>
 * The get<Param>()  set<Param>() defined here >>must<< match whats specified 
 * in connectorInstance.xml and connectorType.xml
 * </p>
 */
public class BaseConnectorType implements ConnectorType {
	
  String blankform = "<tr><td>SFConnector Form</td></tr>" +
  				     "<tr><td>Username:</td><td> <input type=\"username\" size=\"65\" name=\"username\"/></td></tr>" +
  				     "<tr><td>Password:</td><td> <input type=\"password\" size=\"65\" name=\"password\"/></td></tr>" +
  				     "<tr><td>LastQuartzSync (GMT):</td><td> <input type=\"lastsync\" size=\"35\" name=\"lastsync\"/></td></tr>" +
  				     "<tr><td>LoginSrv:</td><td> <input type=\"loginsrv\" size=\"65\" name=\"loginsrv\"/></td></tr>" +
  				     "<tr><td>Schedule:</td><td> <input type=\"schedule\" size=\"35\" name=\"schedule\"/></td></tr>" +
  				     "<tr><td>Query:</td><td>    <textarea  type=\"query\"  rows=\"10\" cols=\"65\" size=\"65\" name=\"query\"></textarea></td></tr>" +
  				     "<tr><td>Response XSLT (base64encoded):</td><td>     <input type=\"xslt\" size=\"65\" name=\"xslt\"/></td></tr>" +
  				     "<tr><td>AZ Query:</td><td> <input type=\"azquery\" rows=\"3\" cols=\"65\" size=\"65\" name=\"azquery\"/></td></tr>" +
  				     "<tr><td>AZ XSLT (base64encoded):</td><td> <input type=\"azxslt\" size=\"65\" name=\"azxslt\"/></td></tr>" +
  				     "<tr><td>StoreType: </td><td><input name=\"storetype\" type=\"radio\" value=\"MemoryStore\">MemoryStore</input> <input name=\"storetype\" type=\"radio\" value=\"FileStore\">FileStore</input> <input name=\"storetype\" type=\"radio\" value=\"DBStore\">DBStore</input> </td></tr>";

  				     
  
   
  private Logger logger;
  
  public BaseConnectorType() {
		logger = Logger.getLogger(this.getClass().getPackage().getName());
		logger.log(Level.FINE, "SalesForceConnectorType initializing");
  }
  
  
  /**
   * Returns the ConfigureResponse objec to the connector which encapsulates the
   * configuration form properties
   * @return ConfigureResponse
   */
  public ConfigureResponse getConfigForm(Locale locale) {
	logger.log(Level.FINER, " SalesForceConnectorType.getConfigForm called");
    return new ConfigureResponse("", blankform);
  }
  
  /**
   * Validates the configuration form thats submitted through the GSA
   * At this time, all fields are required but in the future, we could verify the 
   * login credentials or any other param by actually invoking a salesforce login
   * @return ConfigureResponse
   */

  public ConfigureResponse validateConfig(Map map, Locale locale, ConnectorFactory factory) {

	  String wdir = "";
	  String cdate = "";
	  Iterator i = map.keySet().iterator();
	  while( i.hasNext()) {
		    String key = (String) i.next();
		    String val = (String) map.get(key);
		    logger.log(Level.FINEST,"Validating form input  ===========> Key: " + key + "  value " + val);
		    
		    if (key.equalsIgnoreCase("googleConnectorWorkDir"))
		    	wdir = val;
		    
		    if (key.equalsIgnoreCase("lastsync"))
		    	cdate = val;
		    
		    //if a submitted value is blank, we'll throw an error message back to the 
		    //the GSA
		   	if (val.equalsIgnoreCase("")){
		   		return new ConfigureResponse("All values are required" , blankform);
		   	}
		  }
	  
	  
	  ///uh...a hack to update the lastsync date	  
	  String instance_name =Util.getInstanceNameFromWorkingDir(wdir);
	  System.setProperty(instance_name+"_last_crawl_date",cdate);

	logger.log(Level.FINER, " SalesForceConnectorType.validateConfig called");
	
    return null;
  }
  
  
  /**
   * Returns the populated configuration form back to the GSA.<br>
   * It reads the map file containing the config info from the springframework and shows it
   * @return ConfigureResponse
   */

  public ConfigureResponse getPopulatedConfigForm(Map map, Locale locale) {
	  logger.log(Level.FINE, " SalesForceConnectorType.getPopulatedConfigForm called");
	  String populatedform = "";
	  String username = "";
	  String password = "";
	  String schedule = "";
	  String query = "";
	  String xslt = "";
	  String azquery = "";
	  String azxslt = "";
	  String loginsrv = "";
	  String lastsync = "";
	  String storetype="";
	  	  
	  
	  Iterator i = map.keySet().iterator();
	  String  workingdir=null;
	  while( i.hasNext()) {
		    String key = (String) i.next();
		    String val = (String) map.get(key);
		    logger.log(Level.FINEST,"Reloading form   ===========> Key: " + key + "  value " + val);


		   	if (key.equalsIgnoreCase("username"))
		   		username = val;
		   	
		   	if (key.equalsIgnoreCase("password"))
		   		password = val;	
		   	
		   	if (key.equalsIgnoreCase("schedule"))
		   		schedule = val;
		   	
		   	if (key.equalsIgnoreCase("query"))
		   		query = val;	
		   	
		   	if (key.equalsIgnoreCase("xslt"))
		   		xslt = val;
		   	
		   	if (key.equalsIgnoreCase("azquery"))
		   		azquery = val;	
		   	
		   	if (key.equalsIgnoreCase("azxslt"))
		   		azxslt = val;
		   	
		   	if (key.equalsIgnoreCase("loginsrv"))
		   		loginsrv = val;	
		   	
		   	if (key.equalsIgnoreCase("lastsync"))
		   		lastsync = val;	
		   	
		   	if (key.equalsIgnoreCase("googleConnectorWorkDir"))
		   		workingdir = val;
		   	
		   	if (key.equalsIgnoreCase("storetype"))
		   		storetype = val;

		  }

	  
	  //hack...don't know how to dynamically reload the prop fles yet
	  //so we set a system property with the name of the connector instance 
	  //for the last_crawl_date from the Quartz scheduler.
	  //the lastcrawl date is a quartz property so its not stored in the springframework
		  String instance_name =Util.getInstanceNameFromWorkingDir(workingdir);

		  String str_last_date_crawled = System.getProperty(instance_name+"_last_crawl_date");
		  
		  if (str_last_date_crawled != null)
			  	lastsync=str_last_date_crawled;
		  
		  
		  String lcheckpoint = System.getProperty(instance_name+"_lcheckpoint");
		  
		  
		  if (lcheckpoint == null)
			  lcheckpoint="";

	  
	   populatedform = "<tr><td>SFConnector Form</td></tr>" +
	     "<tr><td>username:</td><td> <input type=\"username\" size=\"65\" name=\"username\" value=\"" + username + "\"/></td></tr>" +
	     "<tr><td>Password:</td><td> <input type=\"password\" size=\"65\" name=\"password\" value=\"" + password + "\"/></td></tr>" +
		 "<tr><td>LastQuartzSync (GMT):</td><td> <input type=\"lastsync\" size=\"35\" name=\"lastsync\" value=\"" + lastsync + "\"/></td></tr>" +
		 "<tr><td>LastBatchRead (GMT):</td><td> <b>" + lcheckpoint + "</b></td></tr>" +
	     "<tr><td>LoginSrv:</td><td> <input type=\"loginsrv\" size=\"65\" name=\"loginsrv\" value=\"" + loginsrv + "\"/></td></tr>" +	     
	     "<tr><td>Schedule:</td><td> <input type=\"schedule\" size=\"35\" name=\"schedule\" value =\"" + schedule + "\"/></td></tr>" +
	     "<tr><td>Query:</td><td>    <textarea type=\"query\"  rows=\"10\" cols=\"65\" size=\"65\" name=\"query\">" + query + "</textarea></td></tr>" +
	     "<tr><td>Response XSLT (base64encoded):</td><td>     <input type=\"xslt\" size=\"65\" name=\"xslt\" value=\"" + xslt + "\"/></td></tr>" +
	     "<tr><td>AZ Query:</td><td> <textarea type=\"azquery\"   rows=\"3\" cols=\"65\" size=\"65\" name=\"azquery\">" + azquery + "</textarea></td></tr>" +
	     "<tr><td>AZ XSLT (base64encoded):</td><td> <input type=\"azxslt\" size=\"65\" name=\"azxslt\" value=\"" + azxslt + "\" /></td></tr>" +
	     "<tr><td>StoreType:</td><td><input name=\"storetype\" type=\"radio\" checked=\"true\" value=\""+ storetype +"\"/>" + storetype + "</td></tr>";

	   //	     "<tr><td>Query:</td><td>    <textarea type=\"query\"  rows=\"10\" cols=\"10\" size=\"65\" name=\"query\" value =\"" + query + "\"/></td></tr>" +
	  
    return new ConfigureResponse("filled", populatedform);
  }
}

