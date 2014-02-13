<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:j="urn:jboss:domain:1.3"
                version="2.0"
                exclude-result-prefixes="xalan j">

    <xsl:param name="config"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="node()[name(.)='extensions']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <extension module="org.keycloak.keycloak-wildfly-subsystem"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="node()[name(.)='profile']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <subsystem xmlns="urn:jboss:domain:keycloak:1.0"/>
        </xsl:copy>
    </xsl:template>

    <!-- for some reason, Wildfly 8 final decided to turn off management-native which means jboss-as-maven-plugin no
         longer works -->
    <xsl:template match="node()[name(.)='management-interfaces']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <native-interface security-realm="ManagementRealm">
                <socket-binding native="management-native"/>
            </native-interface>
        </xsl:copy>
    </xsl:template>

    <!-- for some reason, Wildfly 8 final decided to turn off management-native which means jboss-as-maven-plugin no
         longer works -->
    <xsl:template match="node()[name(.)='socket-binding-group']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <socket-binding name="management-native" interface="management" port="9999"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>