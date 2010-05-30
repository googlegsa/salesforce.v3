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

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.enterprise.connector.spi.Connector;
import com.google.enterprise.connector.spi.Session;


/**
 * Class implementing the actual Connector
 * This class is called by the connector manger on initialization
 * and holds the config/state/session information for the connector instance
 */
public class BaseConnector implements Connector {
  private Logger logger;
  

  private String username;
  private String password;
  private String schedule;
  private String query;
  private String xslt;
  private String azquery;
  private String azxslt;

  private String loginsrv;
  private String lastsync;
  private String googleConnectorWorkDir;
  private String storetype;
  private boolean isRunning=false;
  private boolean allow_weak_authentication = true;
  private HashMap login_sessions;
  private Queue q_sessions;
  private int user_max_sessions = 1000;
  
  private String sf_sessionID = "";
  private String sf_server = "";
  
  /**
   * Default Constructor for the connector called only by the spring framework onstartup
   * of the connector itself.  This class isn't designed to be instantiated by itself manually.
   * <p>When the system starts up, the parameters defined in the 
   * connectorInstance.xml is used inorder to call this connector</p>
   * The login_session hashmap storing the authenticated sessions is also initialized
   * <p>Documentation about the quartz scheduler: 
   *  <a href="http://www.opensymphony.com/quartz/wikidocs/CronTriggers%20Tutorial.html">http://www.opensymphony.com/quartz/wikidocs/CronTriggers%20Tutorial.html</a>
   * </p>
   * <p>
   * The Query terms wildcards right now is $LAST_SYNC_DATE.  THis wildcard, if found in the query parmaeter, is substituted
   * with the last time the quartz scheduler ran.
   * </p>
   * @param  username   The username the connector should use to login as
   * @param password the login password
   * @param schedule the CRON schedule
   * @param query used to get the salesforce data per iteration
   * @param xslt  used to transform the query response into the connector feed and HTML cache copy.  This param must be BASE64 ENCODED
   * @param azquery  the authentication query to see if a user is allowed to even get on salesforce
   * @param azxslt the xslt to transform the authentiation query back into a canonical xml form the connector can process. This param must be BASE64 ENCODED
   * @param loginsrv  the login server the connector should use 
   * @param lastsync  the last time the Connector's Quartz scheduler ran completely
   * @param storetype  the last time the Connector's Quartz scheduler ran completely
   * @param googleConnectorWorkDir  the working directory for this connector WEB-INF/connectors/salesforce-connector/<connectorname>
   */


  public BaseConnector(String username, String password, String schedule, String query, String xslt,
			  			String azquery, String azxslt, String loginsrv, String lastsync, String storetype, String googleConnectorWorkDir) {
	  logger = Logger.getLogger(this.getClass().getPackage().getName());
	  login_sessions = new HashMap();
	  q_sessions = new LinkedList();
	  logger.log(Level.INFO, " SalesForceConnector initializing");  
	  
	  String str_max_sessions = System.getProperty("MAX_USER_SESSIONS");
	  if (str_max_sessions !=null){
		  logger.log(Level.FINER, "Using MAX_USER_SESSIONS " + str_max_sessions);
		  this.user_max_sessions = new Integer(str_max_sessions).intValue();
	  }
	  else{
		  this.user_max_sessions = BaseConstants.MAX_USER_SESSIONS;
	  }
		  
  }
  
  /**
   * Returns the sessionID for the connector has with salesforce
   * @return sessionID
   */
  
  public String getSessionID() {
	     return this.sf_sessionID;
	  }

  public void setSessionID(String new_val) {
		this.sf_sessionID = new_val;
	  }	
  
  /**
   * Returns the session information  for the authenticated user  has with salesforce
   * during a secure search
   * @param username from AuthenticationIdentity.id
   * @return Properties file containing the loginserver, sessionID and auth_type used
   */
  
  public Properties getUserSession(String username){
	  logger.log(Level.FINER, " Returning PROPS FOR " + username);  
	  return (Properties)this.login_sessions.get(username);
  }
  
  /**
   * Sets the user session  for the authenticated user  has with salesforce
   * during a secure search
   * <p>
   * This routine also specifies if the user had a STRONG_AUTHENTICATION (complete login)
   * or a WEAK Authentication (partial/inferred login)
   * </p>
   * @param username from AuthenticationIdentity.id
   * @param login_server the login server this authenticated user should use
   * @param  sessionID for the user in context
   * @param  auth_type type used
   */
  
  public void setUserSession(String username, String login_server, String sessionID, String auth_type){
	  
	  if (q_sessions.contains(username)) {
		  q_sessions.remove(username);		 
	  }
	  
	  
	  if (q_sessions.size() == user_max_sessions){
		  String str_username = (String)q_sessions.poll();
		  logger.log(Level.FINER, "Purging usersession from memory " + str_username);
		  this.purgeSession(str_username);
	  }
	  
	  Properties props = new Properties();
	  props.put(BaseConstants.SESSIONID, sessionID);
	  props.put(BaseConstants.LOGIN_SERVER, login_server);
	  props.put(BaseConstants.AUTHENTICATION_TYPE, auth_type);
	  logger.log(Level.FINER, " Adding PROPS FOR " + username);  
	  
	 this.q_sessions.add(username); 
	 this.login_sessions.put(username,props);
  }
  
