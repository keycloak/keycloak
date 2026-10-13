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

import java.io.StringWriter;
import java.net.URI;
import java.util.Collections;
import javax.xml.stream.XMLStreamWriter;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.saml.SPMetadataDescriptor;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLMetadataWriter;

import org.jboss.logging.Logger;


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
            String assertionUrl;
            String logoutUrl;
            URI loginBinding;
            URI logoutBinding = null;

            if (samlClient.forcePostBinding()) {
                assertionUrl = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE);
                logoutUrl = client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE);
                loginBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri();
            } else { //redirect binding
                assertionUrl = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE);
                logoutUrl = client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE);
                loginBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri();
            }
            
            if (samlClient.forceArtifactBinding()) {
                if (client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE) != null) {
                    logoutBinding = JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri();
                    logoutUrl = client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE);
                } else {
                    logoutBinding = loginBinding;
                }
                
                assertionUrl = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE);
                loginBinding = JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri();

            }

            if (assertionUrl == null || assertionUrl.trim().isEmpty()) assertionUrl = client.getManagementUrl();
            if (assertionUrl == null || assertionUrl.trim().isEmpty()) assertionUrl = FALLBACK_ERROR_URL_STRING;
            if (logoutUrl == null || logoutUrl.trim().isEmpty()) logoutUrl = client.getManagementUrl();
            if (logoutUrl == null || logoutUrl.trim().isEmpty()) logoutUrl = FALLBACK_ERROR_URL_STRING;
            if (logoutBinding == null) logoutBinding = loginBinding;

            String nameIdFormat = samlClient.getNameIDFormat();
            if (nameIdFormat == null) nameIdFormat = SamlProtocol.SAML_DEFAULT_NAMEID_FORMAT;
            KeyDescriptorType spCertificate = SPMetadataDescriptor.buildKeyDescriptorType(
                    SPMetadataDescriptor.buildKeyInfoElement(null, samlClient.getClientSigningCertificate()),
                    KeyTypes.SIGNING,
                    null);

            KeyDescriptorType encCertificate = SPMetadataDescriptor.buildKeyDescriptorType(
                    SPMetadataDescriptor.buildKeyInfoElement(null, samlClient.getClientEncryptingCertificate()),
                    KeyTypes.ENCRYPTION,
                    null);

            StringWriter sw = new StringWriter();
            XMLStreamWriter writer = StaxUtil.getXMLStreamWriter(sw);
            SAMLMetadataWriter metadataWriter = new SAMLMetadataWriter(writer);

            EntityDescriptorType entityDescriptor = SPMetadataDescriptor.buildSPDescriptor(
                loginBinding, logoutBinding, new URI(assertionUrl), new URI(logoutUrl), 
                samlClient.requiresClientSignature(), samlClient.requiresAssertionSignature(), samlClient.requiresEncryption(), 
                client.getClientId(), nameIdFormat, Collections.singletonList(spCertificate), Collections.singletonList(encCertificate));
            
            metadataWriter.writeEntityDescriptor(entityDescriptor);

            return sw.toString();
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
