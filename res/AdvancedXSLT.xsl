<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
				xmlns:sf="urn:sobject.enterprise.soap.sforce.com"
				xmlns:java="http://xml.apache.org/xslt/java"
				xmlns:sfeeder="com.google.enterprise.connector.salesforce.Util"
 			    version="1.0">
 <xsl:output method="xml"  omit-xml-declaration="no"  />
  
 
 <xsl:variable name="doctype">Case</xsl:variable>
 <xsl:variable name="queue">Enterprise</xsl:variable>
 <xsl:variable name="sf_url">https://na4.salesforce.com/</xsl:variable>
 
 <!-- 
DEFAULT_MIMETYPE 	Specifies the default type of file that a connector acquires from a content management system.
PROPNAME_ACTION 	Enables a connector to delete a document from the index of the search appliance.
PROPNAME_CONTENT 	Indicates the content for a document.
PROPNAME_CONTENTURL 	Indicates the URL for the content of a document. This property is reserved for future use.
PROPNAME_DISPLAYURL 	Indicates the URL that appears in the search results.
PROPNAME_DOCID 	Identifies a document in the content management system.
PROPNAME_ISPUBLIC 	Indicates whether a document is publicly readable or is a controlled-access document.
PROPNAME_LASTMODIFIED 	Identifies when a document in the content management system was last modified.
PROPNAME_MIMETYPE	Specifies the type of file that a connector acquires from a content management system.
PROPNAME_SEARCHURL 	Identifies the URL location of a document in the content management system for metadata-and-URL feed. This property is not used by content feed.
PROPNAME_SECURITYTOKEN
  -->
 
 <xsl:template match="/">

    <documents xsl:exclude-result-prefixes="sf soapenv java sfeeder">

	<xsl:for-each select="/soapenv:Envelope/soapenv:Body/*[namespace-uri()='urn:enterprise.soap.sforce.com' and (local-name()='queryResponse' or local-name()='queryMoreResponse') ]/*[namespace-uri()='urn:enterprise.soap.sforce.com' and local-name()='result']/*[namespace-uri()='urn:enterprise.soap.sforce.com' and local-name()='records']"> 
      <document>
      		  <!--  SPIValues
      		  		http://code.google.com/apis/searchappliance/documentation/connectors/110/connector_dev/cdg_traversing.html#mp
      		  -->
      		  <spiheaders>
      		  	<spiheader name="DEFAULT_MIMETYPE">text/html</spiheader>
      		  	<spiheader name="PROPNAME_ACTION">add</spiheader>
      		  	<spiheader name="PROPNAME_CONTENTURL">      		  		
						<xsl:copy-of select="$sf_url"/><xsl:value-of disable-output-escaping="no" select="sf:Id"/> 					
      		  	</spiheader>
      		  	<spiheader name="PROPNAME_DISPLAYURL">
      		  			<xsl:copy-of select="$sf_url"/><xsl:value-of disable-output-escaping="no" select="sf:Id"/>
      		  	</spiheader>
      		  	<spiheader name="PROPNAME_DOCID">
      		  			<xsl:value-of disable-output-escaping="no" select="sf:Id"/>
      		  	</spiheader>
      		  	<spiheader name="PROPNAME_ISPUBLIC">true</spiheader>
      		  	<spiheader name="PROPNAME_LASTMODIFIED">
      		  			<xsl:value-of disable-output-escaping="no" select="sf:LastModifiedDate"/>
      		  	</spiheader>
      		  	<spiheader name="PROPNAME_MIMETYPE">text/html</spiheader>
      		  	<spiheader name="PROPNAME_SEARCHURL"></spiheader>
      		  	<spiheader name="PROPNAME_SECURITYTOKEN"></spiheader>
      		  </spiheaders>

				<metadata>
				
				<xsl:if test="sf:CaseNumber[.!='']">			
					<meta name="CaseNumber">
							<xsl:value-of disable-output-escaping="no" select="sf:CaseNumber"/> 						
					</meta>
			     </xsl:if>
					
				<xsl:if test="sf:ProductLine__c[.!='']">	
					<meta name="ProductLine">
							<xsl:value-of disable-output-escaping="no" select="sf:ProductLine__c"/> 
					</meta>
				</xsl:if>
				
				<xsl:if test="sf:ServiceLevelName__c[.!='']">	
					<meta name="ServiceLevel">
							<xsl:value-of disable-output-escaping="no" select="sf:ServiceLevelName__c"/> 					
					</meta>
				</xsl:if>
				<xsl:if test="sf:Asset/sf:Name[.!='']">	
					<meta name="Asset">
							<xsl:value-of disable-output-escaping="no" select="sf:Asset/sf:Name"/> 
					</meta>
				</xsl:if>
				<xsl:if test="sf:Account/sf:Name[.!='']">	
					<meta name="Account">

