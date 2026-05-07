package org.keycloak.tests.oid4vc.presentation;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oid4vc.model.presentation.AuthorizationRequest;
import org.keycloak.protocol.oid4vc.model.presentation.DirectPostResponse;
import org.keycloak.protocol.oid4vc.presentation.OID4VPConstants;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.URLEncodedUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

public class OID4VPBasicWallet {

    private final SimpleHttp redirectlessHttp;
    private final OAuthClient oauth;
    private final LoginPage loginPage;
    private final ManagedWebDriver driver;

    public OID4VPBasicWallet(OAuthClient oauth, LoginPage loginPage, ManagedWebDriver driver) {
        this.redirectlessHttp = SimpleHttp.create(oauth.httpClient().get())
                .withRequestConfig(RequestConfig.custom().setRedirectsEnabled(false).build());
        this.oauth = oauth;
        this.loginPage = loginPage;
        this.driver = driver;
    }

    public WalletAuthorizationRequest browserAuthorizationRequest(String idpAlias) throws Exception {
        requireBrowser();
        oauth.openLoginForm();

        // HtmlUnit resolves this link correctly through getAttribute(), while getDomAttribute()
        // returns a non-routable value for this test setup.
        String brokerLoginUrl = loginPage.findSocialButton(idpAlias).getAttribute("href");

        try (SimpleHttpResponse brokerLoginResponse = doGet(brokerLoginUrl, browserCookies()).asResponse()) {
            int status = brokerLoginResponse.getStatus();
            if (status != 302 && status != 303) {
                throw new IllegalStateException("Unexpected broker login status: " + status);
            }

            String walletUrl = brokerLoginResponse.getFirstHeader(HttpHeaders.LOCATION);
            return new WalletAuthorizationRequest()
                    .setWalletUrl(walletUrl)
                    .setClientId(getQueryParam(walletUrl, "client_id"))
                    .setRequestUri(getQueryParam(walletUrl, "request_uri"));
        }
    }

    public AuthorizationRequest fetchAuthorizationRequest(WalletAuthorizationRequest walletRequest) throws Exception {
        try (SimpleHttpResponse requestObjectResponse = redirectlessHttp.doGet(walletRequest.getRequestUri()).asResponse()) {
            if (requestObjectResponse.getStatus() != 200) {
                throw new IllegalStateException("Unexpected request object status: " + requestObjectResponse.getStatus());
            }

            byte[] requestObjectContent = new JWSInput(requestObjectResponse.asString()).getContent();
            return JsonSerialization.readValue(requestObjectContent, AuthorizationRequest.class);
        }
    }

    public DirectPostResponse submitDirectPost(AuthorizationRequest authorizationRequest, String vpToken) throws Exception {
        try (SimpleHttpResponse response = submitDirectPostResponse(authorizationRequest, vpToken)) {
            return response.asJson(DirectPostResponse.class);
        }
    }

    public int submitDirectPostStatus(AuthorizationRequest authorizationRequest, String vpToken) throws Exception {
        try (SimpleHttpResponse response = submitDirectPostResponse(authorizationRequest, vpToken)) {
            return response.getStatus();
        }
    }

    private SimpleHttpResponse submitDirectPostResponse(AuthorizationRequest authorizationRequest, String vpToken) throws Exception {
        return redirectlessHttp.doPost(authorizationRequest.getResponseUri())
                .param(OID4VPConstants.STATE, authorizationRequest.getState())
                .param(OID4VPConstants.VP_TOKEN, vpToken)
                .asResponse();
    }

    public AuthorizationEndpointResponse continueInBrowser(DirectPostResponse directPostResponse) {
        requireBrowser();
        driver.open(directPostResponse.getRedirectUri());
        driver.waiting().until(d -> {
            String currentUrl = d.getCurrentUrl();
            return currentUrl.contains("/first-broker-login") || currentUrl.contains("code=") || currentUrl.contains("error=");
        });

        if (driver.getCurrentUrl().contains("/first-broker-login")) {
            completeFirstBrokerLogin();
        }

        return oauth.parseLoginResponse();
    }

    private SimpleHttpRequest doGet(String url, String cookieHeader) {
        SimpleHttpRequest request = redirectlessHttp.doGet(url);
        if (cookieHeader != null && !cookieHeader.isBlank()) {
            request.header(HttpHeaders.COOKIE, cookieHeader);
        }
        return request;
    }

    private String getQueryParam(String uri, String name) {
        return URLEncodedUtils.parse(URI.create(uri), StandardCharsets.UTF_8).stream()
                .filter(param -> name.equals(param.getName()))
                .map(param -> param.getValue())
                .findFirst()
                .orElse(null);
    }

    private String browserCookies() {
        return driver.cookies().getAll().stream()
                .map(this::toCookiePair)
                .collect(Collectors.joining("; "));
    }

    private void requireBrowser() {
        if (loginPage == null || driver == null) {
            throw new IllegalStateException("Browser-based OID4VP flow requires LoginPage and ManagedWebDriver");
        }
    }

    private String toCookiePair(Cookie cookie) {
        return cookie.getName() + "=" + cookie.getValue();
    }

    private void completeFirstBrokerLogin() {
        String uniqueUsername = "oid4vp-" + System.currentTimeMillis();

        fillIfPresent("username", uniqueUsername);
        fillIfPresent("email", uniqueUsername + "@example.org");
        fillIfPresent("firstName", "OID4VP");
        fillIfPresent("lastName", "User");

        WebElement submit = driver.findElement(By.cssSelector("input[type='submit'], button[type='submit']"));
        submit.click();
    }

    private void fillIfPresent(String field, String value) {
        try {
            WebElement input = driver.findElement(By.id(field));
            if (input.getDomProperty("value") == null || input.getDomProperty("value").isBlank()) {
                input.sendKeys(value);
            }
        } catch (NoSuchElementException ignored) {
        }
    }

    public static class WalletAuthorizationRequest {

        private String walletUrl;
        private String clientId;
        private String requestUri;

        public String getWalletUrl() {
            return walletUrl;
        }

        public WalletAuthorizationRequest setWalletUrl(String walletUrl) {
            this.walletUrl = walletUrl;
            return this;
        }

        public String getClientId() {
            return clientId;
        }

        public WalletAuthorizationRequest setClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public String getRequestUri() {
            return requestUri;
        }

        public WalletAuthorizationRequest setRequestUri(String requestUri) {
            this.requestUri = requestUri;
            return this;
        }
    }
}
