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

package org.keycloak.adapters.springsecurity.facade;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.Cookie;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Wrapped HTTP servlet request tests.
 */
public class WrappedHttpServletRequestTest {

    private static final String COOKIE_NAME = "oreo";
    private static final String HEADER_MULTI_VALUE = "Multi";
    private static final String HEADER_SINGLE_VALUE = "Single";
    private static final String REQUEST_METHOD = RequestMethod.GET.name();
    private static final String REQUEST_URI = "/foo/bar";
    private static final String QUERY_PARM_1 = "code";
    private static final String QUERY_PARM_2 = "code2";

    private WrappedHttpServletRequest request;
    private MockHttpServletRequest mockHttpServletRequest;

    @Before
    public void setUp() throws Exception {
        mockHttpServletRequest = new MockHttpServletRequest();
        request = new WrappedHttpServletRequest(mockHttpServletRequest);

        mockHttpServletRequest.setMethod(REQUEST_METHOD);
        mockHttpServletRequest.setRequestURI(REQUEST_URI);

        mockHttpServletRequest.setSecure(true);
        mockHttpServletRequest.setScheme("https");

        mockHttpServletRequest.addHeader(HEADER_SINGLE_VALUE, "baz");
        mockHttpServletRequest.addHeader(HEADER_MULTI_VALUE, "foo");
        mockHttpServletRequest.addHeader(HEADER_MULTI_VALUE, "bar");

        mockHttpServletRequest.addParameter(QUERY_PARM_1, "java");
        mockHttpServletRequest.addParameter(QUERY_PARM_2, "groovy");
        mockHttpServletRequest.setQueryString(String.format("%s=%s&%s=%s", QUERY_PARM_1, "java", QUERY_PARM_2, "groovy"));
        mockHttpServletRequest.setCookies(new Cookie(COOKIE_NAME, "yum"));

        mockHttpServletRequest.setContent("All work and no play makes Jack a dull boy".getBytes());
    }

    @Test
    public void testGetMethod() throws Exception {
        assertNotNull(request.getMethod());
        assertEquals(REQUEST_METHOD, request.getMethod());
    }

    @Test
    public void testGetURI() throws Exception {
        assertEquals("https://localhost:80" + REQUEST_URI + "?code=java&code2=groovy" , request.getURI());
    }

    @Test
    public void testIsSecure() throws Exception {
        assertTrue(request.isSecure());
    }

    @Test
    public void testGetQueryParamValue() throws Exception {
        assertNotNull(request.getQueryParamValue(QUERY_PARM_1));
        assertNotNull(request.getQueryParamValue(QUERY_PARM_2));
    }

    @Test
    public void testGetCookie() throws Exception {
        assertNotNull(request.getCookie(COOKIE_NAME));
    }

    @Test
    public void testGetCookieCookiesNull() throws Exception
    {
        mockHttpServletRequest.setCookies(null);
        request.getCookie(COOKIE_NAME);
    }

    @Test
    public void testGetHeader() throws Exception {
        String header = request.getHeader(HEADER_SINGLE_VALUE);
        assertNotNull(header);
        assertEquals("baz", header);
    }

    @Test
    public void testGetHeaders() throws Exception {
        List<String> headers = request.getHeaders(HEADER_MULTI_VALUE);
        assertNotNull(headers);
        assertEquals(2, headers.size());
        assertTrue(headers.contains("foo"));
        assertTrue(headers.contains("bar"));
    }

    @Test
    public void testGetInputStream() throws Exception {
        assertNotNull(request.getInputStream());
    }

    @Test
    public void testGetRemoteAddr() throws Exception {
        assertNotNull(request.getRemoteAddr());
    }
}