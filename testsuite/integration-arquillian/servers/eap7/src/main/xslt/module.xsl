<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                version="2.0"
                exclude-result-prefixes="xalan">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" />
    
       
    <xsl:param name="database" select="''"/>
    <xsl:param name="version" select="''"/>
    
    <xsl:variable name="newModuleDefinition">
        <module xmlns="urn:jboss:module:1.1" name="com.{$database}">
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