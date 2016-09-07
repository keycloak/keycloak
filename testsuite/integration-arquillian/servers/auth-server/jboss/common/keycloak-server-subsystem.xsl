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

    <xsl:variable name="nsKS" select="'urn:jboss:domain:keycloak-server'"/>
    <xsl:variable name="truststoreDefinition">
        <spi name="truststore">
            <provider name="file" enabled="true">
                <properties>
                    <property name="file" value="${{jboss.home.dir}}/standalone/configuration/keycloak.truststore"/>
                    <property name="password" value="secret"/>
                    <property name="hostname-verification-policy" value="WILDCARD"/>
                    <property name="disabled" value="false"/>
                </properties>
            </provider>
        </spi>
    </xsl:variable>
    <xsl:variable name="themeModuleDefinition">
        <modules>
            <module>org.keycloak.testsuite.integration-arquillian-testsuite-providers</module>
        </modules>
    </xsl:variable>
    
    <!--inject provider-->
    <xsl:template match="//*[local-name()='providers']/*[local-name()='provider']">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
        <provider>
            <xsl:text>module:org.keycloak.testsuite.integration-arquillian-testsuite-providers</xsl:text>
        </provider>
    </xsl:template>

    <!--inject provider for themes -->
    <xsl:template match="//*[local-name()='theme']">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
            <xsl:copy-of select="$themeModuleDefinition"/>
        </xsl:copy>
    </xsl:template>
    
    <!--inject truststore-->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsKS)]">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
            <xsl:copy-of select="$truststoreDefinition"/>
        </xsl:copy>
    </xsl:template>

    <!--copy everything else-->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>