/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console.realm;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.console.page.realm.BruteForceDetection;
import org.keycloak.testsuite.console.page.users.UserAttributes;
import org.keycloak.testsuite.console.page.users.Users;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.junit.Assert.*;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.*;

/**
 * @author Filip Kiss
 * @author mhajas
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
public class SecurityDefensesTest extends AbstractRealmTest {
    
    public static final String INVALID_PWD_MSG = "Invalid username or password.";
    public static final String ACC_DISABLED_MSG = "Invalid username or password.";
    public static final short ATTEMPTS_BAD_PWD = 2;
    public static final short ATTEMPTS_GOOD_PWD = 1;

    @Page
    private BruteForceDetection bruteForceDetectionPage;

    @Page
    private Account testRealmAccountPage;

    @Page
    private Users usersPage;

    @Page
    private UserAttributes userAttributesPage;

    @FindBy(className = "kc-feedback-text")
    private WebElement feedbackTextElement;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmAccountPage.setAuthRealm(TEST);
    }

    @Before
    public void beforeSecurityDefensesTest() {
        bruteForceDetectionPage.navigateTo();
    }

    @Test
    public void maxLoginFailuresTest() throws InterruptedException {
        final short secondsToWait = 30; // For slower browsers/webdrivers (like IE) we need higher value
        final short maxLoginFailures = 2;

        bruteForceDetectionPage.form().setProtectionEnabled(true);
        bruteForceDetectionPage.form().setMaxLoginFailures(String.valueOf(maxLoginFailures));
        bruteForceDetectionPage.form().setWaitIncrementSelect(BruteForceDetection.TimeSelectValues.SECONDS);
        bruteForceDetectionPage.form().setWaitIncrementInput(String.valueOf(secondsToWait));
        bruteForceDetectionPage.form().setQuickLoginCheckInput("1");
        bruteForceDetectionPage.form().save();
        assertAlertSuccess();

        tryToLogin(secondsToWait * (ATTEMPTS_BAD_PWD / maxLoginFailures));
    }

    @Test
    public void quickLoginCheck() throws InterruptedException {
        final short secondsToWait = 30;

        bruteForceDetectionPage.form().setProtectionEnabled(true);
        bruteForceDetectionPage.form().setMaxLoginFailures("100");
        bruteForceDetectionPage.form().setQuickLoginCheckInput("30000"); // IE is very slow
        bruteForceDetectionPage.form().setMinQuickLoginWaitSelect(BruteForceDetection.TimeSelectValues.SECONDS);
        bruteForceDetectionPage.form().setMinQuickLoginWaitInput(String.valueOf(secondsToWait));
        bruteForceDetectionPage.form().save();
        assertAlertSuccess();

        tryToLogin(secondsToWait);
    }

    @Test
    public void maxWaitLoginFailures() throws InterruptedException {
        final short secondsToWait = 40;

        bruteForceDetectionPage.form().setProtectionEnabled(true);
        bruteForceDetectionPage.form().setMaxLoginFailures("1");
        bruteForceDetectionPage.form().setWaitIncrementSelect(BruteForceDetection.TimeSelectValues.MINUTES);
        bruteForceDetectionPage.form().setWaitIncrementInput("30");
        bruteForceDetectionPage.form().setMaxWaitSelect(BruteForceDetection.TimeSelectValues.SECONDS);
        bruteForceDetectionPage.form().setMaxWaitInput(String.valueOf(secondsToWait));
        bruteForceDetectionPage.form().save();

        tryToLogin(secondsToWait);
    }

    @Test
    public void failureResetTime() {
        final short failureResetTime = 10;
        final short waitIncrement = 30;
        final short maxFailures = 2;

        bruteForceDetectionPage.form().setProtectionEnabled(true);
        bruteForceDetectionPage.form().setMaxLoginFailures(String.valueOf(maxFailures));
        bruteForceDetectionPage.form().setWaitIncrementSelect(BruteForceDetection.TimeSelectValues.SECONDS);
        bruteForceDetectionPage.form().setWaitIncrementInput(String.valueOf(waitIncrement));
        bruteForceDetectionPage.form().setFailureResetTimeSelect(BruteForceDetection.TimeSelectValues.SECONDS);
        bruteForceDetectionPage.form().setFailureResetTimeInput(String.valueOf(failureResetTime));
        bruteForceDetectionPage.form().save();
        assertAlertSuccess();

        testRealmAccountPage.navigateTo();
        testRealmLoginPage.form().login(testUser.getUsername(), PASSWORD + "-mismatch");
        pause(failureResetTime * 1000);
        testRealmLoginPage.form().login(testUser.getUsername(), PASSWORD + "-mismatch");
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountPage);
    }

    @Test
    public void userUnlockTest() {
        bruteForceDetectionPage.form().setProtectionEnabled(true);
        bruteForceDetectionPage.form().setMaxLoginFailures("1");
        bruteForceDetectionPage.form().setWaitIncrementSelect(BruteForceDetection.TimeSelectValues.MINUTES);
        bruteForceDetectionPage.form().setWaitIncrementInput("10");
        bruteForceDetectionPage.form().save();
        assertAlertSuccess();

        testRealmAccountPage.navigateTo();

        setPasswordFor(testUser, PASSWORD + "-mismatch");

        testRealmLoginPage.form().login(testUser);

        usersPage.navigateTo();

        usersPage.table().searchUsers(testUser.getUsername());
        usersPage.table().editUser(testUser.getUsername());
        assertFalse(userAttributesPage.form().isEnabled());
        userAttributesPage.form().unlockUser();

        testRealmAccountPage.navigateTo();

        setPasswordFor(testUser, PASSWORD);

        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountPage);
    }

    private void assertFeedbackText(String text) {
        waitUntilElement(feedbackTextElement).is().present();
        assertEquals(text, getTextFromElement(feedbackTextElement));
    }

    private void tryToLogin(int wait) throws InterruptedException {
        tryToLogin(wait, true);
    }

    private void tryToLogin(int wait, boolean finalLogin) throws InterruptedException {
        testRealmAccountPage.navigateTo();

        setPasswordFor(testUser, PASSWORD + "-mismatch");

        for (int i = 0; i < ATTEMPTS_BAD_PWD; i++) {
            testRealmLoginPage.form().login(testUser);
            assertFeedbackText(INVALID_PWD_MSG);
        }

        setPasswordFor(testUser, PASSWORD);
        for (int i = 0; i < ATTEMPTS_GOOD_PWD; i++) {
            testRealmLoginPage.form().login(testUser);
            assertFeedbackText(ACC_DISABLED_MSG);
        }

        wait *= 1000;

        pause(wait);

        if (finalLogin) {
            testRealmLoginPage.form().login(testUser);
            assertCurrentUrlStartsWith(testRealmAccountPage);
        }
    }
}
