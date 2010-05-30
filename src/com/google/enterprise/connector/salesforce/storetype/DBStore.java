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

package com.google.enterprise.connector.salesforce.storetype;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.enterprise.connector.salesforce.BaseConnector;
import javax.sql.*;
import javax.sql.rowset.*;

import java.math.BigDecimal;
import java.sql.*;

import javax.naming.*;
import com.google.enterprise.connector.salesforce.*;


/**
 * Implementation of a storetype for the connector
 * <p>
 * This store type saves the SOAP response traversal data taken from salesforce into a database
 * <br/> Specifically, its stores each query responses set as an individual row in MySQL (the only db thats
 * implemented).
 * <br/> 
 * <p>
 * The connectorDB is setup such that the data for each individual connector exists in the same
 * database but in different tables.  The per connector instance table name is in the format i_<connectorinstancename>.
 * <br/>
 * Each connector instance table is in the form <b>i_<connectorinstancename></b> and has columns:
 * <br/>
 * column:   crawl_set    (Decimal) eg 200905010641010.00000
 * <br/>
 * column:   insert_timestamp   TIMESTAMP
 * <br/>
 * column:   crawl_data    COMPRESSED MEDIUMTEXT BASE64 ENCODED
 * </p>
 * <p>
 * Requires a Database called <i>connectordb</i>
 * </p>
 * The Datasource MUST be called jdbc/ConnectorDS:<br/>
 * TOMCAT setup <b>/connector-manager/WEB-INF/web.xml</b><br/>
 * <i>
 *&lt;resource-ref&gt;<br/>
      &lt;description&gt;Connector DB Connection&lt;/description&gt;<br/>
      &lt;res-ref-name&gt;jdbc/ConnectorDS&lt;/res-ref-name&gt;<br/>
      &lt;res-type&gt;javax.sql.DataSource&lt;/res-type&gt;<br/>
      &lt;res-auth&gt;Container&lt;/res-auth&gt;<br/>
  &lt;/resource-ref&gt;<br/></i>
 *<br/>
 *and for exmple in in <b>TOMCAT_ROOT/conf/context.xml:</b><br/><i>
 *  &lt;Resource name="jdbc/ConnectorDS" auth="Container" type="javax.sql.DataSource"<br/>
               maxActive="100" maxIdle="30" maxWait="10000"<br/>
               username="&lt;DBUSERNAME&gt;" password="&lt;DBPASSWORD&gt;" driverClassName="com.mysql.jdbc.Driver"<br/>
               url="jdbc:mysql://&lt;DB_HOST&gt;:3306/connectordb?autoReconnect=true"/&gt;<br/></i>
   <br/>
 * </p>
 * <p>
 * This means youll find files like
 * <li>200905010641010.00000
 * <li>200905010641010.00001
 * <li>200905010641010.00002
 * <br> and on a different quartz invocation
 * <li>200905010801010.00000
 * <br> and on a different day's quartz 
 * <li>200906020905000.00000
 * </p>

 */

public class DBStore implements IStoreType {

    private Logger logger;
    private BaseConnector connector;
    private String instance_table = "";
    private DataSource ds = null;
    
