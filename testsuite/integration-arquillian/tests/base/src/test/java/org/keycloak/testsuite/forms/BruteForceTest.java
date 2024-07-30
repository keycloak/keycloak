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
package org.keycloak.testsuite.forms;

import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AssertEvents.ExpectedEvent;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmRepUtil;
import org.keycloak.testsuite.util.UserBuilder;

import jakarta.mail.internet.MimeMessage;

import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class BruteForceTest extends AbstractTestRealmKeycloakTest {

    private static String userId;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginPasswordResetPage passwordResetPage;

    @Page
    protected LoginPasswordUpdatePage passwordUpdatePage;

    @Page
    private RegisterPage registerPage;

    @Page
    protected LoginTotpPage loginTotpPage;

    private TimeBasedOTP totp = new TimeBasedOTP();

    private int lifespan;

    private static final Integer failureFactor = 2;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        UserRepresentation user = RealmRepUtil.findUser(testRealm, "test-user@localhost");
        UserBuilder.edit(user).totpSecret("totpSecret");

        testRealm.setBruteForceProtected(true);
        testRealm.setFailureFactor(failureFactor);
        testRealm.setMaxDeltaTimeSeconds(20);
        testRealm.setMaxFailureWaitSeconds(100);
        testRealm.setWaitIncrementSeconds(5);
        testRealm.setOtpPolicyCodeReusable(true);
        //testRealm.setQuickLoginCheckMilliSeconds(0L);

        userId = user.getId();

        RealmRepUtil.findClientByClientId(testRealm, "test-app").setDirectAccessGrantsEnabled(true);
        testRealm.getUsers().add(UserBuilder.create().username("user2").email("user2@localhost").password("password").build());
    }

    @Before
    public void config() {
        try {
            clearUserFailures();
            clearAllUserFailures();
            RealmRepresentation realm = adminClient.realm("test").toRepresentation();
            realm.setFailureFactor(failureFactor);
            realm.setMaxDeltaTimeSeconds(20);
            realm.setMaxFailureWaitSeconds(100);
            realm.setWaitIncrementSeconds(5);
            realm.setOtpPolicyCodeReusable(true);
            adminClient.realm("test").update(realm);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        events.clear();

    }

    @After
    public void slowItDown() throws Exception {
        try {
            clearUserFailures();
            clearAllUserFailures();
            RealmRepresentation realm = adminClient.realm("test").toRepresentation();
            realm.setMaxFailureWaitSeconds(900);
            realm.setMinimumQuickLoginWaitSeconds(60);
            realm.setWaitIncrementSeconds(60);
            realm.setQuickLoginCheckMilliSeconds(1000L);
            realm.setMaxDeltaTimeSeconds(60 * 60 * 12); // 12 hours
            realm.setFailureFactor(30);
            adminClient.realm("test").update(realm);
            testingClient.testing().setTimeOffset(Collections.singletonMap("offset", String.valueOf(0)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        events.clear();
        Thread.sleep(100);
    }

    @Before
    public void before() throws MalformedURLException {
        totp = new TimeBasedOTP();
    }

    public String getAdminToken() throws Exception {
        String clientId = Constants.ADMIN_CLI_CLIENT_ID;
        return oauth.doGrantAccessTokenRequest("master", "admin", "admin", null, clientId, null).getAccessToken();
    }

    public OAuthClient.AccessTokenResponse getTestToken(String password, String totp) throws Exception {
        return oauth.doGrantAccessTokenRequest("test", "test-user@localhost", password, totp, oauth.getClientId(), "password");

    }

    protected void clearUserFailures() throws Exception {
        adminClient.realm("test").attackDetection().clearBruteForceForUser(findUser("test-user@localhost").getId());
    }

    protected void clearAllUserFailures() throws Exception {
        adminClient.realm("test").attackDetection().clearAllBruteForce();
    }

    @Test
    public void testGrantInvalidPassword() throws Exception {
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNotNull(response.getAccessToken());
            Assert.assertNull(response.getError());
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("invalid", totpSecret);
            Assert.assertNull(response.getAccessToken());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("invalid", totpSecret);
            Assert.assertNull(response.getAccessToken());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNull(response.getAccessToken());
            Assert.assertNotNull(response.getError());
            Assert.assertEquals("invalid_grant", response.getError());
            Assert.assertEquals("Invalid user credentials", response.getErrorDescription());
            assertUserDisabledEvent(Errors.USER_TEMPORARILY_DISABLED);
            events.clear();
        }
        clearUserFailures();
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNotNull(response.getAccessToken());
            Assert.assertNull(response.getError());
            events.clear();
        }

    }

    @Test
    public void testGrantInvalidOtp() throws Exception {
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNotNull(response.getAccessToken());
            Assert.assertNull(response.getError());
            events.clear();
        }
        {
            OAuthClient.AccessTokenResponse response = getTestToken("password", "shite");
            Assert.assertNull(response.getAccessToken());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            OAuthClient.AccessTokenResponse response = getTestToken("password", "shite");
            Assert.assertNull(response.getAccessToken());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            assertTokenNull(response);
            Assert.assertNotNull(response.getError());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals("Invalid user credentials", response.getErrorDescription());
            assertUserDisabledEvent(Errors.USER_TEMPORARILY_DISABLED);
            events.clear();
        }
        clearUserFailures();
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNotNull(response.getAccessToken());
            Assert.assertNull(response.getError());
            events.clear();
        }

    }

    public void assertTokenNull(OAuthClient.AccessTokenResponse response) {
        Assert.assertNull(response.getAccessToken());
    }

    @Test
    public void testGrantMissingOtp() throws Exception {
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNotNull(response.getAccessToken());
            Assert.assertNull(response.getError());
            events.clear();
        }
        {
            OAuthClient.AccessTokenResponse response = getTestToken("password", null);
            Assert.assertNull(response.getAccessToken());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            OAuthClient.AccessTokenResponse response = getTestToken("password", null);
            Assert.assertNull(response.getAccessToken());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            assertTokenNull(response);
            Assert.assertNotNull(response.getError());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals("Invalid user credentials", response.getErrorDescription());
            assertUserDisabledEvent(Errors.USER_TEMPORARILY_DISABLED);
            events.clear();
        }
        clearUserFailures();
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNotNull(response.getAccessToken());
            Assert.assertNull(response.getError());
            events.clear();
        }

    }

    @Test
    public void testNumberOfFailuresForDisabledUsersWithPasswordGrantType() throws Exception {
        try {
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            assertUserNumberOfFailures(user.getId(), 0);
            user.setEnabled(false);
            updateUser(user);

            OAuthClient.AccessTokenResponse response = getTestToken("invalid", "invalid");
            Assert.assertNull(response.getAccessToken());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Account disabled");
            events.clear();

            assertUserNumberOfFailures(user.getId(), 0);
        } finally {
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            updateUser(user);
            events.clear();
        }
    }

    @Test
    public void testNumberOfFailuresForTemporaryDisabledUsersWithPasswordGrantType() throws Exception {
        UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);

        // Lock user (temporarily) and make sure the number of failures matches failure factor
        lockUserWithPasswordGrant();
        assertUserNumberOfFailures(user.getId(), failureFactor);

        // Try to login with invalid credentials and make sure the number of failures doesn't change during temporary lockout
        sendInvalidPasswordPasswordGrant();
        assertUserNumberOfFailures(user.getId(), failureFactor);

        events.clear();
    }

    @Test
    public void testNumberOfFailuresForPermanentlyDisabledUsersWithPasswordGrantType() throws Exception {
        RealmRepresentation realm = testRealm().toRepresentation();
        try {
            // Set permanent lockout for the test
            realm.setPermanentLockout(true);
            testRealm().update(realm);

            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);

            // Lock user (permanently) and make sure the number of failures matches failure factor
            lockUserWithPasswordGrant();
            assertUserNumberOfFailures(user.getId(), failureFactor);
            assertUserDisabledReason(BruteForceProtector.DISABLED_BY_PERMANENT_LOCKOUT);

            // Try to login with invalid credentials and make sure the number of failures doesn't change during temporary lockout
            sendInvalidPasswordPasswordGrant();
            assertUserNumberOfFailures(user.getId(), failureFactor);

            events.clear();
        } finally {
            realm.setPermanentLockout(false);
            testRealm().update(realm);
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            updateUser(user);
        }
    }

    @Test
    public void testBrowserInvalidPassword() throws Exception {
        loginSuccess();
        loginInvalidPassword();
        loginInvalidPassword();
        expectTemporarilyDisabled();
        expectTemporarilyDisabled("test-user@localhost", null, "invalid");
        clearUserFailures();
        loginSuccess();
        loginInvalidPassword();
        loginInvalidPassword();
        expectTemporarilyDisabled();
        expectTemporarilyDisabled("test-user@localhost", null, "invalid");
        clearAllUserFailures();
        loginSuccess();
    }

    @Test
    public void testFailureResetForTemporaryLockout() throws Exception {
        RealmRepresentation realm = testRealm().toRepresentation();
        try {
            realm.setMaxDeltaTimeSeconds(5);
            testRealm().update(realm);
            long numExecutors = getNumExecutors();
            loginInvalidPassword();

            //Wait for brute force executor to process the login and then wait for delta time
            waitForExecutors(numExecutors + 1);
            testingClient.testing().setTimeOffset(Collections.singletonMap("offset", String.valueOf(5)));

            loginInvalidPassword();
            loginSuccess();
        } finally {
            realm.setMaxDeltaTimeSeconds(20);
            testRealm().update(realm);
        }
    }

    @Test
    public void testNoFailureResetForPermanentLockout() throws Exception {
        RealmRepresentation realm = testRealm().toRepresentation();
        try {
            realm.setMaxDeltaTimeSeconds(5);
            realm.setPermanentLockout(true);
            testRealm().update(realm);
            long numExecutors = getNumExecutors();
            loginInvalidPassword();

            //Wait for brute force executor to process the login and then wait for delta time
            waitForExecutors(numExecutors + 1);
            testingClient.testing().setTimeOffset(Collections.singletonMap("offset", String.valueOf(5)));

            loginInvalidPassword();
            expectPermanentlyDisabled();
        } finally {
            realm.setPermanentLockout(false);
            realm.setMaxDeltaTimeSeconds(20);
            testRealm().update(realm);
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            updateUser(user);
        }
    }

    private long getNumExecutors() {
        String numExecutors = testingClient.server().fetchString(session -> {
            ExecutorsProvider provider = session.getProvider(ExecutorsProvider.class);
            ExecutorService executor = provider.getExecutor("bruteforce");
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
            return threadPoolExecutor.getTaskCount();
        });
        return Long.valueOf(numExecutors);
    }

    private void waitForExecutors(long numExecutors) {
        testingClient.server().run(session -> {
            ExecutorsProvider provider = session.getProvider(ExecutorsProvider.class);
            ExecutorService executor = provider.getExecutor("bruteforce");
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
            while (!threadPoolExecutor.getQueue().isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
            assertEquals(numExecutors, threadPoolExecutor.getCompletedTaskCount());
        });
    }

    @Test
    public void testWait() throws Exception {
        loginSuccess();
        loginInvalidPassword();
        loginInvalidPassword();
        expectTemporarilyDisabled();
        expectTemporarilyDisabled("test-user@localhost", null, "invalid");

        // KEYCLOAK-5420
        // Test to make sure that temporarily disabled doesn't increment failure count
        testingClient.testing().setTimeOffset(Collections.singletonMap("offset", String.valueOf(6)));
        // should be unlocked now
        loginSuccess();
        clearUserFailures();
        clearAllUserFailures();
        loginSuccess();
        testingClient.testing().setTimeOffset(Collections.singletonMap("offset", String.valueOf(0)));
    }

    @Test
    public void testBrowserInvalidPasswordDifferentCase() throws Exception {
        loginSuccess("test-user@localhost");
        loginInvalidPassword("test-User@localhost");
        loginInvalidPassword("Test-user@localhost");
        expectTemporarilyDisabled();
        expectTemporarilyDisabled("test-user@localhost", null, "invalid");
        clearAllUserFailures();
    }

    @Test
    public void testEmail() throws Exception {
        String userId = adminClient.realm("test").users().search("user2", null, null, null, 0, 1).get(0).getId();

        loginInvalidPassword("user2@localhost");
        loginInvalidPassword("user2@localhost");
        expectTemporarilyDisabled("user2@localhost", userId);
        clearAllUserFailures();
    }

    @Test
    public void testBrowserMissingPassword() throws Exception {
        loginSuccess();
        loginMissingPassword();
        loginMissingPassword();
        loginSuccess();
    }

    @Test
    public void testBrowserInvalidTotp() throws Exception {
        loginSuccess();
        loginInvalidPassword();
        loginWithTotpFailure();
        continueLoginWithCorrectTotpExpectFailure();
    }

    @Test
    public void testBrowserMissingTotp() throws Exception {
        loginSuccess();
        loginWithMissingTotp();
        loginWithMissingTotp();
        continueLoginWithMissingTotp();
    }

    @Test
    public void testBrowserTotpSessionInvalidAfterLockout() throws Exception {
        long start = System.currentTimeMillis();
        loginWithTotpFailure();
        continueLoginWithInvalidTotp();
        continueLoginWithInvalidTotp();
        events.clear();
        continueLoginWithInvalidTotp();
        assertUserDisabledEvent(Errors.INVALID_AUTHENTICATION_SESSION);
    }

    @Test
    public void testPermanentLockout() {
        RealmRepresentation realm = testRealm().toRepresentation();
        try {
            // arrange
            realm.setPermanentLockout(true);
            testRealm().update(realm);

            // act
            loginInvalidPassword("test-user@localhost");
            loginInvalidPassword("test-user@localhost", false);

            // As of now, there are two events: USER_DISABLED_BY_PERMANENT_LOCKOUT and LOGIN_ERROR but Order is not
            // guarantee though since the brute force detector is running separately "in its own thread" named
            // "Brute Force Protector".
            List<EventRepresentation> actualEvents = Arrays.asList(events.poll(), events.poll());
            assertIsContained(events.expect(EventType.USER_DISABLED_BY_PERMANENT_LOCKOUT).client((String) null).detail(Details.REASON, "brute_force_attack detected"), actualEvents);
            assertIsContained(events.expect(EventType.LOGIN_ERROR).error(Errors.INVALID_USER_CREDENTIALS), actualEvents);

            // assert
            expectPermanentlyDisabled();

            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            assertFalse(user.isEnabled());
            assertUserDisabledReason(BruteForceProtector.DISABLED_BY_PERMANENT_LOCKOUT);

            user.setEnabled(true);
            updateUser(user);
            assertUserDisabledReason(null);
        } finally {
            realm.setPermanentLockout(false);
            testRealm().update(realm);
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            updateUser(user);
        }
    }

    @Test
    public void testTemporaryLockout() throws Exception {
        loginInvalidPassword("test-user@localhost");
        loginInvalidPassword("test-user@localhost", false);

        List<EventRepresentation> actualEvents = Arrays.asList(events.poll(), events.poll());
        assertIsContained(events.expect(EventType.USER_DISABLED_BY_TEMPORARY_LOCKOUT).client((String) null).detail(Details.REASON, "brute_force_attack detected"), actualEvents);
        assertIsContained(events.expect(EventType.LOGIN_ERROR).error(Errors.INVALID_USER_CREDENTIALS), actualEvents);
    }

    @Test
    public void testExceedMaxTemporaryLockouts() {
        RealmRepresentation realm = testRealm().toRepresentation();
        try {
            realm.setPermanentLockout(true);
            realm.setMaxTemporaryLockouts(2);
            testRealm().update(realm);

            loginInvalidPassword();
            loginInvalidPassword();
            expectTemporarilyDisabled();
            testingClient.testing().setTimeOffset(Collections.singletonMap("offset", String.valueOf(6)));

            loginInvalidPassword();
            expectTemporarilyDisabled();
            testingClient.testing().setTimeOffset(Collections.singletonMap("offset", String.valueOf(11)));

            loginInvalidPassword();
            expectPermanentlyDisabled();
        } finally {
            realm.setPermanentLockout(false);
            realm.setMaxTemporaryLockouts(0);
            testRealm().update(realm);
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            updateUser(user);
        }
    }

    @Test
    public void testMaxTemporaryLockoutsReset() {
        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setPermanentLockout(true);
        realm.setMaxTemporaryLockouts(2);
        testRealm().update(realm);

        try {
            loginInvalidPassword();
            loginInvalidPassword();
            expectTemporarilyDisabled();
            testingClient.testing().setTimeOffset(Collections.singletonMap("offset", String.valueOf(6)));
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            Map<String, Object> status = adminClient.realm("test").attackDetection().bruteForceUserStatus(user.getId());
            assertEquals(1, status.get("numTemporaryLockouts"));
            loginSuccess();
            status = adminClient.realm("test").attackDetection().bruteForceUserStatus(user.getId());
            assertEquals(0, status.get("numTemporaryLockouts"));
        } finally {
            realm.setPermanentLockout(false);
            realm.setMaxTemporaryLockouts(0);
            testRealm().update(realm);
        }
    }

    @Test
    public void testResetLoginFailureCount() {
        RealmRepresentation realm = testRealm().toRepresentation();

        try {
            // arrange
            realm.setPermanentLockout(true);
            testRealm().update(realm);

            // act
            loginInvalidPassword();
            loginSuccess();
            loginInvalidPassword();
            loginSuccess();

            // assert
            assertTrue(adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0).isEnabled());
        } finally {
            realm.setPermanentLockout(false);
            testRealm().update(realm);
        }
    }

    @Test
    public void testFailureCountResetWithPasswordGrantType() throws Exception {
        String totpSecret = totp.generateTOTP("totpSecret");
        OAuthClient.AccessTokenResponse response = getTestToken("invalid", totpSecret);
        Assert.assertNull(response.getAccessToken());
        Assert.assertEquals(response.getError(), "invalid_grant");
        Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");

        UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
        Map<String, Object> userAttackInfo = adminClient.realm("test").attackDetection().bruteForceUserStatus(user.getId());
        assertThat((Integer) userAttackInfo.get("numFailures"), is(1));

        response = getTestToken("password", totpSecret);
        Assert.assertNotNull(response.getAccessToken());
        Assert.assertNull(response.getError());
        events.clear();

        userAttackInfo = adminClient.realm("test").attackDetection().bruteForceUserStatus(user.getId());
        assertThat((Integer) userAttackInfo.get("numFailures"), is(0));
    }

    @Test
    public void testNonExistingAccounts() throws Exception {

        loginInvalidPassword("non-existent-user");
        loginInvalidPassword("non-existent-user");
        loginInvalidPassword("non-existent-user");

        registerUser("non-existent-user");

    }

    @Test
    public void testResetPassword() throws Exception {
        String userId = adminClient.realm("test").users().search("user2", null, null, null, 0, 1).get(0).getId();

        loginInvalidPassword("user2");
        loginInvalidPassword("user2");
        expectTemporarilyDisabled("user2", userId, "invalid");

        loginPage.resetPassword();

        passwordResetPage.assertCurrent();
        passwordResetPage.changePassword("user2");

        loginPage.assertCurrent();

        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).user(userId).assertEvent();

        MimeMessage message = greenMail.getReceivedMessages()[0];
        String passwordResetEmailLink = MailUtils.getPasswordResetEmailLink(message);

        driver.navigate().to(passwordResetEmailLink.trim());

        assertTrue(passwordUpdatePage.isCurrent());

        UserRepresentation userRepresentation = testRealm().users().get(userId).toRepresentation();
        assertFalse(userRepresentation.isEnabled());

        updatePasswordPage.updatePasswords("password", "password");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).user(userId).assertEvent();

        userRepresentation = testRealm().users().get(userId).toRepresentation();
        assertTrue(userRepresentation.isEnabled());

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        String idTokenHint = oauth.doAccessTokenRequest(code, "password").getIdToken();
        appPage.logout(idTokenHint);

        events.clear();
    }

    @Test
    public void testRaceAttackTemporaryLockout() throws Exception {
        RealmRepresentation realm = testRealm().toRepresentation();
        UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
        try {
            realm.setWaitIncrementSeconds(120);
            realm.setQuickLoginCheckMilliSeconds(120000L);
            testRealm().update(realm);
            clearUserFailures();
            clearAllUserFailures();
            user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            testRealm().users().get(user.getId()).update(user);
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNotNull(response.getAccessToken());
            raceAttack(user);
        } finally {
            realm.setWaitIncrementSeconds(5);
            realm.setQuickLoginCheckMilliSeconds(100L);
            testRealm().update(realm);
            user.setEnabled(true);
            updateUser(user);
        }
    }

    @Test
    public void testRaceAttackPermanentLockout() throws Exception {
        RealmRepresentation realm = testRealm().toRepresentation();
        UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
        try {
            realm.setPermanentLockout(true);
            testRealm().update(realm);
            raceAttack(user);
            clearUserFailures();
            clearAllUserFailures();
            user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            testRealm().users().get(user.getId()).update(user);
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNotNull(response.getAccessToken());
        } finally {
            realm.setPermanentLockout(false);
            testRealm().update(realm);
            user.setEnabled(true);
            updateUser(user);
        }
    }

    private void raceAttack(UserRepresentation user) throws Exception {
        int num = 100;
        LoginThread[] threads = new LoginThread[num];
        for (int i = 0; i < num; ++i) {
            threads[i] = new LoginThread();
        }
        for (int i = 0; i < num; ++i) {
            threads[i].start();
        }
        for (int i = 0; i < num; ++i) {
            threads[i].join();
        }
        int invalidCount =  (int) adminClient.realm("test").attackDetection().bruteForceUserStatus(user.getId()).get("numFailures");
        assertTrue("Invalid count should be less than or equal 2 but was: " + invalidCount, invalidCount <= 2);
    }

    public class LoginThread extends Thread {

        public void run() {
            try {
                String totpSecret = totp.generateTOTP("totpSecret");
                OAuthClient.AccessTokenResponse response = getTestToken("invalid", totpSecret);
                Assert.assertNull(response.getAccessToken());
                Assert.assertEquals(response.getError(), "invalid_grant");
                Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void expectTemporarilyDisabled() {
        expectTemporarilyDisabled("test-user@localhost", null, "password");
    }

    public void expectTemporarilyDisabled(String username, String userId) {
        expectTemporarilyDisabled(username, userId, "password");
    }

    public void expectTemporarilyDisabled(String username, String userId, String password) {
        loginPage.open();
        loginPage.login(username, password);

        loginPage.assertCurrent();
        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());
        ExpectedEvent event = events.expectLogin()
                .session((String) null)
                .error(Errors.USER_TEMPORARILY_DISABLED)
                .detail(Details.USERNAME, username)
                .removeDetail(Details.CONSENT);
        if (userId != null) {
            event.user(userId);
        }
        event.assertEvent();
    }

    public void expectPermanentlyDisabled() {
        expectPermanentlyDisabled("test-user@localhost");
    }

    public void expectPermanentlyDisabled(String username) {
        loginPage.open();
        loginPage.login(username, "password");

        loginPage.assertCurrent();
        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());
        ExpectedEvent event = events.expectLogin()
            .session((String) null)
            .error(Errors.USER_DISABLED)
            .detail(Details.USERNAME, username)
            .removeDetail(Details.CONSENT);
        event.assertEvent();
    }

    public void loginSuccess() {
        loginSuccess("test-user@localhost");
    }

    public void loginSuccess(String username) {
        loginPage.open();
        loginPage.login(username, "password");

        loginTotpPage.assertCurrent();

        String totpSecret = totp.generateTOTP("totpSecret");
        loginTotpPage.login(totpSecret);

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        String idTokenHint = oauth.doAccessTokenRequest(code, "password").getIdToken();
        appPage.logout(idTokenHint);
        events.clear();
    }

    public void loginWithTotpFailure() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        loginTotpPage.assertCurrent();

        loginTotpPage.login("123456");
        loginTotpPage.assertCurrent();
        Assert.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());
        events.clear();
    }

    public void continueLoginWithTotp() {
        loginTotpPage.assertCurrent();

        String totpSecret = totp.generateTOTP("totpSecret");
        loginTotpPage.login(totpSecret);

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        String idTokenHint = oauth.doAccessTokenRequest(code, "password").getIdToken();
        appPage.logout(idTokenHint);
        events.clear();
    }

    public void continueLoginWithCorrectTotpExpectFailure() {
        loginTotpPage.assertCurrent();

        String totpSecret = totp.generateTOTP("totpSecret");
        loginTotpPage.login(totpSecret);

        loginTotpPage.assertCurrent();
        Assert.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());

        events.clear();
    }

    public void continueLoginWithInvalidTotp() {
        loginTotpPage.assertCurrent();

        loginTotpPage.login("123456");

        loginTotpPage.assertCurrent();
        Assert.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());
    }

    public void continueLoginWithMissingTotp() {
        loginTotpPage.assertCurrent();

        loginTotpPage.login(null);

        loginTotpPage.assertCurrent();
        Assert.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());
        events.clear();
    }

    public void loginWithMissingTotp() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        loginTotpPage.assertCurrent();

        loginTotpPage.login(null);
        loginTotpPage.assertCurrent();
        Assert.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());

        events.clear();
    }

    public void loginInvalidPassword() {
        loginInvalidPassword("test-user@localhost");
    }

    public void loginInvalidPassword(String username) {
        loginInvalidPassword(username, true);
    }

    public void loginInvalidPassword(String username, boolean clearEventsQueue) {
        loginPage.open();
        loginPage.login(username, "invalid");

        loginPage.assertCurrent();

        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());

        if (clearEventsQueue) {
            events.clear();
        }
    }

    public void loginMissingPassword() {
        loginPage.open();
        loginPage.missingPassword("test-user@localhost");

        loginPage.assertCurrent();

        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());
        events.clear();
    }

    public void registerUser(String username) {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("user", "name", username + "@localhost", username, "password", "password");

        Assert.assertNull(registerPage.getInstruction());

        events.clear();
    }

    private void assertUserDisabledEvent(String error) {
        events.expect(EventType.LOGIN_ERROR).error(error).assertEvent();
    }

    private void assertUserPermanentlyDisabledEvent() {
        events.expect(EventType.LOGIN_ERROR).error(Errors.USER_DISABLED).assertEvent();
    }

    private void assertUserDisabledReason(String expected) {
        String actual = adminClient.realm("test").users()
                .search("test-user@localhost", 0, 1)
                .get(0)
                .firstAttribute(UserModel.DISABLED_REASON);
        assertEquals(expected, actual);
    }

    private void assertUserNumberOfFailures(String userId, Integer numberOfFailures) {
        Map<String, Object> userAttackInfo = adminClient.realm("test").attackDetection().bruteForceUserStatus(userId);
        MatcherAssert.assertThat((Integer) userAttackInfo.get("numFailures"), is(numberOfFailures));
    }

    private void sendInvalidPasswordPasswordGrant() throws Exception {
        String totpSecret = totp.generateTOTP("totpSecret");
        OAuthClient.AccessTokenResponse response = getTestToken("invalid", totpSecret);
        Assert.assertNull(response.getAccessToken());
        Assert.assertEquals(response.getError(), "invalid_grant");
        Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");
        events.clear();
    }

    private void lockUserWithPasswordGrant() throws Exception {
        String totpSecret = totp.generateTOTP("totpSecret");
        OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
        Assert.assertNotNull(response.getAccessToken());
        Assert.assertNull(response.getError());
        events.clear();

        for (int i = 0; i < failureFactor; ++i) {
            sendInvalidPasswordPasswordGrant();
        }
    }

    /**
     * Verifies the given {@link ExpectedEvent} is "contained" in the collection of actual events. An
     * {@link ExpectedEvent expectedEvent} object is considered equal to a
     * {@link EventRepresentation eventRepresentation} object if {@code
     * expectedEvent.assertEvent(eventRepresentation)} does not throw any {@link AssertionError}.
     *
     * @param expectedEvent the expected event
     * @param actualEvents the collection of {@link EventRepresentation}
     */
    public void assertIsContained(ExpectedEvent expectedEvent, List<? extends EventRepresentation> actualEvents) {
        List<String> messages = new ArrayList<>();
        for (EventRepresentation e : actualEvents) {
            try {
                expectedEvent.assertEvent(e);
                return;
            } catch (AssertionError error) {
                // silently fail
                messages.add(error.getMessage());
            }
        }
        Assert.fail(String.format("Expected event not found. Possible reasons are: %s", messages));
    }
}
