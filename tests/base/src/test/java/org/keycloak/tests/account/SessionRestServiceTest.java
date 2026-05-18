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
package org.keycloak.tests.account;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.representations.account.ClientRepresentation;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.BrowserType;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hamcrest.Matchers;
import org.htmlunit.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class SessionRestServiceTest extends AbstractRestServiceTest {

    private static final String APP_REDIRECT_URI = "http://localhost:8500/app/auth";

    @InjectWebDriver
    protected ManagedWebDriver driver;

    @InjectWebDriver(ref = "secondBrowser")
    protected ManagedWebDriver secondBrowser;

    @InjectWebDriver(ref = "thirdBrowser")
    protected ManagedWebDriver thirdBrowser;

    @BeforeEach
    public void setupExtraClients() {
        createPublicClientIfMissing("public-client-0", "Public Client 0", "http://client0.example.com");
        createPublicClientIfMissing("public-client-1", "Public Client 1", "http://client1.example.com");
        createConfidentialClientIfMissing("confidential-client-0", "Confidential Client 0");
        createConfidentialClientIfMissing("confidential-client-1", "Confidential Client 1");

        // OAuthClient is shared per-test; default to the primary driver so individual tests can swap.
        oauth.driver(driver.driver());
    }

    private void createPublicClientIfMissing(String clientId, String name, String baseUrl) {
        if (!managedRealm.admin().clients().findByClientId(clientId).isEmpty()) {
            return;
        }
        org.keycloak.representations.idm.ClientRepresentation rep = ClientBuilder.create()
                .clientId(clientId)
                .name(name)
                .baseUrl(baseUrl)
                .redirectUris(APP_REDIRECT_URI)
                .publicClient().build();
        try (jakarta.ws.rs.core.Response r = managedRealm.admin().clients().create(rep)) {
            String createdId = org.keycloak.testframework.util.ApiUtil.getCreatedId(r);
            managedRealm.cleanup().add(realm -> realm.clients().get(createdId).remove());
        }
    }

    private void createConfidentialClientIfMissing(String clientId, String name) {
        if (!managedRealm.admin().clients().findByClientId(clientId).isEmpty()) {
            return;
        }
        org.keycloak.representations.idm.ClientRepresentation rep = ClientBuilder.create()
                .clientId(clientId)
                .name(name)
                .secret("secret")
                .serviceAccountsEnabled()
                .directAccessGrantsEnabled()
                .redirectUris(APP_REDIRECT_URI).build();
        try (jakarta.ws.rs.core.Response r = managedRealm.admin().clients().create(rep)) {
            String createdId = org.keycloak.testframework.util.ApiUtil.getCreatedId(r);
            managedRealm.cleanup().add(realm -> realm.clients().get(createdId).remove());
        }
    }

    @Test
    public void testProfilePreviewPermissions() throws IOException {
        TokenUtil noaccessToken = new TokenUtil("no-account-access", "password");
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");

        // Read sessions with no access
        assertEquals(403, SimpleHttpDefault.doGet(getAccountUrl("sessions"), httpClient).header("Accept", "application/json")
                .auth(noaccessToken.getToken()).asStatus());

        // Delete all sessions with no access
        assertEquals(403, SimpleHttpDefault.doDelete(getAccountUrl("sessions"), httpClient).header("Accept", "application/json")
                .auth(noaccessToken.getToken()).asStatus());

        // Delete all sessions with read only
        assertEquals(403, SimpleHttpDefault.doDelete(getAccountUrl("sessions"), httpClient).header("Accept", "application/json")
                .auth(viewToken.getToken()).asStatus());

        // Delete single session with no access
        assertEquals(403,
                SimpleHttpDefault.doDelete(getAccountUrl("sessions/bogusId"), httpClient).header("Accept", "application/json")
                        .auth(noaccessToken.getToken()).asStatus());

        // Delete single session with read only
        assertEquals(403,
                SimpleHttpDefault.doDelete(getAccountUrl("sessions/bogusId"), httpClient).header("Accept", "application/json")
                        .auth(viewToken.getToken()).asStatus());
    }

    @Test
    public void testGetSessions() throws Exception {
        oauth.driver(secondBrowser.driver());
        codeGrant("public-client-0");

        List<SessionRepresentation> sessions = getSessions();
        assertEquals(2, sessions.size());

        for (SessionRepresentation session : sessions) {
            assertNotNull(session.getId());
            assertThat(session.getIpAddress(), anyOf(equalTo("127.0.0.1"), equalTo("0:0:0:0:0:0:0:1")));
            assertTrue(session.getLastAccess() > 0);
            assertTrue(session.getExpires() > 0);
            assertTrue(session.getStarted() > 0);
            assertThat(session.getClients(), Matchers.hasItem(Matchers.hasProperty("clientId",
                    anyOf(Matchers.is("direct-grant"), Matchers.is("public-client-0")))));
        }
    }

    @Test
    public void testGetDevicesResponse() throws Exception {
        assumeHtmlUnit();
        setBrowserHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0) Gecko/20100101 Firefox/15.0.1");
        AccessTokenResponse tokenResponse = codeGrant("public-client-0");
        joinSsoSession("public-client-1");

        List<DeviceRepresentation> devices = getDevicesOtherThanOther(tokenResponse.getAccessToken());

        assertEquals(1, devices.size(), "Should have a single device");

        DeviceRepresentation device = devices.get(0);

        assertTrue(device.getCurrent());
        assertEquals("Windows", device.getOs());
        assertEquals("10", device.getOsVersion());
        assertEquals("Other", device.getDevice());

        List<SessionRepresentation> sessions = device.getSessions();
        assertEquals(1, sessions.size());
        SessionRepresentation session = sessions.get(0);
        assertThat(session.getIpAddress(), anyOf(equalTo("127.0.0.1"), equalTo("0:0:0:0:0:0:0:1")));
        assertTrue(device.getLastAccess() == session.getLastAccess());

        List<ClientRepresentation> clients = session.getClients();
        assertEquals(2, clients.size());
        assertThat(session.getClients(), Matchers.hasItem(Matchers.hasProperty("clientId",
                anyOf(Matchers.is("public-client-0"), Matchers.is("public-client-1")))));
        assertThat(session.getClients(), Matchers.hasItem(Matchers.hasProperty("clientName",
                anyOf(Matchers.is("Public Client 0"), Matchers.is("Public Client 1")))));
    }

    @Test
    public void testGetDevicesSessions() throws Exception {
        assumeHtmlUnit();

        WebDriver firstBrowser = oauth.getDriver();

        // first browser authenticates from Fedora
        setBrowserHeader("User-Agent", "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:15.0) Gecko/20100101 Firefox/15.0.1");
        AccessTokenResponse tokenResponse1 = codeGrant("public-client-0");
        List<DeviceRepresentation> devices = getDevicesOtherThanOther();
        assertEquals(1, devices.size(), "Should have a single device");
        List<DeviceRepresentation> fedoraDevices = devices.stream()
                .filter(deviceRepresentation -> "Fedora".equals(deviceRepresentation.getOs())).collect(Collectors.toList());
        assertEquals(1, fedoraDevices.size(), "Should have a single Fedora device");
        fedoraDevices.stream().forEach(d -> {
            List<SessionRepresentation> sessions = d.getSessions();
            assertEquals(1, sessions.size());
            assertThat(sessions, Matchers.hasItem(Matchers.hasProperty("browser", Matchers.is("Firefox/15.0.1"))));
        });

        // second browser authenticates from Windows
        oauth.driver(secondBrowser.driver());
        setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Gecko/20100101 Firefox/15.0.1");
        AccessTokenResponse tokenResponse2 = codeGrant("public-client-0");
        devices = getDevicesOtherThanOther();
        assertEquals(2, devices.size(), "Should have two devices");
        fedoraDevices = devices.stream()
                .filter(deviceRepresentation -> "Fedora".equals(deviceRepresentation.getOs())).collect(Collectors.toList());
        assertEquals(1, fedoraDevices.size());
        List<DeviceRepresentation> windowsDevices = devices.stream()
                .filter(deviceRepresentation -> "Windows".equals(deviceRepresentation.getOs())).collect(Collectors.toList());
        assertEquals(1, windowsDevices.size());
        windowsDevices.stream().forEach(d -> {
            List<SessionRepresentation> sessions = d.getSessions();
            assertEquals(1, sessions.size());
            assertThat(sessions, Matchers.hasItem(Matchers.hasProperty("browser", Matchers.is("Firefox/15.0.1"))));
        });

        // first browser authenticates from Windows using Edge
        oauth.driver(firstBrowser);
        oauth.logoutForm().idTokenHint(tokenResponse1.getIdToken()).open();
        setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36 Edge/12.0");
        tokenResponse1 = codeGrant("public-client-0");

        // second browser authenticates from Windows using Firefox
        oauth.driver(secondBrowser.driver());
        oauth.logoutForm().idTokenHint(tokenResponse2.getIdToken()).open();
        setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Gecko/20100101 Firefox/15.0.1");
        tokenResponse2 = codeGrant("public-client-0");

        // third browser authenticates from Windows using Safari
        oauth.driver(thirdBrowser.driver());
        setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Version/11.0 Safari/603.1.30");
        setBrowserHeader("X-Forwarded-For", "192.168.10.3");
        AccessTokenResponse tokenResponse3 = codeGrant("public-client-0");
        devices = getDevicesOtherThanOther(tokenResponse3.getAccessToken());
        assertEquals(
                1, devices.size(), "Should have a single device because all browsers (and sessions) are from the same platform (OS + OS version)");
        windowsDevices = devices.stream()
                .filter(d -> "Windows".equals(d.getOs())).collect(Collectors.toList());
        assertEquals(1, windowsDevices.size());
        windowsDevices.stream().forEach(d -> {
            List<SessionRepresentation> sessions = d.getSessions();
            assertEquals(3, sessions.size());
            assertEquals(1, sessions.stream().filter(
                    rep -> rep.getIpAddress().equals("127.0.0.1") && rep.getBrowser().equals("Firefox/15.0.1")
                            && rep.getCurrent() == null).count());
            assertEquals(1, sessions.stream().filter(
                    rep -> rep.getIpAddress().equals("127.0.0.1") && rep.getBrowser().equals("Edge/12.0")
                            && rep.getCurrent() == null).count());
            assertEquals(1, sessions.stream().filter(
                    rep -> rep.getIpAddress().equals("192.168.10.3") && rep.getBrowser().equals("Safari/11.0") && rep
                            .getCurrent()).count());
        });

        // third browser authenticates from Windows using a different Windows version
        oauth.driver(thirdBrowser.driver());
        oauth.logoutForm().idTokenHint(tokenResponse3.getIdToken()).open();
        setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows 7) AppleWebKit/537.36 (KHTML, like Gecko) Version/11.0 Safari/603.1.30");
        setBrowserHeader("X-Forwarded-For", "192.168.10.3");
        tokenResponse3 = codeGrant("public-client-0");
        devices = getDevicesOtherThanOther();
        windowsDevices = devices.stream()
                .filter(d -> "Windows".equals(d.getOs())).collect(Collectors.toList());
        assertEquals(2, devices.size(), "Should have two devices for two distinct Windows versions");
        assertEquals(2, windowsDevices.size());

        oauth.driver(firstBrowser);
        oauth.logoutForm().idTokenHint(tokenResponse1.getIdToken()).open();
        setBrowserHeader("User-Agent",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 5_1_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B206 Safari/7534.48.3");
        tokenResponse1 = codeGrant("public-client-0");

        oauth.driver(secondBrowser.driver());
        oauth.logoutForm().idTokenHint(tokenResponse2.getIdToken()).open();
        setBrowserHeader("User-Agent",
                "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:15.0) Gecko/20100101 Firefox/15.0.1");
        tokenResponse2 = codeGrant("public-client-0");
        devices = getDevicesOtherThanOther();
        assertEquals(3, devices.size(), "Should have 3 devices");
        windowsDevices = devices.stream()
                .filter(d -> "Windows".equals(d.getOs())).collect(Collectors.toList());
        assertEquals(1, windowsDevices.size());
        fedoraDevices = devices.stream()
                .filter(deviceRepresentation -> "Fedora".equals(deviceRepresentation.getOs())).collect(Collectors.toList());
        assertEquals(1, fedoraDevices.size());
        List<DeviceRepresentation> iphoneDevices = devices.stream()
                .filter(d -> "iOS".equals(d.getOs()) && "iPhone".equals(d.getDevice()))
                .collect(Collectors.toList());
        assertEquals(1, iphoneDevices.size());
        iphoneDevices.stream().forEach(d -> {
            assertTrue(d.isMobile());
            List<SessionRepresentation> sessions = d.getSessions();
            assertEquals(1, sessions.size());
            assertEquals(1, sessions.stream().filter(
                    rep -> rep.getBrowser().equals("Mobile Safari/5.1")).count());
        });
    }

    @Test
    public void testLogout() throws IOException {
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        String sessionId = oauth.doLogin("view-account-access", "password").getSessionState();
        List<SessionRepresentation> sessions = getSessions(viewToken.getToken());
        assertEquals(2, sessions.size());

        // With `ViewToken` you can only read
        int status = SimpleHttpDefault.doDelete(getAccountUrl("sessions/" + sessionId), httpClient).acceptJson()
                .auth(viewToken.getToken()).asStatus();
        assertEquals(403, status);
        sessions = getSessions(viewToken.getToken());
        assertEquals(2, sessions.size());

        // Here you can delete the session
        status = SimpleHttpDefault.doDelete(getAccountUrl("sessions/" + sessionId), httpClient).acceptJson().auth(tokenUtil.getToken())
                .asStatus();
        assertEquals(204, status);
        sessions = getSessions(tokenUtil.getToken());
        assertEquals(1, sessions.size());
    }

    @Test
    public void testLogoutAll() throws IOException {
        codeGrant("public-client-0");
        oauth.driver(secondBrowser.driver());
        AccessTokenResponse tokenResponse = codeGrant("public-client-0");

        assertEquals(3, getSessions().size());

        String currentToken = tokenResponse.getAccessToken();
        int status = SimpleHttpDefault.doDelete(getAccountUrl("sessions"), httpClient)
                .acceptJson()
                .auth(currentToken).asStatus();
        assertEquals(204, status);
        assertEquals(1, getSessions(currentToken).size());

        status = SimpleHttpDefault.doDelete(getAccountUrl("sessions?current=true"), httpClient)
                .acceptJson()
                .auth(currentToken).asStatus();
        assertEquals(204, status);

        status = SimpleHttpDefault.doGet(getAccountUrl("sessions"), httpClient)
                .acceptJson()
                .auth(currentToken).asStatus();
        assertEquals(401, status);
    }

    @Test
    public void testNullOrEmptyUserAgent() throws Exception {
        assumeHtmlUnit();

        setBrowserHeader("User-Agent", null);
        AccessTokenResponse tokenResponse = codeGrant("public-client-0");

        List<DeviceRepresentation> devices = queryDevices(tokenResponse.getAccessToken());

        assertEquals(1, devices.size(), "Should have a single device");

        DeviceRepresentation device = devices.get(0);

        assertTrue(device.getCurrent());
        assertEquals("Other", device.getOs());
        assertEquals("Other", device.getDevice());

        List<SessionRepresentation> sessions = device.getSessions();
        assertEquals(1, sessions.size());
        SessionRepresentation session = sessions.get(0);
        assertThat(session.getIpAddress(), anyOf(equalTo("127.0.0.1"), equalTo("0:0:0:0:0:0:0:1")));
        assertEquals(device.getLastAccess(), session.getLastAccess());

        assertEquals(1, session.getClients().size());
    }

    @Test
    public void testNonBrowserSession() throws Exception {
        assumeHtmlUnit();

        // one device
        setBrowserHeader("User-Agent", "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:15.0) Gecko/20100101 Firefox/15.0.1");
        codeGrant("public-client-0");

        // all bellow grouped from a single Other device
        setBrowserHeader("User-Agent", null);
        oauth.client("confidential-client-0", "secret");
        oauth.doPasswordGrantRequest("test-user@localhost", "password");
        oauth.client("confidential-client-1", "secret");
        oauth.doPasswordGrantRequest("test-user@localhost", "password");

        List<DeviceRepresentation> devices = getAllDevices();
        assertEquals(2, devices.size());

        assertThat(devices,
                Matchers.hasItems(Matchers.hasProperty("os", anyOf(Matchers.is("Fedora"), Matchers.is("Other")))));

        // three because tests use another client when booting tests
        assertEquals(3, devices.stream().filter(deviceRepresentation -> "Other".equals(deviceRepresentation.getOs()))
                .map(deviceRepresentation -> deviceRepresentation.getSessions().size())
                .findFirst().get().intValue());
    }

    private List<SessionRepresentation> getSessions(String sessionOne) throws IOException {
        return SimpleHttpDefault
                .doGet(getAccountUrl("sessions"), httpClient).auth(sessionOne)
                .asJson(new TypeReference<List<SessionRepresentation>>() {
                });
    }

    private List<DeviceRepresentation> getDevicesOtherThanOther() throws IOException {
        return getDevicesOtherThanOther(tokenUtil.getToken());
    }

    private List<DeviceRepresentation> getAllDevices() throws IOException {
        return queryDevices(tokenUtil.getToken());
    }

    private List<DeviceRepresentation> getDevicesOtherThanOther(String token) throws IOException {
        return queryDevices(token).stream().filter(rep -> !"Other".equals(rep.getOs())).collect(Collectors.toList());
    }

    private List<DeviceRepresentation> queryDevices(String token) throws IOException {
        return SimpleHttpDefault
                .doGet(getAccountUrl("sessions/devices"), httpClient).auth(token)
                .asJson(new TypeReference<List<DeviceRepresentation>>() {
                });
    }

    private AccessTokenResponse codeGrant(String clientId) {
        oauth.client(clientId);
        oauth.redirectUri(APP_REDIRECT_URI);
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();
        return oauth.doAccessTokenRequest(code);
    }

    private void joinSsoSession(String clientId) {
        oauth.client(clientId);
        oauth.redirectUri(APP_REDIRECT_URI);
        oauth.openLoginForm();
    }

    private List<SessionRepresentation> getSessions() throws IOException {
        return SimpleHttpDefault
                .doGet(getAccountUrl("sessions"), httpClient).auth(tokenUtil.getToken())
                .asJson(new TypeReference<List<SessionRepresentation>>() {
                });
    }

    private void assumeHtmlUnit() {
        assumeTrue(driver.getBrowserType().equals(BrowserType.HTML_UNIT),
                "Browser must be htmlunit. Otherwise we are not able to set desired BrowserHeaders");
    }

    /**
     * HTMLUnit-only header override on the underlying {@link WebClient}, since the new test
     * framework's {@link ManagedWebDriver} doesn't expose this directly. The framework uses plain
     * {@link HtmlUnitDriver}, whose {@code getWebClient()} is public, so the cast is safe.
     */
    private void setBrowserHeader(String name, String value) {
        WebDriver wd = oauth.getDriver();
        if (wd instanceof HtmlUnitDriver) {
            WebClient webClient = ((HtmlUnitDriver) wd).getWebClient();
            webClient.removeRequestHeader(name);
            if (value != null) {
                webClient.addRequestHeader(name, value);
            }
        }
    }
}
