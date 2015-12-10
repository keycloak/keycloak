<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:j="urn:jboss:domain:3.0"
                xmlns:ds="urn:jboss:domain:datasources:3.0"
                xmlns:k="urn:jboss:domain:keycloak:1.1"
                xmlns:sec="urn:jboss:domain:security:1.2"
                xmlns:u="urn:jboss:domain:undertow:2.0"
                version="2.0"
                exclude-result-prefixes="xalan j ds k sec">

    <xsl:param name="config"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="//j:security-realms">
        <xsl:copy>
            <xsl:apply-templates select="node()[name(.)='security-realm']"/>
            <security-realm name="UndertowRealm">
                <server-identities>
                    <ssl>
                        <keystore path="adapter.jks" relative-to="jboss.server.config.dir" keystore-password="secret"/>
                    </ssl>
                </server-identities>
            </security-realm>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="//u:http-listener">
        <http-listener name="default" socket-binding="http" redirect-socket="proxy-https" proxy-address-forwarding="true"/>
    </xsl:template>
    <xsl:template match="//u:host">
        <https-listener name="https" socket-binding="proxy-https" security-realm="UndertowRealm"/>
        <xsl:copy-of select="."/>
    </xsl:template>

    <xsl:template match="//j:socket-binding[@name='http']">
         <xsl:copy-of select="."/>
         <socket-binding name="proxy-https" port="8443"/>
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