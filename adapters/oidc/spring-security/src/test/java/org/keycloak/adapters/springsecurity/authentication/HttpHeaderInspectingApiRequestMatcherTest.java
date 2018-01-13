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

import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * HTTP header inspecting API request matcher tests.
 */
public class HttpHeaderInspectingApiRequestMatcherTest {

    private RequestMatcher apiRequestMatcher = new HttpHeaderInspectingApiRequestMatcher();
    private MockHttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        request = new MockHttpServletRequest();
    }

    @Test
    public void testMatchesBrowserRequest() throws Exception {
        request.addHeader(HttpHeaders.ACCEPT, "application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        assertFalse(apiRequestMatcher.matches(request));
    }

    @Test
    public void testMatchesRequestedWith() throws Exception {
        request.addHeader(
                HttpHeaderInspectingApiRequestMatcher.X_REQUESTED_WITH_HEADER,
                HttpHeaderInspectingApiRequestMatcher.X_REQUESTED_WITH_HEADER_AJAX_VALUE);
        
        assertTrue(apiRequestMatcher.matches(request));
    }
}
