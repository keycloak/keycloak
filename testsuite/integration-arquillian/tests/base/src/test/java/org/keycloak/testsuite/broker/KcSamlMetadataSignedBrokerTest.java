/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.broker;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.saml.SamlDocumentStepBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.keycloak.testsuite.util.Matchers.bodyHC;

/**
 *
 * @author rmartinc
 */
public class KcSamlMetadataSignedBrokerTest extends AbstractBrokerTest {

    public class KcSamlMetadataSignedBrokerConfiguration extends KcSamlBrokerConfiguration {

        @Override
        public List<ClientRepresentation> createProviderClients() {
            List<ClientRepresentation> clientRepresentationList = super.createProviderClients();

            String consumerCert = KeyUtils.findActiveSigningKey(adminClient.realm(consumerRealmName()), Algorithm.RS256).getCertificate();
            MatcherAssert.assertThat(consumerCert, Matchers.notNullValue());

            for (ClientRepresentation client : clientRepresentationList) {
                client.setClientAuthenticatorType("client-secret");
                client.setSurrogateAuthRequired(false);

                Map<String, String> attributes = client.getAttributes();
                if (attributes == null) {
                    attributes = new HashMap<>();
                    client.setAttributes(attributes);
                }

                attributes.put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
                attributes.put(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "true");
                attributes.put(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, SignatureAlgorithm.RSA_SHA512.name());
                attributes.put(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, consumerCert);
            }

            return clientRepresentationList;
        }

        @Override
        public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
            IdentityProviderRepresentation result = super.setUpIdentityProvider(syncMode);

            Map<String, String> config = result.getConfig();

            config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
            config.put(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true");
            config.put(SAMLIdentityProviderConfig.USE_METADATA_DESCRIPTOR_URL, "true");
            config.put(SAMLIdentityProviderConfig.METADATA_DESCRIPTOR_URL,
                    BrokerTestTools.getProviderRoot() + "/auth/realms/" + providerRealmName() + "/protocol/saml/descriptor");

            return result;
        }
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlMetadataSignedBrokerConfiguration();
    }

    @Test
    public void testPostLoginUsingDefaultKeyName() throws Exception {
        // do initial login with the current key
        doSamlPostLogin(Status.OK.getStatusCode(), "Update Account Information", this::identityDocument);

        // rotate the key and do not allow refresh <30 it should fail
        rotateKeys(Algorithm.RS256, "rsa-generated");
        doSamlPostLogin(Status.BAD_REQUEST.getStatusCode(), "Invalid signature in response from identity provider", this::identityDocument);

        // ofsset to allow the refresh of the key
        setTimeOffset(35);
        doSamlPostLogin(Status.OK.getStatusCode(), "Update Account Information", this::identityDocument);
    }

    @Test
    public void testPostLoginUsingOnlyX09Data() throws Exception {
        // do initial login with the current key
        doSamlPostLogin(Status.OK.getStatusCode(), "Update Account Information", this::removeKeyNameFromSignature);

        // rotate the key and do not allow refresh <30 it should fail
        rotateKeys(Algorithm.RS256, "rsa-generated");
        doSamlPostLogin(Status.BAD_REQUEST.getStatusCode(), "Invalid signature in response from identity provider", this::removeKeyNameFromSignature);

        // ofsset to allow the refresh of the key
        setTimeOffset(35);
        doSamlPostLogin(Status.OK.getStatusCode(), "Update Account Information", this::removeKeyNameFromSignature);
    }

