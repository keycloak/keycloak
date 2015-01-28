/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.broker;

import org.junit.Test;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.saml.SAMLIdentityProvider;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.social.facebook.FacebookIdentityProvider;
import org.keycloak.social.facebook.FacebookIdentityProviderFactory;
import org.keycloak.social.github.GitHubIdentityProvider;
import org.keycloak.social.github.GitHubIdentityProviderFactory;
import org.keycloak.social.google.GoogleIdentityProvider;
import org.keycloak.social.google.GoogleIdentityProviderFactory;
import org.keycloak.social.twitter.TwitterIdentityProvider;
import org.keycloak.social.twitter.TwitterIdentityProviderFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author pedroigor
 */
public class ImportIdentityProviderTest extends AbstractIdentityProviderModelTest {

    @Test
    public void testInstallation() throws Exception {
        RealmModel realm = installTestRealm();

        assertIdentityProviderConfig(realm.getIdentityProviders());

        assertTrue(realm.isIdentityFederationEnabled());

        this.realmManager.removeRealm(realm);

        commit();

        realm = this.realmManager.getRealm(realm.getId());

        assertNull(realm);
    }

    @Test
    public void testUpdateIdentityProvider() throws Exception {
        RealmModel realm = installTestRealm();
        List<IdentityProviderModel> identityProviders = realm.getIdentityProviders();

        assertFalse(identityProviders.isEmpty());

        IdentityProviderModel identityProviderModel = identityProviders.get(0);
        String identityProviderId = identityProviderModel.getId();

        identityProviderModel.setName("Changed Name");
        identityProviderModel.getConfig().put("config-added", "value-added");
        identityProviderModel.setEnabled(false);
        identityProviderModel.setUpdateProfileFirstLogin(false);

        realm.updateIdentityProvider(identityProviderModel);

        commit();

        realm = this.realmManager.getRealm(realm.getId());

        identityProviderModel = realm.getIdentityProviderById(identityProviderId);

        assertEquals("Changed Name", identityProviderModel.getName());
        assertEquals("value-added", identityProviderModel.getConfig().get("config-added"));
        assertEquals(false, identityProviderModel.isEnabled());
        assertEquals(false, identityProviderModel.isUpdateProfileFirstLogin());

        identityProviderModel.setName("Changed Name Again");
        identityProviderModel.getConfig().remove("config-added");
        identityProviderModel.setEnabled(true);
        identityProviderModel.setUpdateProfileFirstLogin(true);

        realm.updateIdentityProvider(identityProviderModel);

        commit();

        realm = this.realmManager.getRealm(realm.getId());
        identityProviderModel = realm.getIdentityProviderById(identityProviderId);

        assertEquals("Changed Name Again", identityProviderModel.getName());
        assertFalse(identityProviderModel.getConfig().containsKey("config-added"));
        assertEquals(true, identityProviderModel.isEnabled());
        assertEquals(true, identityProviderModel.isUpdateProfileFirstLogin());
    }

    @Test
    public void testRemoveIdentityProvider() throws Exception {
        RealmModel realm = installTestRealm();
        List<IdentityProviderModel> identityProviders = realm.getIdentityProviders();

        assertFalse(identityProviders.isEmpty());

        IdentityProviderModel identityProviderModel = identityProviders.get(0);
        String expectedId = identityProviderModel.getId();

        realm.removeIdentityProviderById(expectedId);

        commit();

        realm = this.realmManager.getRealm(realm.getId());

        assertNull(realm.getIdentityProviderById(expectedId));
    }

    private void assertIdentityProviderConfig(List<IdentityProviderModel> identityProviders) {
        assertFalse(identityProviders.isEmpty());

        Set<String> checkedProviders = new HashSet<String>(getExpectedProviders());

        for (IdentityProviderModel identityProvider : identityProviders) {
            String providerId = identityProvider.getProviderId();

            if (SAMLIdentityProviderFactory.PROVIDER_ID.equals(providerId)) {
                assertSamlIdentityProviderConfig(identityProvider);
            } else if (GoogleIdentityProviderFactory.PROVIDER_ID.equals(providerId)) {
                assertGoogleIdentityProviderConfig(identityProvider);
            } else if (OIDCIdentityProviderFactory.PROVIDER_ID.equals(providerId)) {
                assertOidcIdentityProviderConfig(identityProvider);
            } else if (FacebookIdentityProviderFactory.PROVIDER_ID.equals(providerId)) {
                assertFacebookIdentityProviderConfig(identityProvider);
            } else if (GitHubIdentityProviderFactory.PROVIDER_ID.equals(providerId)) {
                assertGitHubIdentityProviderConfig(identityProvider);
            } else if (TwitterIdentityProviderFactory.PROVIDER_ID.equals(providerId)) {
                assertTwitterIdentityProviderConfig(identityProvider);
            }

            checkedProviders.remove(providerId);
        }

        assertTrue(checkedProviders.isEmpty());
    }

    private void assertGoogleIdentityProviderConfig(IdentityProviderModel identityProvider) {
        GoogleIdentityProvider googleIdentityProvider = new GoogleIdentityProviderFactory().create(identityProvider);
        OIDCIdentityProviderConfig config = googleIdentityProvider.getConfig();

        assertEquals("google", config.getId());
        assertEquals(GoogleIdentityProviderFactory.PROVIDER_ID, config.getProviderId());
        assertEquals("Google", config.getName());
        assertEquals(true, config.isEnabled());
        assertEquals(true, config.isUpdateProfileFirstLogin());
        assertEquals("clientId", config.getClientId());
        assertEquals("clientSecret", config.getClientSecret());
        assertEquals(GoogleIdentityProvider.AUTH_URL, config.getAuthorizationUrl());
        assertEquals(GoogleIdentityProvider.TOKEN_URL, config.getTokenUrl());
        assertEquals(GoogleIdentityProvider.PROFILE_URL, config.getUserInfoUrl());

    }

