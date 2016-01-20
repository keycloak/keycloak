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
            
            <container qualifier="app-server-eap6" mode="manual" >
                <configuration>
                    <property name="enabled">${app.server.eap6}</property>
                    <property name="adapterImplClass">org.jboss.as.arquillian.container.managed.ManagedDeployableContainer</property>
                    <property name="jbossHome">${app.server.eap6.home}</property>
                    <property name="javaVmArguments">-Djboss.socket.binding.port-offset=${app.server.port.offset} -Xms64m -Xmx512m -XX:MaxPermSize=256m ${adapter.test.props}</property>
                    <property name="managementAddress">localhost</property>
                    <property name="managementProtocol">remote</property>
                    <property name="managementPort">${app.server.management.port.jmx}</property>
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