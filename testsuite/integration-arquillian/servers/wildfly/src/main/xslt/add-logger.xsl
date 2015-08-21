<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    <xsl:output method="xml" indent="yes"/>

    <xsl:variable name="nsDS" select="'urn:jboss:domain:logging:'"/>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsDS)]
                        /*[local-name()='root-logger' and starts-with(namespace-uri(), $nsDS)]">
        <logger category="org.hibernate.dialect.Dialect">
            <level name="ALL"/>
        </logger>
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>