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
                xmlns:m="urn:jboss:module:1.3"
                version="2.0"
                exclude-result-prefixes="xalan m">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" />
    
       
    <xsl:param name="database" select="''"/>
    <xsl:param name="version" select="''"/>
    
    <xsl:variable name="newModuleDefinition">
        <module xmlns="urn:jboss:module:1.3" name="com.{$database}">
            <resources>
                <resource-root path="{$database}-{$version}.jar"/>
            </resources>
            <dependencies>
                <module name="javax.api"/>
                <module name="javax.transaction.api"/>
            </dependencies>
        </module>
    </xsl:variable>
    
    <!-- clear whole document -->
    <xsl:template match="/*" />
    
    <!-- Copy new module definition. -->
    <xsl:template match="/*">
        <xsl:copy-of select="$newModuleDefinition"/>
    </xsl:template>

</xsl:stylesheet>