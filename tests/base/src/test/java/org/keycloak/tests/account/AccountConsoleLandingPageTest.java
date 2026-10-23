package org.keycloak.tests.account;

import java.io.IOException;

import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.utils.InlineScriptNonceUtil;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class AccountConsoleLandingPageTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @Test
    public void accountConsoleNotCacheable() throws IOException {
        HttpGet request = new HttpGet(realm.getBaseUrl() + "/account/");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String cacheControl = response.getFirstHeader("Cache-Control").getValue();
            assertFalse(cacheControl.contains("max-age"), "Account console must not be cacheable, was: " + cacheControl);
            assertTrue(cacheControl.contains("no-store"), "Account console must not be cacheable, was: " + cacheControl);
        }
    }

    @Test
    public void inlineScriptsContainNonce() throws IOException {
        String body1 = getAccountConsolePage();
        String body2 = getAccountConsolePage();

        var inlineScriptsWithoutNonce = InlineScriptNonceUtil.getInlineScriptTagsWithoutNonce(body1);
        Assertions.assertTrue(inlineScriptsWithoutNonce.isEmpty(),
                () -> String.format("Page contains %d scripts without nonce: %s", inlineScriptsWithoutNonce.size(), inlineScriptsWithoutNonce));

        var nonces1 = InlineScriptNonceUtil.getScriptNonceValues(body1);
        var nonces2 = InlineScriptNonceUtil.getScriptNonceValues(body2);

        assertFalse(nonces1.isEmpty(), "Page should contain at least one inline script with a nonce");
        assertNotEquals(nonces1, nonces2, "Nonces should be unique per page invocation");
    }

    private String getAccountConsolePage() throws IOException {
        HttpGet request = new HttpGet(realm.getBaseUrl() + "/account/");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            return EntityUtils.toString(response.getEntity());
        }
    }
}
