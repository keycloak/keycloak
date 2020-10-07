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

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.representations.account.ClientRepresentation;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.testsuite.util.ThirdBrowser;
import org.keycloak.testsuite.util.TokenUtil;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SessionRestServiceTest extends AbstractRestServiceTest {

    @Drone
    @SecondBrowser
    protected WebDriver secondBrowser;

    @Drone
    @ThirdBrowser
    protected WebDriver thirdBrowser;

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
                .clientId("public-client-1")
                .name("Public Client 1")
                .baseUrl("http://client1.example.com")
                .redirectUris(OAuthClient.APP_ROOT + "/auth")
                .publicClient().build());

        testRealm.getClients().add(ClientBuilder.create()
                .clientId("confidential-client-0")
                .name("Confidential Client 0")
                .secret("secret")
                .serviceAccount()
                .directAccessGrants()
                .redirectUris(OAuthClient.APP_ROOT + "/auth").build());

        testRealm.getClients().add(ClientBuilder.create()
                .clientId("confidential-client-1")
                .name("Confidential Client 1")
                .secret("secret")
                .serviceAccount()
                .directAccessGrants()
                .redirectUris(OAuthClient.APP_ROOT + "/auth").build());
    }

    @Test
    public void testProfilePreviewPermissions() throws IOException {
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

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testGetSessions() throws Exception {
        oauth.setDriver(secondBrowser);
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
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testGetDevicesResponse() throws Exception {
        assumeTrue("Browser must be htmlunit. Otherwise we are not able to set desired BrowserHeaders",
                System.getProperty("browser").equals("htmlUnit"));
        oauth.setBrowserHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0) Gecko/20100101 Firefox/15.0.1");
        OAuthClient.AccessTokenResponse tokenResponse = codeGrant("public-client-0");
        joinSsoSession("public-client-1");

        List<DeviceRepresentation> devices = getDevicesOtherThanOther(tokenResponse.getAccessToken());

        assertEquals("Should have a single device", 1, devices.size());

        DeviceRepresentation device = devices.get(0);

        assertTrue(device.getCurrent());
        assertEquals("Windows", device.getOs());
        assertEquals("10", device.getOsVersion());
        assertEquals("Other", device.getDevice());

        List<SessionRepresentation> sessions = device.getSessions();
        assertEquals(1, sessions.size());
        SessionRepresentation session = sessions.get(0);
        assertEquals("127.0.0.1", session.getIpAddress());
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
        ContainerAssume.assumeAuthServerUndertow();
        assumeTrue("Browser must be htmlunit. Otherwise we are not able to set desired BrowserHeaders",
                System.getProperty("browser").equals("htmlUnit"));

        WebDriver firstBrowser = oauth.getDriver();

        // first browser authenticates from Fedora
        oauth.setBrowserHeader("User-Agent", "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:15.0) Gecko/20100101 Firefox/15.0.1");
        codeGrant("public-client-0");
        List<DeviceRepresentation> devices = getDevicesOtherThanOther();
        assertEquals("Should have a single device", 1, devices.size());
        List<DeviceRepresentation> fedoraDevices = devices.stream()
                .filter(deviceRepresentation -> "Fedora".equals(deviceRepresentation.getOs())).collect(Collectors.toList());
        assertEquals("Should have a single Fedora device", 1, fedoraDevices.size());
        fedoraDevices.stream().forEach(device -> {
            List<SessionRepresentation> sessions = device.getSessions();
            assertEquals(1, sessions.size());
            assertThat(sessions, Matchers.hasItem(Matchers.hasProperty("browser", Matchers.is("Firefox/15.0.1"))));
        });

        // second browser authenticates from Windows
        oauth.setDriver(secondBrowser);
        oauth.setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Gecko/20100101 Firefox/15.0.1");
        codeGrant("public-client-0");
        devices = getDevicesOtherThanOther();
        // should have two devices
        assertEquals("Should have two devices", 2, devices.size());
        fedoraDevices = devices.stream()
                .filter(deviceRepresentation -> "Fedora".equals(deviceRepresentation.getOs())).collect(Collectors.toList());
        assertEquals(1, fedoraDevices.size());
        List<DeviceRepresentation> windowsDevices = devices.stream()
                .filter(deviceRepresentation -> "Windows".equals(deviceRepresentation.getOs())).collect(Collectors.toList());
        assertEquals(1, windowsDevices.size());
        windowsDevices.stream().forEach(device -> {
            List<SessionRepresentation> sessions = device.getSessions();
            assertEquals(1, sessions.size());
            assertThat(sessions, Matchers.hasItem(Matchers.hasProperty("browser", Matchers.is("Firefox/15.0.1"))));
        });

        // first browser authenticates from Windows using Edge
        oauth.setDriver(firstBrowser);
        oauth.setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36 Edge/12.0");
        codeGrant("public-client-0");

        // second browser authenticates from Windows using Firefox
        oauth.setDriver(secondBrowser);
        oauth.setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Gecko/20100101 Firefox/15.0.1");
        codeGrant("public-client-0");

        // third browser authenticates from Windows using Safari
        oauth.setDriver(thirdBrowser);
        oauth.setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Version/11.0 Safari/603.1.30");
        oauth.setBrowserHeader("X-Forwarded-For", "192.168.10.3");
        OAuthClient.AccessTokenResponse tokenResponse = codeGrant("public-client-0");
        devices = getDevicesOtherThanOther(tokenResponse.getAccessToken());
        assertEquals(
                "Should have a single device because all browsers (and sessions) are from the same platform (OS + OS version)",
                1, devices.size());
        windowsDevices = devices.stream()
                .filter(device -> "Windows".equals(device.getOs())).collect(Collectors.toList());
        assertEquals(1, windowsDevices.size());
        windowsDevices.stream().forEach(device -> {
            List<SessionRepresentation> sessions = device.getSessions();
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
        oauth.setDriver(thirdBrowser);
        oauth.setBrowserHeader("User-Agent",
                "Mozilla/5.0 (Windows 7) AppleWebKit/537.36 (KHTML, like Gecko) Version/11.0 Safari/603.1.30");
        oauth.setBrowserHeader("X-Forwarded-For", "192.168.10.3");
        codeGrant("public-client-0");
        devices = getDevicesOtherThanOther();
        windowsDevices = devices.stream()
                .filter(device -> "Windows".equals(device.getOs())).collect(Collectors.toList());
        assertEquals("Should have two devices for two distinct Windows versions", 2, devices.size());
        assertEquals(2, windowsDevices.size());

        oauth.setDriver(firstBrowser);
        oauth.setBrowserHeader("User-Agent",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 5_1_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B206 Safari/7534.48.3");
        codeGrant("public-client-0");
        oauth.setDriver(secondBrowser);
        oauth.setBrowserHeader("User-Agent",
                "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:15.0) Gecko/20100101 Firefox/15.0.1");
        codeGrant("public-client-0");
        devices = getDevicesOtherThanOther();
        assertEquals("Should have 3 devices", 3, devices.size());
        windowsDevices = devices.stream()
                .filter(device -> "Windows".equals(device.getOs())).collect(Collectors.toList());
        assertEquals(1, windowsDevices.size());
        fedoraDevices = devices.stream()
                .filter(deviceRepresentation -> "Fedora".equals(deviceRepresentation.getOs())).collect(Collectors.toList());
        assertEquals(1, fedoraDevices.size());
        List<DeviceRepresentation> iphoneDevices = devices.stream()
                .filter(device -> "iOS".equals(device.getOs()) && "iPhone".equals(device.getDevice()))
                .collect(Collectors.toList());
        assertEquals(1, iphoneDevices.size());
        iphoneDevices.stream().forEach(device -> {
            assertTrue(device.isMobile());
            List<SessionRepresentation> sessions = device.getSessions();
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
        int status = SimpleHttp.doDelete(getAccountUrl("sessions/" + sessionId), httpClient).acceptJson()
                .auth(viewToken.getToken()).asStatus();
        assertEquals(403, status);
        sessions = getSessions(viewToken.getToken());
        assertEquals(2, sessions.size());

        // Here you can delete the session
        status = SimpleHttp.doDelete(getAccountUrl("sessions/" + sessionId), httpClient).acceptJson().auth(tokenUtil.getToken())
                .asStatus();
        assertEquals(204, status);
        sessions = getSessions(tokenUtil.getToken());
        assertEquals(1, sessions.size());
    }

    @Test
    public void testLogoutAll() throws IOException {
        codeGrant("public-client-0");
        oauth.setDriver(secondBrowser);
        OAuthClient.AccessTokenResponse tokenResponse = codeGrant("public-client-0");

        assertEquals(3, getSessions().size());

        String currentToken = tokenResponse.getAccessToken();
        int status = SimpleHttp.doDelete(getAccountUrl("sessions"), httpClient)
                .acceptJson()
                .auth(currentToken).asStatus();
        assertEquals(204, status);
        assertEquals(1, getSessions(currentToken).size());

        status = SimpleHttp.doDelete(getAccountUrl("sessions?current=true"), httpClient)
                .acceptJson()
                .auth(currentToken).asStatus();
        assertEquals(204, status);

        status = SimpleHttp.doGet(getAccountUrl("sessions"), httpClient)
                .acceptJson()
                .auth(currentToken).asStatus();
        assertEquals(401, status);
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testNullOrEmptyUserAgent() throws Exception {
        assumeTrue("Browser must be htmlunit. Otherwise we are not able to set desired BrowserHeaders",
                System.getProperty("browser").equals("htmlUnit"));

        oauth.setBrowserHeader("User-Agent", null);
        OAuthClient.AccessTokenResponse tokenResponse = codeGrant("public-client-0");

        List<DeviceRepresentation> devices = queryDevices(tokenResponse.getAccessToken());

        assertEquals("Should have a single device", 1, devices.size());

        DeviceRepresentation device = devices.get(0);

        assertTrue(device.getCurrent());
        assertEquals("Other", device.getOs());
        assertEquals("Other", device.getDevice());

        List<SessionRepresentation> sessions = device.getSessions();
        assertEquals(1, sessions.size());
        SessionRepresentation session = sessions.get(0);
        assertEquals("127.0.0.1", session.getIpAddress());
        assertEquals(device.getLastAccess(), session.getLastAccess());

        assertEquals(1, session.getClients().size());
    }

    @Test
    public void testNonBrowserSession() throws Exception {
        assumeTrue("Browser must be htmlunit. Otherwise we are not able to set desired BrowserHeaders",
                System.getProperty("browser").equals("htmlUnit"));

        // one device
        oauth.setBrowserHeader("User-Agent", "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:15.0) Gecko/20100101 Firefox/15.0.1");
        codeGrant("public-client-0");

        // all bellow grouped from a single Other device
        oauth.setBrowserHeader("User-Agent", null);
        oauth.clientId("confidential-client-0");
        oauth.doGrantAccessTokenRequest("secret", "test-user@localhost", "password");
        oauth.clientId("confidential-client-1");
        oauth.doGrantAccessTokenRequest("secret", "test-user@localhost", "password");

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
        return SimpleHttp
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
        return SimpleHttp
                .doGet(getAccountUrl("sessions/devices"), httpClient).auth(token)
                .asJson(new TypeReference<List<DeviceRepresentation>>() {
                });
    }

    private OAuthClient.AccessTokenResponse codeGrant(String clientId) {
        oauth.clientId(clientId);
        oauth.redirectUri(OAuthClient.APP_ROOT + "/auth");
        oauth.openLogout();
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        return oauth.doAccessTokenRequest(code, "password");
    }

    private void joinSsoSession(String clientId) {
        oauth.clientId(clientId);
        oauth.redirectUri(OAuthClient.APP_ROOT + "/auth");
        oauth.openLoginForm();
    }

    private List<SessionRepresentation> getSessions() throws IOException {
        return SimpleHttp
                .doGet(getAccountUrl("sessions"), httpClient).auth(tokenUtil.getToken())
                .asJson(new TypeReference<List<SessionRepresentation>>() {
                });
    }
}
