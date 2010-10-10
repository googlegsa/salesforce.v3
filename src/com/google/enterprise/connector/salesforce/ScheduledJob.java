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

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


/**
 * Class implementing  a Job for Quartz
 * <p>
 * More about Quartz:  <a href="http://www.opensymphony.com/quartz/">http://www.opensymphony.com/quartz/</a>
 * </p>
 */
public class ScheduledJob implements Job{
	private String feed_name= null;
    private Logger logger = Logger.getLogger(ScheduledJob.class.getPackage().getName());;

	public ScheduledJob(){
	}

  public void execute(JobExecutionContext cntxt) throws JobExecutionException {
	  logger = Logger.getLogger(this.getClass().getPackage().getName());;
	  logger.log(Level.INFO, " ******* Quartz Invoking Job *******   " + cntxt.getJobDetail().getName());  
	  //load up the feeder manager singleton with the name of the connector thats in context
	  FeederManager fm = FeederManager.instance();
	  fm.QuartzCallback(cntxt.getJobDetail().getName());

      //System.out.println( "Generating report - " + cntxt.getJobDetail().getJobDataMap().get("type") + " " + cntxt.getJobDetail().getName());
  }

}
