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

import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.spi.AdapterSessionStore;
import org.keycloak.enums.TokenStore;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Spring Security adapter token store factory tests.
 */
public class SpringSecurityAdapterTokenStoreFactoryTest {

    private AdapterTokenStoreFactory factory = new SpringSecurityAdapterTokenStoreFactory();

    @Mock
    private KeycloakDeployment deployment;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateAdapterTokenStore() throws Exception {
        when(deployment.getTokenStore()).thenReturn(TokenStore.SESSION);
        AdapterSessionStore store = factory.createAdapterTokenStore(deployment, request, response);
        assertTrue(store instanceof SpringSecurityTokenStore);
    }

    @Test
    public void testCreateAdapterTokenStoreUsingCookies() throws Exception {
        when(deployment.getTokenStore()).thenReturn(TokenStore.COOKIE);
        AdapterSessionStore store = factory.createAdapterTokenStore(deployment, request, response);
        assertTrue(store instanceof SpringSecurityCookieTokenStore);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateAdapterTokenStoreNullDeployment() throws Exception {
        factory.createAdapterTokenStore(null, request, response);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateAdapterTokenStoreNullRequest() throws Exception {
        factory.createAdapterTokenStore(deployment, null, response);
    }

    @Test
    public void testCreateAdapterTokenStoreNullResponse() throws Exception {
        when(deployment.getTokenStore()).thenReturn(TokenStore.SESSION);
        factory.createAdapterTokenStore(deployment, request, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateAdapterTokenStoreNullResponseUsingCookies() throws Exception {
        when(deployment.getTokenStore()).thenReturn(TokenStore.COOKIE);
        factory.createAdapterTokenStore(deployment, request, null);
    }
}
