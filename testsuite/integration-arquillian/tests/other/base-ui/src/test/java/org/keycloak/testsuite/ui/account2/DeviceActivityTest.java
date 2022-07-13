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

package org.keycloak.testsuite.ui.account2;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.DeviceActivityPage;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class DeviceActivityTest extends BaseAccountPageTest {
    public static final String TEST_CLIENT_ID = "test-client";
    public static final String TEST_CLIENT_SECRET = "top secret stuff";
    public static final String TEST_CLIENT2_ID = "test-client2";
    public static final String TEST_CLIENT2_SECRET = "even more top secret stuff";
    public static final String TEST_CLIENT3_ID = "test-client3";
    public static final String TEST_CLIENT3_SECRET = "dunno";
    public static final String TEST_CLIENT3_NAME = "Příliš žluťoučký kůň";

    @Page
    private DeviceActivityPage deviceActivityPage;

    @Override
    protected AbstractLoggedInPage getAccountPage() {
        return deviceActivityPage;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        RealmRepresentation realm = testRealms.get(0);

        realm.setClients(Arrays.asList(
                ClientBuilder
                        .create()
                        .clientId(TEST_CLIENT_ID) // client with no name
                        .secret(TEST_CLIENT_SECRET)
                        .directAccessGrants()
                        .build(),
                ClientBuilder
                        .create().
                        clientId(TEST_CLIENT2_ID)
                        .name(LOCALE_CLIENT_NAME) // client with localized name
                        .secret(TEST_CLIENT2_SECRET)
                        .directAccessGrants().build(),
                ClientBuilder
                        .create().
                        clientId(TEST_CLIENT3_ID)
                        .name(TEST_CLIENT3_NAME) // client without localized name
                        .secret(TEST_CLIENT3_SECRET)
                        .directAccessGrants().build()

        ));

        realm.setAccountTheme(LOCALIZED_THEME_PREVIEW); // using localized custom theme for the client localized name
        configureInternationalizationForRealm(testRealms.get(0));
    }

    @Before
    public void beforeDeviceActivityTest() {
        oauth.clientId(TEST_CLIENT3_ID);
    }

    @Test
    public void browsersTest() {
        Map<Browsers, String> browserSessions = new HashMap<>();
        Arrays.stream(Browsers.values()).forEach(b -> {
            browserSessions.put(b, DeviceActivityPage.getTrimmedSessionId(createSession(b)));
        });

        deviceActivityPage.clickRefreshPage();

        browserSessions.forEach((browser, sessionId) -> {
            final Optional<DeviceActivityPage.Session> session = deviceActivityPage.getSession(sessionId);
            assertThat(session.isPresent(), is(true));
            assertSession(browser, session.get());
        });

        assertEquals(Browsers.values().length + 1, deviceActivityPage.getSessionsCount()); // + 1 for the current session
    }

    @Test
    public void currentSessionTest() {
        createSession(Browsers.CHROME);
        createSession(Browsers.SAFARI);

        deviceActivityPage.clickRefreshPage();

        assertEquals(3, deviceActivityPage.getSessionsCount());

        Optional<DeviceActivityPage.Session> currentSession = deviceActivityPage.getSessionByIndex(0); // current session should be first
        assertThat(currentSession.isPresent(), is(true));
        assertSessionRowsAreNotEmpty(currentSession.get(), false);
        assertTrue("Browser identification should be present", currentSession.get().isBrowserDisplayed());
        assertTrue("Current session badge should be present", currentSession.get().hasCurrentBadge());
        assertFalse("Icon should be present", currentSession.get().getIcon().isEmpty());
    }

    @Test
    public void signOutTest() {
        assertFalse("Sign out all shouldn't be displayed", deviceActivityPage.isSignOutAllDisplayed());
        final String chromeSessionId = createSession(Browsers.CHROME);
        deviceActivityPage.clickRefreshPage();

        Optional<DeviceActivityPage.Session> chromeSessionOptional = deviceActivityPage.getSession(chromeSessionId);
        assertThat(chromeSessionOptional.isPresent(), is(true));
        DeviceActivityPage.Session chromeSession = chromeSessionOptional.get();

        createSession(Browsers.SAFARI);
        deviceActivityPage.clickRefreshPage();

        assertTrue("Sign out all should be displayed", deviceActivityPage.isSignOutAllDisplayed());
        assertEquals(3, testUserResource().getUserSessions().size());

        assertThat(testUserResource()
                        .getUserSessions()
                        .stream()
                        .map(f -> f.getId())
                        .map(DeviceActivityPage::getTrimmedSessionId)
                        .collect(Collectors.toList()),
                hasItem(chromeSession.getSessionId()));

        // sign out one session
        assertThat(chromeSession.isSignOutDisplayed(), is(true));
        testModalDialog(chromeSession::clickSignOut, () -> {
            assertEquals(3, testUserResource().getUserSessions().size()); // no change, all sessions still present
        });
        deviceActivityPage.alert().assertSuccess();
        assertFalse("Chrome session should be gone", chromeSession.isPresent());
        assertEquals(2, testUserResource().getUserSessions().size());
        assertThat(testUserResource()
                        .getUserSessions()
                        .stream()
                        .map(f -> f.getId())
                        .map(DeviceActivityPage::getTrimmedSessionId)
                        .collect(Collectors.toList()),
                not(hasItem(chromeSession.getSessionId())));

        // sign out all sessions
        testModalDialog(deviceActivityPage::clickSignOutAll, () -> {
            assertEquals(2, testUserResource().getUserSessions().size()); // no change
        });
        accountWelcomeScreen.assertCurrent();
        assertEquals(0, testUserResource().getUserSessions().size());
    }

    @Test
    public void clientsTest() {
        String sessionId = createSession(Browsers.CHROME);

        // attach more clients to the session
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName(TEST);
            UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);

            ClientModel client2 = session.clients().getClientByClientId(realm, TEST_CLIENT2_ID);
            ClientModel client3 = session.clients().getClientByClientId(realm, TEST_CLIENT3_ID);

            session.sessions().createClientSession(realm, client2, userSession);
            session.sessions().createClientSession(realm, client3, userSession);
        });

        deviceActivityPage.clickRefreshPage();

        List<String> expectedClients = Arrays.asList(TEST_CLIENT_ID, LOCALE_CLIENT_NAME_LOCALIZED, TEST_CLIENT3_NAME);

        final Optional<DeviceActivityPage.Session> sessionById = deviceActivityPage.getSession(sessionId);
        assertThat(sessionById.isPresent(), is(true));
        String[] actualClients = sessionById.get().getClients().split(", ");
        assertThat(expectedClients, containsInAnyOrder(actualClients));

        final Optional<DeviceActivityPage.Session> session = deviceActivityPage.getSessionByIndex(0);
        assertThat(session.isPresent(), is(true));
        assertEquals("Account Console", session.get().getClients());
    }

    @Test
    public void timesTests() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy, h:mm a", Locale.ENGLISH);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nowPlus1 = now.plusMinutes(1);
        String nowStr = now.format(formatter);
        String nowStrPlus1 = nowPlus1.format(formatter);

        String sessionId = createSession(Browsers.CHROME);

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName(TEST);
            UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);

            userSession.setLastSessionRefresh(Time.currentTime() + 120);
        });

        deviceActivityPage.clickRefreshPage();

        final Optional<DeviceActivityPage.Session> session = deviceActivityPage.getSession(sessionId);
        assertThat(session.isPresent(), is(true));

        String startedAtStr = session.get().getStarted();
        LocalDateTime startedAt = LocalDateTime.parse(startedAtStr, formatter);
        LocalDateTime lastAccessed = LocalDateTime.parse(session.get().getLastAccess(), formatter);
        LocalDateTime expiresAt = LocalDateTime.parse(session.get().getExpires(), formatter);

        assertTrue("Last access should be after started at", lastAccessed.isAfter(startedAt));
        assertTrue("Expires at should be after last access", expiresAt.isAfter(lastAccessed));
        assertTrue("Last accessed should be in the future", lastAccessed.isAfter(now));
        assertThat(startedAtStr, either(equalTo(nowStr)).or(equalTo(nowStrPlus1)));

        int ssoLifespan = testRealmResource().toRepresentation().getSsoSessionMaxLifespan();
        assertEquals(startedAt.plusSeconds(ssoLifespan), expiresAt);
    }

    @Test
    public void timeLocaleTest() {
        String sessionId = createSession(Browsers.CHROME);
        UserRepresentation user = testUserResource().toRepresentation();
        final Locale locale = Locale.GERMAN;
        user.setAttributes(new HashMap<String, List<String>>() {{
            put("locale", Collections.singletonList(locale.toLanguageTag()));
        }});
        testUserResource().update(user);

        refreshPageAndWaitForLoad();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d. MMMM yyyy, H:mm", locale);
        Optional<DeviceActivityPage.Session> session = deviceActivityPage.getSession(sessionId);
        assertThat(session.isPresent(), is(true));
        try {
            LocalDateTime.parse(session.get().getLastAccess(), formatter);
        } catch (DateTimeParseException e) {
            fail("Time was not formatted with the locale");
        }
    }

    @Test
    public void ipTest() {
        final String ip = "146.58.69.12";

        String sessionId = "abcdefg";
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName(TEST);
            ClientModel client = session.clients().getClientByClientId(realm, TEST_CLIENT_ID);
            UserModel user = session.users().getUserByUsername(realm, "test"); // cannot use testUser.getUsername() because it throws NotSerializableException for no apparent reason (or maybe I'm just stupid :D)

            UserSessionModel userSession = session.sessions().createUserSession(sessionId, realm, user, "test", ip, "form", false, null, null, null);
            session.sessions().createClientSession(realm, client, userSession);
        });

        deviceActivityPage.clickRefreshPage();

        final Optional<DeviceActivityPage.Session> session = deviceActivityPage.getSession(sessionId);
        assertThat(session.isPresent(), is(true));
        assertEquals(ip, session.get().getIp());
    }

    private String createSession(Browsers browser) {
        log.info("Creating session for " + browser);
        OAuthClient.AccessTokenResponse res;
        try {
            // using direct grant not to use current browser
            res = oauth.doGrantAccessTokenRequest(
                    TEST, testUser.getUsername(), PASSWORD, null, TEST_CLIENT_ID, TEST_CLIENT_SECRET, browser.userAgent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return res.getSessionState(); // session id
    }

    private void assertSession(Browsers browser, DeviceActivityPage.Session session) {
        log.infof("Asserting %s (session %s)", browser, session.getSessionId());
        assertTrue("Session should be present", session.isPresent());
        assertTrue("Browser name should be present", session.isBrowserDisplayed());

        if (browser.sessionBrowser != null) {
            assertEquals(browser.sessionBrowser, session.getBrowser());
        } else {
            assertEquals("Other/Unknown", session.getBrowser());
        }

        assertEquals(browser.iconName, session.getIcon());
        assertFalse("Session shouldn't have current badge", session.hasCurrentBadge()); // we don't test current session
        assertSessionRowsAreNotEmpty(session, true);
    }

    private void assertSessionRowsAreNotEmpty(DeviceActivityPage.Session session, boolean expectSignOutPresent) {
        assertFalse("IP address shouldn't be empty", session.getIp().isEmpty());
        assertFalse("Last accessed shouldn't be empty", session.getLastAccess().isEmpty());
        assertFalse("Started shouldn't be empty", session.getStarted().isEmpty());
        assertFalse("Expires shouldn't be empty", session.getExpires().isEmpty());
        assertFalse("Clients shouldn't be empty", session.getClients().isEmpty());
        assertEquals("Sign out button visibility", expectSignOutPresent, session.isSignOutDisplayed());
    }

    public enum Browsers {
        CHROME(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36",
                "Chrome/78.0.3904",
                DeviceType.DESKTOP
        ),
        CHROMIUM(
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/534.30 (KHTML, like Gecko) Ubuntu/11.04 Chromium/12.0.742.112 Chrome/12.0.742.112 Safari/534.30",
                "Chromium/12.0.742",
                DeviceType.DESKTOP
        ),
        FIREFOX(
                "Mozilla/5.0 (X11; Fedora;Linux x86; rv:60.0) Gecko/20100101 Firefox/60.0",
                "Firefox/60.0",
                DeviceType.DESKTOP
        ),
        EDGE(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.140 Safari/537.36 Edge/18.17763",
                "Edge/18.17763",
                DeviceType.DESKTOP
        ),
        // TODO uncomment this once KEYCLOAK-12445 is resolved
//        CHREDGE( // Edge based on Chromium
//                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.74 Safari/537.36 Edg/79.0.309.43",
//                "Edge/79.0.309 / Mac OS X 10.15.1",
//                "edge"
//        ),
        IE(
                "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko",
                "IE/11.0",
                DeviceType.DESKTOP
        ),
        SAFARI(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Safari/605.1.15",
                "Safari/13.0.3",
                DeviceType.DESKTOP
        ),
        OPERA(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36 OPR/56.0.3051.52",
                "Opera/56.0.3051",
                DeviceType.DESKTOP
        ),
        YANDEX(
                "Mozilla/5.0 (Windows NT 6.3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 YaBrowser/17.6.1.749 Yowser/2.5 Safari/537.36",
                "Yandex Browser/17.6.1",
                DeviceType.DESKTOP
        ),
        CHROME_ANDROID(
                "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Mobile Safari/537.36",
                "Chrome Mobile/68.0.3440",
                DeviceType.MOBILE
        ),
        SAFARI_IOS(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 13_1_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.1 Mobile/15E148 Safari/604.1",
                "Mobile Safari/13.0.1",
                DeviceType.MOBILE
        ),
        UNKNOWN_BROWSER(
                "Top-secret government browser running on top-secret OS",
                null,
                DeviceType.UNKNOWN
        ),
        UNKNOWN_OS(
                "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36",
                "Chrome/78.0.3904",
                DeviceType.UNKNOWN
        ),
        UNKNOWN_OS_VERSION(
                "Mozilla/5.0 (Windows 256.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36",
                "Chrome/78.0.3904",
                DeviceType.UNKNOWN
        );
        // not sure what "Amazon" browser is supposed to be (it's specified in DeviceActivityPage.tsx)

        private final String userAgent;
        private final String sessionBrowser; // how the browser is interpreted by the sessions endpoint
        private final DeviceType deviceType;
        private final String iconName;

        Browsers(String userAgent, String sessionBrowser, DeviceType deviceType) {
            this.userAgent = userAgent;
            this.sessionBrowser = sessionBrowser;
            this.deviceType = deviceType;
            this.iconName = deviceType.getIconName();
        }

        public String userAgent() {
            return userAgent;
        }

        public String sessionBrowser() {
            return sessionBrowser;
        }

        public String iconName() {
            return iconName;
        }

        private enum DeviceType {
            DESKTOP("desktop"),
            MOBILE("mobile"),
            UNKNOWN("desktop"); // Default icon

            private final String iconName;

            DeviceType(String iconName) {
                this.iconName = iconName;
            }

            public String getIconName() {
                return iconName;
            }
        }
    }
}
