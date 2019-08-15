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

package org.keycloak.adapters.springsecurity.filter;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.constants.AdapterConstants;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Keycloak CSRF request matcher tests.
 */
public class KeycloakCsrfRequestMatcherTest {

    private static final String ROOT_CONTEXT_PATH = "";
    private static final String SUB_CONTEXT_PATH = "/foo";

    private KeycloakCsrfRequestMatcher matcher = new KeycloakCsrfRequestMatcher();

    private MockHttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        request = new MockHttpServletRequest();
    }

    @Test
    public void testMatchesMethodGet() throws Exception {
        request.setMethod(HttpMethod.GET.name());
        assertFalse(matcher.matches(request));
    }

    @Test
    public void testMatchesMethodPost() throws Exception {
        prepareRequest(HttpMethod.POST, ROOT_CONTEXT_PATH, "some/random/uri");
        assertTrue(matcher.matches(request));

        prepareRequest(HttpMethod.POST, SUB_CONTEXT_PATH, "some/random/uri");
        assertTrue(matcher.matches(request));
    }

    @Test
    public void testMatchesKeycloakLogout() throws Exception {

        prepareRequest(HttpMethod.POST, ROOT_CONTEXT_PATH, AdapterConstants.K_LOGOUT);
        assertFalse(matcher.matches(request));

        prepareRequest(HttpMethod.POST, SUB_CONTEXT_PATH, AdapterConstants.K_LOGOUT);
        assertFalse(matcher.matches(request));
    }

    @Test
    public void testMatchesKeycloakPushNotBefore() throws Exception {

        prepareRequest(HttpMethod.POST, ROOT_CONTEXT_PATH, AdapterConstants.K_PUSH_NOT_BEFORE);
        assertFalse(matcher.matches(request));

        prepareRequest(HttpMethod.POST, SUB_CONTEXT_PATH, AdapterConstants.K_PUSH_NOT_BEFORE);
        assertFalse(matcher.matches(request));
    }

    @Test
    public void testMatchesKeycloakQueryBearerToken() throws Exception {

        prepareRequest(HttpMethod.POST, ROOT_CONTEXT_PATH, AdapterConstants.K_QUERY_BEARER_TOKEN);
        assertFalse(matcher.matches(request));

        prepareRequest(HttpMethod.POST, SUB_CONTEXT_PATH, AdapterConstants.K_QUERY_BEARER_TOKEN);
        assertFalse(matcher.matches(request));
    }

    @Test
    public void testMatchesKeycloakTestAvailable() throws Exception {

        prepareRequest(HttpMethod.POST, ROOT_CONTEXT_PATH, AdapterConstants.K_TEST_AVAILABLE);
        assertFalse(matcher.matches(request));

        prepareRequest(HttpMethod.POST, SUB_CONTEXT_PATH, AdapterConstants.K_TEST_AVAILABLE);
        assertFalse(matcher.matches(request));
    }

    private void prepareRequest(HttpMethod method, String contextPath, String uri) {
        request.setMethod(method.name());
        request.setContextPath(contextPath);
        request.setRequestURI(contextPath + "/" + uri);
    }
}
