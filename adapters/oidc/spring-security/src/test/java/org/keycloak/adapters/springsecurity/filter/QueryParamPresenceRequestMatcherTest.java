/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

public class QueryParamPresenceRequestMatcherTest {
    private static final String ROOT_CONTEXT_PATH = "";

    private static final String VALID_PARAMETER = "access_token";

    private QueryParamPresenceRequestMatcher matcher = new QueryParamPresenceRequestMatcher(VALID_PARAMETER);

    private MockHttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        request = new MockHttpServletRequest();
    }

    @Test
    public void testDoesNotMatchWithoutQueryParameter() throws Exception {
        prepareRequest(HttpMethod.GET, ROOT_CONTEXT_PATH, "some/random/uri", Collections.EMPTY_MAP);
        assertFalse(matcher.matches(request));
    }

    @Test
    public void testMatchesWithValidParameter() throws Exception {
        prepareRequest(HttpMethod.GET, ROOT_CONTEXT_PATH, "some/random/uri", Collections.singletonMap(VALID_PARAMETER, (Object) "123"));
        assertTrue(matcher.matches(request));
    }

    @Test
    public void testDoesNotMatchWithInvalidParameter() throws Exception {
        prepareRequest(HttpMethod.GET, ROOT_CONTEXT_PATH, "some/random/uri", Collections.singletonMap("some_parameter", (Object) "123"));
        assertFalse(matcher.matches(request));
    }

    private void prepareRequest(HttpMethod method, String contextPath, String uri, Map<String, Object> params) {
        request.setMethod(method.name());
        request.setContextPath(contextPath);
        request.setRequestURI(contextPath + "/" + uri);
        request.setParameters(params);
    }
}
