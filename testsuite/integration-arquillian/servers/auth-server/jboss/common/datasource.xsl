<!--
  ~ Copyright 2016 Red Hat, Inc. and/or its affiliates
  ~ and other contributors as indicated by the @author tags.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                version="2.0"
                exclude-result-prefixes="xalan">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>


    <xsl:variable name="nsDS" select="'urn:jboss:domain:datasources:'"/>
    <xsl:variable name="nsKS" select="'urn:jboss:domain:keycloak-server'"/>
    
    <!-- Remove keycloak datasource definition. -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsDS)]
		         /*[local-name()='datasources' and starts-with(namespace-uri(), $nsDS)]
                         /*[local-name()='datasource' and starts-with(namespace-uri(), $nsDS) and @pool-name='KeycloakDS']">
    </xsl:template>
    
    <xsl:param name="db.jdbc_url"/>
    <xsl:param name="driver"/>
    <xsl:param name="schema"/>

    <xsl:param name="min.poolsize" select="'10'"/>
    <xsl:param name="max.poolsize" select="'50'"/>
    <xsl:param name="pool.prefill" select="'true'"/>
    
    <xsl:param name="username"/>
    <xsl:param name="password"/>
    
    <xsl:variable name="newDatasourceDefinition">
        <datasource jndi-name="java:jboss/datasources/KeycloakDS" pool-name="KeycloakDS" use-java-context="true">
            <connection-url>
                <xsl:value-of select="$db.jdbc_url"/>
            </connection-url>
            <driver>
                <xsl:value-of select="$driver"/>
            </driver>
            <security>
                <user-name>
                    <xsl:value-of select="$username"/>
                </user-name>
                <password>
                    <xsl:value-of select="$password"/>
                </password>
            </security>
            <pool>
                <min-pool-size>
                    <xsl:value-of select="$min.poolsize"/>
                </min-pool-size>
                <max-pool-size>
                    <xsl:value-of select="$max.poolsize"/>
                </max-pool-size>
                <prefill>
                    <xsl:value-of select="$pool.prefill"/>
                </prefill>
            </pool>
        </datasource>
    </xsl:variable>
    
    <xsl:variable name="newSchemaDefinition">
        <xsl:if test="$schema != 'DEFAULT'">
            <property name="schema" value="{$schema}"/>
        </xsl:if>
    </xsl:variable>

    <xsl:variable name="newDriverDefinition">
        <xsl:if test="$driver != 'h2'">
            <driver name="{$driver}" module="test.jdbc.{$driver}" />
        </xsl:if>
    </xsl:variable>
    
    <!-- Add new datasource definition. -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsDS)]
		         /*[local-name()='datasources' and starts-with(namespace-uri(), $nsDS)]">
        <xsl:copy>
            <xsl:copy-of select="$newDatasourceDefinition"/>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>
    
    <!-- Add new driver definition. -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsDS)]
		         /*[local-name()='datasources' and starts-with(namespace-uri(), $nsDS)]
		         /*[local-name()='drivers' and starts-with(namespace-uri(), $nsDS)]">
        <xsl:copy>
            <xsl:copy-of select="$newDriverDefinition"/>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsKS)]
                         /*[local-name()='spi' and starts-with(namespace-uri(), $nsKS) and @name='connectionsJpa']
                         /*[local-name()='provider' and starts-with(namespace-uri(), $nsKS)]
                         /*[local-name()='properties' and starts-with(namespace-uri(), $nsKS)]">
        <xsl:copy>
            <xsl:copy-of select="$newSchemaDefinition"/>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>
    
    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>