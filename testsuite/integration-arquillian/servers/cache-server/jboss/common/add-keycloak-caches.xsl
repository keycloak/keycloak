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

    <xsl:param name="local.site" />
    <xsl:param name="remote.site" />
    <xsl:param name="transactions.enabled" />

    <xsl:variable name="nsCacheServer" select="'urn:infinispan:server:core:'"/>
    <xsl:variable name="nsJGroups" select="'urn:infinispan:server:jgroups:'"/>

    <!-- Configuration of infinispan caches in infinispan-subsystem -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsCacheServer)]
                        /*[local-name()='cache-container' and starts-with(namespace-uri(), $nsCacheServer) and @name='clustered']">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />

            <replicated-cache-configuration name="sessions-cfg" mode="SYNC" start="EAGER" batching="false">
                <xsl:if test="$transactions.enabled='true'">
                    <transaction mode="NON_DURABLE_XA" locking="PESSIMISTIC"/>
                </xsl:if>
                <locking acquire-timeout="0" />
                <backups>
                    <backup site="{$remote.site}" failure-policy="FAIL" strategy="SYNC" enabled="true">
                        <take-offline min-wait="60000" after-failures="3" />
                    </backup>
                </backups>
            </replicated-cache-configuration>

            <replicated-cache name="sessions" configuration="sessions-cfg" />
            <replicated-cache name="offlineSessions" configuration="sessions-cfg" />
            <replicated-cache name="clientSessions" configuration="sessions-cfg" />
            <replicated-cache name="offlineClientSessions" configuration="sessions-cfg" />
            <replicated-cache name="loginFailures" configuration="sessions-cfg" />
            <replicated-cache name="actionTokens" configuration="sessions-cfg" />
            <replicated-cache name="work" configuration="sessions-cfg" />
            <replicated-cache name="employee-distributable-cache.ssoCache" configuration="sessions-cfg"/>
            <replicated-cache name="employee-distributable-cache" configuration="sessions-cfg"/>
        </xsl:copy>
    </xsl:template>

    <!-- Add "xsite" channel in JGroups subsystem -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsJGroups)]
                        /*[local-name()='channels' and starts-with(namespace-uri(), $nsJGroups) and @default='cluster']">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />

            <channel name="xsite" stack="tcp"/>
        </xsl:copy>
    </xsl:template>

    <!-- Add "relay" to JGroups stack "udp" -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsJGroups)]
                        /*[local-name()='stacks' and starts-with(namespace-uri(), $nsJGroups)]
                        /*[local-name()='stack' and @name='udp']">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />

            <relay site="{$local.site}">
                <remote-site name="{$remote.site}" channel="xsite"/>
                <property name="relay_multicasts">false</property>
            </relay>
        </xsl:copy>
    </xsl:template>

    <!-- Replace MPING with TCPPING in JGroups stack "tcp" -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsJGroups)]
                        /*[local-name()='stacks' and starts-with(namespace-uri(), $nsJGroups)]
                        /*[local-name()='stack' and @name='tcp']
                        /*[local-name()='protocol' and @type='MPING']">

        <protocol type="TCPPING">
            <property name="initial_hosts">localhost[8610],localhost[9610]</property>
            <property name="ergonomics">false</property>
        </protocol>

    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>