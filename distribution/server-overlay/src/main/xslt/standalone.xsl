<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:j="urn:jboss:domain:1.7"
                xmlns:ds="urn:jboss:domain:datasources:1.2"
                xmlns:dep="urn:jboss:domain:deployment-scanner:1.1"
                xmlns:k="urn:jboss:domain:keycloak:1.0"
                xmlns:sec="urn:jboss:domain:security:1.2"
                xmlns:log="urn:jboss:domain:logging:1.5"
                version="2.0"
                exclude-result-prefixes="xalan j ds dep k sec log">

    <xsl:param name="config"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="//j:extensions">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <extension module="org.keycloak.keycloak-subsystem"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//ds:datasources">
        <xsl:copy>
            <xsl:apply-templates select="node()[name(.)='datasource']"/>
            <datasource jndi-name="java:jboss/datasources/KeycloakDS" pool-name="KeycloakDS" enabled="true" use-java-context="true">
                <connection-url>jdbc:h2:${jboss.server.data.dir}/keycloak;AUTO_SERVER=TRUE</connection-url>
                <driver>h2</driver>
                <security>
                    <user-name>sa</user-name>
                    <password>sa</password>
                </security>
            </datasource>
            <xsl:apply-templates select="node()[name(.)='drivers']"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//log:root-logger">
        <logger category="org.jboss.resteasy.resteasy_jaxrs.i18n">
            <level name="ERROR"/>
        </logger>
        <xsl:copy-of select="."/>
    </xsl:template>

    <xsl:template match="//j:profile">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <subsystem xmlns="urn:jboss:domain:keycloak:1.0">
                <auth-server name="main-auth-server">
                    <enabled>true</enabled>
                    <web-context>auth</web-context>
                </auth-server>
            </subsystem>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>