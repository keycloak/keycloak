/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.cookie.CookieType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.protocol.ClientData;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.util.ApiUtil;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Regression tests for keycloak/keycloak#53823: redirect-URI validation on the two branches
 * that run without a live authentication session and therefore validate the (untrusted)
 * client_data redirect directly.
 *
 * ul
 * li SamlProtocol#sendError(client, clientData, error) - reached when the authentication
 * session is gone but the user still holds a valid identity cookie: a SAML error response never
 * appends OIDC response parameters to the redirect URI, so the forbidden-parameter check must not
 * reject a registered redirect URI of a SAML client carrying an OIDC-named query parameter.
 * li SessionCodeChecks#restartAuthenticationSessionFromCookie - reached when the
 * authentication session expired but a KC_RESTART cookie remains: the restarted session keeps
 * the protocol it was started with, so a SAML session accepts its redirect as SAML while an OIDC
 * session must keep rejecting a redirect carrying OIDC-named parameters.
 * /ul
 *
 * Both exemptions are resolved from the client/session protocol and never broaden to an OIDC
 * client (parameter pollution, CVE-2026-9689).
 */
@KeycloakIntegrationTest
public class ExpiredAuthenticationSessionRedirectUriTest extends AbstractSamlTest {

    // An OIDC client with the same redirect URIs as the SAML client under test: used for the
    // negative controls proving the exemptions stay scoped to SAML.
    private static final String OIDC_CLIENT_ID = "expired-session-oidc-client";

    // IdP-initiated SSO URL name of the second SAML client from testsaml.json: a plain GET
    // seeds the login form without needing an SP-side AuthnRequest endpoint.
    private static final String SSO_URL_NAME = "sales-post2";

    @InjectUser(config = LoginUserConfig.class)
    ManagedUser user;

