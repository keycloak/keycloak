/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.util.saml;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.Users;
import org.keycloak.testsuite.util.SamlClient.Step;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import static org.keycloak.testsuite.admin.Users.getPasswordOf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 *
 * @author hmlnarik
 */
public class LoginBuilder implements Step {

    private final SamlClientBuilder clientBuilder;
    private static final Pattern RESTART_URL_PATTERN = Pattern.compile("startSessionPolling\\(\\s*\"([^\"]+)\"");
    private UserRepresentation user;
    private boolean sso = false;
    private String idpAlias;

    public LoginBuilder(SamlClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    @Override
    public HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception {
        if (sso) {
            return null;    // skip this step
        } else {
            int statusCode = currentResponse.getStatusLine().getStatusCode();
            URI effectiveUri = currentURI;
            String loginPageText;

            if (statusCode == Response.Status.FOUND.getStatusCode()
                    && currentResponse.getFirstHeader("Location") != null) {
                String location = currentResponse.getFirstHeader("Location").getValue();
                effectiveUri = currentURI.resolve(location);
                HttpGet redirectGet = new HttpGet(effectiveUri);
                attachCookieHeader(redirectGet, context, effectiveUri);
                try (CloseableHttpResponse redirectResponse = client.execute(redirectGet, context)) {
                    statusCode = redirectResponse.getStatusLine().getStatusCode();
                    loginPageText = EntityUtils.toString(redirectResponse.getEntity(), StandardCharsets.UTF_8);
                }
            } else {
                loginPageText = EntityUtils.toString(currentResponse.getEntity(), StandardCharsets.UTF_8);
            }

            if (statusCode != Response.Status.OK.getStatusCode()) {
                HttpUriRequest retryAfterClearingRestartCookie = retryAfterClearingRestartCookie(client, context, effectiveUri);
                if (retryAfterClearingRestartCookie != null) {
                    return retryAfterClearingRestartCookie;
                }

                HttpUriRequest recoveredRequest = attemptCookieRestartRecovery(client, context, effectiveUri, loginPageText);
                if (recoveredRequest != null) {
                    return recoveredRequest;
                }

                String cookies = context.getCookieStore().getCookies().toString();
                Object lastAuthUri = context.getAttribute("kc.test.last.auth.request.uri");
                Object lastAuthCookie = context.getAttribute("kc.test.last.auth.cookie");
                throw new AssertionError("Unexpected status for login page. status=" + statusCode
                        + ", requestUri=" + effectiveUri
                        + ", redirects=" + context.getRedirectLocations()
                        + ", cookies=" + cookies
                        + ", lastAuthUri=" + lastAuthUri
                        + ", lastAuthCookie=" + lastAuthCookie
                        + ", body=" + loginPageText);
            }
            assertThat(loginPageText, containsString("login"));

            HttpUriRequest request = handleLoginPage(loginPageText, effectiveUri);
            attachCookieHeader(request, context, effectiveUri);
            return request;
        }
    }

    private HttpUriRequest retryAfterClearingRestartCookie(CloseableHttpClient client, HttpClientContext context, URI currentURI) throws Exception {
        if (currentURI == null || !currentURI.toString().contains("/login-actions/authenticate")) {
            return null;
        }

        List<Cookie> currentCookies = new ArrayList<>(context.getCookieStore().getCookies());
        context.getCookieStore().clear();
        currentCookies.stream()
                .filter(cookie -> !"KC_RESTART".equals(cookie.getName()))
                .forEach(context.getCookieStore()::addCookie);

        HttpGet retryGet = new HttpGet(currentURI);
        attachCookieHeader(retryGet, context, currentURI);
        try (CloseableHttpResponse retryResponse = client.execute(retryGet, context)) {
            if (retryResponse.getStatusLine().getStatusCode() != Response.Status.OK.getStatusCode()) {
                return null;
            }

            String loginPageText = EntityUtils.toString(retryResponse.getEntity(), StandardCharsets.UTF_8);
            if (!loginPageText.toLowerCase().contains("login")) {
                return null;
            }
            return handleLoginPage(loginPageText, currentURI);
        }
    }

    private HttpUriRequest attemptCookieRestartRecovery(CloseableHttpClient client, HttpClientContext context, URI currentURI, String responseBody) throws Exception {
        Matcher matcher = RESTART_URL_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            return null;
        }

        URI restartUri = currentURI.resolve(matcher.group(1));
        HttpGet restartGet = new HttpGet(restartUri);
        attachCookieHeader(restartGet, context, restartUri);
        try (CloseableHttpResponse restartResponse = client.execute(restartGet, context)) {
            int restartStatus = restartResponse.getStatusLine().getStatusCode();
            if (restartStatus != Response.Status.OK.getStatusCode()) {
                return null;
            }

            String loginPageText = EntityUtils.toString(restartResponse.getEntity(), StandardCharsets.UTF_8);
            if (!loginPageText.toLowerCase().contains("login")) {
                return null;
            }
            return handleLoginPage(loginPageText, restartUri);
        }
    }

