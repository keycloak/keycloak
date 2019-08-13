/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.account;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.keycloak.common.Profile.Feature.ACCOUNT_API;
import static org.keycloak.common.Profile.Feature.DEVICE_ACTIVITY;
import static org.keycloak.testsuite.ProfileAssume.assumeFeatureEnabled;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.client.methods.HttpPost;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.representations.account.ClientRepresentation;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.representations.idm.DeviceRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.TokenUtil;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SessionRestServiceTest extends AbstractRestServiceTest {

    private static void setIPhoneUserAgent(HttpPost httpPost) {
        httpPost.addHeader("User-Agent",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 5_1_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B206 Safari/7534.48.3");
    }

    private static void setIPadUserAgent(HttpPost httpPost) {
        httpPost.addHeader("User-Agent",
                "Mozilla/5.0 (iPad; CPU iPad OS 5_1_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B206 Safari/7534.48.3");
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);

        testRealm.getClients().add(ClientBuilder.create()
                .clientId("public-client-0")
                .name("Public Client 0")
                .baseUrl("http://client0.example.com")
                .redirectUris(OAuthClient.APP_ROOT + "/auth")
                .publicClient().build());

        testRealm.getClients().add(ClientBuilder.create()
                .clientId("mobile-client-0")
                .name("Mobile Client 0")
                .secret("secret")
                .directAccessGrants().build());

        testRealm.getClients().add(ClientBuilder.create()
                .clientId("mobile-client-1")
                .name("Mobile Client 1")
                .secret("secret")
                .directAccessGrants().build());

        testRealm.getClients().add(ClientBuilder.create()
                .clientId("mobile-client-2")
                .name("Mobile Client 1")
                .secret("secret")
                .directAccessGrants().build());
    }

    @Test
    public void testProfilePreviewPermissions() throws IOException {
        assumeFeatureEnabled(ACCOUNT_API);

        TokenUtil noaccessToken = new TokenUtil("no-account-access", "password");
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");

        // Read sessions with no access
        assertEquals(403, SimpleHttp.doGet(getAccountUrl("sessions"), httpClient).header("Accept", "application/json")
                .auth(noaccessToken.getToken()).asStatus());

        // Delete all sessions with no access
        assertEquals(403, SimpleHttp.doDelete(getAccountUrl("sessions"), httpClient).header("Accept", "application/json")
                .auth(noaccessToken.getToken()).asStatus());

        // Delete all sessions with read only
        assertEquals(403, SimpleHttp.doDelete(getAccountUrl("sessions"), httpClient).header("Accept", "application/json")
                .auth(viewToken.getToken()).asStatus());

        // Delete single session with no access
        assertEquals(403,
                SimpleHttp.doDelete(getAccountUrl("sessions/bogusId"), httpClient).header("Accept", "application/json")
                        .auth(noaccessToken.getToken()).asStatus());

        // Delete single session with read only
        assertEquals(403,
                SimpleHttp.doDelete(getAccountUrl("sessions/bogusId"), httpClient).header("Accept", "application/json")
                        .auth(viewToken.getToken()).asStatus());
    }

    @Before
    @Override
    public void before() {
        super.before();
        try {
            Response response = testingClient.testing().enableFeature(DEVICE_ACTIVITY.toString());
            assertEquals(200, response.getStatus());
        } catch (Exception e) {
            throw e;
        }
        oauth.setBrowserHeader("User-Agent",
                "Mozilla/5.0 (X11; CentOS; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.79 Safari/537.36");
    }

    @Test
    public void testGetSessions() throws Exception {
        assumeFeatureEnabled(ACCOUNT_API);

        String sessionOne = codeGrant("public-client-0").getAccessToken();

        List<SessionRepresentation> sessions = getActiveSessions(sessionOne);

        assertEquals(2, sessions.size());

        for (SessionRepresentation session : sessions) {
            assertThat(session.getStarted(), Matchers.greaterThan(0));
            assertThat(session.getLastAccess(), Matchers.greaterThan(0));
            assertThat(session.getExpires(), Matchers.greaterThan(0));
            assertEquals(1, session.getClients().size());
            assertThat(session.getClients(),
                    hasItems(hasProperty("clientId", isOneOf("direct-grant", "public-client-0", "mobile-client-0"))));

            ClientRepresentation client = session.getClients().get(0);

            DeviceRepresentation deviceInfo = session.getDevice();

            if ("direct-grant".equalsIgnoreCase(client.getClientId())) {
                assertNull(deviceInfo);
            } else if ("public-client-0".equalsIgnoreCase(client.getClientId())) {
                assertNotNull(deviceInfo);
                assertNotNull(deviceInfo.getIp());
                assertEquals("Other", deviceInfo.getDevice());
                assertEquals("Chrome/61.0.3163", deviceInfo.getBrowser());
                assertEquals("CentOS", deviceInfo.getOs());
                assertTrue(deviceInfo.getCurrent());
            } else if ("mobile-client-0".equalsIgnoreCase(client.getClientId())) {
                assertNotNull(deviceInfo);
                assertNotNull(deviceInfo.getIp());
                assertEquals("iPhone", deviceInfo.getDevice());
                assertEquals("Mobile Safari/5.1", deviceInfo.getBrowser());
                assertEquals("iOS", deviceInfo.getOs());
                assertEquals("5.1.1", deviceInfo.getOsVersion());
                assertFalse(deviceInfo.getCurrent());
            }
        }
    }

    @Ignore("Only works when running on Undertow. See org.keycloak.testsuite.TestKeycloakSessionServletFilter")
    @Test
    public void testGetDeviceHistory() throws Exception {
        assumeFeatureEnabled(ACCOUNT_API);

        oauth.setBrowserHeader("User-Agent", "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:15.0) Gecko/20100101 Firefox/15.0.1");
        codeGrant("public-client-0");
        List<DeviceRepresentation> devices = getDeviceHistory().stream()
                .filter(deviceRepresentation -> "Fedora".equals(deviceRepresentation.getOs())).collect(Collectors.toList());
        assertEquals(1, devices.size());
        devices.stream().forEach(device -> {
            assertEquals("127.0.0.1", device.getIp());
            assertEquals("Firefox/15.0.1", device.getBrowser());
        });

        // delete all cookies so we can simulate authenticating from anoter device
        deleteAllCookiesForRealm(testRealm().toRepresentation().getRealm());

        oauth.setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36 Edge/12.0");
        codeGrant("public-client-0", true);

        oauth.setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Gecko/20100101 Firefox/15.0.1");
        codeGrant("public-client-0", true);

        devices = getDeviceHistory().stream()
                .filter(deviceRepresentation -> "Fedora".equals(deviceRepresentation.getOs())).collect(Collectors.toList());
        assertEquals(1, devices.size());
        devices.stream().forEach(device -> {
            assertEquals("127.0.0.1", device.getIp());
            // another browser was added to fedora because a request was sent to sign out using the same device
            assertEquals("Firefox/15.0.1", device.getBrowser());
        });

        devices = getDeviceHistory().stream()
                .filter(deviceRepresentation -> "Windows".equals(deviceRepresentation.getOs())).collect(Collectors.toList());
        assertEquals(1, devices.size());
        devices.stream().forEach(device -> {
            assertEquals("127.0.0.1", device.getIp());
            // windows was added although the user is accessing from the same IP, but different OS
            assertEquals("Edge/12.0,Firefox/15.0.1", device.getBrowser());
        });

        oauth.setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36 Edge/12.0");
        oauth.setBrowserHeader("X-Forwarded-For", "192.168.10.3");
        codeGrant("public-client-0", true);
        devices = getDeviceHistory().stream()
                .filter(device -> "Windows".equals(device.getOs())).collect(Collectors.toList());
        assertEquals(1, devices.size());
        devices.stream().forEach(device -> {
            // same windows device although last request was from a different IP. Reason is that the user signed out using the last IP, so the device was just updated
            // because the session was the same when signing out with a different IP
            assertEquals("192.168.10.3", device.getIp());
            assertEquals("Edge/12.0,Firefox/15.0.1", device.getBrowser());
        });

        passwordGrant("mobile-client-0", SessionRestServiceTest::setIPhoneUserAgent);
        devices = getDeviceHistory().stream()
                .filter(device -> "iPhone".equals(device.getDevice())).collect(Collectors.toList());
        assertEquals(1, devices.size());
        devices.stream().forEach(device -> {
            assertEquals("127.0.0.1", device.getIp());
            assertEquals("Mobile Safari/5.1", device.getBrowser());
        });

        passwordGrant("mobile-client-1", SessionRestServiceTest::setIPadUserAgent);
        passwordGrant("mobile-client-2", SessionRestServiceTest::setIPadUserAgent);
        passwordGrant("mobile-client-2", SessionRestServiceTest::setIPadUserAgent);
        devices = getDeviceHistory().stream()
                .filter(device -> "iPad".equals(device.getDevice())).collect(Collectors.toList());
        // only a single iPad device given that the request is from the same IP and user agent info
        assertEquals(1, devices.size());
        devices.stream().forEach(device -> {
            assertEquals("127.0.0.1", device.getIp());
            assertEquals("Mobile Safari/5.1", device.getBrowser());
        });

        // delete all cookies so we can simulate authenticating from anoter device
        deleteAllCookiesForRealm(testRealm().toRepresentation().getRealm());

        oauth.setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36 Edge/12.0");
        oauth.openLogout();
        oauth.setBrowserHeader("X-Forwarded-For", "192.168.10.4");
        codeGrant("public-client-0", false);
        devices = getDeviceHistory().stream()
                .filter(device -> "Windows".equals(device.getOs())).collect(Collectors.toList());
        // now we should have two windows devices because the request came from a different IP from a unknown device (cookies deleted)
        assertEquals(2, devices.size());
        assertThat(devices, hasItems(hasProperty("ip", isOneOf("127.0.0.1", "192.168.10.4"))));

        deleteAllCookiesForRealm(testRealm().toRepresentation().getRealm());

        oauth.setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows 7.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36 Edge/12.0");
        oauth.setBrowserHeader("X-Forwarded-For", "192.168.10.4");
        codeGrant("public-client-0", true);
        devices = getDeviceHistory().stream()
                .filter(device -> "Windows".equals(device.getOs())).collect(Collectors.toList());
        // now we should have three windows devices because the last request is from a different OS version
        assertEquals(3, devices.size());
        assertThat(devices, hasItems(hasProperty("osVersion", isOneOf("10.0", "7"))));

        oauth.setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows 8.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36 Edge/12.0");
        oauth.setBrowserHeader("X-Forwarded-For", "192.168.10.4");
        codeGrant("public-client-0", true);
        devices = getDeviceHistory().stream()
                .filter(device -> "Windows".equals(device.getOs())).collect(Collectors.toList());
        // now we should have four windows devices because the last request is from a different OS
        assertEquals(4, devices.size());
        assertThat(devices, hasItems(hasProperty("osVersion", isOneOf("10.0", "7", "8"))));
    }

    private List<DeviceRepresentation> getDeviceHistory() throws IOException {
        return SimpleHttp
                .doGet(getAccountUrl("sessions/devices"), httpClient).auth(tokenUtil.getToken())
                .asJson(new TypeReference<List<DeviceRepresentation>>() {
                });
    }

    @Test
    public void testDeleteSession() throws IOException {
        assumeFeatureEnabled(ACCOUNT_API);

        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        String sessionId = oauth.doLogin("view-account-access", "password").getSessionState();
        List<SessionRepresentation> sessions = getActiveSessions(viewToken.getToken());
        assertEquals(2, sessions.size());

        // With `ViewToken` you can only read
        int status = SimpleHttp.doDelete(getAccountUrl("sessions/" + sessionId), httpClient).acceptJson()
                .auth(viewToken.getToken()).asStatus();
        assertEquals(403, status);
        sessions = getActiveSessions(viewToken.getToken());
        assertEquals(2, sessions.size());

        // Here you can delete the session
        status = SimpleHttp.doDelete(getAccountUrl("sessions/" + sessionId), httpClient).acceptJson().auth(tokenUtil.getToken())
                .asStatus();
        assertEquals(200, status);
        sessions = getActiveSessions(tokenUtil.getToken());
        assertEquals(1, sessions.size());
    }

    private List<SessionRepresentation> getActiveSessions(String sessionOne) throws IOException {
        return SimpleHttp
                .doGet(getAccountUrl("sessions"), httpClient).auth(sessionOne)
                .asJson(new TypeReference<List<SessionRepresentation>>() {
                });
    }

    private OAuthClient.AccessTokenResponse passwordGrant(String clientId) throws Exception {
        return passwordGrant(clientId, SessionRestServiceTest::setIPhoneUserAgent);
    }

    private OAuthClient.AccessTokenResponse passwordGrant(String clientId, Consumer<HttpPost> userAgent) throws Exception {
        oauth.clientId(clientId);
        return oauth.doBeforeGrantAccessTokenRequest("secret", "test-user@localhost", "password", userAgent);
    }

    private OAuthClient.AccessTokenResponse codeGrant(String clientId) {
        return codeGrant(clientId, false);
    }

    private OAuthClient.AccessTokenResponse codeGrant(String clientId, boolean logout) {
        oauth.clientId(clientId);
        oauth.redirectUri(OAuthClient.APP_ROOT + "/auth");
        if (logout) {
            oauth.openLogout();
        }
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        return oauth.doAccessTokenRequest(code, "password");
    }
}
