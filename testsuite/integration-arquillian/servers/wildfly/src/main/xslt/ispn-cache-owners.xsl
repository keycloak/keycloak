<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:j="urn:jboss:domain:4.0"
                xmlns:i="urn:jboss:domain:infinispan:4.0"
                version="2.0"
                exclude-result-prefixes="xalan i">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:variable name="nsDS" select="'urn:jboss:domain:datasources:'"/>
    
    <xsl:param name="sessionCacheOwners" select="'1'"/>
    <xsl:param name="offlineSessionCacheOwners" select="'1'"/>
    <xsl:param name="loginFailureCacheOwners" select="'1'"/>

    <xsl:template match="//i:cache-container/i:distributed-cache[@name='sessions']/@owners">
        <xsl:attribute name="owners">
            <xsl:value-of select="$sessionCacheOwners"/>
        </xsl:attribute>
    </xsl:template>
    <xsl:template match="//i:cache-container/i:distributed-cache[@name='offlineSessions']/@owners">
        <xsl:attribute name="owners">
            <xsl:value-of select="$offlineSessionCacheOwners"/>
        </xsl:attribute>
    </xsl:template>
    <xsl:template match="//i:cache-container/i:distributed-cache[@name='loginFailures']/@owners">
        <xsl:attribute name="owners">
            <xsl:value-of select="$loginFailureCacheOwners"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>