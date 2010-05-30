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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.enterprise.connector.salesforce.BaseConnector;

/**
 * Implementation of a storetype for the connector
 * <p>
 * This store type saves the SOAP response traversal data taken from salesforce into a filesystem
 * <br/> Specifially, its stores files from each traversal in
 * <br/> /WEB-INF/connectors/salesforce-connector/<connectorname>/filestore/yyyy/mm/dd/yyyyMMddHHmmss.gz.000x
 * <br/> where the yyyy/mm/dd/yyyyMMddHHmmss path is the time the query was run and the filename and for each
 * response that comes back the .00x is incremented.
 * </p>
 * <p>
 * This means youll find files like
 * <li>2009/05/01/200905010641010.0000.gz
 * <li>2009/05/01/200905010641010.0001.gz
 * <li>2009/05/01/200905010641010.0002.gz
 * <br> and on a different quartz invocation
 * <li>2009/05/01/200905010801010.0000.gz
 * <br> and on a different day's quartz 
 * <li>2009/06/02/200906020905000.0000.gz
 * </p>

 */
public class FileStore implements IStoreType{
	

    private Logger logger;
    private BaseConnector connector;
    
	public FileStore(BaseConnector connector){
		logger = Logger.getLogger(this.getClass().getPackage().getName());
		logger.log(Level.INFO,"Initialize FileSystem Store  ");
		this.connector = connector;
	}

	 /** Returns one individual file thats after the checkpoint time
	  * Each file in the filestore is a response document from the
	  * Salesforce query (i.e, one query so a response from a querymore is a second file)
	  * The files are just compressed SOAP responses in the raw form
	  * <p>
	  * According to this routine the file 200905010641010.0001.gz would qualify to get returned if
	  * the checkpoint sent in is 200905010641010.0000  or   200904011221010.0018 but not if 
	  * 200905010641010.0002 or  200906011622000.0000 is sent in.  (look at the file dates as numbers!)
	  * </p>
	   * @param checkpoint in the form yyyyMMddHHmmss
	   */  	
	public DocListEntry getDocsImmediatelyAfter(String checkpoint){
		DocListEntry dret = null;
		logger.log(Level.FINER, "Requsting docs afer  "+ checkpoint );
	    try {
			String filestore_dir = connector.getGoogleConnectorWorkDir()+ 
			System.getProperty("file.separator")  + "filestore" + 
			System.getProperty("file.separator") ;
			
			//take the checkpoint and extract the year, month, date etc
        	String year = checkpoint.substring(0,4);
        	String month = checkpoint.substring(4,6);
        	String day = checkpoint.substring(6,8);
        	String hour = checkpoint.substring(8,10);
			
			String next_file = null;
			
			//create the directory if it doesn't exist
	  	    File yrdir = new File(filestore_dir ); 
	  	    
	  	    if(!yrdir.exists()){
	  		 yrdir.mkdir();
	  		} 
	  	    
	  	    
	  	    //pretty sure the following can be done with recursion...
	  	    //go get the very next file thats after this checkpoint date
	  	    //navigate down the directory tree in order until you find the next document
	  	    if (yrdir==null)
	  	    	return new DocListEntry(checkpoint,null);
	  	    
		    String[] yrkey = yrdir.list();
		    //sort the list so we iterate propertly and in order
		    Arrays.sort(yrkey);
		    for (int i=0; i<yrkey.length;i++){
		    	if (year.compareTo(yrkey[i])<=0){
			    	File modir = new File(filestore_dir + System.getProperty("file.separator") + yrkey[i] ); 
			    	
			  	    if (modir==null)
			  	    	return new DocListEntry(checkpoint,null);
			  	    
				    String[] mokey = modir.list();
				    //sort the list so we iterate propertly and in order
				    Arrays.sort(mokey);
				    for (int j=0; j<mokey.length;j++){
				    	if (month.compareTo(mokey[j])<=0){
				    		File daydir = new File(filestore_dir + System.getProperty("file.separator") + yrkey[i] + System.getProperty("file.separator")+  mokey[j] ); 
				    		
					  	    if (daydir==null)
					  	    	return new DocListEntry(checkpoint,null);
					  	    
						    String[] daykey = daydir.list();
						    //sort the list so we iterate propertly and in order
						    Arrays.sort(daykey);
						    for (int k=0; k<daykey.length;k++){
						    	if (day.compareTo(daykey[k])<=0){
						    		//now iterate to find the file thats greater
						    		File doclists = new File(filestore_dir + System.getProperty("file.separator") + yrkey[i] + System.getProperty("file.separator")+  mokey[j] + System.getProperty("file.separator") + daykey[k] ); 
						    		
							  	    if (doclists==null)
							  	    	return new DocListEntry(checkpoint,null);						    		
						    		
								    String[] doclistskey = doclists.list();
								    //sort the list so we iterate propertly and in order
								    Arrays.sort(doclistskey);
								    for (int l=0; l<doclistskey.length; l++){
								    	String strcmp = doclistskey[l].substring(0, doclistskey[l].indexOf(".gz"));
									    if (checkpoint.compareTo(strcmp)<0){
									    	next_file = doclistskey[l];
									    	//we found the next file
									    	filestore_dir =  filestore_dir + System.getProperty("file.separator") + yrkey[i] + System.getProperty("file.separator")+  mokey[j] + System.getProperty("file.separator") + daykey[k] + System.getProperty("file.separator");
									    	break;
									    }
								    }
						    	}
						    }
				    	}
				    }		    		
		    			    		
		    	}
		    }
			
			
		if (next_file==null)
			return null;
		

	    String filenameminusextension = "";

	    //now get the filename, remove the extension
	    String filename = next_file;
		filenameminusextension = filename.substring(0,filename.indexOf(".gz"));
		    	
				logger.log(Level.FINER, "Returning  DocList contained in  "+ filenameminusextension );
				File file = new File(filestore_dir +  filenameminusextension + ".gz");
				 InputStream is = new FileInputStream(file);
		        long length = file.length();

		        byte[] bytes = new byte[(int)length];
		        int offset = 0;
		        int numRead = 0;
		        while (offset < bytes.length
		               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
		            offset += numRead;
		        }

		        is.close();
		        //unzip it
		         String vret = this.unzipStringFromBytes(bytes);
		         //and convert it to a doclistentry
		         //remember, the filename is a date representation of sorts here
				dret = new DocListEntry(filenameminusextension,vret);				
 
	    if (filenameminusextension.equals(""))
	    	return new DocListEntry(checkpoint,null);

	    return dret;
	    }
	    catch (Exception ex){
	    	logger.log(Level.SEVERE,"Error getting files from Store " + ex);
	    }	    
		return null;
	}
	
	
	
