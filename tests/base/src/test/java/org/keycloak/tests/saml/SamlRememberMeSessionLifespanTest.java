/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.saml;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.StatementAbstractType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.suites.DatabaseTest;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.keycloak.tests.utils.matchers.Matchers.isSamlResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests that SAML SessionNotOnOrAfter honors the Remember Me session lifespan
 * configuration. The production code uses {@code RealmExpiration.fromRealm(realm).getLifespan(rememberMe)}
 * with a {@code Math.max} guard ensuring remember-me sessions are never shorter
 * than regular sessions.
 */
@KeycloakIntegrationTest
@DatabaseTest
public class SamlRememberMeSessionLifespanTest extends AbstractSamlTest {

    private static final int SSO_MAX_LIFESPAN = 36000;           // 10 hours
    private static final int SSO_MAX_LIFESPAN_REMEMBER_ME = 72000; // 20 hours

    /**
     * Disable the VERIFY_PROFILE required action which would otherwise intercept
     * the login flow before authentication completes. The new test framework enables
     * it by default, but this test only needs the basic credential login flow.
     */
    @BeforeEach
    void disableVerifyProfile() {
        for (RequiredActionProviderRepresentation action : samlRealm.admin().flows().getRequiredActions()) {
            if ("VERIFY_PROFILE".equals(action.getAlias())) {
                action.setEnabled(false);
                samlRealm.admin().flows().updateRequiredAction(action.getAlias(), action);
            }
        }
    }

    /**
     * When remember-me lifespan is explicitly configured and larger than the
     * regular lifespan, a remember-me SAML login must produce a
     * SessionNotOnOrAfter equal to authnInstant + rememberMeLifespan.
     */
    @Test
    public void testRememberMeSessionLifespan() throws Exception {
        samlRealm.updateWithCleanup(realm -> realm
                .setRememberMe(true)
                .ssoSessionMaxLifespan(SSO_MAX_LIFESPAN)
                .ssoSessionMaxLifespanRememberMe(SSO_MAX_LIFESPAN_REMEMBER_ME));

        ResponseType response = performSamlLogin(true);
        AuthnStatementType authnStatement = extractAuthnStatement(response);

        assertThat(authnStatement.getSessionNotOnOrAfter(), notNullValue());
        assertThat(authnStatement.getSessionNotOnOrAfter(),
                is(XMLTimeUtil.add(authnStatement.getAuthnInstant(),
                        SSO_MAX_LIFESPAN_REMEMBER_ME * 1000L)));
    }

    /**
     * When remember-me lifespan is zero (not configured), the
     * {@code Math.max(regular, 0)} guard in {@code RealmExpiration.fromRealm()}
     * ensures the regular lifespan is used as the floor. SessionNotOnOrAfter
     * must equal authnInstant + regularLifespan.
     */
    @Test
    public void testRememberMeSessionLifespanFallback() throws Exception {
        samlRealm.updateWithCleanup(realm -> realm
                .setRememberMe(true)
                .ssoSessionMaxLifespan(SSO_MAX_LIFESPAN)
                .ssoSessionMaxLifespanRememberMe(0));

        ResponseType response = performSamlLogin(true);
        AuthnStatementType authnStatement = extractAuthnStatement(response);

        assertThat(authnStatement.getSessionNotOnOrAfter(), notNullValue());
        assertThat(authnStatement.getSessionNotOnOrAfter(),
                is(XMLTimeUtil.add(authnStatement.getAuthnInstant(),
                        SSO_MAX_LIFESPAN * 1000L)));
    }

    /**
     * When logging in without remember-me (even though it is enabled on the
     * realm and a larger remember-me lifespan is configured), the regular
     * lifespan must be used for SessionNotOnOrAfter.
     */
    @Test
    public void testNonRememberMeSessionLifespan() throws Exception {
        samlRealm.updateWithCleanup(realm -> realm
                .setRememberMe(true)
                .ssoSessionMaxLifespan(SSO_MAX_LIFESPAN)
                .ssoSessionMaxLifespanRememberMe(SSO_MAX_LIFESPAN_REMEMBER_ME));

        ResponseType response = performSamlLogin(false);
        AuthnStatementType authnStatement = extractAuthnStatement(response);

        assertThat(authnStatement.getSessionNotOnOrAfter(), notNullValue());
        assertThat(authnStatement.getSessionNotOnOrAfter(),
                is(XMLTimeUtil.add(authnStatement.getAuthnInstant(),
                        SSO_MAX_LIFESPAN * 1000L)));
    }

    // ---- helpers ----

