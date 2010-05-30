<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
				xmlns:sf="urn:sobject.enterprise.soap.sforce.com"
 			    version="1.0">
 <xsl:output method="xml"  omit-xml-declaration="no"  />
 
 
 <xsl:variable name="doctype">Case</xsl:variable>
 <xsl:variable name="queue">Enterprise</xsl:variable>
 <xsl:variable name="sf_url">https://cs2.salesforce.com/</xsl:variable>
  
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
PROPNAME_SEARCHURL 	Identifies the URL location of a document in the content management system for metadata-and-URL feed. 
				This property is not used by content feed.
PROPNAME_SECURITYTOKEN
  -->
 
 <xsl:template match="/">

    <documents xsl:exclude-result-prefixes="sf soapenv">

	<xsl:for-each select="/soapenv:Envelope/soapenv:Body/*[namespace-uri()='urn:enterprise.soap.sforce.com' and 
			(local-name()='queryResponse' or local-name()='queryMoreResponse') ]/*[namespace-uri()='urn:enterprise.soap.sforce.com' 
			and local-name()='result']/*[namespace-uri()='urn:enterprise.soap.sforce.com' and local-name()='records']"> 
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
					
	
				</metadata>
				
			<content encoding="none">
 
                    <xsl:text disable-output-escaping="yes"><![CDATA[<![CDATA[  ]]></xsl:text>
  
					<html>
					<title>Case: <xsl:value-of disable-output-escaping="yes" 
						select="sf:CaseNumber"/> - <xsl:value-of disable-output-escaping="yes" 
						select="sf:Subject"/></title>
					<body>
					    
					    
						<h2><a name="summary">Case Summary</a></h2>						
						<table width="100%" border-width="3px"  border="3" border-style="ridge"	
							bgcolor="#CCFFFF" bordercolor="green" bordercollapse="separate"	
							background-color="#fff5ee">						
							<tr>							
					   		</tr>
					   		<tr>
					   		<td><b>CaseNumber:   </b><xsl:value-of disable-output-escaping="yes" 
									select="sf:CaseNumber"/></td>
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
					</body>
					</html>			

                  <xsl:text disable-output-escaping="yes"> <![CDATA[]]]]><![CDATA[>]]></xsl:text>				
			</content>					

        </document>             
      </xsl:for-each>
    </documents>
   </xsl:template>
</xsl:stylesheet>