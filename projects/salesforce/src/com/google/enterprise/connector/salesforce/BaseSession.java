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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.enterprise.connector.salesforce.security.BaseAuthenticationManager;
import com.google.enterprise.connector.salesforce.security.BaseAuthorizationManager;
import com.google.enterprise.connector.spi.AuthenticationManager;
import com.google.enterprise.connector.spi.AuthorizationManager;
import com.google.enterprise.connector.spi.Session;
import com.google.enterprise.connector.spi.TraversalManager;

/**
 * Class implementing the actual the Session between the connectormanager and connector
 * This class is called by the connector manger on initialization and session creation 
 * between the manager and each session
 */
public class BaseSession implements Session {
	private Logger logger;
	
	private BaseConnector connector=null;
	
	public BaseSession(BaseConnector inConnector) {	
		logger = Logger.getLogger(this.getClass().getPackage().getName());
		logger.log(Level.INFO, " SalesForceSession initializing");	
		this.connector=inConnector;
	}
    public AuthenticationManager getAuthenticationManager() {
      logger.log(Level.INFO, " SalesForceSession getAuthenticationManager called");
      return new BaseAuthenticationManager(connector);
    }

    public AuthorizationManager getAuthorizationManager() {
      logger.log(Level.INFO, " SalesForceSession getAuthorizationManager called");
      return new BaseAuthorizationManager(connector);
    }

    public TraversalManager getTraversalManager() {
    	logger.log(Level.INFO, " SalesForceSession getTraversalManager called");
      return new BaseTraversalManager(connector);
    }
  }