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

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.dom.*;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import com.google.enterprise.connector.salesforce.storetype.DocListEntry;
import com.google.enterprise.connector.salesforce.storetype.FileStore;
import com.google.enterprise.connector.salesforce.storetype.IStoreType;
import com.google.enterprise.connector.salesforce.storetype.MemoryStore;
import com.google.enterprise.connector.salesforce.storetype.DBStore;
import com.google.enterprise.connector.spi.DocumentList;
import com.google.enterprise.connector.spi.SimpleDocument;
import com.google.enterprise.connector.spi.SpiConstants;
import com.google.enterprise.connector.spi.TraversalManager;
import com.google.enterprise.connector.spi.Value;


/**
 * Class implementing the Traversalmanager that gets registered with the
 * connector manager.
 * <p>
 * This class gets called every second or so by the connector-manager requesting a traversal()
 * </p>
 */
public class BaseTraversalManager implements TraversalManager {
    private Logger logger;
    private int batch_limit = 10;
    private BaseConnector connector;
    private FeederManager fm;

    private  IStoreType store;
    private Queue docListIndex;

	private Document XSLTDoc;
	
	//variable that counts the number of traverse() calls
	//used to display every 5th traverse() request to the logger (nothing more)
	private int second_counter =0;
	
	private int running_doc_counter =0;
	
	 /**
	   * Constructor for traversalmanager
	   * @param connector   The connector object that is in context for this traversal
	   */
    public BaseTraversalManager(BaseConnector connector) {
		logger = Logger.getLogger(this.getClass().getPackage().getName());
		logger.log(Level.INFO, " BaseTraversalManager initializing");
		logger.log(Level.INFO, " >>>>>>>>>>>> BaseTraversalManager values from connector " + connector.getGoogleConnectorWorkDir());
		this.connector = connector;
		//get the singleton instance of the feeder manager
  	    fm  =  FeederManager.instance();
  	    //set the callback for this traversalmanager
  	    //this callback is used when the connector so that when quartz invokes the
  	    //sheduledjob we can work our way back to the connector thats its invoked for
  	    fm.setTraversalCallback(this,connector);
  	    //initialize the doclist the traverse() method always looks for
  	    //normally its empty but gets populated by the Quartz schedule job
  	    docListIndex = new LinkedList();	
  	    
  	    if (connector.getStoretype().equalsIgnoreCase("MemoryStore"))
  	    	store = new MemoryStore(connector); 
  	    
  	    if (connector.getStoretype().equalsIgnoreCase("FileStore"))
  	    	store = new FileStore(connector);
  	    
  	    if (connector.getStoretype().equalsIgnoreCase("DBStore"))
  	    	store = new DBStore(connector);
  	    
  	    
  	    if (store==null){
  	    	logger.log(Level.SEVERE,"NO storetype specified..exiting " );
  	    	System.exit(-1);
  	    }
  	    	
	    
		try {
				//initialize the resueable xslt transformer for the SOAP response
				TransformerFactory tFactory = TransformerFactory.newInstance();
				
				String encodedXSLT = connector.getXslt();
				byte[] decode = org.apache.commons.codec.binary.Base64.decodeBase64(encodedXSLT.getBytes());
				XSLTDoc = Util.XMLStringtoDoc(new String(decode));
			}
			catch (Exception ex){
				logger.log(Level.SEVERE, "Unable to create transformer " + ex);
			}	       
    }
    
    
    /**
     * Returns the current batch hint value (not used)
     */
    public int getBatchHint() {
    	return this.batch_limit;
    }

    /**
     * Returns the storetype used by this connector
     */
     public IStoreType getStore(){
     	return this.store;
     }

     /**
      * The connectormanager sets the  batch limit (num of docs to return to it)..
      * this param is ignored by the salesforce connector
      */
    public void setBatchHint(int hint) {
    	logger.log(Level.FINEST, " setBatchHint called " + hint);
    	batch_limit = hint;
    }

