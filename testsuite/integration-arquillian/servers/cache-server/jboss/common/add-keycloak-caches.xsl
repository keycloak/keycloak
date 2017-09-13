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

    <xsl:variable name="nsCacheServer" select="'urn:infinispan:server:core:'"/>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsCacheServer)]
                        /*[local-name()='cache-container' and starts-with(namespace-uri(), $nsCacheServer) and @name='local']">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />

            <local-cache-configuration name="sessions-cfg" start="EAGER" batching="false">
                <transaction mode="NON_XA" locking="PESSIMISTIC"/>
            </local-cache-configuration>

            <local-cache name="sessions" configuration="sessions-cfg" />
            <local-cache name="offlineSessions" configuration="sessions-cfg" />
            <local-cache name="loginFailures" configuration="sessions-cfg" />
            <local-cache name="actionTokens" configuration="sessions-cfg" />
            <local-cache name="work" configuration="sessions-cfg" />
            <local-cache name="employee-distributable-cache.ssoCache" configuration="sessions-cfg"/>
            <local-cache name="employee-distributable-cache" configuration="sessions-cfg"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsCacheServer)]
                        /*[local-name()='cache-container' and starts-with(namespace-uri(), $nsCacheServer) and @name='clustered']">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />

            <replicated-cache-configuration name="sessions-cfg" mode="ASYNC" start="EAGER" batching="false">
                <transaction mode="NON_XA" locking="PESSIMISTIC"/>
            </replicated-cache-configuration>


            <replicated-cache name="sessions" configuration="sessions-cfg" />
            <replicated-cache name="offlineSessions" configuration="sessions-cfg" />
            <replicated-cache name="loginFailures" configuration="sessions-cfg" />
            <replicated-cache name="actionTokens" configuration="sessions-cfg" />
            <replicated-cache name="work" configuration="sessions-cfg" />
            <replicated-cache name="employee-distributable-cache.ssoCache" configuration="sessions-cfg"/>
            <replicated-cache name="employee-distributable-cache" configuration="sessions-cfg"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>