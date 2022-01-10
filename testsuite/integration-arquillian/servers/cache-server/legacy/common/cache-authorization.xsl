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
                exclude-result-prefixes="xalan #all">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:variable name="nsCacheServer" select="'urn:infinispan:server:core:'"/>
    <xsl:variable name="nsDomain" select="'urn:jboss:domain:'"/>
    <xsl:variable name="nsEndpoint" select="'urn:infinispan:server:endpoint:'"/>

    <!-- Configuration of infinispan caches in infinispan-subsystem -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsCacheServer)]
                        /*[local-name()='cache-container' and starts-with(namespace-uri(), $nsCacheServer) and @name='clustered']">
        <xsl:copy>
            <xsl:apply-templates select="@*" />

            <security>
                <authorization>
                    <identity-role-mapper/>
                    <role name="___script_manager" permissions="ALL"/>
                </authorization>
            </security>

            <xsl:apply-templates select="node()" />

        </xsl:copy>
    </xsl:template>

    <!-- Add "authentication" into HotRod connector configuration -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsEndpoint)]
                        /*[local-name()='hotrod-connector' and starts-with(namespace-uri(), $nsEndpoint) and @cache-container='clustered']">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />

            <authentication security-realm="AllowScriptManager">
                <sasl mechanisms="DIGEST-MD5" qop="auth" server-name="keycloak-jdg-server">
                    <policy>
                        <no-anonymous value="false" />
                    </policy>
                </sasl>
            </authentication>
        </xsl:copy>
    </xsl:template>

    <!-- Add "AllowScriptManager" security-realm -->
    <xsl:template match="//*[local-name()='management' and starts-with(namespace-uri(), $nsDomain)]
                        /*[local-name()='security-realms' and starts-with(namespace-uri(), $nsDomain)]">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />

            <xsl:element name="security-realm" namespace="{namespace-uri()}">
                <xsl:attribute name="name">AllowScriptManager</xsl:attribute>
                <xsl:element name="authentication" namespace="{namespace-uri()}">
                    <xsl:element name="users" namespace="{namespace-uri()}">
                        <xsl:element name="user" namespace="{namespace-uri()}">
                            <xsl:attribute name="username">___script_manager</xsl:attribute>
                            <xsl:element name="password" namespace="{namespace-uri()}">not-so-secret-password</xsl:element>
                        </xsl:element>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
        </xsl:copy>
    </xsl:template>
    
    <!-- Configure SSL --> 
    <xsl:template match="//*[local-name()='keystore' and @path='application.keystore']">
        <keystore path="server.jks" relative-to="jboss.server.config.dir" keystore-password="password" alias="server" key-password="password" />
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>