    public DocumentList startTraversal() {
    	logger.log(Level.FINEST, " startTraversal called " );
    	DocumentList rdl = traverse("");
        return  rdl;
    }

    /**
     * The sets the  batch limit (num of docs to return to it)..
     * <p>
     * the checkpoint entered into this method is the numeric checkpoint used by the connector/connectormanager
     *  and not the same setting as the LAST_SYNC_DATE used by the quartz scheduler.
     * </p>
     * @param checkpoint   the checkpoint file the traversal manager should resume from 
     * @return DocumentList to return back to the connector-manager
     */
    public DocumentList resumeTraversal(String checkpoint) {
    	logger.log(Level.FINER, " resumeTraversal called checkpoint " + checkpoint );
    	DocumentList rdl = traverse(checkpoint);
        return rdl;
    }
    
    
    
    private DocumentList traverse(String checkpoint) {
    	
    	//count the number of times the traverse() was called
		second_counter++;
    	if (second_counter==10) {
    		//every 10 seconds print a message
    		logger.log(Level.INFO, "[" + connector.getInstanceName() +"] Traverse after [" + checkpoint + "]"); 
    		second_counter=0;
    	}
    	//set the current crawled checkpoint
        System.setProperty(this.getConnector().getInstanceName()+"_lcheckpoint",checkpoint); 
        
        //initialize the basedocument list to return
        BaseDocumentList sdl= null;

    	try
    	{
    	//convert a Date object into a 'numeric' format like 200906010641010
    	Date conv_checkpoint = Util.getNumericDate_from_String(checkpoint);

    	//if its the first time we're doing this...create the checkpoint for now
    	if (conv_checkpoint == null || checkpoint.equalsIgnoreCase("") ) {
    		Date now = new Date();  
    		//hmmm...we could set the checkpoint here as either
    		checkpoint = this.getConnector().getLastsync();
    		//or
    		//checkpoint = Util.getNumericString_from_Date(now);
    	}
    	
          
         //initialize a document list with 0 items and now as the checkpoint
         sdl = new  BaseDocumentList(0, checkpoint); 

         //if your xslt is not processable, return nothing
     	if ( this.XSLTDoc == null) {
     		logger.log(Level.SEVERE, "[" + connector.getInstanceName() +"] Response XSLT not compiled, not proceeded with transforms."); 
     		return sdl;
     	}
     		
     	
     	//ok...so our document list has nothing in it so lets go ask the store if we
     	//have any more docs to process...
      if (docListIndex.size()==0){
    	  getDocListAfter(checkpoint);
      }
         
      
      //now we have some docs in the document array we need to process
   	  if (docListIndex.size()>0 ){	   		
   		  //get a doclist from the queue
		  DocListEntry de = (DocListEntry)docListIndex.poll();
		  
		  //now convert the doclistentry (which is a SOAP response doc)
		  // XML to <document><document> 
		    logger.log(Level.FINE, "Attempting to convert string row to DOM object  [" + de.getCheckpoint() + "]");
		    Document doc_in_xml = Util.XMLStringtoDoc(de.getResponseXML());
		    logger.log(Level.FINE, "Attempting to Transform DOM object  to <document/> [" + de.getCheckpoint() + "]");
			Document transformed_QueryResult = Util.TransformDoctoDoc(doc_in_xml, this.XSLTDoc);
			
			//TODO:  DTD Validate the transformed SOAP query result
			
					    
	        logger.log(Level.FINE, "Extracting <document> objects from transformed response");

	        NodeList nl_documents = transformed_QueryResult.getElementsByTagName("documents");
	        //get the NodeList under <document>
	        Node n_documents = nl_documents.item(0);
	        Vector  v_batch = new Vector();
		 	for (int i=0;i<n_documents.getChildNodes().getLength(); i++)
		 	{		 			 		
		 		Node n_doc = n_documents.getChildNodes().item(i);

		 		if (n_doc.getNodeType() == Node.ELEMENT_NODE) {
		 		      TransformerFactory transfac = TransformerFactory.newInstance();
		 		      Transformer trans = transfac.newTransformer();
		 		      trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		 		      trans.setOutputProperty(OutputKeys.INDENT, "yes");

		 			if (n_doc.getNodeName().equalsIgnoreCase("document")){	
		 			
		 				DOMResult dr = new DOMResult();
		 				trans.transform(new DOMSource(n_doc), dr);
		 				Document newDoc = (Document) dr.getNode();
		 				newDoc.getDocumentElement().normalize();		 				
			 	       v_batch.add(newDoc);
		 			}
		 		}
		 	}
		 	logger.log(Level.FINE, "Found " + v_batch.size() + "  documents in batch response");		

		 	//so now we've populated a vector (v_batch) with XML elements of <document></document>
		 	//objects
		 
		  sdl = new BaseDocumentList(v_batch.size(), de.getCheckpoint());		  
		  for (int i=0; i<v_batch.size(); i++){
			  
			  //now convert each entry in the vector to a basedocument
			  BaseSimpleDocument bdoc = this.convertXMLtoBaseDocument((Document)v_batch.get(i));
	    	  SimpleDocument sd = (SimpleDocument)bdoc;
	    	  sdl.add(sd);
		  }
	}

   	  if (sdl.size()>0) {
		   		  this.running_doc_counter= this.running_doc_counter+ sdl.size();
		   		  logger.log(Level.INFO , "[" + connector.getInstanceName() +"]" +"  Returning " + sdl.size() + " documents to the connector manager.  ");
		   	  }
    	}
         catch (Exception ex){
        	 logger.log(Level.SEVERE,"traverse() error "+ ex);
         }

         //return the doclist
      return  sdl;
    }
    
