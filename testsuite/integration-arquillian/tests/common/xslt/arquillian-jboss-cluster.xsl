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
                xmlns:a="http://jboss.org/schema/arquillian"
                version="2.0"
                exclude-result-prefixes="xalan a">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="/a:arquillian">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            
    <group qualifier="auth-server-jboss-cluster">
        <container qualifier="auth-server-jboss-balancer" mode="suite" >
            <configuration>
                <property name="enabled">${auth.server.jboss.cluster}</property>
                <property name="adapterImplClass">org.jboss.as.arquillian.container.managed.ManagedDeployableContainer</property>
                <property name="jbossHome">${keycloak.balancer.home}</property>
                <property name="jbossArguments">
                    -Djboss.socket.binding.port-offset=${auth.server.port.offset} 
                </property>
                <property name="javaVmArguments">
                    -Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m
                    -Djava.net.preferIPv4Stack=true
                </property>
                <property name="outputToConsole">${frontend.console.output}</property>
                <property name="managementPort">${auth.server.management.port}</property>
                <property name="startupTimeoutInSeconds">${auth.server.startup.timeout}</property>
            </configuration>
        </container>
        <container qualifier="auth-server-jboss-backend1" mode="manual" >
            <configuration>
                <property name="enabled">${auth.server.jboss.cluster}</property>
                <property name="adapterImplClass">org.jboss.as.arquillian.container.managed.ManagedDeployableContainer</property>
                <property name="jbossHome">${keycloak.backend1.home}</property>
                <property name="serverConfig">standalone-ha.xml</property>
                <property name="jbossArguments">
                    -Djboss.socket.binding.port-offset=${auth.server.backend1.port.offset} 
                    -Djboss.node.name=node1
                    ${adapter.test.props}
                </property>
                <property name="javaVmArguments">
                    -Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m
                    -Djava.net.preferIPv4Stack=true
                </property>
                <property name="outputToConsole">${backends.console.output}</property>
                <property name="managementPort">${auth.server.backend1.management.port}</property>
                <property name="startupTimeoutInSeconds">${auth.server.startup.timeout}</property>
            </configuration>
        </container>
        <container qualifier="auth-server-jboss-backend2" mode="manual" >
            <configuration>
                <property name="enabled">${auth.server.jboss.cluster}</property>
                <property name="adapterImplClass">org.jboss.as.arquillian.container.managed.ManagedDeployableContainer</property>
                <property name="jbossHome">${keycloak.backend2.home}</property>
                <property name="serverConfig">standalone-ha.xml</property>
                <property name="jbossArguments">
                    -Djboss.socket.binding.port-offset=${auth.server.backend2.port.offset} 
                    -Djboss.node.name=node2
                    ${adapter.test.props}
                </property>
                <property name="javaVmArguments">
                    -Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m
                    -Djava.net.preferIPv4Stack=true
                </property>
                <property name="outputToConsole">${backends.console.output}</property>
                <property name="managementPort">${auth.server.backend2.management.port}</property>
                <property name="startupTimeoutInSeconds">${auth.server.startup.timeout}</property>
            </configuration>
        </container>
    </group>

            
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>