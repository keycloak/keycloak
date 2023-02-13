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
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.springsecurity.account.KeycloakRole;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Keycloak logout handler tests.
 */
public class KeycloakLogoutHandlerTest {

    private KeycloakAuthenticationToken keycloakAuthenticationToken;
    private KeycloakLogoutHandler keycloakLogoutHandler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Mock
    private AdapterDeploymentContext adapterDeploymentContext;

    @Mock
    private OidcKeycloakAccount keycloakAccount;

    @Mock
    private KeycloakDeployment keycloakDeployment;

    @Mock
    private RefreshableKeycloakSecurityContext session;

    private Collection<KeycloakRole> authorities = Collections.singleton(new KeycloakRole(UUID.randomUUID().toString()));

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        keycloakAuthenticationToken = mock(KeycloakAuthenticationToken.class);
        keycloakLogoutHandler = new KeycloakLogoutHandler(adapterDeploymentContext);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        when(adapterDeploymentContext.resolveDeployment(any(HttpFacade.class))).thenReturn(keycloakDeployment);
        when(keycloakAuthenticationToken.getAccount()).thenReturn(keycloakAccount);
        when(keycloakAccount.getKeycloakSecurityContext()).thenReturn(session);
    }

    @Test
    public void testLogout() throws Exception {
        keycloakLogoutHandler.logout(request, response, keycloakAuthenticationToken);
        verify(session).logout(eq(keycloakDeployment));
    }

    @Test
    public void testLogoutAnonymousAuthentication() throws Exception {
        Authentication authentication = new AnonymousAuthenticationToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), authorities);
        keycloakLogoutHandler.logout(request, response, authentication);
        verifyZeroInteractions(session);
    }

    @Test
    public void testLogoutUsernamePasswordAuthentication() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), authorities);
        keycloakLogoutHandler.logout(request, response, authentication);
        verifyZeroInteractions(session);
    }

    @Test
    public void testLogoutRememberMeAuthentication() throws Exception {
        Authentication authentication = new RememberMeAuthenticationToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), authorities);
        keycloakLogoutHandler.logout(request, response, authentication);
        verifyZeroInteractions(session);
    }

    @Test
    public void testLogoutNullAuthentication() throws Exception {
        keycloakLogoutHandler.logout(request, response, null);
        verifyZeroInteractions(session);
    }

    @Test
    public void testHandleSingleSignOut() throws Exception {
        keycloakLogoutHandler.handleSingleSignOut(request, response, keycloakAuthenticationToken);
        verify(session).logout(eq(keycloakDeployment));
    }
}
