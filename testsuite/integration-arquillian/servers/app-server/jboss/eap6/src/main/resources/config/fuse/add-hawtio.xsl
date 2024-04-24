<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:variable name="keycloakNamespace" select="'urn:jboss:domain:keycloak:'"/>

    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='system-properties']">
        <!--namespaces can be hadcoded here as no other releases of eap6 are planned-->
        <system-properties xmlns="urn:jboss:domain:1.8">
            <property name="hawtio.authenticationEnabled" value="true" />
            <property name="hawtio.realm" value="hawtio" />
            <property name="hawtio.roles" value="admin,viewer" />
            <property name="hawtio.keycloakEnabled" value="true" />
            <property name="hawtio.keycloakClientConfig" value="${{jboss.server.config.dir}}/keycloak-hawtio-client.json" />
            <property name="hawtio.keycloakServerConfig" value="${{jboss.server.config.dir}}/keycloak-hawtio.json" />
        </system-properties>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $keycloakNamespace)]">
        <xsl:copy>
            <secure-deployment name="hawtio.war" xmlns="urn:jboss:domain:keycloak:1.2"/>
        </xsl:copy>
    </xsl:template>


</xsl:stylesheet>