<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:tu="http://tomcat.apache.org/xml"
                version="2.0"
                exclude-result-prefixes="xalan tu">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no" />
    <xsl:strip-space elements="*"/>

    <xsl:template match="//tu:tomcat-users">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <user username="manager" password="arquillian" roles="manager-script"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>