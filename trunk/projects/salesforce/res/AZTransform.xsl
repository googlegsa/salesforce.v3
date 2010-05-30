<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
				xmlns:sf="urn:sobject.enterprise.soap.sforce.com"
				xmlns:java="http://xml.apache.org/xslt/java" version="1.0">
 <xsl:output method="xml"  omit-xml-declaration="no"  />
  
 
 <xsl:variable name="xsltype">https://cs2.salesforce.com/</xsl:variable>

 
 <xsl:template match="/">

    <azdecisions xsl:exclude-result-prefixes="sf soapenv java">

	<xsl:for-each select="/soapenv:Envelope/soapenv:Body/*[namespace-uri()='urn:enterprise.soap.sforce.com' and (local-name()='queryResponse' or local-name()='queryMoreResponse') ]/*[namespace-uri()='urn:enterprise.soap.sforce.com' and local-name()='result']/*[namespace-uri()='urn:enterprise.soap.sforce.com' and local-name()='records']"> 
      		<docID><xsl:value-of disable-output-escaping="yes" select="sf:Id"/></docID>           
      </xsl:for-each>

    </azdecisions>
   </xsl:template>
   
</xsl:stylesheet>

