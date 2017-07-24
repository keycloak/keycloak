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

package org.keycloak.adapters.springboot.client;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Keycloak spring boot client request factory tests.
 */
public class KeycloakSecurityContextClientRequestInterceptorTest {

    @Spy
    private KeycloakSecurityContextClientRequestInterceptor factory;

    private MockHttpServletRequest servletRequest;

    @Mock
    private KeycloakSecurityContext keycloakSecurityContext;

    @Mock
    private KeycloakPrincipal keycloakPrincipal;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        servletRequest = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
        servletRequest.setUserPrincipal(keycloakPrincipal);
        when(keycloakPrincipal.getKeycloakSecurityContext()).thenReturn(keycloakSecurityContext);
    }

    @Test
    public void testGetKeycloakSecurityContext() throws Exception {
        KeycloakSecurityContext context = factory.getKeycloakSecurityContext();
        assertNotNull(context);
        assertEquals(keycloakSecurityContext, context);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetKeycloakSecurityContextInvalidPrincipal() throws Exception {
        servletRequest.setUserPrincipal(new MarkerPrincipal());
        factory.getKeycloakSecurityContext();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetKeycloakSecurityContextNullAuthentication() throws Exception {
        servletRequest.setUserPrincipal(null);
        factory.getKeycloakSecurityContext();
    }

    private static class MarkerPrincipal implements Principal {
        @Override
        public String getName() {
            return null;
        }
    }
}
