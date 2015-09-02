<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:j="urn:jboss:domain:1.7"
                xmlns:ds="urn:jboss:domain:datasources:1.2"
                xmlns:k="urn:jboss:domain:keycloak:1.1"
                xmlns:sec="urn:jboss:domain:security:1.1"
                version="2.0"
                exclude-result-prefixes="xalan j ds k sec">

    <xsl:param name="config"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="//j:extensions">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <extension module="org.keycloak.keycloak-adapter-subsystem"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//j:profile">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <subsystem xmlns="urn:jboss:domain:keycloak:1.1"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//sec:security-domains">
        <xsl:copy>
            <xsl:apply-templates select="node()[name(.)='security-domain']"/>
            <security-domain name="keycloak">
                <authentication>
                    <login-module code="org.keycloak.adapters.jboss.KeycloakLoginModule" flag="required"/>
                </authentication>
            </security-domain>
            <security-domain name="sp" cache-type="default">
                <authentication>
                    <login-module code="org.picketlink.identity.federation.bindings.wildfly.SAML2LoginModule" flag="required"/>
                </authentication>
            </security-domain>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>