	  private static byte[] zipStringToBytes( String input  ) throws IOException
	  {
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    BufferedOutputStream bufos = new BufferedOutputStream(new GZIPOutputStream(bos));
	    bufos.write( input.getBytes() );
	    bufos.close();
	    byte[] retval= bos.toByteArray();
	    bos.close();
	    return retval;
	  }
	  
	  /**
	   * Unzip a string out of the given gzipped byte array.
	   * @param bytes
	   * @return
	   * @throws IOException 
	   */
	  private static String unzipStringFromBytes( byte[] bytes ) throws IOException
	  {
	    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
	    BufferedInputStream bufis = new BufferedInputStream(new GZIPInputStream(bis));
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    byte[] buf = new byte[1024];
	    int len;
	    while( (len = bufis.read(buf)) > 0 )
	    {
	      bos.write(buf, 0, len);
	    }
	    String retval = bos.toString();
	    bis.close();
	    bufis.close();
	    bos.close();
	    return retval;
	  }
	  
	  /**
	   * Saves the SOAP response with the filename matching the checkpoint
	   * <br/> /WEB-INF/connectors/salesforce-connector/<connectorname>/files/yyyy/mm/dd/yyyyMMddHHmmss.gz.0000x
	   */	
	public  void setDocList(String checkpoint, String  str_store_entry){

		try{
        	
					String filestore_dir = connector.getGoogleConnectorWorkDir()+
															System.getProperty("file.separator") +  "filestore" + 
															System.getProperty("file.separator") ;  
						
					
					
					//extract the year,month,date from the checkpoint
		        	String year = checkpoint.substring(0,4);
		        	String month = checkpoint.substring(4,6);
		        	String day = checkpoint.substring(6,8);
		        	String hour = checkpoint.substring(8,10);
		        	
		        	
		        	//create the folder for the filetime it was crawled
		        	///WEB-INF/connectors/salesforce-connector/<connectorname>/filestore/yyyy/mm/dd/yyyyMMddHHmmss.gz.0000x
		        	
		        
		     	   File dyr = new File(filestore_dir + System.getProperty("file.separator") + year);
		     	   dyr.mkdir();
		     	   File dmonth = new File(filestore_dir + System.getProperty("file.separator")+  year +System.getProperty("file.separator") + month);
		     	   dmonth.mkdir();
		     	   File dday = new File(filestore_dir + System.getProperty("file.separator") + year +System.getProperty("file.separator")+ month+
		     			  System.getProperty("file.separator")+ day);
					dday.mkdir();
					
			  	    File dir = new File(filestore_dir + System.getProperty("file.separator") + year +System.getProperty("file.separator")+ month+
			     			  System.getProperty("file.separator")+ day);				    
				    String[] num_files = dir.list();
				    Arrays.sort(num_files);
				    
		        	filestore_dir = filestore_dir + System.getProperty("file.separator") + year +System.getProperty("file.separator")+ month+
	     			  System.getProperty("file.separator")+ day +  System.getProperty("file.separator");
				    
				    
		        	logger.log(Level.INFO, "Creating filestore path " + filestore_dir);
					
					
					dir = new File(filestore_dir);				    
					//String[] num_files = dir.list();
					Arrays.sort(num_files);

					//if a file exists with the checkpoint time already, increment and add an extension
					//to it to reflect the number 
					BigDecimal  offset = new BigDecimal(".00000");
					for (int i=0; i< num_files.length; i++){
						String key = (String)num_files[i];
						if (key.startsWith(checkpoint)) {
							offset = offset.add(new BigDecimal(".00001"));
						}
					}
					
					String filename = checkpoint+offset; 
					
					//finally compress it
			           FileOutputStream fos = new FileOutputStream(new File(filestore_dir + filename + ".gz"));
			           byte[] bzip = zipStringToBytes(str_store_entry);
			           fos.write(bzip);
	                   fos.close(); 
	                   
	           		logger.log(Level.INFO,"Adding to FileStore. Index Value: " + filename);	                   
	                   
		}
		catch (Exception ex){
			logger.log(Level.SEVERE,"Error in FileSystemStore " + ex);
		}		
		
	}


}