    private void assertSamlIdentityProviderConfig(IdentityProviderModel identityProvider) {
        SAMLIdentityProvider samlIdentityProvider = new SAMLIdentityProviderFactory().create(identityProvider);
        SAMLIdentityProviderConfig config = samlIdentityProvider.getConfig();

        assertEquals("saml-idp", config.getId());
        assertEquals(SAMLIdentityProviderFactory.PROVIDER_ID, config.getProviderId());
        assertEquals("SAML IdP", config.getName());
        assertEquals(true, config.isEnabled());
        assertEquals(true, config.isUpdateProfileFirstLogin());
        assertEquals("http://localhost:8080/idp/", config.getSingleSignOnServiceUrl());
        assertEquals("urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress", config.getNameIDPolicyFormat());
        assertEquals("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB", config.getSigningPublicKey());
        assertEquals(true, config.isWantAuthnRequestsSigned());
        assertEquals(true, config.isForceAuthn());
        assertEquals(true, config.isPostBindingAuthnRequest());
        assertEquals(true, config.isPostBindingResponse());
        assertEquals(true, config.isValidateSignature());
    }

    private void assertOidcIdentityProviderConfig(IdentityProviderModel identityProvider) {
        OIDCIdentityProvider googleIdentityProvider = new OIDCIdentityProviderFactory().create(identityProvider);
        OIDCIdentityProviderConfig config = googleIdentityProvider.getConfig();

        assertEquals("oidc-idp", config.getId());
        assertEquals(OIDCIdentityProviderFactory.PROVIDER_ID, config.getProviderId());
        assertEquals("OIDC IdP", config.getName());
        assertEquals(false, config.isEnabled());
        assertEquals(false, config.isUpdateProfileFirstLogin());
        assertEquals("clientId", config.getClientId());
        assertEquals("clientSecret", config.getClientSecret());
    }

    private void assertFacebookIdentityProviderConfig(IdentityProviderModel identityProvider) {
        FacebookIdentityProvider facebookIdentityProvider = new FacebookIdentityProviderFactory().create(identityProvider);
        OAuth2IdentityProviderConfig config = facebookIdentityProvider.getConfig();

        assertEquals("facebook", config.getId());
        assertEquals(FacebookIdentityProviderFactory.PROVIDER_ID, config.getProviderId());
        assertEquals("Facebook", config.getName());
        assertEquals(true, config.isEnabled());
        assertEquals(true, config.isUpdateProfileFirstLogin());
        assertEquals("clientId", config.getClientId());
        assertEquals("clientSecret", config.getClientSecret());
        assertEquals(FacebookIdentityProvider.AUTH_URL, config.getAuthorizationUrl());
        assertEquals(FacebookIdentityProvider.TOKEN_URL, config.getTokenUrl());
        assertEquals(FacebookIdentityProvider.PROFILE_URL, config.getUserInfoUrl());
    }

    private void assertGitHubIdentityProviderConfig(IdentityProviderModel identityProvider) {
        GitHubIdentityProvider gitHubIdentityProvider = new GitHubIdentityProviderFactory().create(identityProvider);
        OAuth2IdentityProviderConfig config = gitHubIdentityProvider.getConfig();

        assertEquals("github", config.getId());
        assertEquals(GitHubIdentityProviderFactory.PROVIDER_ID, config.getProviderId());
        assertEquals("GitHub", config.getName());
        assertEquals(true, config.isEnabled());
        assertEquals(true, config.isUpdateProfileFirstLogin());
        assertEquals("clientId", config.getClientId());
        assertEquals("clientSecret", config.getClientSecret());
        assertEquals(GitHubIdentityProvider.AUTH_URL, config.getAuthorizationUrl());
        assertEquals(GitHubIdentityProvider.TOKEN_URL, config.getTokenUrl());
        assertEquals(GitHubIdentityProvider.PROFILE_URL, config.getUserInfoUrl());
    }

    private void assertTwitterIdentityProviderConfig(IdentityProviderModel identityProvider) {
        TwitterIdentityProvider gitHubIdentityProvider = new TwitterIdentityProviderFactory().create(identityProvider);
        OAuth2IdentityProviderConfig config = gitHubIdentityProvider.getConfig();

        assertEquals("twitter", config.getId());
        assertEquals(TwitterIdentityProviderFactory.PROVIDER_ID, config.getProviderId());
        assertEquals("Twitter", config.getName());
        assertEquals(true, config.isEnabled());
        assertEquals(true, config.isUpdateProfileFirstLogin());
        assertEquals("clientId", config.getClientId());
        assertEquals("clientSecret", config.getClientSecret());
    }

    private RealmModel installTestRealm() throws IOException {
        RealmRepresentation realmRepresentation = loadJson("model/test-realm-with-identity-provider.json");

        assertNotNull(realmRepresentation);
        assertEquals("test-realm-with-identity-provider", realmRepresentation.getRealm());

        RealmModel realmModel = this.realmManager.importRealm(realmRepresentation);

        commit();

        realmModel = this.realmManager.getRealm(realmModel.getId());

        assertNotNull(realmModel);

        return realmModel;
    }
}
