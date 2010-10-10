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

/**
 * Class implementing the Authentication Iterface of the salesforce connector
 * This class is if the startup parameter in tomcat has <br/>
 * -DAUmodule_connectorinstancename=com.google.enterprise.connector.salesforce.security.PermisiveAuthentication<br/>
 * This module allows all users to get authentication
 */

public class PermissiveAuthentication implements IAuthenticationModule {

	private Logger logger;
	
	public PermissiveAuthentication() {
		logger = Logger.getLogger(this.getClass().getPackage().getName());
	}
	
	 /** Authenticates and allows all users
	  */
	public boolean authenticate(String username, String password) {
		//allow all users in
		logger.log(Level.INFO, "PermissiveAuthentication successful for user " + username );
		return true;
	}

}
