package org.keycloak.tests.admin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
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

@KeycloakIntegrationTest
public class AdminConsoleLandingPageTest {

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpClient
    HttpClient httpClient;

    @Test
    public void adminConsoleNotCacheable() throws IOException {
        var response = httpClient.execute(new HttpGet(keycloakUrls.getBaseUrl().toString() + "/admin/master/console"));
        try {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String cacheControl = response.getFirstHeader("Cache-Control").getValue();
            assertFalse(cacheControl.contains("max-age"), "Admin console must not be cacheable, was: " + cacheControl);
            assertTrue(cacheControl.contains("no-store"), "Admin console must not be cacheable, was: " + cacheControl);
        } finally {
            EntityUtils.consume(response.getEntity());
        }
    }

    @Test
    public void landingPage() throws IOException {
        String body = EntityUtils.toString(httpClient.execute(new HttpGet(keycloakUrls.getBaseUrl().toString() + "/admin/master/console")).getEntity());

        Map<String, String> config = getConfig(body);
        String authUrl = config.get("authUrl");
        Assertions.assertEquals(keycloakUrls.getBaseUrl().toString()+ "", authUrl);

        String resourceUrl = config.get("resourceUrl");
        Assertions.assertTrue(resourceUrl.matches("/resources/[^/]*/admin/keycloak.v2"));

        String consoleBaseUrl = config.get("consoleBaseUrl");
        Assertions.assertEquals(consoleBaseUrl, "/admin/master/console/");

        Pattern p = Pattern.compile("link href=\"([^\"]*)\"");
        Matcher m = p.matcher(body);

        while(m.find()) {
            String url = m.group(1);
            Assertions.assertTrue(url.startsWith("/resources/"));
        }

        p = Pattern.compile("script src=\"([^\"]*)\"");
        m = p.matcher(body);

        while(m.find()) {
            String url = m.group(1);
            if (url.contains("keycloak.js")) {
                Assertions.assertTrue(url.startsWith("/js/"), url);
            } else {
                Assertions.assertTrue(url.startsWith("/resources/"), url);
            }
        }
    }

    @Test
    public void inlineScriptsContainUniqueNonce() throws IOException {
        String body1 = getConsolePage();
        String body2 = getConsolePage();

        var inlineScriptsWithoutNonce = InlineScriptNonceUtil.getInlineScriptTagsWithoutNonce(body1);
        Assertions.assertTrue(inlineScriptsWithoutNonce.isEmpty(),
                () -> String.format("Page contains %d scripts without nonce: %s", inlineScriptsWithoutNonce.size(), inlineScriptsWithoutNonce));

        var nonces1 = InlineScriptNonceUtil.getScriptNonceValues(body1);
        var nonces2 = InlineScriptNonceUtil.getScriptNonceValues(body2);

        assertFalse(nonces1.isEmpty(), "Page should contain at least one inline script with a nonce");
        assertNotEquals(nonces1, nonces2, "Nonces should be unique per page invocation");
    }

    private String getConsolePage() throws IOException {
        return EntityUtils.toString(httpClient.execute(new HttpGet(keycloakUrls.getBaseUrl().toString() + "/admin/master/console")).getEntity());
    }

    private static Map<String, String> getConfig(String body) {
        Map<String, String> variables = new HashMap<>();
        String start = "<script id=\"environment\" type=\"application/json\">";
        String end = "</script>";

        String config = body.substring(body.indexOf(start) + start.length());
        config = config.substring(0, config.indexOf(end)).trim();

        Matcher matcher = Pattern.compile(".*\"(.*)\": \"(.*)\"").matcher(config);
        while (matcher.find()) {
            variables.put(matcher.group(1), matcher.group(2));
        }

        return variables;
    }
}
