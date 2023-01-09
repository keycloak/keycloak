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
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.Cookie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class WrappedHttpServletResponseTest {

    private static final String COOKIE_DOMAIN = ".keycloak.org";
    private static final String COOKIE_NAME = "foo";
    private static final String COOKIE_PATH = "/bar";
    private static final String COOKIE_VALUE = "onegreatcookie";
    private static final String HEADER = "Test";

    private WrappedHttpServletResponse response;
    private MockHttpServletResponse mockResponse;

    @Before
    public void setUp() throws Exception {
        mockResponse = spy(new MockHttpServletResponse());
        response = new WrappedHttpServletResponse(mockResponse);
    }

    @Test
    public void testResetCookie() throws Exception {
        response.resetCookie(COOKIE_NAME, COOKIE_PATH);
        verify(mockResponse).addCookie(any(Cookie.class));
        assertEquals(COOKIE_NAME, mockResponse.getCookie(COOKIE_NAME).getName());
        assertEquals(COOKIE_PATH, mockResponse.getCookie(COOKIE_NAME).getPath());
        assertEquals(0, mockResponse.getCookie(COOKIE_NAME).getMaxAge());
        assertEquals("", mockResponse.getCookie(COOKIE_NAME).getValue());
    }

    @Test
    public void testSetCookie() throws Exception {
        int maxAge = 300;
        response.setCookie(COOKIE_NAME, COOKIE_VALUE, COOKIE_PATH, COOKIE_DOMAIN, maxAge, false, true);
        verify(mockResponse).addCookie(any(Cookie.class));
        assertEquals(COOKIE_NAME, mockResponse.getCookie(COOKIE_NAME).getName());
        assertEquals(COOKIE_PATH, mockResponse.getCookie(COOKIE_NAME).getPath());
        assertEquals(COOKIE_DOMAIN, mockResponse.getCookie(COOKIE_NAME).getDomain());
        assertEquals(maxAge, mockResponse.getCookie(COOKIE_NAME).getMaxAge());
        assertEquals(COOKIE_VALUE, mockResponse.getCookie(COOKIE_NAME).getValue());
        assertEquals(true, mockResponse.getCookie(COOKIE_NAME).isHttpOnly());
    }

    @Test
    public void testSetStatus() throws Exception {
        int status = HttpStatus.OK.value();
        response.setStatus(status);
        verify(mockResponse).setStatus(eq(status));
        assertEquals(status, mockResponse.getStatus());
    }

    @Test
    public void testAddHeader() throws Exception {
        String headerValue = "foo";
        response.addHeader(HEADER, headerValue);
        verify(mockResponse).addHeader(eq(HEADER), eq(headerValue));
        assertTrue(mockResponse.containsHeader(HEADER));
    }

    @Test
    public void testSetHeader() throws Exception {
        String headerValue = "foo";
        response.setHeader(HEADER, headerValue);
        verify(mockResponse).setHeader(eq(HEADER), eq(headerValue));
        assertTrue(mockResponse.containsHeader(HEADER));
    }

    @Test
    public void testGetOutputStream() throws Exception {
        assertNotNull(response.getOutputStream());
        verify(mockResponse).getOutputStream();
    }

    @Test
    public void testSendError() throws Exception {
        int status = HttpStatus.UNAUTHORIZED.value();
        String reason = HttpStatus.UNAUTHORIZED.getReasonPhrase();

        response.sendError(status, reason);
        verify(mockResponse).sendError(eq(status), eq(reason));
        assertEquals(status, mockResponse.getStatus());
        assertEquals(reason, mockResponse.getErrorMessage());
    }

    @Test
    @Ignore
    public void testEnd() throws Exception {
        // TODO: what is an ended response, one that's committed?
    }
}