	public DBStore(BaseConnector connector){
	    Connection connection = null;

		logger = Logger.getLogger(this.getClass().getPackage().getName());
		logger.log(Level.INFO,"Initialize DBStore  ");
		this.connector = connector;
		//each connector instance has its own table in the same database
		this.instance_table = "i_" + connector.getInstanceName();

		Statement Stmt = null;
        ResultSet RS = null;
        DatabaseMetaData dbm = null;
        
        boolean table_exists = false;
        
        try {        	
        	//check if the datasource/database exists
	        Context initCtx = new InitialContext();
			Context envCtx = (Context) initCtx.lookup("java:comp/env");
	        ds = (DataSource) envCtx.lookup(BaseConstants.CONNECTOR_DATASOURCE);
	        connection = ds.getConnection();	    	        
  		    connection.setAutoCommit(true);
		    dbm = connection.getMetaData();	      	      
	        logger.log(Level.INFO, "Connected to databaseType " + dbm.getDatabaseProductName()); 
        }
        catch (Exception ex) {
        	logger.log(Level.SEVERE,"Exception initializing Store Datasource " + ex);
        	connection = null;
        	return;
        }
        

		try {	       
	        if (dbm.getDatabaseProductName().equals("MySQL")) {
	        	       
	        	//check if the per-connector table exists
	        	logger.log(Level.FINE, "Checking to see if  connector DB exists...");
	        	Stmt = connection.createStatement();
	        	RS = Stmt.executeQuery("desc " + instance_table);
	        	ResultSetMetaData rsMetaData = RS.getMetaData();
	        	if (rsMetaData.getColumnCount() > 0)
	        		table_exists = true; 
	        	
	        	RS.close();
	        	Stmt.close();
	        }	        
		    else {
		    	logger.log(Level.SEVERE, "Unsupported DATABASE TYPE..." + dbm.getDatabaseProductName());
		    }
	        
		}
		catch (Exception ex){
			logger.log(Level.SEVERE,"Exception initializing Store " + ex);
		}
		
		
		try {
			//if the per-instance table doesn't exist, create it
			if (!table_exists){
				logger.log(Level.INFO, "Creating Instance Table " + instance_table); 
				
				if (dbm.getDatabaseProductName().equals("MySQL")) {
				Statement statement =
		          connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
				String create_stmt = "";
				
				create_stmt = "CREATE TABLE  `" + this.instance_table +"` (" + 
								  "`crawl_set` decimal(19,5) NOT NULL," + 
								  "`insert_timestamp` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP," +
								  "`crawl_data` MEDIUMTEXT default NULL,"+
								  "PRIMARY KEY  (`crawl_set`)," +
								  "KEY `set_index` (`crawl_set`)" +
								  ") ENGINE=MyISAM;" ;
				statement.addBatch(create_stmt);			        								
				statement.executeBatch();
				statement.close();
			}
			else {
				logger.log(Level.INFO, "Instance Table " + instance_table + " already exists"); 
				//connection.close();
				//TODO: somehow figure out if we should delete this table here
			  }			
		}
			
			boolean qrtz_table_exists = false;
		    if (dbm.getDatabaseProductName().equals("MySQL")) {
	        	       
		        	//check if the per-connector table exists
		        	logger.log(Level.FINE, "Checking to see if  quartz tables exists...");
		        	Stmt = connection.createStatement();
		        	try {
		        	RS = Stmt.executeQuery("desc QRTZ_JOB_DETAILS");
		        	ResultSetMetaData rsMetaData = RS.getMetaData();
		        	if (rsMetaData.getColumnCount() > 0)
		        		qrtz_table_exists = true; 
		        	}
		        	catch (Exception ex) {
				    	logger.log(Level.INFO, "Could not find Quartz Tables...creating now..");		        		
		        	}
		        	RS.close();
		        	Stmt.close();
		        }	
		    else {
		    	logger.log(Level.SEVERE, "Unsupported DATABASE TYPE..." + dbm.getDatabaseProductName());
		    }
		    
			if (!qrtz_table_exists){
				logger.log(Level.INFO, "Creating Global Quartz Table "); 
				//the quartz db setup scripts are at
				//quartz-1.8.0/docs/dbTables/tables_mysql.sql
				//one set of Quartz tables can handle any number of triggers/crons
				Statement statement =
			          connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
				
				String create_stmt = "CREATE TABLE QRTZ_JOB_DETAILS (JOB_NAME  VARCHAR(200) NOT NULL,JOB_GROUP VARCHAR(200) NOT NULL,DESCRIPTION VARCHAR(250) NULL,JOB_CLASS_NAME   VARCHAR(250) NOT NULL,IS_DURABLE VARCHAR(1) NOT NULL,IS_VOLATILE VARCHAR(1) NOT NULL,IS_STATEFUL VARCHAR(1) NOT NULL,REQUESTS_RECOVERY VARCHAR(1) NOT NULL,JOB_DATA BLOB NULL,PRIMARY KEY (JOB_NAME,JOB_GROUP));";
				statement.addBatch(create_stmt);
				create_stmt = "CREATE TABLE QRTZ_JOB_LISTENERS  (    JOB_NAME  VARCHAR(200) NOT NULL,    JOB_GROUP VARCHAR(200) NOT NULL,    JOB_LISTENER VARCHAR(200) NOT NULL,    PRIMARY KEY (JOB_NAME,JOB_GROUP,JOB_LISTENER),    FOREIGN KEY (JOB_NAME,JOB_GROUP)    REFERENCES QRTZ_JOB_DETAILS(JOB_NAME,JOB_GROUP));";
				statement.addBatch(create_stmt);					
				create_stmt = "CREATE TABLE QRTZ_FIRED_TRIGGERS  (    ENTRY_ID VARCHAR(95) NOT NULL,    TRIGGER_NAME VARCHAR(200) NOT NULL,    TRIGGER_GROUP VARCHAR(200) NOT NULL,    IS_VOLATILE VARCHAR(1) NOT NULL,    INSTANCE_NAME VARCHAR(200) NOT NULL,    FIRED_TIME BIGINT(13) NOT NULL,    PRIORITY INTEGER NOT NULL,    STATE VARCHAR(16) NOT NULL,    JOB_NAME VARCHAR(200) NULL,    JOB_GROUP VARCHAR(200) NULL,    IS_STATEFUL VARCHAR(1) NULL,    REQUESTS_RECOVERY VARCHAR(1) NULL,    PRIMARY KEY (ENTRY_ID));";
				statement.addBatch(create_stmt);				
				create_stmt = "CREATE TABLE QRTZ_TRIGGERS  (    TRIGGER_NAME VARCHAR(200) NOT NULL,    TRIGGER_GROUP VARCHAR(200) NOT NULL,    JOB_NAME  VARCHAR(200) NOT NULL,    JOB_GROUP VARCHAR(200) NOT NULL,    IS_VOLATILE VARCHAR(1) NOT NULL,    DESCRIPTION VARCHAR(250) NULL,    NEXT_FIRE_TIME BIGINT(13) NULL,    PREV_FIRE_TIME BIGINT(13) NULL,    PRIORITY INTEGER NULL,    TRIGGER_STATE VARCHAR(16) NOT NULL,    TRIGGER_TYPE VARCHAR(8) NOT NULL,    START_TIME BIGINT(13) NOT NULL,    END_TIME BIGINT(13) NULL,    CALENDAR_NAME VARCHAR(200) NULL,    MISFIRE_INSTR SMALLINT(2) NULL,    JOB_DATA BLOB NULL,    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),    FOREIGN KEY (JOB_NAME,JOB_GROUP)        REFERENCES QRTZ_JOB_DETAILS(JOB_NAME,JOB_GROUP));";
				statement.addBatch(create_stmt);				
				create_stmt = "CREATE TABLE QRTZ_SIMPLE_TRIGGERS  (    TRIGGER_NAME VARCHAR(200) NOT NULL,    TRIGGER_GROUP VARCHAR(200) NOT NULL,    REPEAT_COUNT BIGINT(7) NOT NULL,    REPEAT_INTERVAL BIGINT(12) NOT NULL,    TIMES_TRIGGERED BIGINT(10) NOT NULL,    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)        REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP));";
				statement.addBatch(create_stmt);				
				create_stmt = "CREATE TABLE QRTZ_CRON_TRIGGERS  (    TRIGGER_NAME VARCHAR(200) NOT NULL,    TRIGGER_GROUP VARCHAR(200) NOT NULL,    CRON_EXPRESSION VARCHAR(200) NOT NULL,    TIME_ZONE_ID VARCHAR(80),    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)        REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP));";
				statement.addBatch(create_stmt);				
				create_stmt = "CREATE TABLE QRTZ_BLOB_TRIGGERS  (    TRIGGER_NAME VARCHAR(200) NOT NULL,    TRIGGER_GROUP VARCHAR(200) NOT NULL,    BLOB_DATA BLOB NULL,    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)        REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP));";
				statement.addBatch(create_stmt);				
				create_stmt = "CREATE TABLE QRTZ_TRIGGER_LISTENERS  (    TRIGGER_NAME  VARCHAR(200) NOT NULL,    TRIGGER_GROUP VARCHAR(200) NOT NULL,    TRIGGER_LISTENER VARCHAR(200) NOT NULL,    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_LISTENER),    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)        REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP));";
				statement.addBatch(create_stmt);				
				create_stmt = "CREATE TABLE QRTZ_CALENDARS  (    CALENDAR_NAME  VARCHAR(200) NOT NULL,    CALENDAR BLOB NOT NULL,    PRIMARY KEY (CALENDAR_NAME));";
				statement.addBatch(create_stmt);				
				create_stmt = "CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS  (    TRIGGER_GROUP  VARCHAR(200) NOT NULL,     PRIMARY KEY (TRIGGER_GROUP));";
				statement.addBatch(create_stmt);
				create_stmt = "CREATE TABLE QRTZ_SCHEDULER_STATE  (    INSTANCE_NAME VARCHAR(200) NOT NULL,    LAST_CHECKIN_TIME BIGINT(13) NOT NULL,    CHECKIN_INTERVAL BIGINT(13) NOT NULL,    PRIMARY KEY (INSTANCE_NAME));";
				statement.addBatch(create_stmt);
				create_stmt = "CREATE TABLE QRTZ_LOCKS  (    LOCK_NAME  VARCHAR(40) NOT NULL,     PRIMARY KEY (LOCK_NAME));";
				statement.addBatch(create_stmt);				
				create_stmt = "INSERT INTO QRTZ_LOCKS values('TRIGGER_ACCESS');";
				statement.addBatch(create_stmt);
				create_stmt = "INSERT INTO QRTZ_LOCKS values('JOB_ACCESS');";
				statement.addBatch(create_stmt);
				create_stmt = "INSERT INTO QRTZ_LOCKS values('CALENDAR_ACCESS');";
				statement.addBatch(create_stmt);
				create_stmt = "INSERT INTO QRTZ_LOCKS values('STATE_ACCESS');";
				statement.addBatch(create_stmt);
				create_stmt = "INSERT INTO QRTZ_LOCKS values('MISFIRE_ACCESS');";
				statement.addBatch(create_stmt);
				statement.executeBatch();
				statement.close();				
			}
			else {
				logger.log(Level.INFO, "Global Quartz Table already exists "); 
			}			
			connection.close();
			
		}
		catch (Exception ex){
			logger.log(Level.SEVERE,"Exception Creating StoreTable " + ex);
		}		
	}
	
	
	
