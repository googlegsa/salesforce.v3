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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import com.google.enterprise.connector.spi.Document;
import com.google.enterprise.connector.spi.DocumentList;
import com.google.enterprise.connector.spi.RepositoryException;


/**
 * Class implementing the actual a DocumentList object to return back to the connector
 * <p>
 * A document list object is what is returned back to the connector after it invokes traverse() on
 * The documentlist object contains a checkpoint string variable the connector uses to store the
 * last 'state' of the document list that was sent.
 * </p>
 * <p>
 * the add() method is inherited on the ArrayList is used to populate the internal arraylist
 * </p>
 * <p>
 * For the salesforce documentlist implementation, the documentlist contains all the documents
 * retrieved from a query set at the time quartz is invoked.  That means, all the checkpoints are the same
 * (except for the .00xx extension which just signifies the number of round trips done by the queryresponse
 * given the query cursor).
 * </p>
 * <p>
 * The document checkpoint for the salesforce implementation is just a string in 'numeric' format
 * like 200906010641010.0015   the .0015 extension is the batch ran on that sync date.  The checkpoint
 * number is stored in the /WEB-INF/connectors/salesforce-connector/<connector>/<connector>_state.txt
 * </p>
 */
public class BaseDocumentList extends ArrayList implements DocumentList  {


    // The Iterator over the DocumentList.
    private Iterator iterator = null;
    private String checkpoint = null;
    private Logger logger;

    
    /**
     * Default constructor for a document List
     * @param size  size of the documentlist to initialize.
     * @param checkpoint the common/shared checkpoint for this entire arraylist
     */    
    public BaseDocumentList(int size, String checkpoint) {
      super(size);
		logger = Logger.getLogger(this.getClass().getPackage().getName());
      this.checkpoint = checkpoint;
    }

    public String checkpoint() throws RepositoryException {
          return checkpoint;
      }
   
    public Document nextDocument() throws RepositoryException {
    	
        if (iterator == null)
          iterator = super.iterator();

        if (iterator.hasNext()) {
          BaseSimpleDocument document = (BaseSimpleDocument) iterator.next();
          return (Document)document;
        } else {
          return null; 
        }
      }


}
