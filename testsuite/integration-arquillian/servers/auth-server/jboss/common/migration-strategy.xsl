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

    <xsl:param name="migration.strategy" />
    <xsl:param name="initialize.empty" />
    
    <xsl:variable name="nsKS" select="'urn:jboss:domain:keycloak-server'"/>

    <!--set migrationStrategy-->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsKS)]
                         /*[local-name()='spi' and starts-with(namespace-uri(), $nsKS) and @name='connectionsJpa']
                         /*[local-name()='provider' and starts-with(namespace-uri(), $nsKS)]
                         /*[local-name()='properties' and starts-with(namespace-uri(), $nsKS)]
                         /*[local-name()='property' and starts-with(namespace-uri(), $nsKS) and @name='migrationStrategy']">
        <property name="migrationStrategy" value="{$migration.strategy}"/>
    </xsl:template>

    <!--set initializeEmpty-->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsKS)]
                         /*[local-name()='spi' and starts-with(namespace-uri(), $nsKS) and @name='connectionsJpa']
                         /*[local-name()='provider' and starts-with(namespace-uri(), $nsKS)]
                         /*[local-name()='properties' and starts-with(namespace-uri(), $nsKS)]
                         /*[local-name()='property' and starts-with(namespace-uri(), $nsKS) and @name='initializeEmpty']">
        <property name="initializeEmpty" value="{$initialize.empty}"/>
    </xsl:template>

    <!--copy everything else-->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

