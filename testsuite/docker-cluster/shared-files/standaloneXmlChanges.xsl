<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:ds="urn:jboss:domain:datasources:3.0"
                xmlns:logging="urn:jboss:domain:logging:3.0"
                xmlns:ispn="urn:jboss:domain:infinispan:3.0"
                xmlns:mcluster="urn:jboss:domain:modcluster:2.0"
                xmlns:server="urn:jboss:domain:3.0"
                exclude-result-prefixes='ds logging ispn mcluster xalan server'
                >

    <xsl:output method="xml" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="//ds:subsystem/ds:datasources/ds:datasource[@jndi-name='java:jboss/datasources/KeycloakDS']" >
        <ds:datasource jndi-name="java:jboss/datasources/KeycloakDS" pool-name="KeycloakDS" enabled="true" use-java-context="true">
            <ds:connection-url>jdbc:mysql://${mysql.host}/keycloak_db</ds:connection-url>
            <ds:driver>mysql</ds:driver>
            <ds:security>
                <ds:user-name>root</ds:user-name>
                <ds:password>mysecretpassword</ds:password>
            </ds:security>
        </ds:datasource>
    </xsl:template>

    <xsl:template match="//ds:subsystem/ds:datasources/ds:drivers">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
            <ds:driver name="mysql" module="com.mysql">
                <ds:xa-datasource-class>com.mysql.jdbc.jdbc2.optional.MysqlXADataSource</ds:xa-datasource-class>
            </ds:driver>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//logging:subsystem/logging:periodic-rotating-file-handler">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
        <logging:logger category="org.keycloak">
            <logging:level name="DEBUG" />
        </logging:logger>
        <logging:logger category="org.jboss.resteasy.core.ResourceLocator">
            <logging:level name="ERROR" />
        </logging:logger>
    </xsl:template>

    <xsl:template match="//ispn:subsystem/ispn:cache-container[@name='keycloak']">
        <ispn:cache-container name="keycloak" jndi-name="infinispan/Keycloak">
            <ispn:transport lock-timeout="60000"/>
            <ispn:invalidation-cache name="realms" mode="SYNC"/>
            <ispn:invalidation-cache name="users" mode="SYNC"/>
            <ispn:distributed-cache name="sessions" mode="SYNC" owners="2"/>
            <ispn:distributed-cache name="loginFailures" mode="SYNC" owners="2"/>
        </ispn:cache-container>
    </xsl:template>

    <xsl:template match="//mcluster:subsystem/mcluster:mod-cluster-config">
        <mcluster:mod-cluster-config advertise-socket="modcluster" proxies='myproxy' proxy-url="/" balancer="mycluster" advertise="false" connector="ajp" sticky-session="true">
            <mcluster:dynamic-load-provider>
                <mcluster:load-metric type="cpu"/>
            </mcluster:dynamic-load-provider>
        </mcluster:mod-cluster-config>
    </xsl:template>

    <xsl:template match="//server:socket-binding-group">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
            <server:outbound-socket-binding name="myproxy">
                <server:remote-destination host="${{httpd.proxyHost}}" port="${{httpd.proxyPort}}"/>
            </server:outbound-socket-binding>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>