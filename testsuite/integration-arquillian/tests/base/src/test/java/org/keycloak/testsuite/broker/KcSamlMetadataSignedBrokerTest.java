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

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.KeyUtils;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class KcSamlMetadataSignedBrokerTest extends AbstractKcSamlMetadataBrokerTest {

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
        rotateKeys(bc.providerRealmName(), Algorithm.RS256, "rsa-generated");
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
        rotateKeys(bc.providerRealmName(), Algorithm.RS256, "rsa-generated");
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
            rotateKeys(bc.providerRealmName(), Algorithm.RS256, "rsa-generated");
            doSamlRedirectLogin(Status.BAD_REQUEST.getStatusCode(), "Invalid signature in response from identity provider");

            // offset of 35 is not enough (REDIRECT require iteration of keys)
            setTimeOffset(35);
            doSamlRedirectLogin(Status.BAD_REQUEST.getStatusCode(), "Invalid signature in response from identity provider.");

            // offset more than one day
            setTimeOffset(24*60*60 + 5);
            doSamlRedirectLogin(Status.OK.getStatusCode(), "Update Account Information");

            // rotate keys it should fail again
            rotateKeys(bc.providerRealmName(), Algorithm.RS256, "rsa-generated");
            doSamlRedirectLogin(Status.BAD_REQUEST.getStatusCode(), "Invalid signature in response from identity provider");

            // manually refresh after 1d plus 20s (15s more min refresh is 10s)
            setTimeOffset(24*60*60 + 20);
            Assert.assertTrue(adminClient.realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()).reloadKeys());
            doSamlRedirectLogin(Status.OK.getStatusCode(), "Update Account Information");
        }
    }

    @Test
    public void testRedirectLoginCacheDuration() throws Exception {
        try (Closeable realmUpdater = new RealmAttributeUpdater(adminClient.realm(bc.providerRealmName()))
                .setAttribute(SamlConfigAttributes.SAML_DESCRIPTOR_CACHE_SECONDS, "3600") // set cache in the descriptor to 1h
                .update();
             Closeable clientUpdater = ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm())
                .setAttribute(SamlConfigAttributes.SAML_FORCE_POST_BINDING, "false")
                .update();
             Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "false")
                .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_RESPONSE, "false")
                .update()) {
            // do initial login with the current key
            doSamlRedirectLogin(Status.OK.getStatusCode(), "Update Account Information");

            // rotate keys it should fail
            rotateKeys(bc.providerRealmName(), Algorithm.RS256, "rsa-generated");
            doSamlRedirectLogin(Status.BAD_REQUEST.getStatusCode(), "Invalid signature in response from identity provider");

            // offset of 35 is not enough (REDIRECT require iteration of keys)
            setTimeOffset(35);
            doSamlRedirectLogin(Status.BAD_REQUEST.getStatusCode(), "Invalid signature in response from identity provider.");

            // offset more than one hour set as cache duration in the realm
            setTimeOffset(3600 + 5);
            doSamlRedirectLogin(Status.OK.getStatusCode(), "Update Account Information");

            // rotate keys it should fail again
            rotateKeys(bc.providerRealmName(), Algorithm.RS256, "rsa-generated");
            doSamlRedirectLogin(Status.BAD_REQUEST.getStatusCode(), "Invalid signature in response from identity provider");

            // manually refresh after 1d plus 20s (15s more min refresh is 10s)
            setTimeOffset(3600 + 20);
            Assert.assertTrue(adminClient.realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()).reloadKeys());
            doSamlRedirectLogin(Status.OK.getStatusCode(), "Update Account Information");
        }
    }
}
