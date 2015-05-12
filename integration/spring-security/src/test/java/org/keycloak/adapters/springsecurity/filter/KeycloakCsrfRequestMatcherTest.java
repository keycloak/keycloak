package org.keycloak.adapters.springsecurity.filter;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.constants.AdapterConstants;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.*;

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

    @Test
    public void testMatchesKeycloakVersion() throws Exception {

        prepareRequest(HttpMethod.POST, ROOT_CONTEXT_PATH, AdapterConstants.K_VERSION);
        assertFalse(matcher.matches(request));

        prepareRequest(HttpMethod.POST, SUB_CONTEXT_PATH, AdapterConstants.K_VERSION);
        assertFalse(matcher.matches(request));
    }

    private void prepareRequest(HttpMethod method, String contextPath, String uri) {
        request.setMethod(method.name());
        request.setContextPath(contextPath);
        request.setRequestURI(contextPath + "/" + uri);
    }
}
