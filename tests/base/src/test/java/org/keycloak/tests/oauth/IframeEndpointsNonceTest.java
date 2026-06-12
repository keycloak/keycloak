package org.keycloak.tests.oauth;

import java.io.IOException;

import org.keycloak.common.Version;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.tests.utils.InlineScriptNonceUtil;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts that OIDC protocol iframe pages carry a CSP nonce on their inline scripts,
 * and are not cacheable as the nonce is only valid for a single response.
 */
@KeycloakIntegrationTest
public class IframeEndpointsNonceTest {

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpClient
    HttpClient httpClient;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void loginStatusIframeContainsNonce() throws IOException {
        assertPageContainsUniqueNonce("/realms/master/protocol/openid-connect/login-status-iframe.html");
    }

    @Test
    public void thirdPartyCookiesStep1ContainsNonce() throws IOException {
        assertPageContainsUniqueNonce("/realms/master/protocol/openid-connect/3p-cookies/step1.html");
    }

    @Test
    public void thirdPartyCookiesStep2ContainsNonce() throws IOException {
        assertPageContainsUniqueNonce("/realms/master/protocol/openid-connect/3p-cookies/step2.html");
    }

    @Test
    public void iframePagesAreNotCacheable() throws IOException {
        String version = "?version=" + runOnServer.fetch(session -> Version.RESOURCES_VERSION, String.class);

        assertNotCacheable("/realms/master/protocol/openid-connect/login-status-iframe.html");
        assertNotCacheable("/realms/master/protocol/openid-connect/login-status-iframe.html" + version);
        assertNotCacheable("/realms/master/protocol/openid-connect/3p-cookies/step1.html");
        assertNotCacheable("/realms/master/protocol/openid-connect/3p-cookies/step1.html" + version);
        assertNotCacheable("/realms/master/protocol/openid-connect/3p-cookies/step2.html");
        assertNotCacheable("/realms/master/protocol/openid-connect/3p-cookies/step2.html" + version);
    }

    private void assertNotCacheable(String path) throws IOException {
        var response = httpClient.execute(new HttpGet(keycloakUrls.getBaseUrl().toString() + path));
        try {
            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected 200 for " + path);
            String cacheControl = response.getFirstHeader("Cache-Control").getValue();
            assertFalse(cacheControl.contains("max-age"), "Page must not be cacheable, was: " + cacheControl);
            assertTrue(cacheControl.contains("no-store"), "Page must not be cacheable, was: " + cacheControl);
        } finally {
            EntityUtils.consume(response.getEntity());
        }
    }

    private void assertPageContainsUniqueNonce(String path) throws IOException {
        String body1 = getPage(path);
        String body2 = getPage(path);

        var inlineScriptsWithoutNonce = InlineScriptNonceUtil.getInlineScriptTagsWithoutNonce(body1);
        Assertions.assertTrue(inlineScriptsWithoutNonce.isEmpty(),
                () -> String.format("Page contains %d scripts without nonce: %s", inlineScriptsWithoutNonce.size(), inlineScriptsWithoutNonce));

        var nonces1 = InlineScriptNonceUtil.getScriptNonceValues(body1);
        var nonces2 = InlineScriptNonceUtil.getScriptNonceValues(body2);

        assertFalse(nonces1.isEmpty(), "Page should contain at least one inline script with a nonce");
        assertNotEquals(nonces1, nonces2, "Nonces should be unique per page invocation");
    }

    private String getPage(String path) throws IOException {
        var response = httpClient.execute(new HttpGet(keycloakUrls.getBaseUrl().toString() + path));
        assertEquals(200, response.getStatusLine().getStatusCode(), "Expected 200 for " + path);
        return EntityUtils.toString(response.getEntity());
    }
}
