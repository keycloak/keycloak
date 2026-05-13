package org.keycloak.tests.oid4vc.presentation;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oid4vc.model.presentation.AuthorizationRequest;
import org.keycloak.protocol.oid4vc.model.presentation.DcqlQuery;
import org.keycloak.protocol.oid4vc.model.presentation.DirectPostResponse;
import org.keycloak.protocol.oid4vc.presentation.OID4VPConstants;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.URLEncodedUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import static org.keycloak.tests.oid4vc.OID4VCTestContext.AUTHORIZATION_REQUEST_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.DIRECT_POST_RESPONSE_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.WALLET_AUTHORIZATION_REQUEST_ATTACHMENT_KEY;

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

    public WalletAuthorizationRequest browserAuthorizationRequest(OID4VCTestContext ctx, String idpAlias) throws Exception {
        WalletAuthorizationRequest walletRequest = browserAuthorizationRequest(idpAlias);
        ctx.putAttachment(WALLET_AUTHORIZATION_REQUEST_ATTACHMENT_KEY, walletRequest);
        return walletRequest;
    }

    public AuthorizationRequest fetchAuthorizationRequest(OID4VCTestContext ctx) throws Exception {
        AuthorizationRequest authorizationRequest = fetchAuthorizationRequest(
                ctx.assertAttachment(WALLET_AUTHORIZATION_REQUEST_ATTACHMENT_KEY));
        ctx.putAttachment(AUTHORIZATION_REQUEST_ATTACHMENT_KEY, authorizationRequest);
        return authorizationRequest;
    }

    public DirectPostResponse submitDirectPost(OID4VCTestContext ctx, TestVpToken vpToken) throws Exception {
        try (SimpleHttpResponse response = submitDirectPostResponse(ctx.getAuthorizationRequest(), vpToken)) {
            int status = response.getStatus();
            String body = response.asString();
            if (status != 200) {
                throw new IllegalStateException("Unexpected direct_post status: " + status + ", body: " + body);
            }

            DirectPostResponse directPostResponse = JsonSerialization.readValue(body, DirectPostResponse.class);
            ctx.putAttachment(DIRECT_POST_RESPONSE_ATTACHMENT_KEY, directPostResponse);
            return directPostResponse;
        }
    }

    public int submitDirectPostStatus(OID4VCTestContext ctx, TestVpToken vpToken) throws Exception {
        try (SimpleHttpResponse response = submitDirectPostResponse(ctx.getAuthorizationRequest(), vpToken)) {
            return response.getStatus();
        }
    }

    private WalletAuthorizationRequest browserAuthorizationRequest(String idpAlias) throws Exception {
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
            Map<String, String> queryParams = getQueryParams(walletUrl);
            return new WalletAuthorizationRequest()
                    .setWalletUrl(walletUrl)
                    .setQueryParams(queryParams)
                    .setClientId(queryParams.get(OID4VPConstants.CLIENT_ID))
                    .setRequestUri(queryParams.get(OID4VPConstants.REQUEST_URI));
        }
    }

    private AuthorizationRequest fetchAuthorizationRequest(WalletAuthorizationRequest walletRequest) throws Exception {
        if (walletRequest.getRequestUri() == null) {
            return authorizationRequestFromQueryParameters(walletRequest);
        }

        try (SimpleHttpResponse requestObjectResponse = redirectlessHttp.doGet(walletRequest.getRequestUri()).asResponse()) {
            if (requestObjectResponse.getStatus() != 200) {
                throw new IllegalStateException("Unexpected request object status: " + requestObjectResponse.getStatus());
            }

            byte[] requestObjectContent = new JWSInput(requestObjectResponse.asString()).getContent();
            return JsonSerialization.readValue(requestObjectContent, AuthorizationRequest.class);
        }
    }

    private AuthorizationRequest authorizationRequestFromQueryParameters(WalletAuthorizationRequest walletRequest) throws Exception {
        Map<String, String> queryParams = walletRequest.getQueryParams();
        return new AuthorizationRequest()
                .setClientId(queryParams.get(OID4VPConstants.CLIENT_ID))
                .setResponseType(queryParams.get(OID4VPConstants.RESPONSE_TYPE))
                .setResponseMode(queryParams.get(OID4VPConstants.RESPONSE_MODE))
                .setResponseUri(queryParams.get(OID4VPConstants.RESPONSE_URI))
                .setState(queryParams.get(OID4VPConstants.STATE))
                .setNonce(queryParams.get(OID4VPConstants.NONCE))
                .setDcqlQuery(JsonSerialization.readValue(queryParams.get(OID4VPConstants.DCQL_QUERY), DcqlQuery.class))
                .setClientMetadata(JsonSerialization.readValue(
                        queryParams.get(OID4VPConstants.CLIENT_METADATA),
                        new TypeReference<Map<String, Object>>() {
                        }));
    }

    private SimpleHttpResponse submitDirectPostResponse(AuthorizationRequest authorizationRequest, TestVpToken vpToken) throws Exception {
        return redirectlessHttp.doPost(authorizationRequest.getResponseUri())
                .param(OID4VPConstants.STATE, authorizationRequest.getState())
                .param(OID4VPConstants.VP_TOKEN, vpToken.value())
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

    private Map<String, String> getQueryParams(String uri) {
        return URLEncodedUtils.parse(URI.create(uri), StandardCharsets.UTF_8).stream()
                .collect(Collectors.toMap(
                        param -> param.getName(),
                        param -> param.getValue()));
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
        private Map<String, String> queryParams = Map.of();

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

        public Map<String, String> getQueryParams() {
            return queryParams;
        }

        public WalletAuthorizationRequest setQueryParams(Map<String, String> queryParams) {
            this.queryParams = queryParams;
            return this;
        }
    }
}