	public  DocListEntry getDocsImmediatelyAfter(String checkpoint) {
        DatabaseMetaData dbm = null;
        Connection connection = null;

        
		try{
	        
		        connection = ds.getConnection();	
	  		    connection.setAutoCommit(true);

			    dbm = connection.getMetaData();	      	      
			    //get the most recent database row after 'checkpoint'
		        if (dbm.getDatabaseProductName().equals("MySQL")) {
					Statement statement =
			          connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
				    String update_stmt="select crawl_set,insert_timestamp,UNCOMPRESS(crawl_data) as cdata from " + this.instance_table + " where crawl_set>"+checkpoint + "  LIMIT 1";
				    logger.log(Level.FINER,update_stmt);
					ResultSet rs = statement.executeQuery(update_stmt);
					
			        boolean ret_rows = rs.first();
			        
			        if (!ret_rows){
					    logger.log(Level.FINER,"No Rows Returned.");	
					    connection.close();
					    return null;
			        }
			        BigDecimal crawl_set = null;
			        String crawl_data =null;
			        while(ret_rows) {
			        	crawl_set = rs.getBigDecimal("crawl_set");
			            //crawl_data = rs.getString("crawl_data");
			        	crawl_data = rs.getString("cdata");
			            ret_rows = rs.next();
			        }
			        
				      rs.close();
				      statement.close();
				      connection.close();	

				    //BASE64 DECODE 
				    byte[] byte_decoded_entry = org.apache.commons.codec.binary.Base64.decodeBase64(crawl_data.getBytes());
				    crawl_data = new String(byte_decoded_entry);
				      
				    logger.log(Level.INFO,"Returning from DBStore. Index Value: " + crawl_set.toPlainString());	
				    logger.log(Level.FINEST,"Returning from DBStore. " + crawl_data);	
				    DocListEntry dret = new DocListEntry(crawl_set.toPlainString(),crawl_data);  
				    return dret;
		        }
		}
		catch (Exception ex){
			logger.log(Level.SEVERE,"Unable to retrieve docListEntry " + ex);
		}
		return new DocListEntry(checkpoint,null);
	}
	 	
