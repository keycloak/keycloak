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
    
    <!-- Remove keycloak datasource definition. For versions from 2.3.0-->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsDS)]
		         /*[local-name()='datasources' and starts-with(namespace-uri(), $nsDS)]
                         /*[local-name()='xa-datasource' and starts-with(namespace-uri(), $nsDS) and @pool-name='KeycloakDS']">
    </xsl:template>

    <!-- Remove keycloak xa-datasource definition. For versions below 2.3.0-->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsDS)]
		         /*[local-name()='datasources' and starts-with(namespace-uri(), $nsDS)]
                         /*[local-name()='datasource' and starts-with(namespace-uri(), $nsDS) and @pool-name='KeycloakDS']">
    </xsl:template>
    
    <xsl:param name="db.jdbc_url"/>
    <xsl:param name="db.hostname"/>
    <xsl:param name="db.name"/>
    <xsl:param name="db.port"/>
    <xsl:param name="driver"/>
    <xsl:param name="datasource.class.xa"/>
    
    <xsl:param name="username"/>
    <xsl:param name="password"/>
    
    <xsl:variable name="newDatasourceDefinition">
        <xa-datasource jndi-name="java:jboss/datasources/KeycloakDS" pool-name="KeycloakDS" enabled="true" use-java-context="true">
            <xsl:choose>
                <xsl:when test="contains($driver, 'oracle')">
                    <xa-datasource-property name="URL">
                        <xsl:value-of select="$db.jdbc_url"/>
                    </xa-datasource-property>
                </xsl:when>
                <xsl:otherwise>
                    <xa-datasource-property name="ServerName">
                        <xsl:value-of select="$db.hostname"/>
                    </xa-datasource-property>
                    <xa-datasource-property name="PortNumber">
                        <xsl:value-of select="$db.port"/>
                    </xa-datasource-property>
                    <xa-datasource-property name="DatabaseName">
                        <xsl:value-of select="$db.name"/>
                    </xa-datasource-property>
                    <xsl:if test="contains($driver, 'db2')">
                        <xa-datasource-property name="DriverType">4</xa-datasource-property>
                    </xsl:if>
                </xsl:otherwise>
            </xsl:choose>
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
        </xa-datasource>
    </xsl:variable>
    
    <xsl:variable name="newDriverDefinition">
        <xsl:if test="$driver != 'h2'">
            <driver name="{$driver}" module="com.{$driver}">
                <xa-datasource-class>
                    <xsl:value-of select="$xa.datasource.class"/>
                </xa-datasource-class>
            </driver>
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
    
    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>