    // Stateless on purpose: every request sets its cookies explicitly, so no
    // authentication-session, identity or restart cookie is ever carried implicitly.
    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @Test
    public void sendErrorAcceptsOidcNamedParameterOnSamlClientRedirect() throws IOException, URISyntaxException {
        String identityCookie = loginAndReturnIdentityCookie();

        String acsWithOidcParam = getSamlAssertionConsumerUrl() + "?state=foo";
        // SAML client data: responseMode selects the POST binding of the error response
        String clientData = new ClientData(acsWithOidcParam, null, SamlProtocol.SAML_POST_BINDING, null).encode();

        HttpGet get = authenticateGet(SAML_CLIENT_ID_SALES_POST, clientData, identityCookie);
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            String body = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            // No status assertion on purpose: with the authentication session gone the request
            // first takes the restart-login branch, whose missing-cookie error page leaves
            // status 400 on the session-scoped LoginFormsProvider; createSamlPostForm() then
            // inherits that status when it renders the SAML response (FreeMarkerLoginFormsProvider
            // renders `status == null ? OK : status`). The inherited 400 is pre-existing upstream
            // behavior on this branch, identical on main for a clean redirect URI, so what this
            // test pins is the response CONTENT: the SAML error response must be rendered as a
            // POST binding form bound for the registered redirect URI even though it carries an
            // OIDC-named parameter.
            assertThat(body, Matchers.containsString("action=\"" + acsWithOidcParam + "\""));
            assertThat(body, Matchers.containsString("SAMLResponse"));
        }
    }

    @Test
    public void sendErrorStillRejectsUnregisteredRedirectForSamlClient() throws IOException, URISyntaxException {
        String identityCookie = loginAndReturnIdentityCookie();

        String clientData = new ClientData("https://evil.example.com/acs?state=foo", null, SamlProtocol.SAML_POST_BINDING, null).encode();

        HttpGet get = authenticateGet(SAML_CLIENT_ID_SALES_POST, clientData, identityCookie);
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            Assertions.assertEquals(400, response.getStatusLine().getStatusCode(),
                    "the exemption for OIDC-named parameters must not weaken the registered-redirect check");
            String body = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            assertThat(body, Matchers.containsString("Invalid redirect uri"));
        }
    }

    @Test
    public void sendErrorStillRejectsOidcNamedParameterForOidcClient() throws IOException, URISyntaxException {
        String identityCookie = loginAndReturnIdentityCookie();
        createOidcClientWithCleanup();

        String acsWithOidcParam = getSamlAssertionConsumerUrl() + "?state=foo";
        String clientData = new ClientData(acsWithOidcParam, "code", null, null).encode();

        HttpGet get = authenticateGet(OIDC_CLIENT_ID, clientData, identityCookie);
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            Assertions.assertEquals(400, response.getStatusLine().getStatusCode(),
                    "an OIDC client redirect carrying an OIDC-named parameter stays rejected (parameter pollution, CVE-2026-9689)");
            String body = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            assertThat(body, Matchers.containsString("redirect_uri"));
        }
    }

    @Test
    public void restartCookieAcceptsOidcNamedParameterOnSamlClientRedirect()
            throws ParsingException, ConfigurationException, ProcessingException, IOException, URISyntaxException {
        String acsWithOidcParam = getSamlAssertionConsumerUrl() + "?state=foo";

        // A SAML login request seeds the restart cookie and the login form URL carrying client_data
        AuthnRequestType loginRep = createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, acsWithOidcParam, REALM_NAME);
        Document samlRequest = SAML2Request.convert(loginRep);
        URI samlEndpoint = getAuthServerSamlEndpoint(REALM_NAME);
        HttpUriRequest post = SamlClient.Binding.POST.createSamlUnsignedRequest(samlEndpoint, null, samlRequest);

        String loginFormUrl;
        String restartCookieValue;
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            Assertions.assertEquals(302, response.getStatusLine().getStatusCode(),
                    "a SAML login request for a registered ACS URL carrying an OIDC-named parameter must reach the login form");
            loginFormUrl = response.getFirstHeader("Location").getValue();
            restartCookieValue = cookieFromHeaders(response, RestartLoginCookie.KC_RESTART);
        }
        Assertions.assertNotNull(restartCookieValue, "the SAML login request must set the restart cookie");

        // The authentication session expired (no AUTH_SESSION_ID cookie is sent): the restart
        // restores the SAML session and must keep the redirect URI with the OIDC-named parameter
        String clientData = rawQueryParam(loginFormUrl, "client_data");
        if (clientData == null) {
            clientData = new ClientData(acsWithOidcParam, null, SamlProtocol.SAML_POST_BINDING, null).encode();
        }

        HttpGet get = restartGet(SAML_CLIENT_ID_SALES_POST, clientData, restartCookieValue);
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            Assertions.assertEquals(302, response.getStatusLine().getStatusCode(),
                    "the SAML restart must adopt the registered redirect URI carrying an OIDC-named parameter and continue the flow");
            String next = response.getFirstHeader("Location").getValue();
            ClientData restarted = ClientData.decodeClientDataFromParameter(rawQueryParam(next, "client_data"));
            Assertions.assertEquals(acsWithOidcParam, restarted.getRedirectUri(),
                    "the restarted SAML session must keep the redirect URI with the OIDC-named parameter");
        }
    }

    @Test
    public void restartCookieRejectsOidcNamedParameterForOidcClient() throws IOException, URISyntaxException {
        createOidcClientWithCleanup();
        String cleanAcs = getSamlAssertionConsumerUrl();
        String pollutedAcs = cleanAcs + "?state=foo";

        // An OIDC authorization request for a clean redirect seeds the restart cookie. The OIDC
        // authorize endpoint renders the login form inline (HTTP 200) instead of redirecting to it,
        // but the restart cookie is set on that very response.
        String authorizeUrl = getAuthServerRealmBase(REALM_NAME) + "/protocol/openid-connect/auth"
                + "?client_id=" + URLEncoder.encode(OIDC_CLIENT_ID, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(cleanAcs, StandardCharsets.UTF_8)
                + "&response_type=code&scope=openid&state=xyz";
        String restartCookieValue;
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(authorizeUrl))) {
            Assertions.assertEquals(200, response.getStatusLine().getStatusCode(),
                    "the OIDC authorization request for a clean redirect URI must reach the login form (rendered inline for OIDC)");
            restartCookieValue = cookieFromHeaders(response, RestartLoginCookie.KC_RESTART);
        }
        Assertions.assertNotNull(restartCookieValue, "the OIDC authorization request must set the restart cookie");

        // An attacker-controlled client_data redirect carrying an OIDC-named parameter: the
        // restarted OIDC session must not adopt it and keeps the cookie clean redirect
        String clientData = new ClientData(pollutedAcs, "code", null, "xyz").encode();
        HttpGet get = restartGet(OIDC_CLIENT_ID, clientData, restartCookieValue);
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            Assertions.assertEquals(302, response.getStatusLine().getStatusCode(),
                    "the OIDC restart must continue the flow instead of failing on the untrusted client_data redirect");
            String next = response.getFirstHeader("Location").getValue();
            ClientData restarted = ClientData.decodeClientDataFromParameter(rawQueryParam(next, "client_data"));
            Assertions.assertEquals(cleanAcs, restarted.getRedirectUri(),
                    "the restarted OIDC session must reject a redirect URI carrying an OIDC-named parameter (CVE-2026-9689)");
        }
    }

    /**
     * Completes a login over raw HTTP through the second SAML client's IdP-initiated SSO URL and
     * returns the value of the identity cookie set on login completion. The login response itself
     * targets an unreachable ACS, which is irrelevant here: the identity cookie is set when the
     * login completes on the server side. The login runs on a throwaway cookie-less client, so
     * the returned cookie is the only state this test method introduces.
     */
    private String loginAndReturnIdentityCookie() throws IOException, URISyntaxException {
        try (CloseableHttpClient statelessClient = HttpClientBuilder.create()
                .disableRedirectHandling()
                .disableCookieManagement()
                .build()) {
            // The IdP-initiated SSO URL renders the login form inline on the plain GET
            HttpGet openLoginPage = new HttpGet(getAuthServerRealmBase(REALM_NAME) + "/protocol/saml/clients/" + SSO_URL_NAME);
            String actionUrl;
            String sessionCookies;
            try (CloseableHttpResponse loginPageResponse = statelessClient.execute(openLoginPage)) {
                Assertions.assertEquals(200, loginPageResponse.getStatusLine().getStatusCode(),
                        "the IdP-initiated SSO URL must render the login form");
                String html = IOUtils.toString(loginPageResponse.getEntity().getContent(), StandardCharsets.UTF_8);
                actionUrl = formActionUrl(html);
                // the authentication session of the rendered form lives in the AUTH_SESSION_ID
                // cookie, so the credential POST must echo the cookies of this response
                sessionCookies = requestCookies(loginPageResponse);
            }
            Assertions.assertNotNull(actionUrl, "the login form must carry an action URL");

            // Submit the credentials; the login completes on the server side and the response
            // (the SAML POST form for the unreachable ACS) carries the identity cookie
            HttpPost submitCredentials = new HttpPost(actionUrl);
            submitCredentials.setHeader("Cookie", sessionCookies);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("username", user.getUsername()));
            params.add(new BasicNameValuePair("password", user.getPassword()));
            submitCredentials.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
            try (CloseableHttpResponse loginResponse = statelessClient.execute(submitCredentials)) {
                String identityCookie = cookieFromHeaders(loginResponse, CookieType.IDENTITY.getName());
                Assertions.assertNotNull(identityCookie,
                        "the login must set the identity cookie (login status "
                                + loginResponse.getStatusLine().getStatusCode() + ")");
                return identityCookie;
            }
        }
    }

    /**
     * Creates an OIDC client with the same redirect URIs as the SAML client under test and
     * registers its removal with the realm cleanup.
     */
    private void createOidcClientWithCleanup() throws URISyntaxException {
        String cleanAcs = getSamlAssertionConsumerUrl();
        String createdId = ApiUtil.getCreatedId(samlRealm.admin().clients().create(ClientBuilder.create()
                .clientId(OIDC_CLIENT_ID)
                .protocol("openid-connect")
                // the clean and the OIDC-named variants are both registered, so a redirect is only
                // ever rejected because of the forbidden OIDC parameter, never for being unregistered
                .redirectUris(cleanAcs, cleanAcs + "?state=foo")
                .publicClient()
                .build()));
        samlRealm.cleanup().add(r -> r.clients().get(createdId).remove());
    }

    private HttpGet authenticateGet(String clientId, String clientData, String identityCookieValue) throws URISyntaxException {
        HttpGet get = new HttpGet(getAuthServerRealmBase(REALM_NAME) + "/login-actions/authenticate"
                + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_data=" + clientData);
        // no authentication-session and no restart cookie: the session is gone, only the identity
        // cookie remains, so the request takes the already-logged-in sendError branch
        get.setHeader("Cookie", CookieType.IDENTITY.getName() + "=" + identityCookieValue);
        return get;
    }

    private HttpGet restartGet(String clientId, String clientData, String restartCookieValue) throws URISyntaxException {
        HttpGet get = new HttpGet(getAuthServerRealmBase(REALM_NAME) + "/login-actions/authenticate"
                + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_data=" + clientData);
        // only the restart cookie: the authentication session expired, so the request takes the
        // restart-from-cookie branch
        get.setHeader("Cookie", RestartLoginCookie.KC_RESTART + "=" + restartCookieValue);
        return get;
    }

    /**
     * Folds every Set-Cookie header of the response into a single Cookie request header value, the
     * minimal browser behavior the credential POST needs to reach its authentication session.
     */
    private static String requestCookies(HttpResponse response) {
        StringBuilder cookies = new StringBuilder();
        for (Header header : response.getHeaders("Set-Cookie")) {
            String nameValue = header.getValue().split(";", 2)[0];
            if (cookies.length() > 0) {
                cookies.append("; ");
            }
            cookies.append(nameValue);
        }
        return cookies.toString();
    }

    private static String formActionUrl(String html) {
        Matcher matcher = Pattern.compile("action=\"([^\"]*)\"").matcher(html);
        if (matcher.find()) {
            // '&' separators are HTML-escaped as '&amp;' in the rendered form action; decode them
            // so the POST carries the real query parameters
            return matcher.group(1).replace("&amp;", "&");
        }
        return null;
    }

    private static String cookieFromHeaders(HttpResponse response, String cookieName) {
        for (Header header : response.getHeaders("Set-Cookie")) {
            String value = header.getValue();
            if (value.startsWith(cookieName + "=")) {
                return value.substring(cookieName.length() + 1).split(";", 2)[0];
            }
        }
        return null;
    }

    private static String rawQueryParam(String uri, String name) {
        String query = uri.substring(uri.indexOf(63) + 1);
        for (String param : query.split("&")) {
            int eq = param.indexOf(61);
            if (eq > 0 && param.substring(0, eq).equals(name)) {
                return param.substring(eq + 1);
            }
        }
        return null;
    }

    public static class LoginUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("expired-session-user").password("password").name("Session", "User")
                    .email("expired-session-user@example.org");
        }
    }
}
