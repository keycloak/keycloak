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

package org.keycloak.adapters.springsecurity.authentication;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OAuthRequestAuthenticator;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.collections.Sets;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Spring Security request authenticator tests.
 */
public class SpringSecurityRequestAuthenticatorTest {

    private SpringSecurityRequestAuthenticator authenticator;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Mock
    private KeycloakDeployment deployment;

    @Mock
    private AdapterTokenStore tokenStore;

    @Mock
    private KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal;

    @Mock
    private AccessToken accessToken;

    @Mock
    private AccessToken.Access access;

    @Mock
    private RefreshableKeycloakSecurityContext refreshableKeycloakSecurityContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        request = spy(new MockHttpServletRequest());
        response = new MockHttpServletResponse();
        HttpFacade facade = new SimpleHttpFacade(request, response);

        authenticator = new SpringSecurityRequestAuthenticator(facade, request, deployment, tokenStore, 443);

        // mocks
        when(principal.getKeycloakSecurityContext()).thenReturn(refreshableKeycloakSecurityContext);

        when(refreshableKeycloakSecurityContext.getDeployment()).thenReturn(deployment);
        when(refreshableKeycloakSecurityContext.getToken()).thenReturn(accessToken);

        when(accessToken.getRealmAccess()).thenReturn(access);
        when(access.getRoles()).thenReturn(Sets.newSet("user", "admin"));

        when(deployment.isUseResourceRoleMappings()).thenReturn(false);
    }

    @Test
    public void testCreateOAuthAuthenticator() throws Exception {
        OAuthRequestAuthenticator oathAuthenticator = authenticator.createOAuthAuthenticator();
        assertNotNull(oathAuthenticator);
    }

    @Test
    public void testCompleteOAuthAuthentication() throws Exception {
        authenticator.completeOAuthAuthentication(principal);
        verify(request).setAttribute(eq(KeycloakSecurityContext.class.getName()), eq(refreshableKeycloakSecurityContext));
        verify(tokenStore).saveAccountInfo(any(OidcKeycloakAccount.class)); // FIXME: should verify account
    }

    @Test
    public void testCompleteBearerAuthentication() throws Exception {
        authenticator.completeBearerAuthentication(principal, "foo");
        verify(request).setAttribute(eq(KeycloakSecurityContext.class.getName()), eq(refreshableKeycloakSecurityContext));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(KeycloakAuthenticationToken.class.isAssignableFrom(SecurityContextHolder.getContext().getAuthentication().getClass()));
    }

    @Test
    public void testGetHttpSessionIdTrue() throws Exception {
        String sessionId = authenticator.changeHttpSessionId(true);
        assertNotNull(sessionId);
    }

    @Test
    public void testGetHttpSessionIdFalse() throws Exception {
        String sessionId = authenticator.changeHttpSessionId(false);
        assertNull(sessionId);
    }
}
