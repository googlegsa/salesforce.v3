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

/**
 * Class implementing the Authorization Interface of the salesforce connector
 * This class is if the startup parameter in tomcat has <br/>
 * -DAZmodule_connectorinstancename=com.google.enterprise.connector.salesforce.security.PermisiveAuthorization<br/>
 * This module authorizes all docs
 */

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PermissiveAuthorization implements IAuthorizationModule {

	private Logger logger;
	public PermissiveAuthorization () {
		logger = Logger.getLogger(this.getClass().getPackage().getName());
	}
	
	public Collection authorizeDocids(Collection col, String username) {
		logger.log(Level.INFO, "PermissiveAuthorization successful for user " + username  + " num docs to authorize: " + col.size());
		return col;
	}

}
