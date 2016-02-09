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
                xmlns:s="urn:jboss:domain:4.0"
                xmlns:u="urn:jboss:domain:undertow:3.0"
                version="2.0"
                exclude-result-prefixes="xalan j u">

    <xsl:param name="config"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <!--enable mod_cluster extension-->
    <xsl:template match="//s:extensions">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <extension module="org.jboss.as.modcluster"/>
        </xsl:copy>
    </xsl:template>

    <!--add filter-ref-->
    <xsl:template match="//u:server[@name='default-server']/u:host[@name='default-host']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <filter-ref name="modcluster"/>
        </xsl:copy>
    </xsl:template>
    
    <!--add filter-->
    <xsl:template match="//u:filters">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <mod-cluster 
                name="modcluster" 
                advertise-socket-binding="modcluster" 
                management-socket-binding="http"
                enable-http2="true"
            />
        </xsl:copy>
    </xsl:template>

    <!--add socket binding-->
    <xsl:template match="//s:socket-binding-group[@name='standard-sockets']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <socket-binding name="modcluster" port="23364" multicast-address="224.0.1.105"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>