/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.crypto.Algorithm;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 *
 * @author rmartinc
 */
public class KcSamlMetadataSignedAndEncryptedBrokerTest extends AbstractKcSamlMetadataBrokerTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    public class KcSamlMetadataSignedAndEncryptedBrokerConfiguration extends KcSamlBrokerConfiguration {

        @Override
        public List<ClientRepresentation> createProviderClients() {
            List<ClientRepresentation> clientRepresentationList = super.createProviderClients();
            for (ClientRepresentation client : clientRepresentationList) {
                client.setClientAuthenticatorType("client-secret");
                client.setSurrogateAuthRequired(false);

                Map<String, String> attributes = client.getAttributes();
                if (attributes == null) {
                    attributes = new HashMap<>();
                    client.setAttributes(attributes);
                }

                attributes.put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
                attributes.put(SamlConfigAttributes.SAML_ENCRYPT, "true");
                attributes.put(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "true");
                attributes.put(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, SignatureAlgorithm.RSA_SHA256.name());
                attributes.put(SamlConfigAttributes.SAML_USE_METADATA_DESCRIPTOR_URL, "true");
                attributes.put(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL,
                        BrokerTestTools.getProviderRoot() + "/auth/realms/" + consumerRealmName() + "/broker/" + bc.getIDPAlias() + "/endpoint/descriptor");
            }

            return clientRepresentationList;
        }

        @Override
        public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
            IdentityProviderRepresentation result = super.setUpIdentityProvider(syncMode);

            String providerCert = KeyUtils.findActiveSigningKey(adminClient.realm(providerRealmName()), Algorithm.RS256).getCertificate();
            MatcherAssert.assertThat(providerCert, Matchers.notNullValue());

            Map<String, String> config = result.getConfig();

            config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
            config.put(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true");
            config.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, "true");
            config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, providerCert);

            return result;
        }
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlMetadataSignedAndEncryptedBrokerConfiguration();
    }

    @Test
    public void testPostLogin() throws Exception {
        // do initial login with the current key
        doSamlPostLogin();

        // rotate the key and do not allow refresh <30 it should fail
        rotateKeys(bc.consumerRealmName(), Algorithm.RS256, "rsa-generated");
        doSamlLoginError(SamlClient.Binding.POST);

        // ofsset to allow the refresh of the key
        setTimeOffset(35);
        doSamlPostLogin();
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
            doSamlRedirectLogin();

            // rotate keys it should fail
            rotateKeys(bc.consumerRealmName(), Algorithm.RS256, "rsa-generated");
            doSamlLoginError(SamlClient.Binding.REDIRECT);

            // offset of 35 is not enough (REDIRECT require iteration of keys)
            setTimeOffset(35);
            doSamlLoginError(SamlClient.Binding.REDIRECT);

            // offset more than one day
            setTimeOffset(24*60*60 + 5);
            doSamlRedirectLogin();
        }
    }

    @Test
    public void testRedirectLoginCacheDuration() throws Exception {
        try (Closeable clientUpdater = ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm())
                .setAttribute(SamlConfigAttributes.SAML_FORCE_POST_BINDING, "false")
                .update();
             Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "false")
                .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_RESPONSE, "false")
                .setAttribute(SAMLIdentityProviderConfig.DESCRIPTOR_CACHE_SECONDS, "3600") // cache duration for 1h
                .update()) {

            // do initial login with the current key
            doSamlRedirectLogin();

            // rotate keys it should fail
            rotateKeys(bc.consumerRealmName(), Algorithm.RS256, "rsa-generated");
            doSamlLoginError(SamlClient.Binding.REDIRECT);

            // offset of 35 is not enough (REDIRECT require iteration of keys)
            setTimeOffset(35);
            doSamlLoginError(SamlClient.Binding.REDIRECT);

            // offset more than one hour defined in the descriptor
            setTimeOffset(3600 + 5);
            doSamlRedirectLogin();
        }
    }

    @Test
    public void testEncryption() throws Exception {
        // do initial login with the current key
        doSamlPostLogin();

        // rotate the encryption key, previous one still enabled
        rotateKeys(bc.consumerRealmName(), Algorithm.RSA_OAEP, "rsa-enc-generated");
        updateKeyProvider(bc.consumerRealmName(), "rsa-enc-generated", false, true);
        doSamlPostLogin();

        // disable the previous one, error expected decrypting the response
        updateKeyProvider(bc.consumerRealmName(), "rsa-enc-generated", false, false);
        doSamlPostLogin(Response.Status.BAD_REQUEST.getStatusCode(), null, this::identityDocument);

        // offset one day to force refresh and use the new encryption key
        setTimeOffset(24*60*60 + 5);
        doSamlPostLogin();
    }

    @Test
    public void testEncryptionCacheDuration() throws Exception {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.DESCRIPTOR_CACHE_SECONDS, "3600") // cache duration for 1h
                .update()) {

            // do initial login with the current key
            doSamlPostLogin();

            // rotate the encryption key, previous one still enabled
            rotateKeys(bc.consumerRealmName(), Algorithm.RSA_OAEP, "rsa-enc-generated");
            updateKeyProvider(bc.consumerRealmName(), "rsa-enc-generated", false, true);
            doSamlPostLogin();

            // disable the previous one, error expected decrypting the response
            updateKeyProvider(bc.consumerRealmName(), "rsa-enc-generated", false, false);
            doSamlPostLogin(Response.Status.BAD_REQUEST.getStatusCode(), null, this::identityDocument);

            // offset 1h to force refresh and use the new encryption key
            setTimeOffset(3600 + 5);
            doSamlPostLogin();
        }
    }

    private void doSamlLoginError(SamlClient.Binding binding) throws ProcessingException, ConfigurationException, ParsingException {
        events.clear();

        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST,
                BrokerTestTools.getConsumerRoot() + "/sales-post/saml", null);
        Document doc = SAML2Request.convert(loginRep);
        new SamlClientBuilder()
                .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, SamlClient.Binding.POST)
                .build() // Request to consumer IdP
                .login().idp(bc.getIDPAlias()).build()
                .processSamlResponse(binding).build() // AuthnRequest to producer IdP
                .execute(currentResponse -> {
                    // the login page should present an error because signature is invalid
                    Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), currentResponse.getStatusLine().getStatusCode());
                });

        events.expect(EventType.LOGIN_ERROR)
                .realm(realmsResouce().realm(bc.providerRealmName()).toRepresentation().getId())
                .client((String) null)
                .user((String) null)
                .error(Errors.INVALID_SIGNATURE)
                .assertEvent();
    }
}