	public  void setDocList(String checkpoint, String str_store_entry){

        DatabaseMetaData dbm = null; 
        Connection connection = null;

        
        logger.log(Level.FINEST,"Setting doclist " + checkpoint);
        logger.log(Level.FINEST,"Setting store_entry " + str_store_entry);
        try {
	        
	        connection = ds.getConnection();	
  		    connection.setAutoCommit(true);

		    dbm = connection.getMetaData();	   

		    //logger.log(Level.FINE,"Base64 ENCODING...");
		    String encode_entry = new String(org.apache.commons.codec.binary.Base64.encodeBase64(str_store_entry.getBytes()));
			str_store_entry = encode_entry;

			//logger.log(Level.FINE,"Setting store_entry ENCODED " + str_store_entry);
			
	        if (dbm.getDatabaseProductName().equals("MySQL")) {
	        	//get the most recent row
				Statement statement =
		          connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
				String update_stmt="select crawl_set from " + this.instance_table + " where crawl_set=(select max(crawl_set) from " + this.instance_table + ")";
				 logger.log(Level.FINE,"Getting lastentryp in db: " + update_stmt);
				ResultSet rs = statement.executeQuery(update_stmt);

		        boolean ret_rows = rs.first();
		        
		        String last_entry_in_db =null;

			    while(ret_rows) {
			        	BigDecimal crawl_set = rs.getBigDecimal("crawl_set");
			            last_entry_in_db = crawl_set.toPlainString();
			            ret_rows = rs.next();
			    }

			    
			    logger.log(Level.FINER,"Last_Entry_in_Database " + last_entry_in_db);	

		        if (last_entry_in_db!=null){
		        	if (last_entry_in_db.startsWith(checkpoint)) {
					    //increment if in the same set
		        		BigDecimal bd = new BigDecimal(last_entry_in_db);
		        		bd = bd.add(new BigDecimal(".00001"));
				        logger.log(Level.INFO,"Adding to DBStore. Index Value: " + bd.toPlainString());	
		        		update_stmt="insert into "+ this.instance_table + " (crawl_set,crawl_data) values (?,COMPRESS(?))";
		        		
		        		PreparedStatement ps = connection.prepareStatement(update_stmt);
		        		ps.setString(1,bd.toPlainString() ); 
		        		ps.setString(2, str_store_entry); 
		        		ps.executeUpdate();
		        		ps.close();												
		        	}
		        	else {	        		
		        		//otherwise add the the 0th row for this set
				        logger.log(Level.INFO,"Adding to DBStore. Index Value: " + checkpoint+ ".00000");	
		        		update_stmt="insert into "+ this.instance_table + " (crawl_set,crawl_data) values (?,COMPRESS(?))";		        		
		        		PreparedStatement ps = connection.prepareStatement(update_stmt);
		        		ps.setString(1,checkpoint+ ".00000" ); 
		        		ps.setString(2, str_store_entry); 
		        		ps.executeUpdate();
		        		ps.close();
		        	}
		        }
		        else {
			        logger.log(Level.INFO,"Adding to DBStore. Index Value: " + checkpoint+ ".00000");	
	        		update_stmt="insert into "+ this.instance_table + " (crawl_set,crawl_data) values (?,COMPRESS(?))";        		
	        		PreparedStatement ps = connection.prepareStatement(update_stmt);
	        		ps.setString(1,checkpoint+ ".00000" ); 
	        		ps.setString(2, str_store_entry); 
	        		ps.executeUpdate();
	        		ps.close();
					
		        }
		        
			      rs.close();
			      statement.close();
			      connection.close();
	        }
        }
        catch (Exception ex) {
        	logger.log(Level.SEVERE,"Exception initializing context Datasource " + ex);
        	return;
        }
	}
}
