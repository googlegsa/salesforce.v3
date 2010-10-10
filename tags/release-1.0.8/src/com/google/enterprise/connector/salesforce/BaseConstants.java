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

public class BaseConstants {

	public static String CHECKPOINT_DATE_FORMAT = "yyyyMMddHHmmss";
	public static String DB_JNDI_NAME = "java:jdbc/ConnectorDS";
	public static String ENTERPRISE_WSDL = "enterprise.wsdl";
	public static int QUERY_TYPE = 1;
	public static int QUERYMORE_TYPE = 2;
	public static String SF_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	public static String SESSIONID = "SESSIONID";
	public static String LOGIN_SERVER = "LOGIN_SERVER";
	public static String WEAK_AUTHENTICATION = "WEAK_AUTHENTICATION";
	public static String STRONG_AUTHENTICATION = "STRONG_AUTHENTICATION";
	public static String AUTHENTICATION_TYPE = "AUTH_TYPE";
	public static int MAX_USER_SESSIONS=1000;
	public static String CONNECTOR_DATASOURCE="jdbc/ConnectorDS";
	public static String CALLABLE_AU="AUmodule";
	public static String CALLABLE_AZ="AZmodule";
}