  /**
   * Purges a session from the hash_map of authenticated users
   * This should get implemented so that older sessions don't reside in memory
   * @param username from AuthenticationIdentity.id
   */
  
  
  public void purgeSession(String username){
	  this.login_sessions.remove(username);
	  return;
  }
 
  
  public String getEndPointServer() {	     
	     return this.sf_server;
	  }

  public void setEndPointServer(String new_val) {
		this.sf_server = new_val;
	  }	


  public boolean allowWeakAuth(){
	  return allow_weak_authentication;
  }
  
  public boolean IsRunning(){
	  return this.isRunning;
  }
  public void setIsRunning(boolean new_val){
	   this.isRunning = new_val;
  }

  
  public Session login() {
	logger.log(Level.FINE, " SalesForceConnector login called");
    return new BaseSession(this);
  }
   
  public String getLoginsrv() {
	    return loginsrv;
	  }
  
  public void setLoginsrv(String loginsrv) {
		logger.log(Level.FINE,"Setting loginsrv " + loginsrv);
	    this.loginsrv = loginsrv;
	  }	
  
  
  public String getStoretype() {
	    return storetype;
	  }

 public void setStoretype(String storetype) {
		logger.log(Level.FINE,"Setting storetype " + storetype);
	    this.storetype = storetype;
	  }	

  
  public String getLastsync() {
	    return lastsync;
	  }
  
  public void setLastsync(String lastsync) {
		logger.log(Level.FINE,"Setting lastsync " + lastsync);
	    this.lastsync = lastsync;
	  }	
  
  
   public String getAzxslt() {
	    return azxslt;
	  }
   public void setAzxslt(String azxslt) {
		logger.log(Level.FINE,"Setting azxslt " + azxslt);
	    this.azxslt = azxslt;
	  }	
   

  
  public String getAzquery() {
	    return azquery;
	  }
  public void setAzquery(String azquery) {
		  logger.log(Level.FINER,"Setting azquery " + azquery);
	    this.azquery = azquery;
	  }	
  
   public String getXslt() {
	    return xslt;
	  }
   
   public void setXslt(String xslt) {
		  logger.log(Level.FINE,"Setting xslt " + xslt);
	    this.xslt = xslt;
	  }	

  public String getQuery() {
	    return query;
	  }
  public void setQuery(String query) {
		  logger.log(Level.FINE,"Setting query " + query);
	    this.query = query;
	  }	  

  
  	  public String getUsername() {
	    return username;
	  }
	  public void setUsername(String username) {
		  logger.log(Level.INFO,"Setting username " + username);
	    this.username = username;
	  }
	  
	  public String getPassword() {
	    return password;
	  }
	  
	  public void setPassword(String password) {
		  logger.log(Level.FINEST,"Setting password " + password);
	    this.password = password;
	  }
	  
	  public String getSchedule() {
		    return schedule.trim();
		  }
	  
	  public void setSchedule(String schedule) {
		    this.schedule = schedule;
		  }	  
	  
	  public String getGoogleConnectorWorkDir() {
		    return googleConnectorWorkDir;
		  }
	  
	  public void setGoogleConnectorWorkDir(String googleConnectorWorkDir) {
			  logger.log(Level.FINE,"Setting googleConnectorWorkDir " + googleConnectorWorkDir);
		    this.googleConnectorWorkDir = googleConnectorWorkDir;
		  }	  

	  public String getInstanceName() {
		  return Util.getInstanceNameFromWorkingDir(this.googleConnectorWorkDir);
	  }

	  //hack...don't know how to dynamically save the prop fles yet using spring...
	  /**
	   * Saves the current configuration settings to disk into the /WEB-INFO/connectors/salesforce-connector/<connectorname>/<connectorname>.properties
	   * <p>
	   * .>>>> there's got to be some better for the springframework to persist it but I don't know how to
	   * at the time of wriring
	   * </p>
	   */
	  
	  public void commit() {
		  try {
			  Properties prop = new Properties();
			  prop.setProperty("username", this.getUsername());
			  prop.setProperty("password", this.getPassword());
			  prop.setProperty("schedule", this.getSchedule());
			  prop.setProperty("query", this.getQuery());
			  prop.setProperty("xslt", this.getXslt());
			  prop.setProperty("azquery", this.getAzquery());
			  prop.setProperty("azxslt", this.getAzxslt());
			  prop.setProperty("loginsrv", this.getLoginsrv());
			  prop.setProperty("lastsync", this.getLastsync());
			  prop.setProperty("storetype", this.storetype);
			  prop.setProperty("googleConnectorWorkDir", this.getGoogleConnectorWorkDir());
			  
			  prop.store( new FileOutputStream(this.getGoogleConnectorWorkDir() + System.getProperty("file.separator") + getInstanceName() + ".properties"), "Autosave" ); 
			  
		  }
		  catch (Exception ex) {
			  logger.log(Level.SEVERE, "Error saving properties " +ex);
		  }
	    
	  }	  
	  
}