<xsl:text disable-output-escaping="no"><![CDATA[<![CDATA[]]></xsl:text><xsl:value-of disable-output-escaping="no" select="sf:Account/sf:Name"/><xsl:text disable-output-escaping="yes"><![CDATA[]]]]><![CDATA[>]]></xsl:text>	

					
					</meta>
				</xsl:if>
				<xsl:if test="sf:Status[.!='']">	
					<meta name="CaseStatus">
							<xsl:value-of disable-output-escaping="no" select="sf:Status"/> 					
					</meta>
				</xsl:if>
				<xsl:if test="sf:Owner/sf:Alias[.!='']">	
					<meta name="CaseOwner">
							<xsl:value-of disable-output-escaping="no" select="sf:Owner/sf:Alias"/> 						
					</meta>
				</xsl:if>
							
										
				<xsl:if test="sf:LastModifiedDate[.!='']">	
					<meta name="lm">
						<xsl:call-template name="formatlmDate">
							<xsl:with-param name="sfdate">
							<xsl:value-of disable-output-escaping="no" select="sf:LastModifiedDate"/>
							</xsl:with-param>
						</xsl:call-template>  										   										 			  
					</meta>
				</xsl:if>
				
					<meta name="RecordType">
							<xsl:value-of disable-output-escaping="no" select="$doctype"/> 
					</meta>
										
					
				</metadata>
				
			<content encoding="none">
 
                    <xsl:text disable-output-escaping="yes"><![CDATA[<![CDATA[  ]]></xsl:text>
  
					<html>
					<title>Case: <xsl:value-of disable-output-escaping="yes" select="sf:CaseNumber"/> - <xsl:value-of disable-output-escaping="yes" select="sf:Subject"/></title>
					<body>
					    
					    <!--  local anchors don't work well in the cached copy
					    <table>
					    <tr>
					    <td><a href="#summary">summary</a></td>
					    <td><a href="#emails">emails</a></td>
					    <td><a href="#comments">comments</a></td>
					    <td><a href="#bugs">bugs</a></td>
					    <td><a href="#history">history</a></td>
					    </tr>
					    </table>
					     -->
					    
						<h2><a name="summary">Case Summary</a></h2>						
						<table width="100%" border-width="3px"  border="3" border-style="ridge"	bgcolor="#CCFFFF" bordercolor="green" bordercollapse="separate"	background-color="#fff5ee">						
							<tr>							
					   			<td><b>Account:      </b><xsl:value-of disable-output-escaping="no" select="sf:Account/sf:Name"/></td>









					   			<td><b>Asset:        </b> <xsl:value-of disable-output-escaping="yes" select="sf:Asset/sf:Name"/> </td>
					   			<td><b>ProductLine:  </b><xsl:value-of disable-output-escaping="yes" select="sf:ProductLine__c"/></td>
					   		</tr>
					   		<tr>
					   			<td><b>CaseNumber:   </b><xsl:value-of disable-output-escaping="yes" select="sf:CaseNumber"/></td>
					   			<td><b>Case Subject: </b><xsl:value-of disable-output-escaping="yes" select="sf:Subject"/></td>
					   			<td><b>Case Status:  </b><xsl:value-of disable-output-escaping="yes" select="sf:Status"/></td>				   		
					   		</tr>
					   		<tr>
					   			<td><b>Case Owner:   </b><xsl:value-of disable-output-escaping="yes" select="sf:Owner/sf:Alias"/></td>
					   			<td><b>ServiceLevel: </b><xsl:value-of disable-output-escaping="yes" select="sf:ServiceLevelName__c"/></td>
					   			<td><b>Indexed on:   </b><xsl:value-of select="java:format(java:java.text.SimpleDateFormat.new('[dd/MM/yyyy hh:mm:ss]'), java:java.util.Date.new())"/></td>
							</tr>
					   		<tr>
					   			<td><b>Case Created:   </b>
					   			         <xsl:call-template name="convertdate">
    											<xsl:with-param name="sfdate"><xsl:value-of disable-output-escaping="yes" select="sf:CreatedDate"/></xsl:with-param>
  										 </xsl:call-template>							        
					   			</td>
					   			<td><b></b></td>
					   			<td><b></b></td>
							</tr>							
						</table>
				    <br/>
				    <p>
				    <h3>Description:</h3>
					      <div STYLE="word-wrap:  break-word"  >
					        <pre width="100%">
					    	<xsl:value-of disable-output-escaping="yes" select="sf:Description"/>
					    	</pre>
					      </div>
				    
				    </p>
				    
					<hr/>				   		
	
					<br/>
					  <h3><a name="emails">EmailMessages</a></h3>
				     <xsl:if test="sf:EmailMessages[.!='']">					  
					    <xsl:for-each select="sf:EmailMessages/*[namespace-uri()='urn:enterprise.soap.sforce.com' and local-name()='records']"> 	
					    
					    <!-- 
					    	Salesforce Date is GMT  2009-05-15T01:38:11Z
					    	to sort it, the following concat converts it to
					    	20090515013811    (numeric sort)
					     -->
					    
					   <xsl:sort select="concat(substring(sf:CreatedDate,1,4),substring(sf:CreatedDate,6,2),substring(sf:CreatedDate,9,2),substring(sf:CreatedDate,12,2),substring(sf:CreatedDate,15,2),substring(sf:CreatedDate,18,2) )" data-type="numeric" order="descending"/> 
					    		
					    							    				    
					    	<table  width="100%" border-width="3px"  border="3" border-style="ridge"	bordercolor="blue" bordercollapse="separate"	background-color="#fff5ee">	
					    		<tr>	
									<td><b>From: </b><xsl:value-of disable-output-escaping="yes" select="sf:FromAddress"/></td> 
								</tr>
								<tr>	
									<td><b>To: </b><xsl:value-of disable-output-escaping="yes" select="sf:ToAddress"/></td> 
								</tr>
								<tr>	
									<td><b>CC: </b><xsl:value-of disable-output-escaping="yes" select="sf:CcAddress"/></td> 
								</tr>
								<tr>
								  
								  	<td><b>Date: </b>  
								       								      
								         <xsl:call-template name="convertdate">
    											<xsl:with-param name="sfdate"><xsl:value-of disable-output-escaping="yes" select="sf:CreatedDate"/></xsl:with-param>
  										 </xsl:call-template>
								        
								    </td> 
								</tr>
								<tr>	
									<td><b>Subject: </b><xsl:value-of disable-output-escaping="yes" select="sf:Subject"/></td>
								</tr>
								<tr>
									<td>  <div STYLE="word-wrap:  break-word"  >
								    	    <pre width="120">
									    		<xsl:value-of disable-output-escaping="yes" select="sf:TextBody"/> 		
								    		</pre>
								      	  </div>
								    </td>
								</tr>
							</table>		
							<br/>																				
					    </xsl:for-each>		
					  </xsl:if> 
					<hr/>   
					<br/>
					   <h3><a name="comments">CaseComments</a></h3>
					    <xsl:if test="sf:CaseComments[.!='']">					   
					    <xsl:for-each select="sf:CaseComments/*[namespace-uri()='urn:enterprise.soap.sforce.com' and local-name()='records']"> 
					       		
					       		<!-- SF GMT date format is 2009-05-15T01:38:11Z   the 
					       		following converts it to 20090515T013811 for sorting
					       		 -->
					       		<xsl:sort select="concat(substring(sf:CreatedDate,1,4),substring(sf:CreatedDate,6,2),substring(sf:CreatedDate,9,2),substring(sf:CreatedDate,12,2),substring(sf:CreatedDate,15,2),substring(sf:CreatedDate,18,2) )" data-type="numeric" order="descending"/> 
					    		
					    	<table  width="100%" border-width="3px"  border="3" border-style="ridge"	bordercolor="black" bordercollapse="separate"	background-color="#fff5ee">	
					    		<tr>
					    			<td width="20%"><b>Date:    </b>
									
								
													<xsl:call-template name="convertdate">
    													<xsl:with-param name="sfdate"><xsl:value-of disable-output-escaping="yes" select="sf:CreatedDate"/></xsl:with-param>
  										 			</xsl:call-template>
  										 			
  										 			
  										 			</td>									
									<td width="80%"><b>Comment: </b>
												<div STYLE="word-wrap:  break-word"  >
								    	   			 <pre>
									    		          <xsl:value-of disable-output-escaping="yes" select="sf:CommentBody"/>		
								    				 </pre>
								      	  		</div>	
									</td>
								</tr>								
							</table>
					    </xsl:for-each>	
					 </xsl:if> 
					
					<hr/>    
					<br/>
					   <h3><a name="bugs">Bugs Cited</a></h3>
					   <xsl:if test="sf:RelatedIssue__r[.!='']">					 					   

					    	<table  width="100%" border-width="3px"  border="3" border-style="ridge"	bordercolor="black" bordercollapse="separate"	background-color="#fff5ee">	
								   <xsl:for-each select="sf:RelatedIssue__r/*[namespace-uri()='urn:enterprise.soap.sforce.com' and local-name()='records']"> 								   
								    	    <tr>
								   					<td><xsl:value-of disable-output-escaping="yes" select="sf:System__c"/></td>
								   					<td><xsl:value-of disable-output-escaping="yes" select="sf:Type__c"/></td>
								   					<td><xsl:value-of disable-output-escaping="yes" select="sf:IssueId__c"/></td>
											
									   					<xsl:if test="sf:System__c[.='Buganizer']">
									   						<td>
									   						<a>
																	<xsl:attribute name="href"><xsl:text>http://b/issue?id=</xsl:text>
																		<xsl:value-of disable-output-escaping="yes" select="sf:IssueId__c"/> 
																	</xsl:attribute><xsl:text>http://b/issue?id=</xsl:text>
																	<xsl:value-of disable-output-escaping="yes" select="sf:IssueId__c"/> 
															</a>
																					   						
															</td>								   					
									   					</xsl:if>

											</tr>																	
									</xsl:for-each>					    		
							</table>

					   </xsl:if>  
					 
					<hr/>    
					<br/>
					   <h3><a name="history">Case History</a></h3>					   
					   <xsl:if test="sf:Histories[.!='']">					 										   
					   <table  width="100%" border-width="3px"  border="3" border-style="ridge"	bordercolor="brown" bordercollapse="separate"	background-color="#fff5ee">	
					    <xsl:for-each select="sf:Histories/*[namespace-uri()='urn:enterprise.soap.sforce.com' and local-name()='records']"> 
					    
					    
 							<xsl:sort select="concat(substring(sf:CreatedDate,1,4),substring(sf:CreatedDate,6,2),substring(sf:CreatedDate,9,2),substring(sf:CreatedDate,12,2),substring(sf:CreatedDate,15,2),substring(sf:CreatedDate,18,2) )" data-type="numeric" order="descending"/> 

				  					    
					    
					    	<tr>
								<td width="33%">Date: <xsl:call-template name="convertdate">
    														<xsl:with-param name="sfdate"><xsl:value-of disable-output-escaping="yes" select="sf:CreatedDate"/></xsl:with-param>
  										 			  </xsl:call-template></td> 
  										 <!--  <xsl:value-of disable-output-escaping="yes" select="sf:CreatedDate"/> -->								
								<td width="33%">OldState: <xsl:value-of disable-output-escaping="yes" select="sf:OldValue"/> </td>
								<td width="33%">NewState: <xsl:value-of disable-output-escaping="yes" select="sf:NewValue"/> </td>								
							</tr>
					    </xsl:for-each>							  			   				     					    
						</table>	
					  </xsl:if>  					    			   					    						    
					</body>
					</html>			

                  <xsl:text disable-output-escaping="yes"> <![CDATA[]]]]><![CDATA[>]]></xsl:text>				
			</content>					

        </document>             
      </xsl:for-each>

    </documents>
   </xsl:template>
   
   
   
   
<xsl:template name="convertdate">
   <xsl:param name="sfdate"/>					  	   
   <xsl:value-of select="sfeeder:convertSF_GMTToLocalTime($sfdate)"/>	
</xsl:template>

   <xsl:template name="formatlmDate">	
   <xsl:param name="sfdate"/>   					  	   
   <xsl:value-of select="sfeeder:formatlmDate($sfdate)"/>	
</xsl:template>
   
   
</xsl:stylesheet>