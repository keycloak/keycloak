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
                xmlns:j="urn:jboss:domain:4.0"
                xmlns:ds="urn:jboss:domain:datasources:4.0"
                xmlns:k="urn:jboss:domain:keycloak:1.1"
                xmlns:sec="urn:jboss:domain:security:1.2"
                version="2.0"
                exclude-result-prefixes="xalan j ds k sec">

    <xsl:param name="config"/>
    <xsl:variable name="inf" select="'urn:jboss:domain:infinispan:'"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="//j:extensions">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <extension module="org.keycloak.keycloak-server-subsystem"/>
            <extension module="org.keycloak.keycloak-adapter-subsystem"/>
            <extension module="org.keycloak.keycloak-saml-adapter-subsystem"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//ds:datasources">
        <xsl:copy>
            <xsl:apply-templates select="node()[name(.)='datasource']"/>
            <datasource jndi-name="java:jboss/datasources/KeycloakDS" jta="false" pool-name="KeycloakDS" use-java-context="true">
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

    <xsl:template match="//j:profile">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <xsl:copy-of select="document('../../../target/dependency/default-config/keycloak-server-default-config.xml')"/>
            <subsystem xmlns="urn:jboss:domain:keycloak:1.1"/>
            <subsystem xmlns="urn:jboss:domain:keycloak-saml:1.1"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//sec:security-domains">
        <xsl:copy>
            <xsl:apply-templates select="node()[name(.)='security-domain']"/>
            <security-domain name="keycloak">
                <authentication>
                    <login-module code="org.keycloak.adapters.jboss.KeycloakLoginModule" flag="required"/>
                </authentication>
            </security-domain>
            <security-domain name="sp" cache-type="default">
                <authentication>
                    <login-module code="org.picketlink.identity.federation.bindings.wildfly.SAML2LoginModule" flag="required"/>
                </authentication>
            </security-domain>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $inf)]">
        <xsl:copy>
            <cache-container name="keycloak" jndi-name="infinispan/Keycloak">
                <local-cache name="realms"/>
                <local-cache name="users"/>
                <local-cache name="sessions"/>
                <local-cache name="offlineSessions"/>
                <local-cache name="loginFailures"/>
                <local-cache name="authorization"/>
                <local-cache name="work"/>
            </cache-container>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>