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

package org.keycloak.testsuite.broker;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.junit.Before;
import org.junit.Test;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.*;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.crypto.Algorithm;
import org.keycloak.keys.KeyProvider;
import org.keycloak.keys.PublicKeyStorageUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.resources.TestingCacheResource;
import org.keycloak.testsuite.util.OAuthClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KcOIDCBrokerWithSignatureTest extends AbstractBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @Before
    public void createUser() {
        log.debug("creating user for realm " + bc.providerRealmName());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(bc.getUserLogin());
        user.setEmail(bc.getUserEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);

        RealmResource realmResource = adminClient.realm(bc.providerRealmName());
        String userId = createUserWithAdminClient(realmResource, user);

        resetUserPassword(realmResource.users().get(userId), bc.getUserPassword(), false);
    }

    // TODO: Possibly move to parent superclass
    @Before
    public void addIdentityProviderToProviderRealm() {
        log.debug("adding identity provider to realm " + bc.consumerRealmName());

        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        Response resp = realm.identityProviders().create(bc.setUpIdentityProvider());
        resp.close();
    }


    @Before
    public void addClients() {
        addClientsToProviderAndConsumer();
    }


    @Test
    public void testSignatureVerificationJwksUrl() throws Exception {
        // Configure OIDC identity provider with JWKS URL
        updateIdentityProviderWithJwksUrl();

        // Check that user is able to login
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        // Rotate public keys on the parent broker
        rotateKeys();

        // User not able to login now as new keys can't be yet downloaded (10s timeout)
        logInAsUserInIDP();
        assertErrorPage("Unexpected error when authenticating with identity provider");

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        // Set time offset. New keys can be downloaded. Check that user is able to login.
        setTimeOffset(20);

        logInAsUserInIDP();
        assertLoggedInAccountManagement();
    }

    // Configure OIDC identity provider with JWKS URL and validateSignature=true
    private void updateIdentityProviderWithJwksUrl() {
        IdentityProviderRepresentation idpRep = getIdentityProvider();
        OIDCIdentityProviderConfigRep cfg = new OIDCIdentityProviderConfigRep(idpRep);
        cfg.setValidateSignature(true);
        cfg.setUseJwksUrl(true);

        UriBuilder b = OIDCLoginProtocolService.certsUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT));
        String jwksUrl = b.build(bc.providerRealmName()).toString();
        cfg.setJwksUrl(jwksUrl);
        updateIdentityProvider(idpRep);
    }


    @Test
    public void testSignatureVerificationHardcodedPublicKey() throws Exception {
        // Configure OIDC identity provider with JWKS URL
        IdentityProviderRepresentation idpRep = getIdentityProvider();
        OIDCIdentityProviderConfigRep cfg = new OIDCIdentityProviderConfigRep(idpRep);
        cfg.setValidateSignature(true);
        cfg.setUseJwksUrl(false);

        KeysMetadataRepresentation.KeyMetadataRepresentation key = ApiUtil.findActiveSigningKey(providerRealm());
        cfg.setPublicKeySignatureVerifier(key.getPublicKey());
        updateIdentityProvider(idpRep);

        // Check that user is able to login
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        // Rotate public keys on the parent broker
        rotateKeys();

        // User not able to login now as new keys can't be yet downloaded (10s timeout)
        logInAsUserInIDP();
        assertErrorPage("Unexpected error when authenticating with identity provider");

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        // Even after time offset is user not able to login, because it uses old key hardcoded in identityProvider config
        setTimeOffset(20);

        logInAsUserInIDP();
        assertErrorPage("Unexpected error when authenticating with identity provider");
    }

    @Test
    public void testSignatureVerificationHardcodedPublicKeyWithKeyIdSetExplicitly() throws Exception {
        // Configure OIDC identity provider with JWKS URL
        IdentityProviderRepresentation idpRep = getIdentityProvider();
        OIDCIdentityProviderConfigRep cfg = new OIDCIdentityProviderConfigRep(idpRep);
        cfg.setValidateSignature(true);
        cfg.setUseJwksUrl(false);

        KeysMetadataRepresentation.KeyMetadataRepresentation key = ApiUtil.findActiveSigningKey(providerRealm());
        String pemData = key.getPublicKey();
        cfg.setPublicKeySignatureVerifier(pemData);
        String expectedKeyId = KeyUtils.createKeyId(PemUtils.decodePublicKey(pemData));
        updateIdentityProvider(idpRep);

        // Check that user is able to login
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        // Set key id to an invalid one
        cfg.setPublicKeySignatureVerifierKeyId("invalid-key-id");
        updateIdentityProvider(idpRep);

        logInAsUserInIDP();
        assertErrorPage("Unexpected error when authenticating with identity provider");

        // Set key id to a valid one
        cfg.setPublicKeySignatureVerifierKeyId(expectedKeyId);
        updateIdentityProvider(idpRep);
        logInAsUserInIDP();
        assertLoggedInAccountManagement();
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        // Set key id to empty
        cfg.setPublicKeySignatureVerifierKeyId("");
        updateIdentityProvider(idpRep);
        logInAsUserInIDP();
        assertLoggedInAccountManagement();
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        // Unset key id
        cfg.setPublicKeySignatureVerifierKeyId(null);
        updateIdentityProvider(idpRep);
        logInAsUserInIDP();
        assertLoggedInAccountManagement();
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());
    }


    @Test
    public void testClearKeysCache() throws Exception {
        // Configure OIDC identity provider with JWKS URL
        updateIdentityProviderWithJwksUrl();

        // Check that user is able to login
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        // Check that key is cached
        IdentityProviderRepresentation idpRep = getIdentityProvider();
        String expectedCacheKey = PublicKeyStorageUtils.getIdpModelCacheKey(consumerRealm().toRepresentation().getId(), idpRep.getInternalId());
        TestingCacheResource cache = testingClient.testing(bc.consumerRealmName()).cache(InfinispanConnectionProvider.KEYS_CACHE_NAME);
        Assert.assertTrue(cache.contains(expectedCacheKey));

        // Clear cache and check nothing cached
        consumerRealm().clearKeysCache();
        Assert.assertFalse(cache.contains(expectedCacheKey));
        Assert.assertEquals(cache.size(), 0);
    }


    // Test that when I update identityProvier, then the record in publicKey cache is cleared and it's not possible to authenticate with it anymore
    @Test
    public void testPublicKeyCacheInvalidatedWhenProviderUpdated() throws Exception {
        // Configure OIDC identity provider with JWKS URL
        updateIdentityProviderWithJwksUrl();

        // Check that user is able to login
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        // Check that key is cached
        IdentityProviderRepresentation idpRep = getIdentityProvider();
        String expectedCacheKey = PublicKeyStorageUtils.getIdpModelCacheKey(consumerRealm().toRepresentation().getId(), idpRep.getInternalId());
        TestingCacheResource cache = testingClient.testing(bc.consumerRealmName()).cache(InfinispanConnectionProvider.KEYS_CACHE_NAME);
        Assert.assertTrue(cache.contains(expectedCacheKey));

        // Update identityProvider to some bad JWKS_URL
        OIDCIdentityProviderConfigRep cfg = new OIDCIdentityProviderConfigRep(idpRep);
        cfg.setJwksUrl("https://localhost:43214/non-existent");
        updateIdentityProvider(idpRep);

        // Check that key is not cached anymore
        Assert.assertFalse(cache.contains(expectedCacheKey));

        // Check that user is not able to login with IDP
        setTimeOffset(20);
        logInAsUserInIDP();
        assertErrorPage("Unexpected error when authenticating with identity provider");
    }



    private void rotateKeys() {
        String activeKid = providerRealm().keys().getKeyMetadata().getActive().get(Algorithm.RS256);

        // Rotate public keys on the parent broker
        String realmId = providerRealm().toRepresentation().getId();
        ComponentRepresentation keys = new ComponentRepresentation();
        keys.setName("generated");
        keys.setProviderType(KeyProvider.class.getName());
        keys.setProviderId("rsa-generated");
        keys.setParentId(realmId);
        keys.setConfig(new MultivaluedHashMap<>());
        keys.getConfig().putSingle("priority", Long.toString(System.currentTimeMillis()));
        Response response = providerRealm().components().add(keys);
        assertEquals(201, response.getStatus());
        response.close();

        String updatedActiveKid = providerRealm().keys().getKeyMetadata().getActive().get(Algorithm.RS256);
        assertNotEquals(activeKid, updatedActiveKid);
    }


    private RealmResource providerRealm() {
        return adminClient.realm(bc.providerRealmName());
    }

    private IdentityProviderRepresentation getIdentityProvider() {
        return consumerRealm().identityProviders().get(BrokerTestConstants.IDP_OIDC_ALIAS).toRepresentation();
    }

    private void updateIdentityProvider(IdentityProviderRepresentation rep) {
        consumerRealm().identityProviders().get(BrokerTestConstants.IDP_OIDC_ALIAS).update(rep);
    }

    private RealmResource consumerRealm() {
        return adminClient.realm(bc.consumerRealmName());
    }




}
