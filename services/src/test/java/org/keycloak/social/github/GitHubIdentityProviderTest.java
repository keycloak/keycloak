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
package org.keycloak.social.github;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for {@link org.keycloak.social.github.GitHubIdentityProvider}.
 *
 * @author Neon Ngo
 */
public class GitHubIdentityProviderTest {

    /**
     * Test constructor with empty config (i.e. to use default values).
     * This also tests GitHubIdentityProvider.getProfileEndpointForValidation(null).
     */
    @Test
    public void testGitHubIdentityProvider() {
        OAuth2IdentityProviderConfig config = new OAuth2IdentityProviderConfig();
        GitHubIdentityProvider idp = new GitHubIdentityProvider(null, config);

        validateUrls(idp, GitHubIdentityProvider.DEFAULT_BASE_URL, GitHubIdentityProvider.DEFAULT_API_URL);
    }

    /**
     * Test constructor with config overrides of default base URL and API URL.
     */
    @Test
    public void testGitHubIdentityProviderOverrides() {
        OAuth2IdentityProviderConfig config = new OAuth2IdentityProviderConfig();
        String baseUrl = "https://test.com";
        String apiUrl  = "https://api.test.com";
        config.getConfig().put(GitHubIdentityProvider.BASE_URL_KEY, baseUrl);
        config.getConfig().put(GitHubIdentityProvider.API_URL_KEY, apiUrl);
        GitHubIdentityProvider idp = new GitHubIdentityProvider(null, config);

        validateUrls(idp, baseUrl, apiUrl);
    }

    /**
     * Test that default scope does not include read:org when no organizations are configured.
     */
    @Test
    public void testDefaultScopeWithoutOrganizations() {
        OAuth2IdentityProviderConfig config = new OAuth2IdentityProviderConfig();
        GitHubIdentityProvider idp = new GitHubIdentityProvider(null, config);

        assertEquals(GitHubIdentityProvider.DEFAULT_SCOPE, idp.getDefaultScopes());
    }

    /**
     * Test that read:org scope is added when organizations are configured.
     */
    @Test
    public void testDefaultScopeWithOrganizations() {
        OAuth2IdentityProviderConfig config = new OAuth2IdentityProviderConfig();
        config.getConfig().put(GitHubIdentityProvider.ORGANIZATIONS_KEY, "my-org,another-org");
        GitHubIdentityProvider idp = new GitHubIdentityProvider(null, config);

        String scopes = idp.getDefaultScopes();
        assertTrue("Scope should contain user:email", scopes.contains("user:email"));
        assertTrue("Scope should contain read:org when organizations are configured", scopes.contains("read:org"));
    }

    /**
     * Test that read:org scope is not added when organizations value is blank.
     */
    @Test
    public void testDefaultScopeWithBlankOrganizations() {
        OAuth2IdentityProviderConfig config = new OAuth2IdentityProviderConfig();
        config.getConfig().put(GitHubIdentityProvider.ORGANIZATIONS_KEY, "   ");
        GitHubIdentityProvider idp = new GitHubIdentityProvider(null, config);

        assertEquals(GitHubIdentityProvider.DEFAULT_SCOPE, idp.getDefaultScopes());
    }

    protected void validateUrls(GitHubIdentityProvider idp, String baseUrl, String apiUrl) {
        OAuth2IdentityProviderConfig config = idp.getConfig();
        assertEquals(baseUrl + GitHubIdentityProvider.AUTH_FRAGMENT, config.getAuthorizationUrl());
        assertEquals(baseUrl + GitHubIdentityProvider.TOKEN_FRAGMENT, config.getTokenUrl());
        assertEquals(apiUrl + GitHubIdentityProvider.EMAIL_FRAGMENT, config.getConfig().get(GitHubIdentityProvider.EMAIL_URL_KEY));
        assertEquals(apiUrl + GitHubIdentityProvider.PROFILE_FRAGMENT, config.getUserInfoUrl());
        assertEquals(apiUrl + GitHubIdentityProvider.PROFILE_FRAGMENT, idp.getProfileEndpointForValidation(null));
    }

}
