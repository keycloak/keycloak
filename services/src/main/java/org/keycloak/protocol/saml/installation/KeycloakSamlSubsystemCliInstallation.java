/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.saml.installation;

import static org.keycloak.protocol.util.ClientCliInstallationUtil.quote;
import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocol;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import javax.ws.rs.core.UriBuilder;
import org.keycloak.services.resources.RealmsResource;

public class KeycloakSamlSubsystemCliInstallation implements ClientInstallationProvider {

    @Override
    public Response generateInstallation(KeycloakSession session, RealmModel realm, ClientModel client, URI baseUri) {
        SamlClient samlClient = new SamlClient(client);
        StringBuilder builder = new StringBuilder();
        String entityId = client.getBaseUrl() == null ? "SPECIFY YOUR entityID!" : client.getBaseUrl();
        String bindingUrl = RealmsResource.protocolUrl(UriBuilder.fromUri(baseUri))
                .build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString();
        
        builder.append("/subsystem=keycloak-saml/secure-deployment=YOUR-WAR.war/:add\n\n")
                .append("/subsystem=keycloak-saml/secure-deployment=YOUR-WAR.war/SP=")
                .append(quote(entityId))
                .append("/:add(sslPolicy=")
                .append(realm.getSslRequired().name())
                .append(",logoutPage=")
                .append(quote("SPECIFY YOUR LOGOUT PAGE!"))
                .append("\n\n");
        if (samlClient.requiresClientSignature()) {
            builder.append("/subsystem=keycloak-saml/secure-deployment=YOUR-WAR.war/SP=")
                    .append(quote(entityId))
                    .append("/Key=KEY1:add(signing=true, \\\nPrivateKeyPem=")
                    .append(quote(samlClient.getClientSigningPrivateKey() == null ? "PRIVATE KEY NOT SET UP OR KNOWN" : samlClient.getClientSigningPrivateKey()))
                    .append(", \\\nCertificatePem=")
                    .append(quote(samlClient.getClientSigningCertificate() == null ? "YOU MUST CONFIGURE YOUR_CLIENT's SIGNING CERTIFICATE" : samlClient.getClientSigningCertificate()))
                    .append(")\n\n");
        }
        if (samlClient.requiresEncryption()) {
            builder.append("/subsystem=keycloak-saml/secure-deployment=YOUR-WAR.war/SP=")
                    .append(quote(entityId))
                    .append("/Key=KEY2:add(encryption=true,PrivateKeyPem=")
                    .append(quote(samlClient.getClientEncryptingPrivateKey() == null ? "PRIVATE KEY NOT SET UP OR KNOWN" : samlClient.getClientEncryptingPrivateKey()))
                    .append(")\n\n");
        }
        
        builder.append("/subsystem=keycloak-saml/secure-deployment=YOUR-WAR.war/SP=")
                .append(quote(entityId))
                .append("/IDP=idp/:add( \\\n    SingleSignOnService={ \\\n        signRequest=")
                .append(Boolean.toString(samlClient.requiresClientSignature()))
                .append(", \\\n        validateResponseSignature=")
                .append(Boolean.toString(samlClient.requiresRealmSignature()))
                .append(", \\\n        validateAssertionSignature=")
                .append(Boolean.toString(samlClient.requiresAssertionSignature()))
                .append(", \\\n        requestBinding=POST, \\\n        bindingUrl=")
                .append(bindingUrl)
                .append("}, \\\n    SingleLogoutService={ \\\n        signRequest=")
                .append(Boolean.toString(samlClient.requiresClientSignature()))
                .append(", \\\n        signResponse=")
                .append(Boolean.toString(samlClient.requiresClientSignature()))
                .append(", \\\n        validateRequestSignature=")
                .append(Boolean.toString(samlClient.requiresRealmSignature()))
                .append(", \\\n        validateResponseSignature=")
                .append(Boolean.toString(samlClient.requiresRealmSignature()))
                .append(", \\\n        requestBinding=POST, \\\n        responseBinding=POST, \\\n        postBindingUrl=")
                .append(bindingUrl)
                .append(", \\\n        redirectBindingUrl=")
                .append(bindingUrl)
                .append("} \\\n)\n\n");

        if (samlClient.requiresClientSignature()) {
            builder.append("/subsystem=keycloak-saml/secure-deployment=YOUR-WAR.war/SP=")
                    .append(quote(entityId))
                    .append("/IDP=idp/:write-attribute(name=signatureAlgorithm,value=")
                    .append(samlClient.getSignatureAlgorithm())
                    .append(")\n\n");
            if (samlClient.getCanonicalizationMethod() != null) {
                builder.append("/subsystem=keycloak-saml/secure-deployment=YOUR-WAR.war/SP=")
                        .append(quote(entityId))
                        .append("/IDP=idp/:write-attribute(name=signatureCanonicalizationMethod,value=")
                        .append(samlClient.getCanonicalizationMethod())
                        .append(")\n");
            }
        }
        
        return Response.ok(builder.toString(), MediaType.TEXT_PLAIN_TYPE).build();
    }

    @Override
    public String getProtocol() {
        return SamlProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getDisplayType() {
        return "Keycloak SAML JBoss Subsystem CLI";
    }

    @Override
    public String getHelpText() {
        return "CLI script you must edit and apply to your client app server. This type of configuration is useful when you can't or don't want to crack open your WAR file.";
    }

    @Override
    public String getFilename() {
        return "keycloak-saml-subsystem.cli";
    }

    @Override
    public String getMediaType() {
        return MediaType.TEXT_PLAIN;
    }

    @Override
    public boolean isDownloadOnly() {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public ClientInstallationProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return "keycloak-saml-subsystem-cli";
    }
}
