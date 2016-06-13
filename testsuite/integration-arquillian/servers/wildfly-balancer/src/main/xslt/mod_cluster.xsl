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

    <!--enable mod_cluster extension-->
    <xsl:template match="//*[local-name()='extensions']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <extension module="org.jboss.as.modcluster"/>
        </xsl:copy>
    </xsl:template>

    <!--add filter-ref-->
    <xsl:template match="//*[local-name()='server' and @name='default-server']/*[local-name()='host' and @name='default-host']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <filter-ref name="modcluster"/>
        </xsl:copy>
    </xsl:template>
    
    <!--add filter-->
    <xsl:template match="//*[local-name()='filters']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <mod-cluster 
                name="modcluster" 
                advertise-socket-binding="modcluster" 
                advertise-frequency="${{modcluster.advertise-frequency:2000}}"
                management-socket-binding="http"
                enable-http2="true"
            />
        </xsl:copy>
    </xsl:template>
    
    <!--add private interface -->
    <xsl:template match="/*[local-name()='server']/*[local-name()='interfaces']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <interface name="private">
                <inet-address value="${{jboss.bind.address.private:127.0.0.1}}"/>
            </interface>
        </xsl:copy>
    </xsl:template>

    <!--add socket binding-->
    <xsl:template match="//*[local-name()='socket-binding-group' and @name='standard-sockets']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <socket-binding name="modcluster" interface="private" port="23364" multicast-address="${{jboss.default.multicast.address:230.0.0.4}}"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>