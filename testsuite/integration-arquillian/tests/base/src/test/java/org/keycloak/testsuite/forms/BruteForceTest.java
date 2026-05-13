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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.BadRequestException;

import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.events.email.EmailEventListenerProviderFactory;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AssertEvents.ExpectedEvent;
import org.keycloak.testsuite.model.infinispan.InfinispanTestUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.MailServer;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.RealmRepUtil;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class BruteForceTest extends AbstractChangeImportedUserPasswordsTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public MailServer mail = new MailServer();

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

    private static final Integer failureFactor = 2;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        UserRepresentation user = RealmRepUtil.findUser(testRealm, "test-user@localhost");
        UserBuilder.update(user).totpSecret("totpSecret").emailVerified(true);

        testRealm.setBruteForceProtected(true);
        testRealm.setBruteForceStrategy(RealmRepresentation.BruteForceStrategy.MULTIPLE);
        testRealm.setFailureFactor(failureFactor);
        testRealm.setMaxDeltaTimeSeconds(60);
        testRealm.setMaxFailureWaitSeconds(100);
        testRealm.setWaitIncrementSeconds(20);
        testRealm.setOtpPolicyCodeReusable(true);
        testRealm.setMaxSecondaryAuthFailures(10);

        RealmRepUtil.findClientByClientId(testRealm, "test-app").setDirectAccessGrantsEnabled(true);
        testRealm.getUsers().add(UserBuilder.create().username("user2").email("user2@localhost").password(generatePassword("user2")).build());
    }

    @Before
    public void config() {
        try {
            testingClient.server().run(InfinispanTestUtil::setTestingTimeService);
            clearUserFailures();
            clearAllUserFailures();
            RealmRepresentation realm = adminClient.realm("test").toRepresentation();
            realm.setBruteForceStrategy(RealmRepresentation.BruteForceStrategy.MULTIPLE);
            realm.setFailureFactor(failureFactor);
            realm.setMaxDeltaTimeSeconds(60);
            realm.setMaxFailureWaitSeconds(100);
            realm.setWaitIncrementSeconds(20);
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
            testingClient.server().run(InfinispanTestUtil::revertTimeService);
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

    public String getAdminToken() {
        return oauth.realm("master").client(Constants.ADMIN_CLI_CLIENT_ID).doPasswordGrantRequest( "admin", "admin").getAccessToken();
    }

    public AccessTokenResponse getTestToken(String password, String totp) {
        return oauth.passwordGrantRequest("test-user@localhost", password).otp(totp).send();
    }

    protected void clearUserFailures() {
        adminClient.realm("test").attackDetection().clearBruteForceForUser(findUser("test-user@localhost").getId());
    }

    protected void clearAllUserFailures() {
        adminClient.realm("test").attackDetection().clearAllBruteForce();
    }
    
    @Test
    public void testInvalidConfiguration() {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setFailureFactor(-1);
        try {
            managedRealm.admin().update(realm);
        } catch (BadRequestException ex) {
            ErrorRepresentation error = ex.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Failure factor may not be a negative value", error.getErrorMessage());
        }

        realm = managedRealm.admin().toRepresentation();
        realm.setMaxTemporaryLockouts(-1);
        try {
            managedRealm.admin().update(realm);
        }  catch (BadRequestException ex) {
            ErrorRepresentation error = ex.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Maximum temporary lockouts may not be a negative value", error.getErrorMessage());
        }

        realm = managedRealm.admin().toRepresentation();
        realm.setMaxFailureWaitSeconds(-1);
        try {
            managedRealm.admin().update(realm);
        }  catch (BadRequestException ex) {
            ErrorRepresentation error = ex.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Maximum failure wait seconds may not be a negative value", error.getErrorMessage());
        }

        realm = managedRealm.admin().toRepresentation();
        realm.setWaitIncrementSeconds(-1);
        try {
            managedRealm.admin().update(realm);
        }   catch (BadRequestException ex) {
            ErrorRepresentation error = ex.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Wait increment seconds may not be a negative value", error.getErrorMessage());
        }

        realm = managedRealm.admin().toRepresentation();
        realm.setMinimumQuickLoginWaitSeconds(-1);
        try {
            managedRealm.admin().update(realm);
        } catch (BadRequestException ex) {
            ErrorRepresentation error = ex.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Minimum quick login wait seconds may not be a negative value", error.getErrorMessage());
        }

        realm = managedRealm.admin().toRepresentation();
        realm.setQuickLoginCheckMilliSeconds(-1L);
        try {
            managedRealm.admin().update(realm);
        }  catch (BadRequestException ex) {
            ErrorRepresentation error = ex.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Quick login check milliseconds may not be a negative value", error.getErrorMessage());
        }

        realm = managedRealm.admin().toRepresentation();
        realm.setMaxDeltaTimeSeconds(-1);
        try {
            managedRealm.admin().update(realm);
        }   catch (BadRequestException ex) {
            ErrorRepresentation error = ex.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Maximum delta time seconds may not be a negative value",  error.getErrorMessage());
        }
    }

    @Test
    public void testGrantInvalidPassword() {
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), totpSecret);
            Assertions.assertNotNull(response.getAccessToken());
            Assertions.assertNull(response.getError());
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            AccessTokenResponse response = getTestToken("invalid" + getPassword("test-user@localhost"), totpSecret);
            Assertions.assertNull(response.getAccessToken());
            Assertions.assertEquals(response.getError(), "invalid_grant");
            Assertions.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            AccessTokenResponse response = getTestToken("invalid" + getPassword("test-user@localhost"), totpSecret);
            Assertions.assertNull(response.getAccessToken());
            Assertions.assertEquals(response.getError(), "invalid_grant");
            Assertions.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), totpSecret);
            Assertions.assertNull(response.getAccessToken());
            Assertions.assertNotNull(response.getError());
            Assertions.assertEquals("invalid_grant", response.getError());
            Assertions.assertEquals("Invalid user credentials", response.getErrorDescription());
            assertUserDisabledEvent(Errors.USER_TEMPORARILY_DISABLED);
            events.clear();
        }
        clearUserFailures();
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), totpSecret);
            Assertions.assertNotNull(response.getAccessToken());
            Assertions.assertNull(response.getError());
            events.clear();
        }

    }

    @Test
    public void testGrantInvalidOtp() {
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), totpSecret);
            Assertions.assertNotNull(response.getAccessToken());
            Assertions.assertNull(response.getError());
            events.clear();
        }
        {
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), "shite");
            Assertions.assertNull(response.getAccessToken());
            Assertions.assertEquals(response.getError(), "invalid_grant");
            Assertions.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), "shite");
            Assertions.assertNull(response.getAccessToken());
            Assertions.assertEquals(response.getError(), "invalid_grant");
            Assertions.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), totpSecret);
            assertTokenNull(response);
            Assertions.assertNotNull(response.getError());
            Assertions.assertEquals(response.getError(), "invalid_grant");
            Assertions.assertEquals("Invalid user credentials", response.getErrorDescription());
            assertUserDisabledEvent(Errors.USER_TEMPORARILY_DISABLED);
            events.clear();
        }
        clearUserFailures();
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), totpSecret);
            Assertions.assertNotNull(response.getAccessToken());
            Assertions.assertNull(response.getError());
            events.clear();
        }

    }

    public void assertTokenNull(AccessTokenResponse response) {
        Assertions.assertNull(response.getAccessToken());
    }

    @Test
    public void testGrantMissingOtp() {
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), totpSecret);
            Assertions.assertNotNull(response.getAccessToken());
            Assertions.assertNull(response.getError());
            events.clear();
        }
        {
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), null);
            Assertions.assertNull(response.getAccessToken());
            Assertions.assertEquals(response.getError(), "invalid_grant");
            Assertions.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), null);
            Assertions.assertNull(response.getAccessToken());
            Assertions.assertEquals(response.getError(), "invalid_grant");
            Assertions.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), totpSecret);
            assertTokenNull(response);
            Assertions.assertNotNull(response.getError());
            Assertions.assertEquals(response.getError(), "invalid_grant");
            Assertions.assertEquals("Invalid user credentials", response.getErrorDescription());
            assertUserDisabledEvent(Errors.USER_TEMPORARILY_DISABLED);
            events.clear();
        }
        clearUserFailures();
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), totpSecret);
            Assertions.assertNotNull(response.getAccessToken());
            Assertions.assertNull(response.getError());
            events.clear();
        }

    }

    @Test
    public void testGrantPermamentOtpAbsolut() throws Exception {
        try (RealmAttributeUpdater updater = new RealmAttributeUpdater(managedRealm.admin())
                .setFailureFactor(120) // more than 100
                .setQuickLoginCheckMilliSeconds(0L) // allow fail OTP
                .update()) {
            // assert that brute force is checking for secondary auth failures
            Assertions.assertTrue(managedRealm.admin().toRepresentation().getMaxSecondaryAuthFailures() > 0);
            { // successful login
                String totpSecret = totp.generateTOTP("totpSecret");
                AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), totpSecret);
                Assertions.assertNotNull(response.getAccessToken());
                Assertions.assertNull(response.getError());
                events.clear();
            }
            for (int i = 0; i <= managedRealm.admin().toRepresentation().getMaxSecondaryAuthFailures(); i++) {
                AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), null);
                Assertions.assertNull(response.getAccessToken());
                Assertions.assertEquals("invalid_grant", response.getError());
                Assertions.assertEquals("Invalid user credentials", response.getErrorDescription());
                events.clear();
            }
            {
                String totpSecret = totp.generateTOTP("totpSecret");
                AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), totpSecret);
                assertTokenNull(response);
                Assertions.assertNotNull(response.getError());
                Assertions.assertEquals("invalid_grant", response.getError());
                Assertions.assertEquals("Invalid user credentials", response.getErrorDescription());

                assertUserDisabledReason(BruteForceProtector.DISABLED_BY_PERMANENT_LOCKOUT);
            }
        } finally {
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            updateUser(user);
        }
    }


    @Test
    public void testNumberOfFailuresForDisabledUsersWithPasswordGrantType() {
        try {
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            assertUserNumberOfFailures(user.getId(), 0);
            user.setEnabled(false);
            updateUser(user);

            // Wrong password on disabled user should return "Invalid user credentials" (not reveal disabled status)
            AccessTokenResponse response = getTestToken("invalid", "invalid");
            Assertions.assertNull(response.getAccessToken());
            Assertions.assertEquals(response.getError(), "invalid_grant");
            Assertions.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();

            assertUserNumberOfFailures(user.getId(), 0);

            // Correct password on disabled user should return "Account disabled"
            String totpSecret = totp.generateTOTP("totpSecret");
            response = getTestToken(getPassword("test-user@localhost"), totpSecret);
            Assertions.assertNull(response.getAccessToken());
            Assertions.assertEquals(response.getError(), "invalid_grant");
            Assertions.assertEquals(response.getErrorDescription(), "Account disabled");
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
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        try {
            // Set permanent lockout for the test
            realm.setPermanentLockout(true);
            managedRealm.admin().update(realm);

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
            managedRealm.admin().update(realm);
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            updateUser(user);
        }
    }

    @Test
    public void testBrowserInvalidPassword() {
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
    public void testFailureResetForTemporaryLockout() {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        try {
            realm.setMaxDeltaTimeSeconds(5);
            managedRealm.admin().update(realm);
            loginInvalidPassword();

            //Wait for brute force executor to process the login and then wait for delta time
            WaitUtils.waitForBruteForceExecutors(testingClient);
            timeOffSet.set(5);

            loginInvalidPassword();
            loginSuccess();
        } finally {
            realm.setMaxDeltaTimeSeconds(20);
            managedRealm.admin().update(realm);
        }
    }

    @Test
    public void testCacheExpiryForTemporaryLockout() {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        loginInvalidPassword();

        //Wait for brute force executor to process the login and then wait for delta time
        WaitUtils.waitForBruteForceExecutors(testingClient);
        timeOffSet.set(realm.getMaxDeltaTimeSeconds());

        String realmId = realm.getId();
        testingClient.server().run(session -> {
            RealmModel realmModel = session.realms().getRealm(realmId);
            UserModel userModel = session.users().getUserByEmail(realmModel, "test-user@localhost");
            UserLoginFailureModel userLoginFailure = session.loginFailures().getUserLoginFailure(realmModel, userModel.getId());
            Assertions.assertNull(userLoginFailure, "cache entry should have expired");
        });
    }

    @Test
    public void testNoFailureResetForPermanentLockout() {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        try {
            realm.setMaxDeltaTimeSeconds(5);
            realm.setPermanentLockout(true);
            managedRealm.admin().update(realm);
            loginInvalidPassword();

            //Wait for brute force executor to process the login and then wait for delta time
            WaitUtils.waitForBruteForceExecutors(testingClient);
            timeOffSet.set(5);

            loginInvalidPassword();
            expectPermanentlyDisabled();
        } finally {
            realm.setPermanentLockout(false);
            realm.setMaxDeltaTimeSeconds(20);
            managedRealm.admin().update(realm);
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            updateUser(user);
        }
    }

    @Test
    public void testWait() {
        loginSuccess();
        loginInvalidPassword();
        loginInvalidPassword();
        expectTemporarilyDisabled();
        expectTemporarilyDisabled("test-user@localhost", null, "invalid");

        // KEYCLOAK-5420
        // Test to make sure that temporarily disabled doesn't increment failure count
        timeOffSet.set(21);
        // should be unlocked now
        loginSuccess();
        clearUserFailures();
        clearAllUserFailures();
        loginSuccess();
    }

    // Issue 30939
    @Test
    public void testNoOverflowDuringBruteForceCalculation() throws Exception {
        int waitTime = Integer.MAX_VALUE - 172800; // Max int value without 2 days

        try (RealmAttributeUpdater updater = new RealmAttributeUpdater(managedRealm.admin())
                .setWaitIncrementSeconds(waitTime)
                .setMaxFailureWaitSeconds(waitTime)
                .setMaxDeltaTimeSeconds(900) // 15 minutes
                .update()) {
            loginInvalidPassword("test-user@localhost");
            loginInvalidPassword("test-user@localhost");
            expectTemporarilyDisabled();
        }
    }

    @Test
    public void testByMultipleStrategy() {

        try {
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            loginSuccess();
            loginInvalidPassword();
            loginInvalidPassword();
            expectTemporarilyDisabled();
            assertUserNumberOfFailures(user.getId(), 2);
            timeOffSet.set(30);

            loginInvalidPassword();
            assertUserNumberOfFailures(user.getId(), 3);
            timeOffSet.set(60);
            loginSuccess();
        } finally {
            timeOffSet.set(0);
        }
    }

    @Test
    public void testLinearStrategy() {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
        try {
            realm.setBruteForceStrategy(RealmRepresentation.BruteForceStrategy.LINEAR);
            managedRealm.admin().update(realm);

            loginSuccess();

            loginInvalidPassword();
            loginInvalidPassword();
            expectTemporarilyDisabled();
            assertUserNumberOfFailures(user.getId(), 2);
            timeOffSet.set(30);

            loginInvalidPassword();
            assertUserNumberOfFailures(user.getId(), 3);
            timeOffSet.set(60);
            expectTemporarilyDisabled();

        } finally {
            realm.setPermanentLockout(false);
            realm.setBruteForceStrategy(RealmRepresentation.BruteForceStrategy.MULTIPLE);
            managedRealm.admin().update(realm);
            timeOffSet.set(0);
        }
    }

    @Test
    public void testBrowserInvalidPasswordDifferentCase() {
        loginSuccess("test-user@localhost");
        loginInvalidPassword("test-User@localhost");
        loginInvalidPassword("Test-user@localhost");
        expectTemporarilyDisabled();
        expectTemporarilyDisabled("test-user@localhost", null, "invalid");
        clearAllUserFailures();
    }

    @Test
    public void testEmail() {
        String userId = adminClient.realm("test").users().search("user2", null, null, null, 0, 1).get(0).getId();

        loginInvalidPassword("user2");
        loginInvalidPassword("user2");
        expectTemporarilyDisabled("user2", userId);
        clearAllUserFailures();
    }
    
    @Test
    public void testUserDisabledTemporaryLockout() {
        String userId = adminClient.realm("test").users().search("test-user@localhost", null, null, null, 0, 1).get(0).getId();

        loginInvalidPassword();
        loginInvalidPassword();
        expectTemporarilyDisabled();

        assertTrue(managedRealm.admin().users().get(userId).toRepresentation().isEnabled());
        assertTrue(managedRealm.admin().users().search("test-user@localhost", true).get(0).isEnabled());
        assertEquals(Boolean.TRUE, managedRealm.admin().attackDetection().bruteForceUserStatus(userId).get("disabled"));
    }

    @Test
    public void testUserDisabledAfterSwitchFromMixedToPermanentLockout() {
        UsersResource users = managedRealm.admin().users();
        UserRepresentation user = users.search("test-user@localhost", null, null, null, 0, 1).get(0);

        // temporarily lockout
        loginInvalidPassword();
        loginInvalidPassword();
        expectTemporarilyDisabled();
        assertUserNumberOfFailures(user.getId(), 2);
        // user is still enabled during temporary lockout
        assertTrue(users.get(user.getId()).toRepresentation().isEnabled());
        assertTrue(users.search("test-user@localhost", true).get(0).isEnabled());
        assertEquals(Boolean.TRUE, managedRealm.admin().attackDetection().bruteForceUserStatus(user.getId()).get("disabled"));

        RealmRepresentation realm = managedRealm.admin().toRepresentation();

        try {
            // switch to permanent lockout before waiting to successful login
            realm.setPermanentLockout(true);
            managedRealm.admin().update(realm);

            // expires the temporary lockout
            timeOffSet.set(60);

            // after switching to permanent lockout the user status is disabled because there are login failures
            // the user did not try to successfully authenticate yet to clear the login failures
            user = users.get(user.getId()).toRepresentation();
            assertFalse(user.isEnabled());
            assertFalse(users.search("test-user@localhost", true).get(0).isEnabled());
            assertEquals(Boolean.TRUE, managedRealm.admin().attackDetection().bruteForceUserStatus(user.getId()).get("disabled"));
            expectPermanentlyDisabled();

            // attempt to re-enable the user and login successfully
            user.setEnabled(true);
            users.get(user.getId()).update(user);
            user = users.get(user.getId()).toRepresentation();
            assertTrue(user.isEnabled());
            assertTrue(users.search("test-user@localhost", true).get(0).isEnabled());
            Map<String, Object> userAttackInfo = managedRealm.admin().attackDetection().bruteForceUserStatus(user.getId());
            assertEquals(Boolean.FALSE, userAttackInfo.get("disabled"));
            assertThat((Integer) userAttackInfo.get("numFailures"), is(0));
            // login failures should be removed after re-enabling the user and the user able to authenticate
            loginSuccess();
        } finally {
            timeOffSet.set(0);
            realm.setPermanentLockout(false);
            managedRealm.admin().update(realm);
        }
    }

    @Test
    public void testBrowserMissingPassword() {
        loginSuccess();
        loginMissingPassword();
        loginMissingPassword();
        loginSuccess();
    }

    @Test
    public void testBrowserInvalidTotp() {
        loginSuccess();
        loginInvalidPassword();
        loginWithTotpFailure();
        continueLoginWithCorrectTotpExpectFailure();
    }

    @Test
    public void testBrowserInvalidTotpAbsolut() {
        // assert that brute force is checking for secondary auth failures
        Assertions.assertTrue(managedRealm.admin().toRepresentation().getMaxSecondaryAuthFailures() > 0);
        loginWithTotpFailure();
        for (int i = 0; i < managedRealm.admin().toRepresentation().getMaxSecondaryAuthFailures(); i++ ) {
            continueLoginWithInvalidTotp();
        }
        continueLoginWithCorrectTotpExpectFailure();
    }

    @Test
    public void testBrowserMissingTotp() {
        loginSuccess();
        loginWithMissingTotp();
        loginWithMissingTotp();
        continueLoginWithMissingTotp();
    }

    @Test
    public void testBrowserTotpSessionInvalidAfterLockout() {
        long start = System.currentTimeMillis();
        loginWithTotpFailure();
        continueLoginWithInvalidTotp();
        continueLoginWithInvalidTotp();
        events.clear();
        continueLoginWithInvalidTotp();
        assertUserDisabledEvent(Errors.USER_TEMPORARILY_DISABLED);
    }

    private void checkEmailPresent(String subject) {
        Assertions.assertFalse(Arrays.stream(mail.getReceivedMessages()).filter(m -> {
            try {
                return subject.equals(m.getSubject());
            } catch (MessagingException ex) {
                return false;
            }
        }).findAny().isEmpty(), "No email with subject: " + subject);
    }

    @Test
    public void testPermanentLockout() throws Exception {
        testingClient.testing().addEventsToEmailEventListenerProvider(Collections.singletonList(EventType.USER_DISABLED_BY_PERMANENT_LOCKOUT));
        try (RealmAttributeUpdater updater = new RealmAttributeUpdater(managedRealm.admin()).setPermanentLockout(true)
                .setQuickLoginCheckMilliSeconds(0L)
                .addEventsListener(EmailEventListenerProviderFactory.ID).update()) {
            // act
            loginInvalidPassword("test-user@localhost");
            loginInvalidPassword("test-user@localhost", false);

            // As of now, there are two events: USER_DISABLED_BY_PERMANENT_LOCKOUT and LOGIN_ERROR but Order is not
            // guarantee though since the brute force detector is running separately "in its own thread" named
            // "Brute Force Protector".
            List<EventRepresentation> actualEvents = Arrays.asList(events.poll(), events.poll(5));
            assertIsContained(events.expect(EventType.USER_DISABLED_BY_PERMANENT_LOCKOUT).client((String) null).detail(Details.REASON, "brute_force_attack detected"), actualEvents);
            assertIsContained(events.expect(EventType.LOGIN_ERROR).error(Errors.INVALID_USER_CREDENTIALS), actualEvents);

            checkEmailPresent("User disabled by permanent lockout");

            // assert
            expectPermanentlyDisabled();

            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            assertFalse(user.isEnabled());
            assertUserDisabledReason(BruteForceProtector.DISABLED_BY_PERMANENT_LOCKOUT);

            user.setEnabled(true);
            updateUser(user);
            assertUserDisabledReason(null);
        } finally {
            testingClient.testing().removeEventsToEmailEventListenerProvider(Collections.singletonList(EventType.USER_DISABLED_BY_PERMANENT_LOCKOUT));
            UserRepresentation user = managedRealm.admin().users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            updateUser(user);
        }
    }

    // https://github.com/keycloak/keycloak/issues/30969
    @Test
    public void testPermanentLockoutWithTempLockoutParamsSet()
    {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        try {
            // arrange
            realm.setWaitIncrementSeconds(0);
            realm.setPermanentLockout(true);
            realm.setMaxTemporaryLockouts(0);
            realm.setQuickLoginCheckMilliSeconds(0L);
            managedRealm.admin().update(realm);

            // act
            loginInvalidPassword("test-user@localhost");
            loginInvalidPassword("test-user@localhost", false);

            // As of now, there are two events: USER_DISABLED_BY_PERMANENT_LOCKOUT and LOGIN_ERROR but Order is not
            // guarantee though since the brute force detector is running separately "in its own thread" named
            // "Brute Force Protector".
            List<EventRepresentation> actualEvents = Arrays.asList(events.poll(), events.poll(5));
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
            managedRealm.admin().update(realm);
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            updateUser(user);
        }
    }

    @Test
    public void testTemporaryLockout() throws Exception {
        testingClient.testing().addEventsToEmailEventListenerProvider(Collections.singletonList(EventType.USER_DISABLED_BY_TEMPORARY_LOCKOUT));
        try (RealmAttributeUpdater updater = new RealmAttributeUpdater(managedRealm.admin())
                .addEventsListener(EmailEventListenerProviderFactory.ID).update()) {
            loginInvalidPassword("test-user@localhost");
            loginInvalidPassword("test-user@localhost", false);

            List<EventRepresentation> actualEvents = Arrays.asList(events.poll(), events.poll(5));
            assertIsContained(events.expect(EventType.USER_DISABLED_BY_TEMPORARY_LOCKOUT).client((String) null).detail(Details.REASON, "brute_force_attack detected"), actualEvents);
            assertIsContained(events.expect(EventType.LOGIN_ERROR).error(Errors.INVALID_USER_CREDENTIALS), actualEvents);

            checkEmailPresent("User disabled by temporary lockout");
        } finally {
            testingClient.testing().removeEventsToEmailEventListenerProvider(Collections.singletonList(EventType.USER_DISABLED_BY_TEMPORARY_LOCKOUT));
        }
    }

    @Test
    public void testExceedMaxTemporaryLockouts() {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        try {
            realm.setPermanentLockout(true);
            realm.setMaxTemporaryLockouts(2);
            managedRealm.admin().update(realm);

            loginInvalidPassword();
            loginInvalidPassword();
            expectTemporarilyDisabled();
            timeOffSet.set(21);

            loginInvalidPassword();
            expectTemporarilyDisabled();
            timeOffSet.set(42);

            loginInvalidPassword();
            expectPermanentlyDisabled();
        } finally {
            realm.setPermanentLockout(false);
            realm.setMaxTemporaryLockouts(0);
            managedRealm.admin().update(realm);
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            updateUser(user);
        }
    }

    @Test
    public void testMaxTemporaryLockoutsReset() {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setPermanentLockout(true);
        realm.setMaxTemporaryLockouts(2);
        managedRealm.admin().update(realm);

        try {
            loginInvalidPassword();
            loginInvalidPassword();
            expectTemporarilyDisabled();
            timeOffSet.set(21);
            UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            Map<String, Object> status = adminClient.realm("test").attackDetection().bruteForceUserStatus(user.getId());
            assertEquals(1, status.get("numTemporaryLockouts"));
            loginSuccess();
            status = adminClient.realm("test").attackDetection().bruteForceUserStatus(user.getId());
            assertEquals(0, status.get("numTemporaryLockouts"));
        } finally {
            realm.setPermanentLockout(false);
            realm.setMaxTemporaryLockouts(0);
            managedRealm.admin().update(realm);
        }
    }

    @Test
    public void testResetLoginFailureCount() {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();

        try {
            // arrange
            realm.setPermanentLockout(true);
            managedRealm.admin().update(realm);

            // act
            loginInvalidPassword();
            loginSuccess();
            loginInvalidPassword();
            loginSuccess();

            // assert
            assertTrue(adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0).isEnabled());
        } finally {
            realm.setPermanentLockout(false);
            managedRealm.admin().update(realm);
        }
    }

    @Test
    public void testFailureCountResetWithPasswordGrantType() {
        String totpSecret = totp.generateTOTP("totpSecret");
        AccessTokenResponse response = getTestToken("invalid", totpSecret);
        Assertions.assertNull(response.getAccessToken());
        Assertions.assertEquals(response.getError(), "invalid_grant");
        Assertions.assertEquals(response.getErrorDescription(), "Invalid user credentials");

        UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
        Map<String, Object> userAttackInfo = adminClient.realm("test").attackDetection().bruteForceUserStatus(user.getId());
        assertThat((Integer) userAttackInfo.get("numFailures"), is(1));

        response = getTestToken(getPassword("test-user@localhost"), totpSecret);
        Assertions.assertNotNull(response.getAccessToken());
        Assertions.assertNull(response.getError());
        events.clear();

        userAttackInfo = adminClient.realm("test").attackDetection().bruteForceUserStatus(user.getId());
        assertThat((Integer) userAttackInfo.get("numFailures"), is(0));
    }

    @Test
    public void testNonExistingAccounts() {

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

        MimeMessage message = mail.getReceivedMessages()[0];
        String passwordResetEmailLink = MailUtils.getPasswordResetEmailLink(message);

        driver.navigate().to(passwordResetEmailLink.trim());

        assertTrue(passwordUpdatePage.isCurrent());

        UserRepresentation userRepresentation = managedRealm.admin().users().get(userId).toRepresentation();
        assertTrue(userRepresentation.isEnabled());
        Map<String, Object> bruteForceStatus = managedRealm.admin().attackDetection().bruteForceUserStatus(userId);
        assertEquals(Boolean.TRUE, bruteForceStatus.get("disabled"));

        updatePasswordPage.updatePasswords(getPassword("user2"), getPassword("user2"));

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).user(userId).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).user(userId).assertEvent();

        userRepresentation = managedRealm.admin().users().get(userId).toRepresentation();
        assertTrue(userRepresentation.isEnabled());
        bruteForceStatus = managedRealm.admin().attackDetection().bruteForceUserStatus(userId);
        assertEquals(Boolean.FALSE, bruteForceStatus.get("disabled"));

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String code = oauth.parseLoginResponse().getCode();
        String idTokenHint = oauth.doAccessTokenRequest(code).getIdToken();
        appPage.logout(idTokenHint);

        events.clear();
    }

    @Test
    public void testRaceAttackTemporaryLockout() throws Exception {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
        try {
            realm.setWaitIncrementSeconds(120);
            realm.setQuickLoginCheckMilliSeconds(120000L);
            managedRealm.admin().update(realm);
            clearUserFailures();
            clearAllUserFailures();
            user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            managedRealm.admin().users().get(user.getId()).update(user);
            String totpSecret = totp.generateTOTP("totpSecret");
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), totpSecret);
            Assertions.assertNotNull(response.getAccessToken());
            raceAttack(user);
        } finally {
            realm.setWaitIncrementSeconds(5);
            realm.setQuickLoginCheckMilliSeconds(100L);
            managedRealm.admin().update(realm);
            user.setEnabled(true);
            updateUser(user);
        }
    }

    @Test
    public void testRaceAttackPermanentLockout() throws Exception {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        UserRepresentation user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
        try {
            realm.setPermanentLockout(true);
            managedRealm.admin().update(realm);
            raceAttack(user);
            clearUserFailures();
            clearAllUserFailures();
            user = adminClient.realm("test").users().search("test-user@localhost", 0, 1).get(0);
            user.setEnabled(true);
            managedRealm.admin().users().get(user.getId()).update(user);
            String totpSecret = totp.generateTOTP("totpSecret");
            AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), totpSecret);
            Assertions.assertNotNull(response.getAccessToken());
        } finally {
            realm.setPermanentLockout(false);
            managedRealm.admin().update(realm);
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
        assertTrue(invalidCount <= 3, "Invalid count should be less than or equal 3 but was: " + invalidCount);
    }

    public class LoginThread extends Thread {

        public void run() {
            try {
                String totpSecret = totp.generateTOTP("totpSecret");
                AccessTokenResponse response = getTestToken("invalid", totpSecret);
                Assertions.assertNull(response.getAccessToken());
                Assertions.assertEquals(response.getError(), "invalid_grant");
                Assertions.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void expectTemporarilyDisabled() {
        expectTemporarilyDisabled("test-user@localhost", null, getPassword("test-user@localhost"));
    }

    public void expectTemporarilyDisabled(String username, String userId) {
        expectTemporarilyDisabled(username, userId, getPassword(username));
    }

    public void expectTemporarilyDisabled(String username, String userId, String password) {
        oauth.openLoginForm();
        loginPage.login(username, password);

        loginPage.assertCurrent();
        Assertions.assertEquals("Invalid username or password.", loginPage.getInputError());
        EventRepresentation event = EventAssertion.expectLoginError(events.poll())
                .sessionId(null)
                .error(Errors.USER_TEMPORARILY_DISABLED)
                .details(Details.USERNAME, username)
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .withoutDetails(Details.CONSENT).getEvent();
        if (userId != null) {
            Assertions.assertEquals(userId, event.getUserId());
        }
    }

    public void expectPermanentlyDisabled() {
        expectPermanentlyDisabled("test-user@localhost");
    }

    public void expectPermanentlyDisabled(String username) {
        oauth.openLoginForm();
        loginPage.login(username, getPassword(username));

        loginPage.assertCurrent();
        Assertions.assertEquals("Invalid username or password.", loginPage.getInputError());
        EventAssertion.expectLoginError(events.poll())
            .sessionId(null)
            .error(Errors.USER_DISABLED)
            .details(Details.USERNAME, username)
            .details(Details.REDIRECT_URI, oauth.getRedirectUri())
            .withoutDetails(Details.CONSENT);
        UserRepresentation user = managedRealm.admin().users().search(username, true).get(0);
        user = managedRealm.admin().users().get(user.getId()).toRepresentation();
        List<String> disabledReason = user.getAttributes().get(UserModel.DISABLED_REASON);
        assertNotNull(disabledReason);
        assertEquals(1, disabledReason.size());
        assertEquals(BruteForceProtector.DISABLED_BY_PERMANENT_LOCKOUT, disabledReason.get(0));
    }

    public void loginSuccess() {
        loginSuccess("test-user@localhost");
    }

    public void loginSuccess(String username) {
        oauth.openLoginForm();
        loginPage.login(username, getPassword(username));

        loginTotpPage.assertCurrent();

        String totpSecret = totp.generateTOTP("totpSecret");
        loginTotpPage.login(totpSecret);

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventAssertion.expectLoginSuccess(events.poll());

        String code = oauth.parseLoginResponse().getCode();
        String idTokenHint = oauth.doAccessTokenRequest(code ).getIdToken();
        appPage.logout(idTokenHint);
        events.clear();
    }

    public void loginWithTotpFailure() {
        oauth.openLoginForm();
        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

        loginTotpPage.assertCurrent();

        loginTotpPage.login("123456");
        loginTotpPage.assertCurrent();
        Assertions.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());
        events.clear();
    }

    public void continueLoginWithTotp() {
        loginTotpPage.assertCurrent();

        String totpSecret = totp.generateTOTP("totpSecret");
        loginTotpPage.login(totpSecret);

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventAssertion.expectLoginSuccess(events.poll());
        String code = oauth.parseLoginResponse().getCode();
        String idTokenHint = oauth.doAccessTokenRequest(code).getIdToken();
        appPage.logout(idTokenHint);
        events.clear();
    }

    public void continueLoginWithCorrectTotpExpectFailure() {
        loginTotpPage.assertCurrent();

        String totpSecret = totp.generateTOTP("totpSecret");
        loginTotpPage.login(totpSecret);

        loginTotpPage.assertCurrent();
        Assertions.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());

        events.clear();
    }

    public void continueLoginWithInvalidTotp() {
        loginTotpPage.assertCurrent();

        loginTotpPage.login("123456");

        loginTotpPage.assertCurrent();
        Assertions.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());
    }

    public void continueLoginWithMissingTotp() {
        loginTotpPage.assertCurrent();

        loginTotpPage.login(null);

        loginTotpPage.assertCurrent();
        Assertions.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());
        events.clear();
    }

    public void loginWithMissingTotp() {
        oauth.openLoginForm();
        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

        loginTotpPage.assertCurrent();

        loginTotpPage.login(null);
        loginTotpPage.assertCurrent();
        Assertions.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());

        events.clear();
    }

    public void loginInvalidPassword() {
        loginInvalidPassword("test-user@localhost");
    }

    public void loginInvalidPassword(String username) {
        loginInvalidPassword(username, true);
    }

    public void loginInvalidPassword(String username, boolean clearEventsQueue) {
        oauth.openLoginForm();
        loginPage.login(username, "invalid");

        loginPage.assertCurrent();

        Assertions.assertEquals("Invalid username or password.", loginPage.getInputError());

        if (clearEventsQueue) {
            events.clear();
        }
    }

    public void loginMissingPassword() {
        oauth.openLoginForm();
        loginPage.missingPassword("test-user@localhost");

        loginPage.assertCurrent();

        Assertions.assertEquals("Invalid username or password.", loginPage.getInputError());
        events.clear();
    }

    public void registerUser(String username) {
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        final String password = generatePassword("user");
        registerPage.register("user", "name", username + "@localhost", username, password, password);

        Assertions.assertNull(registerPage.getInstruction());

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

    private void sendInvalidPasswordPasswordGrant() {
        String totpSecret = totp.generateTOTP("totpSecret");
        AccessTokenResponse response = getTestToken("invalid", totpSecret);
        Assertions.assertNull(response.getAccessToken());
        Assertions.assertEquals(response.getError(), "invalid_grant");
        Assertions.assertEquals(response.getErrorDescription(), "Invalid user credentials");
        events.clear();
    }

    private void lockUserWithPasswordGrant() {
        String totpSecret = totp.generateTOTP("totpSecret");
        AccessTokenResponse response = getTestToken(getPassword("test-user@localhost"), totpSecret);
        Assertions.assertNotNull(response.getAccessToken());
        Assertions.assertNull(response.getError());
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
        Assertions.fail(String.format("Expected event not found. Possible reasons are: %s", messages));
    }
}
