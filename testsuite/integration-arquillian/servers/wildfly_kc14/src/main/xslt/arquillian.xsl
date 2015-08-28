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
            
            <container qualifier="keycloak-1.4.0.Final" mode="suite" >
                <configuration>
                    <property name="enabled">${migration}</property>
                    <property name="adapterImplClass">org.jboss.as.arquillian.container.managed.ManagedDeployableContainer</property>
                    <property name="jbossHome">${keycloak-1.4.0.Final.home}</property>
                    <property name="javaVmArguments">-Djboss.socket.binding.port-offset=${auth.server.port.offset} -Xms64m -Xmx512m -XX:MaxPermSize=256m</property>
                    <property name="managementPort">${auth.server.management.port}</property>
                    <property name="startupTimeoutInSeconds">${startup.timeout.sec}</property>
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