<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:j="urn:jboss:domain:1.7"
                xmlns:w="urn:jboss:domain:web:2.2"
                version="2.0"
                exclude-result-prefixes="xalan j ds k sec">

    <xsl:param name="config"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="//w:connector[@name='http']">
        <xsl:copy-of select="."/>
        <connector name="https" protocol="HTTP/1.1" scheme="https" socket-binding="https" secure="true">
                <ssl name="https" password="secret" certificate-key-file="${{jboss.server.config.dir}}/keycloak.jks"/>
            </connector>
    </xsl:template>

    <xsl:template match="//j:extensions">
         <xsl:copy-of select="."/>
         <system-properties>
             <property name="javax.net.ssl.trustStore" value="${{jboss.server.config.dir}}/keycloak.truststore"/>
             <property name="javax.net.ssl.trustStorePassword" value="secret"/>
         </system-properties>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>