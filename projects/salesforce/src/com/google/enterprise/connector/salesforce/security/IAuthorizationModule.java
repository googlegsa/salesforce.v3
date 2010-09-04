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

import java.util.Collection;

/**
 * Interface class that all loadable authorization modules should implement.
 * <p>
 * Startup param for tomcat is is -DAZmodule_connectorinstancename=com.yourmodule.YourAZClass
 * </p>
 */

public interface IAuthorizationModule {

	 /** Authorizes the user for a set of documents
	   * @param col Collection of the docs to authorize
	   * @param username username of the user to authorize for 
	   * @return Collection collection of the docs the allowed to see
	   */  
	public Collection authorizeDocids(Collection col, String username);
		
}