    public BaseConnector getConnector(){
    	return this.connector; 
    }
    

    /**
     * Looks into the storetype and populates any the internal doclist
     * with items in the store that was crawled by quartz after the checkpoint date
     * @param checkpoint   the checkpoint file the traversal manager should resume from 
     */
    
    private void getDocListAfter(String checkpoint) {
    	logger.log(Level.FINER, "[" + connector.getInstanceName() +"]" +  " Traversal manager requesting docs after " + checkpoint );
	    	DocListEntry  dr =store.getDocsImmediatelyAfter(checkpoint);
	    	if (dr != null)
	    		docListIndex.add(dr);
	}
    
    
    /**
     * Converts the <document></document> xml into a BaseSimpleDocument object
     * that we can send into a documentList object that ultimately gets
     * returned to the connector-manager
     * @param inxml   the xml form of and individual <document></document> object
     */   
	
	private  BaseSimpleDocument convertXMLtoBaseDocument(Document doc) {
		try{
			
	 		HashMap hm_spi = new HashMap();
	 		HashMap hm_meta_tags = new HashMap();
	 		Map props = new HashMap();
	 		String content_value= "";
	 		
	 	      TransformerFactory transfac = TransformerFactory.newInstance();
	 	      Transformer trans = transfac.newTransformer();
	 	      trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	 	      trans.setOutputProperty(OutputKeys.INDENT, "yes");
	 	      trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
	 	      trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
              
	 	      
	 	      //TODO: figure out why the initial doc passed in to the method
	 	      //doesn't have the stylesheet 'fully' applied
	 	      //this is why we do the conversion back and forth below
	 	      //because for some reason the doc->string->doc has the stylesheet applied.
	 	     String sdoc = Util.XMLDoctoString(doc);	
	 	     logger.log(Level.FINEST,"About to convert STORE XML to BaseDocument " + sdoc);
	 	     doc = Util.XMLStringtoDoc(sdoc);

              NodeList nl_document = doc.getElementsByTagName("document");

              Node ndoc = nl_document.item(0);
	
		 		NodeList nl_doc_child = ndoc.getChildNodes();

		 			for (int j=0; j<nl_doc_child.getLength(); j++){
		 				Node cnode = nl_doc_child.item(j);
		 				String doc_child_node_name = cnode.getNodeName();

		 				if (doc_child_node_name.equalsIgnoreCase("spiheaders")){
		 					NodeList nl_spi = cnode.getChildNodes();
		 					for (int k=0; k<nl_spi.getLength();k++){
		 						Node n_spi = nl_spi.item(k);
		 						if (n_spi.getNodeType() == Node.ELEMENT_NODE) {
		 							String spi_name=n_spi.getAttributes().getNamedItem("name").getNodeValue();
		 							String spi_value = "";
		 							if (n_spi.getFirstChild() != null) {
		 								spi_value=n_spi.getFirstChild().getNodeValue();	
			 							logger.log(Level.FINEST,"Adding SPI " + spi_name + " " + spi_value);
		 							}
		 							hm_spi.put(spi_name,spi_value);
		 						}
		 					}			 					
		 				}

		 				if (doc_child_node_name.equalsIgnoreCase("metadata")){
		 					NodeList nl_meta = cnode.getChildNodes();
		 					for (int k=0; k<nl_meta.getLength();k++){
		 						Node n_meta = nl_meta.item(k);
		 						if (n_meta.getNodeType() == Node.ELEMENT_NODE) {
		 							String meta_name=n_meta.getAttributes().getNamedItem("name").getNodeValue();
		 							String meta_value = "";
		 							if (n_meta.getFirstChild() != null) {
		 								meta_value=n_meta.getFirstChild().getNodeValue();	
			 							logger.log(Level.FINEST,"Adding METATAG " + meta_name + " " + meta_value);
		 							}
		 							hm_meta_tags.put(meta_name,meta_value);
		 						}
		 					}				 					
		 				}
		 				
		 				if (doc_child_node_name.equalsIgnoreCase("content")){
		 				   content_value = cnode.getChildNodes().item(0).getNodeValue();
		 				   String encoding_type = "";
		 				   NamedNodeMap attribs = cnode.getAttributes();
		 				   if (attribs.getLength() > 0) {
		 					   Node attrib = attribs.getNamedItem("encoding");
		 					   if (attrib != null)
		 						   encoding_type = attrib.getNodeValue();
		 					   
		 					   if (encoding_type.equalsIgnoreCase("base64") || encoding_type.equalsIgnoreCase("base64binary")){
		 				  	    	byte[] b = org.apache.commons.codec.binary.Base64.decodeBase64(content_value.getBytes());
		 				  	    	ByteArrayInputStream input1 = new ByteArrayInputStream(b); 	
		 				  	        logger.log(Level.FINEST,"Adding base64 encoded CONTENT " + content_value);
		 				  	    	props.put(SpiConstants.PROPNAME_CONTENT, input1);		 						   
		 					   }
		 					   else {
		 						  logger.log(Level.FINEST,"Adding Text/HTML CONTENT " + content_value); 
		 						  props.put(SpiConstants.PROPNAME_CONTENT, content_value);
		 					   }
		 				   }
		 				   else {
		 						  logger.log(Level.FINEST,"Adding default Text/HTML CONTENT " + content_value); 
		 						  props.put(SpiConstants.PROPNAME_CONTENT, content_value);		 					   
		 				   }
		 				}	
		 			}

	  	       
	  	      
	  	      //the hashmap holding the spi headers
	  	      Iterator itr_spi = hm_spi.keySet().iterator();
	  	      
	  	      while (itr_spi.hasNext())
	  	      {
	  	    	  String key = (String)itr_spi.next();
	  	    	  String value = (String)hm_spi.get(key);
	  	    	 
	  	    	  if (key.equals("DEFAULT_MIMETYPE"))
	  	    		  props.put(SpiConstants.DEFAULT_MIMETYPE, value);
	  	    	  
	  	    	  if (key.equals("PROPNAME_ACTION"))
	  	    		  props.put(SpiConstants.PROPNAME_ACTION, value);
	  	    	  
	  	    	  if (key.equals("PROPNAME_CONTENTURL"))
	  	    		  props.put(SpiConstants.PROPNAME_CONTENTURL, value);
	  	    	  
	  	    	  if (key.equals("PROPNAME_DISPLAYURL"))
	  	    		  props.put(SpiConstants.PROPNAME_DISPLAYURL, value);
	  	    	  
	  	    	  if (key.equals("PROPNAME_DOCID"))
	  	    		  props.put(SpiConstants.PROPNAME_DOCID, value);
	  	    	  
	  	    	  if (key.equals("PROPNAME_ISPUBLIC"))
	  	    		  props.put(SpiConstants.PROPNAME_ISPUBLIC, value);

	  	    	  if (key.equals("PROPNAME_LASTMODIFIED"))
	  	    		  props.put(SpiConstants.PROPNAME_LASTMODIFIED, value);
	  	    	  
	  	    	  if (key.equals("PROPNAME_MIMETYPE"))
	  	    		  props.put(SpiConstants.PROPNAME_MIMETYPE, value);
	  	    	  
	  	    	//  if (key.equals("PROPNAME_SEARCHURL"))
	  	    	//	  props.put(SpiConstants.PROPNAME_SEARCHURL, value);
	  	    	  
	  	    	//  if (key.equals("PROPNAME_SECURITYTOKEN"))
	  	    	//	  props.put(SpiConstants.PROPNAME_SECURITYTOKEN, value);

	  	    	  

	  	      }
	  	      
	  	      //hashmap holding the custom metatags
	  	      Iterator itr_meta = hm_meta_tags.keySet().iterator();
	  	      
	  	      while (itr_meta.hasNext())
	  	      {
	  	    	  String key = (String)itr_meta.next();
	  	    	  String value = (String)hm_meta_tags.get(key);	  	    	  
	  	    	  props.put(key, value);
	  	      }
 
		 	BaseSimpleDocument bsd =  createSimpleDocument(new Date(),props);	
		 	return bsd;
			}
			catch (Exception ex) {
				logger.log(Level.SEVERE, "Error " + ex);
			}	
			return null;

	}

	
    /**
     * Creates/converts the custom objects stored in the properties into processable objects
     * most are string...
     */ 
	
