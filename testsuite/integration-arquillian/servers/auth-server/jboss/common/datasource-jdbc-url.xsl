<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                version="2.0"
                exclude-result-prefixes="xalan">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>


    <xsl:variable name="nsDS" select="'urn:jboss:domain:datasources:'"/>
    
    <xsl:param name="pool.name" select="'KeycloakDS'"/>
    <xsl:param name="jdbc.url" />

    <!-- replace JDBC URL -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsDS)]
		         /*[local-name()='datasources' and starts-with(namespace-uri(), $nsDS)]
                         /*[local-name()='datasource' and starts-with(namespace-uri(), $nsDS) and @pool-name=$pool.name]
                         /*[local-name()='connection-url' and starts-with(namespace-uri(), $nsDS)]">
        <connection-url>
            <xsl:value-of select="$jdbc.url"/>
        </connection-url>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>