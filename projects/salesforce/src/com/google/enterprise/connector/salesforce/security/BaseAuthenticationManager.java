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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.enterprise.connector.salesforce.BaseConnector;
import com.google.enterprise.connector.salesforce.BaseConstants;
import com.google.enterprise.connector.salesforce.exception.BaseException;
import com.google.enterprise.connector.salesforce.modules.salesforce.SalesForceLogin;
import com.google.enterprise.connector.spi.AuthenticationIdentity;
import com.google.enterprise.connector.spi.AuthenticationManager;
import com.google.enterprise.connector.spi.AuthenticationResponse;




/**
 * Class implementing  the connectors AuthenticationManager interface 
 * of the connector. This class is called by the connector when it needs to authenticate a user
 */

public class BaseAuthenticationManager implements AuthenticationManager {
	private Logger logger;
	private BaseConnector connector;
	
	/**
	 * Initialize the authentication manager and pass in the {@link BaseConnector}  BaseConnector
	 * that is in context
	 * @param connector BaseConnector the Connector object that is in context
	 */
	public BaseAuthenticationManager(BaseConnector connector) {
		logger = Logger.getLogger(this.getClass().getPackage().getName());
		logger.log(Level.INFO, " SalesForceAuthenticationManager initializing");
		this.connector = connector;
	}
	
	/**
	 * Method invoked by the connector when it needs an Authentication
	 * decision for a secure search.
	 * @param id AuthenticationIdentity object from the connector
	 * @return      AuthenticationResponse object describing the AU decision
	 */
    public AuthenticationResponse authenticate(AuthenticationIdentity id) {
      logger.log(Level.FINER, " SalesForceAuthenticationManager authenticate called " + id.getDomain()+ "\\" +  id.getUsername());  
    
      
      //first see if we have a callable authentication module to try
      
      String callable_au_module = System.getProperty(BaseConstants.CALLABLE_AU + "_" + connector.getInstanceName());
      
      if (callable_au_module != null){
	      logger.log(Level.INFO, "Using Loadable Authentication Module : " + callable_au_module);  
			try{
			  	  Class cls = Class.forName(callable_au_module);
				  java.lang.reflect.Constructor co = cls.getConstructor();
				  IAuthenticationModule icau = (IAuthenticationModule)co.newInstance();
				  
				  if  (icau.authenticate(id.getUsername(), id.getPassword())) {
  				    logger.log(Level.FINE, "Login Succeeded for User " + id.getUsername());  
				  	return  new AuthenticationResponse(true, id.getUsername());
				  }
				  else {
					logger.log(Level.INFO, "Login Failed for User " + id.getUsername());  
					new AuthenticationResponse(false, "LOGINFAILED");
				  }
				}
			catch (Exception ex) {
				logger.log(Level.SEVERE, "Unable to load Authentication Module " + callable_au_module); 
			}	      
      }
      else {
	      logger.log(Level.FINER, "Using Default Authentication Module");  
      }
      
      
      
      try{
    	  //get the username/password from the AuthenticationIdentity
    	  //and attempt to login to salesforce
    	  SalesForceLogin sfl = new SalesForceLogin(id.getUsername(), id.getPassword(),connector.getLoginsrv());     
    	  //capture the response (good or bad)
    	  String str_response = sfl.getLoginResponseXML();
          	  
    	  if (sfl.isLoggedIn()){
    		  //if we have a good/full login response, then we consider it to be strong
    		  //we would get a strong authentication response if the user entered the username/password and
    		  //the connector is running from within the trusted IPs that salesforce knows about
    	      logger.log(Level.FINER, ">>>>>>>>>>>>>> Strong Authentication successful for " + id.getUsername());  
    	      //set the sessionID and login server for this user in a local connector hashmap
    	      //TODO:  figure out how to purge the sessions.
    	      connector.setUserSession(id.getUsername(),sfl.getEndPointServer(),sfl.getSessionID(),BaseConstants.STRONG_AUTHENTICATION);
    		  return new AuthenticationResponse(true, id.getUsername() + "," + sfl.getEndPointServer() + "," + sfl.getSessionID());
    	  }
    	  else{
	    	  //a weak login occurs if the connector is running from an untrusted zone.
    		  //In this case, even if the users login/password is correct, salesforce will send a 
    		  //soap fault back BUT go and tell us that the user must append a security token
    		  //I'm guessing that this means the users is technically authenticated but
    		  //not authorized to run the SOAP APIs from this IP
	    	  if (str_response.contains("sf:LOGIN_MUST_USE_SECURITY_TOKEN")){
	    	      logger.log(Level.FINER, ">>>>>>>>>>>> Weak Authentication successful for " + id.getUsername());  
	    	      connector.setUserSession(id.getUsername(),"","",BaseConstants.WEAK_AUTHENTICATION);
	    	      //although we could pass it, lets fail it for enhanced security
	    		  //return new   AuthenticationResponse(true, id.getUsername());
	    	      return new AuthenticationResponse(false, "LOGINFAILED");
	    	  }
	    	  else {
	    		  //the user is just not authenticated..
	    		  logger.log(Level.FINER, ">>>>>>>>>>>>>>>>> NO AUTHENTICATON  " + id.getUsername());  
	    		  return new AuthenticationResponse(false, "LOGINFAILED"); 
	    	  }
    	  }
      }
      catch (BaseException bex){
    	  logger.log(Level.SEVERE, "Error on Authentication step " +bex );
      }
      
      return new AuthenticationResponse(false, "LOGINFAILED"); 
      
    }
  }
