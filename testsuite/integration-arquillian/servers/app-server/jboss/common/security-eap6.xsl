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
                xmlns:j="urn:jboss:domain:1.8"
                xmlns:w="urn:jboss:domain:web:2.2"
                version="2.0"
                exclude-result-prefixes="xalan j ds k sec">

    <xsl:param name="config"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="//w:connector[@name='http']">
        <xsl:copy-of select="."/>
        <connector name="https" protocol="HTTP/1.1" scheme="https" socket-binding="https" secure="true">
            <ssl name="https" password="secret" certificate-key-file="${{jboss.server.config.dir}}/adapter.jks"/>
        </connector>
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