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
            
            <container qualifier="app-server-tomcat" mode="manual" >
                <configuration>
                    <property name="enabled">${app.server.tomcat}</property>
                    <property name="adapterImplClass">org.jboss.arquillian.container.tomcat.managed_7.TomcatManagedContainer</property>
                    <property name="catalinaHome">${tomcat.home}</property>
                    <property name="catalinaBase">${tomcat.home}</property>
                    <property name="bindHttpPort">${app.server.http.port}</property>
                    <property name="jmxPort">${app.server.management.port.tomcat}</property>
                    <property name="user">manager</property>
                    <property name="pass">arquillian</property>
                    <property name="javaVmArguments">${adapter.test.props}</property>
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