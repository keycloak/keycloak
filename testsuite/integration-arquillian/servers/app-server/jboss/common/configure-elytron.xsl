<!--
  ~ * Copyright 2016 Red Hat, Inc. and/or its affiliates
  ~ * and other contributors as indicated by the @author tags.
  ~ *
  ~ * Licensed under the Apache License, Version 2.0 (the "License");
  ~ * you may not use this file except in compliance with the License.
  ~ * You may obtain a copy of the License at
  ~ *
  ~ * http://www.apache.org/licenses/LICENSE-2.0
  ~ *
  ~ * Unless required by applicable law or agreed to in writing, software
  ~ * distributed under the License is distributed on an "AS IS" BASIS,
  ~ * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ * See the License for the specific language governing permissions and
  ~ * limitations under the License.
  -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:variable name="undertowNamespace" select="'urn:jboss:domain:undertow:'"/>
    <xsl:variable name="elytronNamespace" select="'urn:wildfly:elytron:'"/>
    <xsl:variable name="securityNamespace" select="'urn:jboss:domain:security:'"/>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $elytronNamespace)]/*[local-name()='security-realms']">
        <xsl:copy>
            <xsl:apply-templates select="@* | *"/>
            <custom-realm name="KeycloakSAMLRealm" module="org.keycloak.keycloak-saml-wildfly-elytron-adapter" class-name="org.keycloak.adapters.saml.elytron.KeycloakSecurityRealm"/>
            <custom-realm name="KeycloakOIDCRealm" module="org.keycloak.keycloak-wildfly-elytron-oidc-adapter" class-name="org.keycloak.adapters.elytron.KeycloakSecurityRealm"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $elytronNamespace)]/*[local-name()='security-domains']">
        <xsl:copy>
            <xsl:apply-templates select="@* | *"/>
            <security-domain name="KeycloakDomain" default-realm="KeycloakOIDCRealm" permission-mapper="default-permission-mapper" security-event-listener="local-audit">
                <realm name="KeycloakOIDCRealm"/>
                <realm name="KeycloakSAMLRealm"/>
            </security-domain>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $elytronNamespace)]/*[local-name()='mappers']">
        <xsl:copy>
            <xsl:apply-templates select="@* | *"/>
            <constant-realm-mapper name="keycloak-saml-realm-mapper" realm-name="KeycloakSAMLRealm"/>
            <constant-realm-mapper name="keycloak-oidc-realm-mapper" realm-name="KeycloakOIDCRealm"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $elytronNamespace)]/*[local-name()='http']">
        <xsl:copy>
            <xsl:apply-templates select="@* | *"/>
            <http-authentication-factory name="keycloak-http-authentication" http-server-mechanism-factory="keycloak-http-server-mechanism-factory" security-domain="KeycloakDomain">
                <mechanism-configuration>
                    <mechanism mechanism-name="KEYCLOAK">
                        <mechanism-realm realm-name="KeycloakOIDCRealm" realm-mapper="keycloak-oidc-realm-mapper"/>
                    </mechanism>
                    <mechanism mechanism-name="KEYCLOAK-SAML">
                        <mechanism-realm realm-name="KeycloakSAMLRealm" realm-mapper="keycloak-saml-realm-mapper"/>
                    </mechanism>
                </mechanism-configuration>
            </http-authentication-factory>
            <service-loader-http-server-mechanism-factory name="keycloak-oidc-http-server-mechanism-factory" module="org.keycloak.keycloak-wildfly-elytron-oidc-adapter"/>
            <service-loader-http-server-mechanism-factory name="keycloak-saml-http-server-mechanism-factory" module="org.keycloak.keycloak-saml-wildfly-elytron-adapter"/>
            <aggregate-http-server-mechanism-factory name="keycloak-http-server-mechanism-factory">
                <http-server-mechanism-factory name="keycloak-oidc-http-server-mechanism-factory"/>
                <http-server-mechanism-factory name="keycloak-saml-http-server-mechanism-factory"/>
            </aggregate-http-server-mechanism-factory>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $undertowNamespace)]">
        <xsl:copy>
            <xsl:apply-templates select="@* | *"/>
            <application-security-domains>
                <application-security-domain name="other" http-authentication-factory="keycloak-http-authentication"/>
            </application-security-domains>
        </xsl:copy>
    </xsl:template>

    <!-- Need to remove the legacy security-domain otherwise Elytron will not be enabled to deployments -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $securityNamespace)]/*[local-name()='security-domains']/*[local-name()='security-domain'][@name='keycloak']"/>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>