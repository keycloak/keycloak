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

package org.keycloak.adapters.springsecurity.token;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.springsecurity.account.KeycloakRole;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.security.Principal;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Spring Security token store tests.
 */
public class SpringSecurityTokenStoreTest {

    private SpringSecurityTokenStore store;

    @Mock
    private KeycloakDeployment deployment;

    @Mock
    private Principal principal;

    @Mock
    private RequestAuthenticator requestAuthenticator;

    @Mock
    private RefreshableKeycloakSecurityContext keycloakSecurityContext;

    private MockHttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        request = new MockHttpServletRequest();
        store = new SpringSecurityTokenStore(deployment, request);
    }

    @After
    public void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testIsCached() throws Exception {
        Authentication authentication = new PreAuthenticatedAuthenticationToken("foo", "bar", Collections.singleton(new KeycloakRole("ROLE_FOO")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        assertFalse(store.isCached(requestAuthenticator));
    }

    @Test
    public void testSaveAccountInfo() throws Exception {
        OidcKeycloakAccount account = new SimpleKeycloakAccount(principal, Collections.singleton("FOO"), keycloakSecurityContext);
        Authentication authentication;

        store.saveAccountInfo(account);
        authentication = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertTrue(authentication instanceof KeycloakAuthenticationToken);
    }

    @Test(expected = IllegalStateException.class)
    public void testSaveAccountInfoInvalidAuthenticationType() throws Exception {
        OidcKeycloakAccount account = new SimpleKeycloakAccount(principal, Collections.singleton("FOO"), keycloakSecurityContext);
        Authentication authentication = new PreAuthenticatedAuthenticationToken("foo", "bar", Collections.singleton(new KeycloakRole("ROLE_FOO")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        store.saveAccountInfo(account);
    }

    @Test
    public void testLogout() throws Exception {
        MockHttpSession session = (MockHttpSession) request.getSession(true);
        assertFalse(session.isInvalid());
        store.logout();
        assertTrue(session.isInvalid());
    }
}
