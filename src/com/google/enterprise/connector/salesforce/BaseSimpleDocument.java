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

import java.util.Date;
import java.util.Map;

import com.google.enterprise.connector.spi.SimpleDocument;

/**
 * Class implementing  a SimpleDocument object that the connector manager can process 
 * <p>
 * Its basically a wrapper around the Simpledocument object but this extension contains 
 * the crawl date that this doc was retrieved by the quartz scheduler from salesforce
 * </p>
 */

public class BaseSimpleDocument extends SimpleDocument  {
	private Date crawled_date;

	public BaseSimpleDocument(Date crawled_date, Map spivalues ){
		super(spivalues);
		this.crawled_date=crawled_date;
	}
	
	public Date getCrawledDate(){
		return crawled_date;
	}
	

}