    @Test
    public void testRedirectLogin() throws Exception {
        try (Closeable clientUpdater = ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm())
                .setAttribute(SamlConfigAttributes.SAML_FORCE_POST_BINDING, "false")
                .update();
             Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "false")
                .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_RESPONSE, "false")
                .update()) {
            // do initial login with the current key
            doSamlRedirectLogin(Status.OK.getStatusCode(), "Update Account Information");

            // rotate keys it should fail
            rotateKeys(Algorithm.RS256, "rsa-generated");
            doSamlRedirectLogin(Status.BAD_REQUEST.getStatusCode(), "Invalid signature in response from identity provider");

            // offset of 35 is not enough (POST require iteration of keys)
            setTimeOffset(35);
            doSamlRedirectLogin(Status.BAD_REQUEST.getStatusCode(), "Invalid signature in response from identity provider.");

            // offset more than one day
            setTimeOffset(24*60*60 + 5);
            doSamlRedirectLogin(Status.OK.getStatusCode(), "Update Account Information");

            // rotate keys it should fail again
            rotateKeys(Algorithm.RS256, "rsa-generated");
            doSamlRedirectLogin(Status.BAD_REQUEST.getStatusCode(), "Invalid signature in response from identity provider");

            // manually refresh after 1d plus 20s (15s more min refresh is 10s)
            setTimeOffset(24*60*60 + 20);
            Assert.assertTrue(adminClient.realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()).reloadKeys());
            doSamlRedirectLogin(Status.OK.getStatusCode(), "Update Account Information");
        }
    }

    private Document identityDocument(Document doc) {
        return doc;
    }

    private Document removeKeyNameFromSignature(Document doc) {
        NodeList nodes = doc.getElementsByTagNameNS(JBossSAMLURIConstants.XMLDSIG_NSURI.get(), JBossSAMLConstants.KEY_INFO.get());
        if (nodes != null && nodes.getLength() > 0) {
            Element keyInfo = (Element) nodes.item(0);
            nodes = keyInfo.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (JBossSAMLURIConstants.XMLDSIG_NSURI.get().equals(node.getNamespaceURI())
                        && "KeyName".equals(node.getLocalName())) {
                    keyInfo.removeChild(node);
                    break;
                }
            }
        }
        return doc;
    }

    private void doSamlPostLogin(int statusCode, String expectedString, SamlDocumentStepBuilder.Saml2DocumentTransformer transformer)
            throws ProcessingException, ConfigurationException, ParsingException {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST,
                BrokerTestTools.getConsumerRoot() + "/sales-post/saml", null);
        Document doc = SAML2Request.convert(loginRep);
        new SamlClientBuilder()
                .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, SamlClient.Binding.POST)
                .build() // Request to consumer IdP
                .login().idp(bc.getIDPAlias()).build()
                .processSamlResponse(SamlClient.Binding.POST).build() // AuthnRequest to producer IdP
                .login().user(bc.getUserLogin(), bc.getUserPassword()).build()
                .processSamlResponse(SamlClient.Binding.POST) // Response from producer IdP
                .transformDocument(transformer)
                .build()
                // first-broker flow: if valid request, it displays an update profile page on consumer realm
                .execute(currentResponse -> {
                    Assert.assertEquals(statusCode, currentResponse.getStatusLine().getStatusCode());
                    MatcherAssert.assertThat(currentResponse, bodyHC(Matchers.containsString(expectedString)));
                });
    }

    private void doSamlRedirectLogin(int statusCode, String expectedString) throws ProcessingException, ConfigurationException, ParsingException {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST,
                BrokerTestTools.getConsumerRoot() + "/sales-post/saml", null);
        Document doc = SAML2Request.convert(loginRep);
        new SamlClientBuilder()
                .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, SamlClient.Binding.REDIRECT)
                .build() // Request to consumer IdP
                .login().idp(bc.getIDPAlias()).build()
                .processSamlResponse(SamlClient.Binding.REDIRECT).build() // AuthnRequest to producer IdP
                .login().user(bc.getUserLogin(), bc.getUserPassword()).build()
                .processSamlResponse(SamlClient.Binding.REDIRECT) // Response from producer IdP
                .build()
                // first-broker flow: if valid request, it displays an update profile page on consumer realm
                .execute(currentResponse -> {
                    Assert.assertEquals(statusCode, currentResponse.getStatusLine().getStatusCode());
                    MatcherAssert.assertThat(currentResponse, bodyHC(Matchers.containsString(expectedString)));
                });
    }

    private ComponentRepresentation createComponentRep(String algorithm, String providerId, String realmId) {
        ComponentRepresentation keys = new ComponentRepresentation();
        keys.setName("generated");
        keys.setProviderType(KeyProvider.class.getName());
        keys.setProviderId(providerId);
        keys.setParentId(realmId);
        keys.setConfig(new MultivaluedHashMap<>());
        keys.getConfig().putSingle("priority", Long.toString(System.currentTimeMillis()));
        keys.getConfig().putSingle("algorithm", algorithm);
        return keys;
    }

    private void rotateKeys(String algorithm, String providerId) {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        String activeKid = providerRealm.keys().getKeyMetadata().getActive().get(algorithm);

        // Rotate public keys on the parent broker
        String realmId = providerRealm.toRepresentation().getId();
        ComponentRepresentation keys = createComponentRep(algorithm, providerId, realmId);
        try (Response response = providerRealm.components().add(keys)) {
            Assert.assertEquals(201, response.getStatus());
        }

        String updatedActiveKid = providerRealm.keys().getKeyMetadata().getActive().get(algorithm);
        Assert.assertNotEquals(activeKid, updatedActiveKid);
    }
}
