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
    
    <xsl:param name="load.metric" select="'simple'" />\
    
    <!-- mod-cluster-config -->
    <xsl:template match="//*[local-name()='mod-cluster-config']">
        <mod-cluster-config advertise-socket="modcluster" connector="ajp">
            <xsl:choose>
                <xsl:when test="$load.metric='simple'">
                    <simple-load-provider factor="1"/>
                </xsl:when>
                <xsl:otherwise>
                    <dynamic-load-provider>
                        <load-metric type="{$load.metric}"/>
                    </dynamic-load-provider>
                </xsl:otherwise>
            </xsl:choose>
        </mod-cluster-config>
    </xsl:template>
    
    <!--add socket-binding-->
    <xsl:template match="//*[local-name()='socket-binding-group' and @name='standard-sockets']/*[local-name()='socket-binding' and @name='modcluster']">
        <socket-binding name="modcluster" interface="private" port="0" multicast-address="${{jboss.default.multicast.address:230.0.0.4}}" multicast-port="23364"/>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>