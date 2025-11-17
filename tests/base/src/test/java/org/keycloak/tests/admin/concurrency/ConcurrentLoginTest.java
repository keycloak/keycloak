/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.concurrency;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Retry;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
@KeycloakIntegrationTest
public class ConcurrentLoginTest extends AbstractConcurrencyTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectUser(ref = "one", config = ConcurrentUserConfigOne.class)
    ManagedUser user1;

    @InjectUser(ref = "two", config = ConcurrentUserConfigTwo.class)
    ManagedUser user2;

    @InjectUser(ref = "three", config = ConcurrentUserConfigThree.class)
    ManagedUser user3;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectWebDriver
    WebDriver driver;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    private static final Logger LOGGER = Logger.getLogger(ConcurrentLoginTest.class);

    protected static final int DEFAULT_THREADS = 4;
    protected static final int CLIENTS_PER_THREAD = 30;
    protected static final int DEFAULT_CLIENTS_COUNT = CLIENTS_PER_THREAD * DEFAULT_THREADS;

    @BeforeEach
    public void beforeTest() {
        createClients();
    }

    protected void createClients() {
        final ClientsResource clients = managedRealm.admin().clients();
        for (int i = 0; i < DEFAULT_CLIENTS_COUNT; i++) {
            ClientRepresentation client = ClientConfigBuilder.create()
              .clientId("client" + i)
              .directAccessGrantsEnabled(true)
              .redirectUris("*")
              .webOrigins("*")
              .attribute(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+")
              .secret("password")
              .build();

            Response create = clients.create(client);
            String clientId = ApiUtil.getCreatedId(create);
            create.close();
            LOGGER.debugf("created %s [uuid=%s]", client.getClientId(), clientId);
        }
        LOGGER.debug("clients created");
    }

    @Test
    public void concurrentLoginSingleUser() throws Throwable {
        LOGGER.info("*********************************************");
        long start = System.currentTimeMillis();

        AtomicReference<String> userSessionId = new AtomicReference<>();
        LoginTask loginTask = null;

        try (CloseableHttpClient httpClient = getHttpsAwareClient()) {
            loginTask = new LoginTask(httpClient, userSessionId, 100, 1, false, List.of(
                getHttpClientCookiesForUser(httpClient, user1.getUsername(), user1.getPassword())
            ));
            run(DEFAULT_THREADS, DEFAULT_CLIENTS_COUNT, loginTask);
            int clientSessionsCount = runOnServer.fetch(
                    f -> f.sessions().getUserSession(f.realms().getRealm(REALM_NAME), userSessionId.get())
                            .getAuthenticatedClientSessions().size(), Integer.class);
            Assertions.assertEquals(1 + DEFAULT_CLIENTS_COUNT, clientSessionsCount);
        } finally {
            long end = System.currentTimeMillis() - start;
            LOGGER.infof("Statistics: %s", loginTask == null ? "??" : loginTask.getHistogram());
            LOGGER.info("concurrentLoginSingleUser took " + (end/1000) + "s");
            LOGGER.info("*********************************************");
        }
    }

    @Test
    public void concurrentLoginSingleUserSingleClientRehash() throws Throwable {
        LOGGER.info("*********************************************");
        final RealmRepresentation realmRep = managedRealm.admin().toRepresentation();

        try {
            realmRep.setPasswordPolicy("hashAlgorithm(pbkdf2-sha256)");
            managedRealm.admin().update(realmRep);
            // change the password of the test user to the same to force re-hashing
            CredentialRepresentation rep = new CredentialRepresentation();
            rep.setTemporary(Boolean.FALSE);
            rep.setValue("password");
            rep.setType(CredentialRepresentation.PASSWORD);
            AdminApiUtil.findUserByUsernameId(managedRealm.admin(), user1.getUsername()).resetPassword(rep);
        } finally {
            realmRep.setPasswordPolicy("");
            managedRealm.admin().update(realmRep);
        }

        // execute the login to re-hash in parallel
        run(2, 10, (KeycloakRunnable) (int threadIndex, Keycloak keycloak, RealmResource realm) -> {
            try (CloseableHttpClient httpClient = getHttpsAwareClient()) {
                getHttpClientCookiesForUser(httpClient, user1.getUsername(), user1.getPassword());
            }
        });
    }

    @Test
    public void concurrentLoginSingleUserSingleClient() throws Throwable {
        LOGGER.info("*********************************************");
        long start = System.currentTimeMillis();

        AtomicReference<String> userSessionId = new AtomicReference<>();
        LoginTask loginTask = null;

        try (CloseableHttpClient httpClient = getHttpsAwareClient()) {
            loginTask = new LoginTask(httpClient, userSessionId, 100, 1, true, List.of(
                getHttpClientCookiesForUser(httpClient, user1.getUsername(), user1.getPassword())
            ));
            run(DEFAULT_THREADS, DEFAULT_CLIENTS_COUNT, loginTask);
            int clientSessionsCount = runOnServer.fetch(f -> f.sessions().getUserSession(f.realms().getRealm(REALM_NAME), userSessionId.get())
                            .getAuthenticatedClientSessions().size(), Integer.class);
            Assertions.assertEquals(2, clientSessionsCount);
        } finally {
            long end = System.currentTimeMillis() - start;
            LOGGER.infof("Statistics: %s", loginTask == null ? "??" : loginTask.getHistogram());
            LOGGER.info("concurrentLoginSingleUserSingleClient took " + (end/1000) + "s");
            LOGGER.info("*********************************************");
        }
    }

    @Test
    public void concurrentLoginMultipleUsers() throws Throwable {
        LOGGER.info("*********************************************");
        long start = System.currentTimeMillis();

        AtomicReference<String> userSessionId = new AtomicReference<>();
        LoginTask loginTask = null;

        try (CloseableHttpClient httpClient = getHttpsAwareClient()) {
            loginTask = new LoginTask(httpClient, userSessionId, 100, 1, false, List.of(
                getHttpClientCookiesForUser(httpClient, user1.getUsername(), user1.getPassword()),
                getHttpClientCookiesForUser(httpClient, user2.getUsername(), user2.getPassword()),
                getHttpClientCookiesForUser(httpClient, user3.getUsername(), user3.getPassword())
            ));

            run(DEFAULT_THREADS, DEFAULT_CLIENTS_COUNT, loginTask);
            int clientSessionsCount = runOnServer.fetch(f -> f.sessions().getUserSession(f.realms().getRealm(REALM_NAME), userSessionId.get())
                    .getAuthenticatedClientSessions().size(), Integer.class);
            Assertions.assertEquals(1 + DEFAULT_CLIENTS_COUNT / 3 + (DEFAULT_CLIENTS_COUNT % 3 <= 0 ? 0 : 1), clientSessionsCount);
        } finally {
            long end = System.currentTimeMillis() - start;
            LOGGER.infof("Statistics: %s", loginTask == null ? "??" : loginTask.getHistogram());
            LOGGER.info("concurrentLoginMultipleUsers took " + (end/1000) + "s");
            LOGGER.info("*********************************************");
        }
    }


    @Test
    public void concurrentCodeReuseShouldFail() throws Throwable {
        LOGGER.info("*********************************************");
        long start = System.currentTimeMillis();


        for (int i=0 ; i<10 ; i++) {
            OAuthClient oauth1 = new OAuthClient(keycloakUrls.getBase(), HttpClientBuilder.create().build(), driver);
            oauth1.config()
                    .realm(managedRealm.getName())
                    .redirectUri(oauth.getRedirectUri())
                    .postLogoutRedirectUri(oauth.config().getPostLogoutRedirectUri())
                    .responseType(OAuth2Constants.CODE)
                    .client("client0", "password");

            AuthorizationEndpointResponse resp = oauth1.doLogin(user1.getUsername(), user1.getPassword());
            String code = resp.getCode();
            Assertions.assertNotNull(code);
            String codeURL = driver.getCurrentUrl();


            AtomicInteger codeToTokenSuccessCount = new AtomicInteger(0);
            AtomicInteger codeToTokenErrorsCount = new AtomicInteger(0);

            KeycloakRunnable codeToTokenTask = new KeycloakRunnable() {

                @Override
                public void run(int threadIndex, Keycloak keycloak, RealmResource realm) throws Throwable {
                    LOGGER.infof("Trying to execute codeURL: %s, threadIndex: %d", codeURL, threadIndex);

                    AccessTokenResponse resp = oauth1.doAccessTokenRequest(code);
                    if (resp.getAccessToken() != null && resp.getError() == null) {
                        codeToTokenSuccessCount.incrementAndGet();
                    } else if (resp.getAccessToken() == null && resp.getError() != null) {
                        codeToTokenErrorsCount.incrementAndGet();
                    }
                }

            };

            run(DEFAULT_THREADS, DEFAULT_THREADS, codeToTokenTask);

            // Logout user
            AdminApiUtil.findUserByUsernameId(managedRealm.admin(), user1.getUsername()).logout();

            // Code should be successfully exchanged for the token at max once. In some cases (EG. Cross-DC) it may not be even successfully exchanged
            assertThat(codeToTokenSuccessCount.get(), Matchers.equalTo(1));
            assertThat(codeToTokenErrorsCount.get(), Matchers.greaterThanOrEqualTo(DEFAULT_THREADS - 1));

            LOGGER.infof("Iteration %d passed successfully", i);
        }

        long end = System.currentTimeMillis() - start;
        LOGGER.info("concurrentCodeReuseShouldFail took " + (end/1000) + "s");
        LOGGER.info("*********************************************");

    }

    private CloseableHttpClient getHttpsAwareClient() {
        return HttpClientBuilder.create()
                .disableRedirectHandling()
                .build();
    }

    private String getHttpClientCookiesForUser(final CloseableHttpClient httpClient, String userName, String password) throws IOException {
        HttpResponse formResponse = getPageContent(oauth.loginForm().build(), null, httpClient);

        HttpUriRequest request = handleLogin(EntityUtils.toString(formResponse.getEntity()), formResponse.getAllHeaders(), userName, password);

        HttpResponse loginResponse = httpClient.execute(request);
        Assertions.assertEquals(302, loginResponse.getStatusLine().getStatusCode());
        Assertions.assertTrue(loginResponse.getFirstHeader("Location").getValue().contains("callback/oauth"));

        return parseCookies(loginResponse.getAllHeaders());
    }

    private HttpResponse getPageContent(String url, String cookies, CloseableHttpClient httpClient) throws IOException {
        HttpGet request = new HttpGet(url);

        request.setHeader("User-Agent", "Mozilla/5.0");
        request.setHeader("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setHeader("Accept-Language", "en-US,en;q=0.5");

        if (cookies != null) {
            request.setHeader("Cookie", cookies);
        }

        HttpResponse resp = httpClient.execute(request);

        int statusCode = resp.getStatusLine().getStatusCode();
        Assertions.assertTrue(statusCode == 302 || statusCode == 200, "Expected 302 or 200, got: " + statusCode);

        return resp;
    }

    private HttpUriRequest handleLogin(String html, Header[] headers, String username, String password) {
        LOGGER.debug("Extracting form's data...");

        // Keycloak form id
        List<NameValuePair> paramList = new ArrayList<>();
        paramList.add(new BasicNameValuePair("username", username));
        paramList.add(new BasicNameValuePair("password", password));

        String actionUrl = getActionUrl(html);

        HttpPost req = new HttpPost(actionUrl);

        // Hack to just take cookies directly from response and send in next request bypassing cookie store
        String cookies = parseCookies(headers);
        req.setHeader("Cookie", cookies);

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(paramList, StandardCharsets.UTF_8);
        req.setEntity(formEntity);

        return req;
    }

    private String getActionUrl(String html) {
        Pattern pattern = Pattern.compile("action=\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(html);
        matcher.find();
        String action = matcher.group(1);
        return action;
    }

    private String parseCookies(Header[] headers) {
        return Arrays.stream(headers).filter(h -> h.getName().equals("Set-Cookie"))
                .map(h -> h.getValue().split(";")[0]).collect(Collectors.joining("; "));
    }

    private static Map<String, String> getQueryFromUrl(String url) throws URISyntaxException {
        return URLEncodedUtils.parse(new URI(url), StandardCharsets.UTF_8).stream()
                .collect(Collectors.toMap(p -> p.getName(), p -> p.getValue()));
    }

    public class LoginTask implements KeycloakRunnable {

        private final AtomicInteger clientIndex = new AtomicInteger();
        private final ThreadLocal<OAuthClient> oauthClient = new ThreadLocal<OAuthClient>() {
                @Override
                protected OAuthClient initialValue() {
                    OAuthClient oauth1 = new OAuthClient(keycloakUrls.getBase(), HttpClientBuilder.create().build(), driver);
                    oauth1.config().realm(managedRealm.getName());
                    // Add some randomness to state, nonce and redirectUri. Verify that login is successful and "state" and "nonce" will match
                    oauth1.redirectUri(oauth.getRedirectUri() + "?some=" + new Random().nextInt(1024));
                    return oauth1;
                }
            };

        private final CloseableHttpClient httpClient;
        private final AtomicReference<String> userSessionId;

        private final int retryDelayMs;
        private final int retryCount;
        private final AtomicInteger[] retryHistogram;
        private final AtomicInteger totalInvocations = new AtomicInteger();
        private final boolean sameClient;
        private final List<String> clientCookies;

        public LoginTask(CloseableHttpClient httpClient, AtomicReference<String> userSessionId, int retryDelayMs, int retryCount, boolean sameClient, List<String> clientCookies) {
            this.httpClient = httpClient;
            this.userSessionId = userSessionId;
            this.retryDelayMs = retryDelayMs;
            this.retryCount = retryCount;
            this.retryHistogram = new AtomicInteger[retryCount];
            for (int i = 0; i < retryHistogram.length; i ++) {
                retryHistogram[i] = new AtomicInteger();
            }
            this.sameClient = sameClient;
            this.clientCookies = clientCookies;
        }

        @Override
        public void run(int threadIndex, Keycloak keycloak, RealmResource realm) throws Throwable {
            int i = sameClient ? 0 : clientIndex.getAndIncrement();
            OAuthClient oauth1 = oauthClient.get();
            oauth1.client("client" + i, "password");
            LOGGER.infof("%d [%s]: Accessing login page for %s", threadIndex, Thread.currentThread().getName(), oauth1.getClientId());

            String requestState = KeycloakModelUtils.generateId();
            String requestNonce = KeycloakModelUtils.generateId();

            final String cookies = clientCookies.get(i % clientCookies.size());
            HttpResponse pageContent = getPageContent(oauth1.loginForm().nonce(requestNonce).state(requestState).build(), cookies, httpClient);

            String redirectUrl = pageContent.getFirstHeader("Location").getValue();
            Assertions.assertTrue(redirectUrl.contains("callback/oauth"), "Expected callback URL, got: " + redirectUrl);

            Map<String, String> query = getQueryFromUrl(redirectUrl);
            String code = query.get(OAuth2Constants.CODE);
            String state = query.get(OAuth2Constants.STATE);

            Assertions.assertEquals(requestState, state, "Invalid state.");

            AtomicReference<AccessTokenResponse> accessResRef = new AtomicReference<>();
            totalInvocations.incrementAndGet();

            // obtain access + refresh token via code-to-token flow
            AccessTokenResponse accessRes = oauth1.doAccessTokenRequest(code);
            Assertions.assertEquals(200, accessRes.getStatusCode(), "AccessTokenResponse: client: " + oauth1.getClientId() + ", error: '" + accessRes.getError() + "' desc: '" + accessRes.getErrorDescription() + "'");

            AccessToken token = JsonSerialization.readValue(new JWSInput(accessRes.getAccessToken()).getContent(), AccessToken.class);
            Assertions.assertNull(token.getNonce());

            AccessToken refreshedToken = JsonSerialization.readValue(new JWSInput(accessRes.getRefreshToken()).getContent(), AccessToken.class);
            Assertions.assertNull(refreshedToken.getNonce());

            AccessToken idToken = JsonSerialization.readValue(new JWSInput(accessRes.getIdToken()).getContent(), AccessToken.class);
            Assertions.assertEquals(requestNonce, idToken.getNonce());

            accessResRef.set(accessRes);

            // Refresh access + refresh token using refresh token
            AtomicReference<AccessTokenResponse> refreshResRef = new AtomicReference<>();

            int invocationIndex = Retry.execute(() -> {
                AccessTokenResponse refreshRes = oauth1.doRefreshTokenRequest(accessResRef.get().getRefreshToken());
                Assertions.assertEquals(200, refreshRes.getStatusCode(), "AccessTokenResponse: client: " + oauth1.getClientId() + ", error: '" + refreshRes.getError() + "' desc: '" + refreshRes.getErrorDescription() + "'");

                refreshResRef.set(refreshRes);
            }, retryCount, retryDelayMs);

            retryHistogram[invocationIndex].incrementAndGet();

            token = JsonSerialization.readValue(new JWSInput(accessResRef.get().getAccessToken()).getContent(), AccessToken.class);
            Assertions.assertNull(token.getNonce());

            refreshedToken = JsonSerialization.readValue(new JWSInput(refreshResRef.get().getRefreshToken()).getContent(), AccessToken.class);
            Assertions.assertNull(refreshedToken.getNonce());

            idToken = JsonSerialization.readValue(new JWSInput(refreshResRef.get().getIdToken()).getContent(), AccessToken.class);
            Assertions.assertNull(idToken.getNonce());

            if (userSessionId.get() == null) {
                userSessionId.set(token.getSessionState());
            }
        }

        public int getRetryDelayMs() {
            return retryDelayMs;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public Map<Integer, Integer> getHistogram() {
            Map<Integer, Integer> res = new LinkedHashMap<>(retryCount);
            for (int i = 0; i < retryHistogram.length; i ++) {
                AtomicInteger item = retryHistogram[i];

                res.put(i * retryDelayMs, item.get());
            }
            return res;
        }
    }

    private static class ConcurrentUserConfigOne implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder config) {
            return config.username("username1").password("password").emailVerified(true).name("User 1", "Name").email("user1@name.com");
        }
    }

    private static class ConcurrentUserConfigTwo implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder config) {
            return config.username("username2").password("password").emailVerified(true).name("User 2", "Name").email("user2@name.com");
        }
    }

    private static class ConcurrentUserConfigThree implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder config) {
            return config.username("username3").password("password").emailVerified(true).name("User 3", "Name").email("user3@name.com");
        }
    }
}
