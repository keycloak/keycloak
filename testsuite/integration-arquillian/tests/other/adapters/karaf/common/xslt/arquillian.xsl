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
            
            <container qualifier="app-server-${{app.server}}" mode="manual" >
                <configuration>
                    <property name="enabled">true</property>
<!--                     <property name="adapterImplClass">org.jboss.arquillian.container.osgi.karaf.managed.KarafManagedDeployableContainer</property> -->
                    <property name="adapterImplClass">org.keycloak.testsuite.arquillian.karaf.CustomKarafContainer</property>
                    <property name="autostartBundle">false</property>
                    <property name="karafHome">${app.server.home}</property>
                    <property name="javaHome">${app.server.java.home}</property>
                    <property name="javaVmArguments">
                        ${app.server.karaf.jvm.debug.args}
                        ${adapter.test.props}
                    </property>
                    <property name="jmxServiceURL">service:jmx:rmi://127.0.0.1:44444/jndi/rmi://127.0.0.1:1099/karaf-root</property>
                    <property name="jmxUsername">${app.server.management.user}</property>
                    <property name="jmxPassword">${app.server.management.password}</property>
                </configuration>
            </container>
        </xsl:copy>
    </xsl:template>
    

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>
    

</xsl:stylesheet>