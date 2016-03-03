/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.saml.SPMetadataDescriptor;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlSPDescriptorClientInstallation implements ClientInstallationProvider {
    public static String getSPDescriptorForClient(ClientModel client) {
        SamlClient samlClient = new SamlClient(client);
        String assertionUrl = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE);
        if (assertionUrl == null) assertionUrl = client.getManagementUrl();
        String logoutUrl = client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE);
        if (logoutUrl == null) logoutUrl = client.getManagementUrl();
        String nameIdFormat = samlClient.getNameIDFormat();
        if (nameIdFormat == null) nameIdFormat = SamlProtocol.SAML_DEFAULT_NAMEID_FORMAT;
        return SPMetadataDescriptor.getSPDescriptor(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get(), assertionUrl, logoutUrl, samlClient.requiresClientSignature(), client.getClientId(), nameIdFormat, samlClient.getClientSigningCertificate());
    }

    @Override
    public Response generateInstallation(KeycloakSession session, RealmModel realm, ClientModel client, URI serverBaseUri) {
        String descriptor = getSPDescriptorForClient(client);
        return Response.ok(descriptor, MediaType.TEXT_PLAIN_TYPE).build();
    }

    @Override
    public String getProtocol() {
        return SamlProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getDisplayType() {
        return "SAML Metadata SPSSODescriptor";
    }

    @Override
    public String getHelpText() {
        return "SAML SP Metadata EntityDescriptor or rather SPSSODescriptor. This is an XML file.";
    }

    @Override
    public String getFilename() {
        return "saml-sp-metadata.xml";
    }

    public String getMediaType() {
        return MediaType.APPLICATION_XML;
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
        return "saml-sp-descriptor";
    }
}
