<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:m="urn:jboss:module:1.3"
                version="2.0"
                exclude-result-prefixes="xalan m">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" />
    
       
    <xsl:param name="database" select="''"/>
    <xsl:param name="version" select="''"/>
    
    <xsl:variable name="newModuleDefinition">
        <module xmlns="urn:jboss:module:1.3" name="com.{$database}">
            <resources>
                <resource-root path="{$database}-{$version}.jar"/>
            </resources>
            <dependencies>
                <module name="javax.api"/>
                <module name="javax.transaction.api"/>
            </dependencies>
        </module>
    </xsl:variable>
    
    <!-- clear whole document -->
    <xsl:template match="/*" />
    
    <!-- Copy new module definition. -->
    <xsl:template match="/*">
        <xsl:copy-of select="$newModuleDefinition"/>
    </xsl:template>

</xsl:stylesheet>