    private  BaseSimpleDocument createSimpleDocument(Date cdate, Map props) {
        Map spiValues = new HashMap();
        for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
          String key = (String) iter.next();
          Object obj = props.get(key);
          Value val = null;
          if (obj instanceof String) {
            val = Value.getStringValue((String) obj);
          } else if (obj instanceof Calendar) {
            val = Value.getDateValue((Calendar) obj);
          } else if ((obj instanceof Integer) || (obj instanceof Long) || (obj instanceof Short)) {
              val= Value.getLongValue(((Number) obj).longValue());
          } else if ((obj instanceof Float) || (obj instanceof Double)) {
        	  val = Value.getDoubleValue(((Number) obj).doubleValue());
          } else if (obj instanceof Boolean) {
              val = Value.getBooleanValue(((Boolean) obj).booleanValue());
          } else if (obj instanceof InputStream) {
              val =  Value.getBinaryValue((InputStream) obj);
          } else if (obj instanceof Date) {
              Calendar calendar = new GregorianCalendar();
              calendar.setTime((Date) obj);
              val= Value.getDateValue(calendar); 
          } else {
            throw new AssertionError(obj);
          }
          List values = new ArrayList();
          values.add(val);
          spiValues.put(key, values);
        }       
        return new BaseSimpleDocument(cdate,spiValues);
      }
    
}
    
    



