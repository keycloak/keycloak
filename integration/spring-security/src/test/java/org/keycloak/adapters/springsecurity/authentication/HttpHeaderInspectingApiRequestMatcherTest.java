package org.keycloak.adapters.springsecurity.authentication;

import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.junit.Assert.*;

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
    public void testMatches() throws Exception {
        assertTrue(apiRequestMatcher.matches(request));
    }

    @Test
    public void testMatchesBrowserRequest() throws Exception {
        request.addHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
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
