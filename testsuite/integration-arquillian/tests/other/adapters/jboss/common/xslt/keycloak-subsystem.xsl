<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                version="2.0"
                exclude-result-prefixes="xalan">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:variable name="keycloakSubsystem" select="'urn:jboss:domain:keycloak:1.1'"/>
    <xsl:param name="auth-server-host"/>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $keycloakSubsystem)]">
            <xsl:copy>
                <xsl:apply-templates select="@* | node()" />

                <secure-deployment name="customer-portal-subsystem.war">
                    <realm>demo</realm>
                    <realm-public-key>MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB</realm-public-key>
                    <auth-server-url><xsl:value-of select="$auth-server-host"/>/auth</auth-server-url>
                    <ssl-required>EXTERNAL</ssl-required>
                    <resource>customer-portal-subsystem</resource>
                    <credential name="secret">password</credential>
                </secure-deployment>
            </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>