    private void attachCookieHeader(HttpUriRequest request, HttpClientContext context, URI baseUri) {
        if (request == null || context == null || context.getCookieStore() == null) {
            return;
        }

        URI requestUri = request.getURI();
        if (requestUri != null && !requestUri.isAbsolute() && baseUri != null) {
            requestUri = baseUri.resolve(requestUri);
        }

        String path = requestUri != null && requestUri.getPath() != null ? requestUri.getPath() : "/";
        Date now = new Date();
        String cookieHeader = context.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getExpiryDate() == null || cookie.getExpiryDate().after(now))
                .filter(cookie -> {
                    String cookiePath = cookie.getPath() == null ? "/" : cookie.getPath();
                    return path.startsWith(cookiePath);
                })
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .collect(Collectors.joining("; "));

        if (!cookieHeader.isEmpty()) {
            request.setHeader("Cookie", cookieHeader);
        }
    }

    public SamlClientBuilder build() {
        return this.clientBuilder;
    }

    public LoginBuilder user(UserRepresentation user) {
        this.user = user;
        return this;
    }

    public LoginBuilder user(String userName, String password) {
        this.user = new UserRepresentation();
        this.user.setUsername(userName);
        Users.setPasswordFor(user, password);
        return this;
    }

    public LoginBuilder sso(boolean sso) {
        this.sso = sso;
        return this;
    }

    /**
     * When the step is executed and {@code idpAlias} is not {@code null}, it attempts to find and follow the link to
     * identity provider with the given alias.
     * @param idpAlias
     * @return
     */
    public LoginBuilder idp(String idpAlias) {
        this.idpAlias = idpAlias;
        return this;
    }

    /**
     * Prepares a GET/POST request for logging the given user into the given login page. The login page is expected
     * to have at least input fields with id "username" and "password".
     *
     * @param user
     * @param loginPage
     * @return
     */
    private HttpUriRequest handleLoginPage(String loginPage, URI currentURI) {
        if (idpAlias != null) {
            org.jsoup.nodes.Document theLoginPage = Jsoup.parse(loginPage);
            Element socialLink = theLoginPage.getElementById("social-" + this.idpAlias);
            assertThat("Unknown idp: " + this.idpAlias, socialLink, Matchers.notNullValue());
            final String link = socialLink.attr("href");
            assertThat("Invalid idp link: " + this.idpAlias, link, Matchers.notNullValue());
            return new HttpGet(currentURI.resolve(link));
        }

        return handleLoginPage(user, loginPage);
    }

    public static HttpUriRequest handleLoginPage(UserRepresentation user, String loginPage) {
        String username = user.getUsername();
        String password = getPasswordOf(user);
        org.jsoup.nodes.Document theLoginPage = Jsoup.parse(loginPage);

        List<NameValuePair> parameters = new LinkedList<>();
        for (Element form : theLoginPage.getElementsByTag("form")) {
            String method = form.attr("method");
            String action = form.attr("action");
            boolean isPost = method != null && "post".equalsIgnoreCase(method);

            for (Element input : form.getElementsByTag("input")) {
                if (Objects.equals(input.id(), "username")) {
                    parameters.add(new BasicNameValuePair(input.attr("name"), username));
                } else if (Objects.equals(input.id(), "password")) {
                    parameters.add(new BasicNameValuePair(input.attr("name"), password));
                } else {
                    parameters.add(new BasicNameValuePair(input.attr("name"), input.val()));
                }
            }

            if (isPost) {
                HttpPost res = new HttpPost(action);

                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
                res.setEntity(formEntity);

                return res;
            } else {
                UriBuilder b = UriBuilder.fromPath(action);
                for (NameValuePair parameter : parameters) {
                    b.queryParam(parameter.getName(), parameter.getValue());
                }
                return new HttpGet(b.build());
            }
        }

        throw new IllegalArgumentException("Invalid login form: " + loginPage);
    }

}
