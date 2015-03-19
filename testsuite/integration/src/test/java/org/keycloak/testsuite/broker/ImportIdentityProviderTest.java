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
import org.keycloak.models.ClientIdentityProviderMappingModel;
import org.keycloak.models.ClientModel;
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
    }

    @Test
    public void testUpdateIdentityProvider() throws Exception {
        RealmModel realm = installTestRealm();
        List<IdentityProviderModel> identityProviders = realm.getIdentityProviders();

        assertFalse(identityProviders.isEmpty());

        IdentityProviderModel identityProviderModel = identityProviders.get(0);
        String identityProviderId = identityProviderModel.getAlias();

        identityProviderModel.getConfig().put("config-added", "value-added");
        identityProviderModel.setEnabled(false);
        identityProviderModel.setUpdateProfileFirstLogin(false);
        identityProviderModel.setStoreToken(true);
        identityProviderModel.setAuthenticateByDefault(true);

        realm.updateIdentityProvider(identityProviderModel);

        commit();

        realm = this.realmManager.getRealm(realm.getId());

        identityProviderModel = realm.getIdentityProviderByAlias(identityProviderId);

        assertEquals("value-added", identityProviderModel.getConfig().get("config-added"));
        assertFalse(identityProviderModel.isEnabled());
        assertFalse(identityProviderModel.isUpdateProfileFirstLogin());
        assertTrue(identityProviderModel.isStoreToken());
        assertTrue(identityProviderModel.isAuthenticateByDefault());

        identityProviderModel.getConfig().remove("config-added");
        identityProviderModel.setEnabled(true);
        identityProviderModel.setUpdateProfileFirstLogin(true);
        identityProviderModel.setAuthenticateByDefault(false);

        realm.updateIdentityProvider(identityProviderModel);

        commit();

        realm = this.realmManager.getRealm(realm.getId());
        identityProviderModel = realm.getIdentityProviderByAlias(identityProviderId);

        assertFalse(identityProviderModel.getConfig().containsKey("config-added"));
        assertTrue(identityProviderModel.isEnabled());
        assertTrue(identityProviderModel.isUpdateProfileFirstLogin());
        assertFalse(identityProviderModel.isAuthenticateByDefault());
        this.realmManager.removeRealm(realm);
    }

    @Test
    public void testApplicationIdentityProviders() throws Exception {
        RealmModel realm = installTestRealm();

        ClientModel client = realm.findClient("test-app-with-allowed-providers");
        List<ClientIdentityProviderMappingModel> identityProviders = client.getIdentityProviders();

        assertEquals(1, identityProviders.size());

        ClientIdentityProviderMappingModel identityProviderMappingModel = identityProviders.get(0);

        assertEquals("kc-oidc-idp", identityProviderMappingModel.getIdentityProvider());
        assertEquals(false, identityProviderMappingModel.isRetrieveToken());

        identityProviders.remove(identityProviderMappingModel);

        client.updateIdentityProviders(identityProviders);

        client = realm.findClientById(client.getId());
        identityProviders = client.getIdentityProviders();

        assertEquals(0, identityProviders.size());
        this.realmManager.removeRealm(realm);
    }


    private void assertIdentityProviderConfig(List<IdentityProviderModel> identityProviders) {
        assertFalse(identityProviders.isEmpty());

        Set<String> checkedProviders = new HashSet<String>(getExpectedProviders());

        for (IdentityProviderModel identityProvider : identityProviders) {
            if (identityProvider.getAlias().startsWith("model-")) {
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
                } else {
                    continue;
                }

                checkedProviders.remove(providerId);
            }
        }

        assertTrue(checkedProviders.isEmpty());
    }

    private void assertGoogleIdentityProviderConfig(IdentityProviderModel identityProvider) {
        GoogleIdentityProvider googleIdentityProvider = new GoogleIdentityProviderFactory().create(identityProvider);
        OIDCIdentityProviderConfig config = googleIdentityProvider.getConfig();

        assertEquals("model-google", config.getAlias());
        assertEquals(GoogleIdentityProviderFactory.PROVIDER_ID, config.getProviderId());
        assertEquals(true, config.isEnabled());
        assertEquals(true, config.isUpdateProfileFirstLogin());
        assertEquals(false, config.isAuthenticateByDefault());
        assertEquals(true, config.isStoreToken());
        assertEquals("clientId", config.getClientId());
        assertEquals("clientSecret", config.getClientSecret());
        assertEquals(GoogleIdentityProvider.AUTH_URL, config.getAuthorizationUrl());
        assertEquals(GoogleIdentityProvider.TOKEN_URL, config.getTokenUrl());
        assertEquals(GoogleIdentityProvider.PROFILE_URL, config.getUserInfoUrl());

    }

    private void assertSamlIdentityProviderConfig(IdentityProviderModel identityProvider) {
        SAMLIdentityProvider samlIdentityProvider = new SAMLIdentityProviderFactory().create(identityProvider);
        SAMLIdentityProviderConfig config = samlIdentityProvider.getConfig();

        assertEquals("model-saml-signed-idp", config.getAlias());
        assertEquals(SAMLIdentityProviderFactory.PROVIDER_ID, config.getProviderId());
        assertEquals(true, config.isEnabled());
        assertEquals(true, config.isUpdateProfileFirstLogin());
        assertEquals(false, config.isAuthenticateByDefault());
        assertEquals(false, config.isStoreToken());
        assertEquals("http://localhost:8082/auth/realms/realm-with-saml-identity-provider/protocol/saml", config.getSingleSignOnServiceUrl());
        assertEquals("urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress", config.getNameIDPolicyFormat());
        assertEquals("MIIDdzCCAl+gAwIBAgIEbySuqTANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE1MDEyODIyMTYyMFoXDTE3MTAyNDIyMTYyMFowbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAII/K9NNvXi9IySl7+l2zY/kKrGTtuR4WdCI0xLW/Jn4dLY7v1/HOnV4CC4ecFOzhdNFPtJkmEhP/q62CpmOYOKApXk3tfmm2rwEz9bWprVxgFGKnbrWlz61Z/cjLAlhD3IUj2ZRBquYgSXQPsYfXo1JmSWF5pZ9uh1FVqu9f4wvRqY20ZhUN+39F+1iaBsoqsrbXypCn1HgZkW1/9D9GZug1c3vB4wg1TwZZWRNGtxwoEhdK6dPrNcZ+6PdanVilWrbQFbBjY4wz8/7IMBzssoQ7Usmo8F1Piv0FGfaVeJqBrcAvbiBMpk8pT+27u6p8VyIX6LhGvnxIwM07NByeSUCAwEAAaMhMB8wHQYDVR0OBBYEFFlcNuTYwI9W0tQ224K1gFJlMam0MA0GCSqGSIb3DQEBCwUAA4IBAQB5snl1KWOJALtAjLqD0mLPg1iElmZP82Lq1htLBt3XagwzU9CaeVeCQ7lTp+DXWzPa9nCLhsC3QyrV3/+oqNli8C6NpeqI8FqN2yQW/QMWN1m5jWDbmrWwtQzRUn/rh5KEb5m3zPB+tOC6e/2bV3QeQebxeW7lVMD0tSCviUg1MQf1l2gzuXQo60411YwqrXwk6GMkDOhFDQKDlMchO3oRbQkGbcP8UeiKAXjMeHfzbiBr+cWz8NYZEtxUEDYDjTpKrYCSMJBXpmgVJCZ00BswbksxJwaGqGMPpUKmCV671pf3m8nq3xyiHMDGuGwtbU+GE8kVx85menmp8+964nin", config.getSigningCertificate());
        assertEquals(true, config.isWantAuthnRequestsSigned());
        assertEquals(true, config.isForceAuthn());
        assertEquals(true, config.isPostBindingAuthnRequest());
        assertEquals(true, config.isPostBindingResponse());
        assertEquals(true, config.isValidateSignature());
    }

    private void assertOidcIdentityProviderConfig(IdentityProviderModel identityProvider) {
        OIDCIdentityProvider googleIdentityProvider = new OIDCIdentityProviderFactory().create(identityProvider);
        OIDCIdentityProviderConfig config = googleIdentityProvider.getConfig();

        assertEquals("model-oidc-idp", config.getAlias());
        assertEquals(OIDCIdentityProviderFactory.PROVIDER_ID, config.getProviderId());
        assertEquals(false, config.isEnabled());
        assertEquals(false, config.isUpdateProfileFirstLogin());
        assertEquals(false, config.isAuthenticateByDefault());
        assertEquals(false, config.isStoreToken());
        assertEquals("clientId", config.getClientId());
        assertEquals("clientSecret", config.getClientSecret());
    }

    private void assertFacebookIdentityProviderConfig(IdentityProviderModel identityProvider) {
        FacebookIdentityProvider facebookIdentityProvider = new FacebookIdentityProviderFactory().create(identityProvider);
        OAuth2IdentityProviderConfig config = facebookIdentityProvider.getConfig();

        assertEquals("model-facebook", config.getAlias());
        assertEquals(FacebookIdentityProviderFactory.PROVIDER_ID, config.getProviderId());
        assertEquals(true, config.isEnabled());
        assertEquals(true, config.isUpdateProfileFirstLogin());
        assertEquals(false, config.isAuthenticateByDefault());
        assertEquals(false, config.isStoreToken());
        assertEquals("clientId", config.getClientId());
        assertEquals("clientSecret", config.getClientSecret());
        assertEquals(FacebookIdentityProvider.AUTH_URL, config.getAuthorizationUrl());
        assertEquals(FacebookIdentityProvider.TOKEN_URL, config.getTokenUrl());
        assertEquals(FacebookIdentityProvider.PROFILE_URL, config.getUserInfoUrl());
    }

    private void assertGitHubIdentityProviderConfig(IdentityProviderModel identityProvider) {
        GitHubIdentityProvider gitHubIdentityProvider = new GitHubIdentityProviderFactory().create(identityProvider);
        OAuth2IdentityProviderConfig config = gitHubIdentityProvider.getConfig();

        assertEquals("model-github", config.getAlias());
        assertEquals(GitHubIdentityProviderFactory.PROVIDER_ID, config.getProviderId());
        assertEquals(true, config.isEnabled());
        assertEquals(true, config.isUpdateProfileFirstLogin());
        assertEquals(false, config.isAuthenticateByDefault());
        assertEquals(false, config.isStoreToken());
        assertEquals("clientId", config.getClientId());
        assertEquals("clientSecret", config.getClientSecret());
        assertEquals(GitHubIdentityProvider.AUTH_URL, config.getAuthorizationUrl());
        assertEquals(GitHubIdentityProvider.TOKEN_URL, config.getTokenUrl());
        assertEquals(GitHubIdentityProvider.PROFILE_URL, config.getUserInfoUrl());
    }

    private void assertTwitterIdentityProviderConfig(IdentityProviderModel identityProvider) {
        TwitterIdentityProvider twitterIdentityProvider = new TwitterIdentityProviderFactory().create(identityProvider);
        OAuth2IdentityProviderConfig config = twitterIdentityProvider.getConfig();

        assertEquals("model-twitter", config.getAlias());
        assertEquals(TwitterIdentityProviderFactory.PROVIDER_ID, config.getProviderId());
        assertEquals(true, config.isEnabled());
        assertEquals(true, config.isUpdateProfileFirstLogin());
        assertEquals(false, config.isAuthenticateByDefault());
        assertEquals(true, config.isStoreToken());
        assertEquals("clientId", config.getClientId());
        assertEquals("clientSecret", config.getClientSecret());
    }

    private RealmModel installTestRealm() throws IOException {
        RealmRepresentation realmRepresentation = loadJson("broker-test/test-realm-with-broker.json");

        assertNotNull(realmRepresentation);
        assertEquals("realm-with-broker", realmRepresentation.getRealm());

        RealmModel realmModel = this.realmManager.getRealm("realm-with-broker");

        if (realmModel == null) {
            realmModel = this.realmManager.importRealm(realmRepresentation);

            commit();

            realmModel = this.realmManager.getRealm(realmModel.getId());

            assertNotNull(realmModel);
        }

        return realmModel;
    }
}
