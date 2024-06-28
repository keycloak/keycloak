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

    <xsl:param name="hotrod.sasl.mechanism" />
    
    <xsl:template match="//*[local-name()='infinispan']/*[local-name()='cache-container' and @name='default']">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='hotrod-connector' and @name='hotrod']">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />
            <!--     Add "authentication" into HotRod connector configuration -->
            <authentication security-realm="default">
                      <!--qop="auth"--> 
                <!--<sasl mechanisms="SCRAM-SHA-512 SCRAM-SHA-384 SCRAM-SHA-256 SCRAM-SHA-1 DIGEST-SHA-512 DIGEST-SHA-384 DIGEST-SHA-256 DIGEST-SHA DIGEST-MD5 PLAIN"-->
                <sasl mechanisms="{$hotrod.sasl.mechanism}"
                      server-name="infinispan">
                    <policy>
                        <no-anonymous value="false" />
                    </policy>
                </sasl>
            </authentication>
        </xsl:copy>
    </xsl:template>
    
    <!-- Configure SSL -->
    <xsl:template match="//*[local-name()='infinispan']
                        /*[local-name()='server']
                        //*[local-name()='security-realm' and @name='default']">
        <xsl:copy copy-namespaces="no">
            <xsl:apply-templates select="@*" />
            <server-identities>
                <ssl>
                    <keystore 
                        path="server.jks" relative-to="infinispan.server.config.path"
                        keystore-password="password" 
                        alias="server" 
                        key-password="password"
                    />
                </ssl>
            </server-identities>
            <xsl:apply-templates select="node()" />
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>