    /**
     * Performs a full SAML POST-binding login flow via HTTP:
     * <ol>
     *   <li>POST the AuthnRequest to the SAML endpoint</li>
     *   <li>Follow the 302 redirect to the login page, forwarding cookies manually</li>
     *   <li>Parse the login form and POST credentials (with optional rememberMe)</li>
     *   <li>Follow redirects until the SAMLResponse auto-submit page is returned</li>
     * </ol>
     *
     * Cookies are forwarded manually because Apache HttpClient 4.x rejects
     * Set-Cookie headers containing the SameSite attribute (not in RFC 6265).
     */
    private ResponseType performSamlLogin(boolean rememberMe) throws Exception {
        java.util.Map<String, String> cookies = new java.util.LinkedHashMap<>();
        try (CloseableHttpClient client = HttpClientBuilder.create()
                .disableRedirectHandling()
                .disableCookieManagement()
                .build()) {
            // Build SAML AuthnRequest
            AuthnRequestType loginReq = createLoginRequestDocument(
                    SAML_CLIENT_ID_SALES_POST, getSamlAssertionConsumerUrl(), REALM_NAME);
            Document samlRequest = SAML2Request.convert(loginReq);
            URI samlEndpoint = getAuthServerSamlEndpoint(REALM_NAME);

            // POST the SAML request; Keycloak returns a 302 redirect to the login page
            HttpUriRequest samlPost = SamlClient.Binding.POST
                    .createSamlUnsignedRequest(samlEndpoint, null, samlRequest);

            String redirectUrl;
            try (CloseableHttpResponse resp = client.execute(samlPost)) {
                assertThat("Expected 302 from SAML endpoint", resp.getStatusLine().getStatusCode(), is(302));
                redirectUrl = resp.getFirstHeader("Location").getValue();
                captureResponseCookies(resp, cookies);
                EntityUtils.consumeQuietly(resp.getEntity());
            }

            // Follow the redirect chain to the login page, capturing cookies at each hop
            String loginPageHtml = followRedirectChain(client, cookies, redirectUrl);

            // Build the login POST from the form, including all hidden fields
            HttpPost loginPost = buildLoginPost(loginPageHtml, "bburke", "password", rememberMe);
            addCookieHeader(loginPost, cookies);

            // Submit login POST and follow redirects to the SAML response page
            String samlResponsePage;
            try (CloseableHttpResponse resp = client.execute(loginPost)) {
                int status = resp.getStatusLine().getStatusCode();
                captureResponseCookies(resp, cookies);
                if (status == 302) {
                    String nextUrl = resp.getFirstHeader("Location").getValue();
                    EntityUtils.consumeQuietly(resp.getEntity());
                    samlResponsePage = followRedirectChain(client, cookies, nextUrl);
                } else {
                    samlResponsePage = EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
                }
            }

            // Extract the base64-encoded SAMLResponse from the auto-submit form
            String samlResponseBase64 = extractSamlResponseValue(samlResponsePage);
            SAMLDocumentHolder holder = SAMLRequestParser.parseResponsePostBinding(samlResponseBase64);
            assertThat(holder, notNullValue());
            assertThat(holder.getSamlObject(), isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));

            return (ResponseType) holder.getSamlObject();
        }
    }

    /**
     * Follows a chain of 302 redirects until a 200 response is received, capturing
     * cookies at each hop and adding them to outgoing requests via headers.
     */
    private static String followRedirectChain(CloseableHttpClient client,
            java.util.Map<String, String> cookies, String url) throws Exception {
        String currentUrl = url;
        for (int i = 0; i < 10; i++) {
            HttpGet get = new HttpGet(currentUrl);
            addCookieHeader(get, cookies);
            try (CloseableHttpResponse resp = client.execute(get)) {
                captureResponseCookies(resp, cookies);
                int status = resp.getStatusLine().getStatusCode();
                if (status == 302) {
                    currentUrl = resp.getFirstHeader("Location").getValue();
                    EntityUtils.consumeQuietly(resp.getEntity());
                } else {
                    assertThat("Expected 200 after redirect chain, url=" + currentUrl, status, is(200));
                    return EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("Too many redirects following " + url);
    }

    /**
     * Adds a Cookie header to the request from the cookie map.
     */
    private static void addCookieHeader(HttpUriRequest request, java.util.Map<String, String> cookies) {
        if (!cookies.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (java.util.Map.Entry<String, String> e : cookies.entrySet()) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(e.getKey()).append("=").append(e.getValue());
            }
            request.setHeader("Cookie", sb.toString());
        }
    }

    /**
     * Captures cookies from Set-Cookie response headers into a simple map.
     * Bypasses Apache HttpClient's cookie spec which rejects the SameSite attribute.
     */
    private static void captureResponseCookies(CloseableHttpResponse response,
            java.util.Map<String, String> cookies) {
        for (Header header : response.getHeaders("Set-Cookie")) {
            String value = header.getValue();
            String[] parts = value.split(";");
            if (parts.length == 0) continue;
            String[] nameValue = parts[0].split("=", 2);
            if (nameValue.length < 2) continue;
            cookies.put(nameValue[0].trim(), nameValue[1].trim());
        }
    }

    /**
     * Extracts the {@link AuthnStatementType} from the first assertion in the response.
     */
    private AuthnStatementType extractAuthnStatement(ResponseType response) {
        assertThat(response.getAssertions(), notNullValue());
        assertThat(response.getAssertions().isEmpty(), is(false));
        assertThat(response.getAssertions().get(0).getAssertion(), notNullValue());

        Set<StatementAbstractType> statements =
                response.getAssertions().get(0).getAssertion().getStatements();
        assertThat(statements, notNullValue());

        AuthnStatementType authnStatement = statements.stream()
                .filter(AuthnStatementType.class::isInstance)
                .map(AuthnStatementType.class::cast)
                .findFirst()
                .orElse(null);

        assertThat("AuthnStatement must be present in the SAML assertion", authnStatement, notNullValue());
        return authnStatement;
    }

    /**
     * Builds an HttpPost from the login page HTML, extracting the form action URL
     * and including ALL form inputs (hidden fields, CSRF tokens, etc.).
     * Sets the username, password, and optionally rememberMe checkbox.
     */
    private static HttpPost buildLoginPost(String html, String username, String password, boolean rememberMe) {
        // Extract form action URL
        Pattern actionPattern = Pattern.compile("<form[^>]*action=\"([^\"]*)\"[^>]*>", Pattern.DOTALL);
        Matcher actionMatcher = actionPattern.matcher(html);
        assertThat("Login form not found in page", actionMatcher.find(), is(true));
        String actionUrl = actionMatcher.group(1).replace("&amp;", "&");

        // Extract all input fields from the form
        List<NameValuePair> params = new LinkedList<>();
        Pattern inputPattern = Pattern.compile("<input[^>]*>", Pattern.DOTALL);
        Matcher inputMatcher = inputPattern.matcher(html);

        while (inputMatcher.find()) {
            String inputTag = inputMatcher.group();
            String name = extractAttr(inputTag, "name");
            if (name == null || name.isEmpty()) continue;

            String id = extractAttr(inputTag, "id");
            String type = extractAttr(inputTag, "type");
            String value = extractAttr(inputTag, "value");

            if ("username".equals(id) || "username".equals(name)) {
                params.add(new BasicNameValuePair(name, username));
            } else if ("password".equals(id) || "password".equals(name)) {
                params.add(new BasicNameValuePair(name, password));
            } else if ("rememberMe".equals(name) || "rememberMe".equals(id)) {
                if (rememberMe) {
                    params.add(new BasicNameValuePair(name, value != null ? value : "on"));
                }
            } else if ("checkbox".equals(type)) {
                // Skip unchecked checkboxes
            } else {
                params.add(new BasicNameValuePair(name, value != null ? value : ""));
            }
        }

        // Ensure username and password are present even if not found as inputs
        boolean hasUsername = params.stream().anyMatch(p -> "username".equals(p.getName()));
        boolean hasPassword = params.stream().anyMatch(p -> "password".equals(p.getName()));
        if (!hasUsername) params.add(new BasicNameValuePair("username", username));
        if (!hasPassword) params.add(new BasicNameValuePair("password", password));

        HttpPost post = new HttpPost(actionUrl);
        post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        return post;
    }

    /**
     * Extracts an HTML attribute value from a tag string.
     */
    private static String extractAttr(String tag, String attrName) {
        Pattern p = Pattern.compile(attrName + "=\"([^\"]*)\"");
        Matcher m = p.matcher(tag);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Extracts the base64-encoded SAMLResponse value from the auto-submit form page
     * returned by Keycloak after successful authentication.
     */
    private static String extractSamlResponseValue(String html) {
        // Try name-then-value and value-then-name with possible intervening attributes
        Pattern pattern = Pattern.compile("name=\"SAMLResponse\"[^>]*value=\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) return matcher.group(1);

        pattern = Pattern.compile("value=\"([^\"]*)\"[^>]*name=\"SAMLResponse\"");
        matcher = pattern.matcher(html);
        assertThat("SAMLResponse input not found in response page", matcher.find(), is(true));
        return matcher.group(1);
    }
}
