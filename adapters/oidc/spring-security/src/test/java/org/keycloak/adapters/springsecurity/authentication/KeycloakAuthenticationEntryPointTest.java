package org.keycloak.adapters.springsecurity.authentication;

import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.*;

/**
 * Keycloak authentication entry point tests.
 */
public class KeycloakAuthenticationEntryPointTest {

    private KeycloakAuthenticationEntryPoint authenticationEntryPoint;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        authenticationEntryPoint = new KeycloakAuthenticationEntryPoint();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void testCommenceWithRedirect() throws Exception {
        configureBrowserRequest();
        authenticationEntryPoint.commence(request, response, null);
        assertEquals(HttpStatus.FOUND.value(), response.getStatus());
        assertEquals(KeycloakAuthenticationEntryPoint.DEFAULT_LOGIN_URI, response.getHeader("Location"));
    }

    @Test
    public void testCommenceWithRedirectNotRootContext() throws Exception {
        configureBrowserRequest();
        String contextPath = "/foo";
        request.setContextPath(contextPath);
        authenticationEntryPoint.commence(request, response, null);
        assertEquals(HttpStatus.FOUND.value(), response.getStatus());
        assertEquals(contextPath + KeycloakAuthenticationEntryPoint.DEFAULT_LOGIN_URI, response.getHeader("Location"));
    }

    @Test
    public void testCommenceWithUnauthorizedWithAccept() throws Exception {
        request.addHeader(HttpHeaders.ACCEPT, "application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        authenticationEntryPoint.commence(request, response, null);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertNotNull(response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    public void testSetLoginUri() throws Exception {
        configureBrowserRequest();
        final String logoutUri = "/foo";
        authenticationEntryPoint.setLoginUri(logoutUri);
        authenticationEntryPoint.commence(request, response, null);
        assertEquals(HttpStatus.FOUND.value(), response.getStatus());
        assertEquals(logoutUri, response.getHeader("Location"));
    }

    private void configureBrowserRequest() {
        request.addHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    }
}
