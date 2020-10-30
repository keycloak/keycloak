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

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.saml.SPMetadataDescriptor;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.model.SPDescriptorModel;
import org.w3c.dom.Element;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.keycloak.dom.saml.v2.metadata.KeyTypes;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlSPDescriptorClientInstallation implements ClientInstallationProvider {

    protected static final Logger logger = Logger.getLogger(SamlSPDescriptorClientInstallation.class);

    public static final String SAML_CLIENT_INSTALATION_SP_DESCRIPTOR = "saml-sp-descriptor";
    private static final String FALLBACK_ERROR_URL_STRING = "ERROR:ENDPOINT_NOT_SET";

    public static String getSPDescriptorForClient(ClientModel client) {
        try {
            SamlClient samlClient = new SamlClient(client);
            SPDescriptorModel sp = new SPDescriptorModel();
            String assertionUrl;
            String logoutUrl;
            if (samlClient.forcePostBinding()) {
                assertionUrl = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE);
                logoutUrl = client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE);
                sp.setBinding(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri());
            } else { // redirect binding
                assertionUrl = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE);
                logoutUrl = client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE);
                sp.setBinding(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri());
            }
            if (assertionUrl == null || assertionUrl.trim().isEmpty())
                assertionUrl = client.getManagementUrl();
            if (assertionUrl == null || assertionUrl.trim().isEmpty())
                assertionUrl = FALLBACK_ERROR_URL_STRING;
            if (logoutUrl == null || logoutUrl.trim().isEmpty())
                logoutUrl = client.getManagementUrl();
            if (logoutUrl == null || logoutUrl.trim().isEmpty())
                logoutUrl = FALLBACK_ERROR_URL_STRING;

            sp.setAssertionEndpoint(new URI(assertionUrl));
            sp.setLogoutEndpoint(new URI(logoutUrl));
            sp.setNameIDPolicyFormat(
                samlClient.getNameIDFormat() != null ? samlClient.getNameIDFormat() : SamlProtocol.SAML_DEFAULT_NAMEID_FORMAT);
            sp.setDisplayName(client.getName());
            sp.setDescription(client.getDescription());
            // add client protocol mapper

            sp.setWantAuthnRequestsSigned(samlClient.requiresClientSignature()); 
            sp.setWantAssertionsSigned(samlClient.requiresAssertionSignature());
            sp.setWantAssertionsEncrypted(samlClient.requiresEncryption());
            sp.setEntityId(client.getClientId());
            sp.setSigningCerts(
                Arrays.asList(SPMetadataDescriptor.buildKeyInfoElement(null, samlClient.getClientSigningCertificate())));
            sp.setEncryptionCerts(
                Arrays.asList(SPMetadataDescriptor.buildKeyInfoElement(null, samlClient.getClientEncryptingCertificate())));
            RealmModel realm = client.getRealm();
            sp.setLocal(realm.getDefaultLocale());
            return SPMetadataDescriptor.getSPDescriptor(sp);
        } catch (Exception ex) {
            logger.error("Cannot generate SP metadata", ex);
            return "";
        }
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
        return SAML_CLIENT_INSTALATION_SP_DESCRIPTOR;
    }
}
