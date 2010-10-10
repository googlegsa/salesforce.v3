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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.jdbcjobstore.JobStoreCMT;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobStore;
import org.quartz.utils.DBConnectionManager;
import org.quartz.utils.JNDIConnectionProvider;

import com.google.enterprise.connector.salesforce.modules.salesforce.SalesForceModule;
import com.google.enterprise.connector.salesforce.storetype.DBStore;
/**
 * Singleton Class used for a quartz callback.
 * <p>
 * This class holds a hashmap with ALL the {@link BaseTraversalManager} BaseTraversalManager
 * inside this VM.   THe quartz callback will pass in the connector name thats in context
 * and we use that name to lookup the BaseTraversalManager.  
 * </p> 
 */
public class FeederManager {
	private Logger logger;
    protected   HashMap hm_sf_traversal;
    
    private static FeederManager instance = null;
	static private FeederManager _instance = null;
	
	Scheduler sched;

	/**
	 * Singleton Design pattern instance
	 */
	static public FeederManager instance() {
		      if(null == _instance) {
		         _instance = new FeederManager();
		      }
		      return _instance;
		   }
	
	protected  FeederManager(){	
		logger = Logger.getLogger(this.getClass().getPackage().getName());
		hm_sf_traversal = new HashMap();

		try {
		      logger.log(Level.INFO,">>>>>>>>>>>>>>>> FeederManager initialize ");
		}
		catch (Exception ex){
			 
		}
	}
	
	
    /**
     * Registers the traversal manager and connector instances with this singleton
     * so that we have a handle to the connector when quartz invokes it.
     * @param btm the traversal manager to register
     * @param connector the connector in context
     */
	
	public void setTraversalCallback(BaseTraversalManager btm,BaseConnector connector){
		try {			
		  logger.log(Level.INFO,">>>>>>>>>>>>>>>> FeederManager setConnector " + connector.getInstanceName());	
		  
		  //add the base traversal manager to the singleton's hashmap
		  hm_sf_traversal.put(connector.getInstanceName(),btm);
		  
		  //initialize the quartz scheduler factory
		  SchedulerFactory schedFact=  new org.quartz.impl.StdSchedulerFactory(); 
		  
		  //get the scheduler in context.
		  //the naming convention used is the scheduler is called 'connectorinstancenameScheduler'
		  sched = schedFact.getScheduler(connector.getInstanceName()+"Scheduler");
		  
		  //if this is the first time initializeing, create a new scheduler from scratch
		  if (sched==null){
			  SimpleThreadPool threadPool = new SimpleThreadPool(3, Thread.NORM_PRIORITY); 
			  threadPool.initialize();
			  JobStore jobStore = null;
			  if (connector.getStoretype().equalsIgnoreCase("DBStore")) {
				  //first check if its a dbstore, then we're storing hte quartz info in
				  //the database so we should initialize it first:
				  DBStore db = new DBStore(connector);
				  
		          DBConnectionManager  dcm = DBConnectionManager.getInstance();
		          JNDIConnectionProvider jcp = new JNDIConnectionProvider("java:comp/env/" + BaseConstants.CONNECTOR_DATASOURCE,true);
		          dcm.addConnectionProvider("java:comp/env/" + BaseConstants.CONNECTOR_DATASOURCE,jcp);
                  JobStoreCMT  jsCMT = new JobStoreCMT();
                  //todo:  the driverdelegate works for mysql+tomcat...others may
                  //need a different delegate.
                  jsCMT.setDriverDelegateClass("org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
                  jsCMT.setMisfireThreshold(60000);
                  jsCMT.setUseProperties("false");
                  jsCMT.setNonManagedTXDataSource("java:comp/env/" + BaseConstants.CONNECTOR_DATASOURCE);
                  jsCMT.setDataSource("java:comp/env/" + BaseConstants.CONNECTOR_DATASOURCE);
                  jsCMT.setTablePrefix("QRTZ_");                 
                  jsCMT.setIsClustered(true);
                  jsCMT.setInstanceId(StdSchedulerFactory.AUTO_GENERATE_INSTANCE_ID);
                  jsCMT.setInstanceName(connector.getInstanceName()+"Instance");
                  jsCMT.setClusterCheckinInterval(20000);	
                  jobStore = jsCMT;
			  }
			  else {
				  jobStore = new RAMJobStore();
			  }
			  //jobStore.in.initialize();
			  DirectSchedulerFactory.getInstance().createScheduler(connector.getInstanceName()+"Scheduler", connector.getInstanceName()+"Instance", threadPool, jobStore);
		  }
		  
		  //otherwise, reschedule the cron here
		  sched = schedFact.getScheduler(connector.getInstanceName()+"Scheduler");
          JobDetail jobDetail =  new JobDetail(connector.getInstanceName(),connector.getInstanceName(), ScheduledJob.class);
          jobDetail.getJobDataMap().put("type","FULL");
          //just do failover
          jobDetail.setRequestsRecovery(true);
          CronTrigger trigger = new CronTrigger(connector.getInstanceName(),connector.getInstanceName() );
          logger.log(Level.INFO,"ATTEMPTING TO SET SCHEDULE  [" + connector.getSchedule() + "] for " + connector.getInstanceName());
          trigger.setCronExpression(connector.getSchedule());
          sched.deleteJob(connector.getInstanceName(),connector.getInstanceName());
          sched.scheduleJob(jobDetail, trigger);
          sched.start();
		}
		catch (Exception ex){
			logger.log(Level.SEVERE,"Could not set quartz schedule " + ex);
		}
	}
	
	
    /**
     * When quartz fires, it calls this class in the singelton 
     * with the name of the connector that just got fired
     * @param connector_name the connector whose quartz just got triggered
     */
	
	public void QuartzCallback(String connector_name){
		//lookup the basegtraversal manager for this instance
        logger.log(Level.INFO,"QuartzCallback called for: ["+connector_name+"]");
		BaseTraversalManager btm = (BaseTraversalManager)hm_sf_traversal.get(connector_name);   
		//create the module's context
		SalesForceModule sfm = new SalesForceModule(btm);
    	Date now = new Date();
    	String last_crawled_date = Util.getNumericString_from_Date(now);
    	//see if the connector is currently processing items...if so, skip it
    	//normally we should make the interval between quartz triggers
    	//large enough that there is no overlap between triggers
    	if (btm.getConnector().IsRunning()) {
                logger.log(Level.INFO,"Previous quartz run is active. Aborting current run  ["+connector_name+"]");    		
    			return;
    	}
    	
    	btm.getConnector().setIsRunning(true);
    	//if we've successfully logged in
    	if (sfm.login()) {
    		//set the sessionID etc on the traversal manager
    		//TODO: there is probably a better way that we can just pass in
    		//the sfm object into the btm to get all the data instead of using
    		//get/set methods
    		btm.getConnector().setSessionID(sfm.getSessionID());
    		btm.getConnector().setEndPointServer(sfm.getEndPointServer());
    		//finally go get the data
    		sfm.populateStore(btm.getStore());
    		//update the sync date now that w're done and quit
    		btm.getConnector().setLastsync(last_crawled_date);
    		System.setProperty(btm.getConnector().getInstanceName()+"_last_crawl_date",last_crawled_date);
    		btm.getConnector().commit();
    	}
    	  btm.getConnector().setIsRunning(false);

	}

}

