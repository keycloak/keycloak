<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ds="urn:jboss:domain:datasources:4.0">

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="//ds:subsystem/ds:datasources/ds:datasource[@jndi-name='java:jboss/datasources/KeycloakDS']">
        <ds:datasource jndi-name="java:jboss/datasources/KeycloakDS" enabled="true" use-java-context="true" pool-name="KeycloakDS" use-ccm="true">
            <ds:connection-url>jdbc:postgresql://${env.POSTGRES_PORT_5432_TCP_ADDR}:${env.POSTGRES_PORT_5432_TCP_PORT:5432}/${env.POSTGRES_DATABASE:keycloak}</ds:connection-url>
            <ds:driver>postgresql</ds:driver>
            <ds:security>
              <ds:user-name>${env.POSTGRES_USER:keycloak}</ds:user-name>
              <ds:password>${env.POSTGRES_PASSWORD:password}</ds:password>
            </ds:security>
            <ds:validation>
                <ds:check-valid-connection-sql>SELECT 1</ds:check-valid-connection-sql>
                <ds:background-validation>true</ds:background-validation>
                <ds:background-validation-millis>60000</ds:background-validation-millis>
            </ds:validation>
            <ds:pool>
                <ds:flush-strategy>IdleConnections</ds:flush-strategy>
            </ds:pool>
        </ds:datasource>
    </xsl:template>

    <xsl:template match="//ds:subsystem/ds:datasources/ds:drivers">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
                <ds:driver name="postgresql" module="org.postgresql.jdbc">
                    <ds:xa-datasource-class>org.postgresql.xa.PGXADataSource</ds:xa-datasource-class>
                </ds:driver>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

