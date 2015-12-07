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
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.console.page.realm.BruteForceDetection;
import org.keycloak.testsuite.console.page.users.UserAttributes;
import org.keycloak.testsuite.console.page.users.Users;
import org.openqa.selenium.By;

import java.util.Date;

import static org.jboss.arquillian.graphene.Graphene.waitGui;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 * @author Filip Kiss
 * @author mhajas
 */
@Ignore
public class SecurityDefensesTest extends AbstractRealmTest {

    @Page
    private BruteForceDetection bruteForceDetectionPage;

    @Page
    private Account testRealmAccountPage;

    @Page
    private Users usersPage;

    @Page
    private UserAttributes userAttributesPage;

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
    public void maxLoginFailuresTest() {
        int secondsToWait = 3;

        bruteForceDetectionPage.form().setProtectionEnabled(true);
        bruteForceDetectionPage.form().setMaxLoginFailures("1");
        bruteForceDetectionPage.form().setWaitIncrementSelect(BruteForceDetection.TimeSelectValues.SECONDS);
        bruteForceDetectionPage.form().setWaitIncrementInput(String.valueOf(secondsToWait));
        bruteForceDetectionPage.form().save();
        assertAlertSuccess();

        testRealmAccountPage.navigateTo();

        setPasswordFor(testUser, PASSWORD + "-mismatch");

        testRealmLoginPage.form().login(testUser);
        waitForFeedbackText("Invalid username or password.");
        Date endTime = new Date(new Date().getTime() + secondsToWait * 1000);

        testRealmLoginPage.form().login(testUser);
        waitGui().until().element(By.className("instruction"))
                .text().contains("Account is temporarily disabled, contact admin or try again later.");
        endTime = new Date(endTime.getTime() + secondsToWait * 1000);
        testRealmAccountPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        endTime = new Date(endTime.getTime() + secondsToWait * 1000);

        while (new Date().compareTo(endTime) < 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        setPasswordFor(testUser, PASSWORD);
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountPage);
    }

    @Test
    public void quickLoginCheck() {
        int secondsToWait = 3;

        bruteForceDetectionPage.form().setProtectionEnabled(true);
        bruteForceDetectionPage.form().setMaxLoginFailures("100");
        bruteForceDetectionPage.form().setQuickLoginCheckInput("1500");
        bruteForceDetectionPage.form().setMinQuickLoginWaitSelect(BruteForceDetection.TimeSelectValues.SECONDS);
        bruteForceDetectionPage.form().setMinQuickLoginWaitInput(String.valueOf(secondsToWait));
        bruteForceDetectionPage.form().save();
        assertAlertSuccess();

        testRealmAccountPage.navigateTo();

        setPasswordFor(testUser, PASSWORD + "-mismatch");

        testRealmLoginPage.form().login(testUser);
        testRealmLoginPage.form().login(testUser);
        Date endTime = new Date(new Date().getTime() + secondsToWait * 1000);
        testRealmLoginPage.form().login(testUser);
        waitGui().until().element(By.className("instruction"))
                .text().contains("Account is temporarily disabled, contact admin or try again later.");
        endTime = new Date(endTime.getTime() + secondsToWait * 1000);

        testRealmAccountPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        endTime = new Date(endTime.getTime() + secondsToWait * 1000);

        while (new Date().compareTo(endTime) < 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        setPasswordFor(testUser, PASSWORD);
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountPage);
    }

    @Test
    public void maxWaitLoginFailures() {
        int secondsToWait = 5;

        bruteForceDetectionPage.form().setProtectionEnabled(true);
        bruteForceDetectionPage.form().setMaxLoginFailures("1");
        bruteForceDetectionPage.form().setMaxWaitSelect(BruteForceDetection.TimeSelectValues.SECONDS);
        bruteForceDetectionPage.form().setMaxWaitInput(String.valueOf(secondsToWait));
        bruteForceDetectionPage.form().save();

        testRealmAccountPage.navigateTo();

        setPasswordFor(testUser, PASSWORD + "-mismatch");

        testRealmLoginPage.form().login(testUser);
        Date endTime = new Date(new Date().getTime() + secondsToWait * 1000);
        waitForFeedbackText("Invalid username or password.");

        testRealmLoginPage.form().login(testUser);
        endTime = new Date(endTime.getTime() + secondsToWait * 1000);
        waitGui().until().element(By.className("instruction"))
                .text().contains("Account is temporarily disabled, contact admin or try again later.");
        testRealmAccountPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        endTime = new Date(endTime.getTime() + secondsToWait * 1000);
        waitForFeedbackText("Account is temporarily disabled, contact admin or try again later.");

        while (new Date().compareTo(endTime) < 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        setPasswordFor(testUser, PASSWORD);
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountPage);
    }

    @Test
    public void failureResetTime() {
        int secondsToWait = 3;

        bruteForceDetectionPage.form().setProtectionEnabled(true);
        bruteForceDetectionPage.form().setMaxLoginFailures("2");
        bruteForceDetectionPage.form().setFailureResetTimeSelect(BruteForceDetection.TimeSelectValues.SECONDS);
        bruteForceDetectionPage.form().setFailureResetTimeInput(String.valueOf(secondsToWait));
        bruteForceDetectionPage.form().save();
        assertAlertSuccess();

        testRealmAccountPage.navigateTo();

        setPasswordFor(testUser, PASSWORD + "-mismatch");

        testRealmLoginPage.form().login(testUser);
        waitForFeedbackText("Invalid username or password.");
        Date endTime = new Date(new Date().getTime() + secondsToWait * 1000);

        while (new Date().compareTo(endTime) < 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        testRealmLoginPage.form().login(testUser);
        waitForFeedbackText("Invalid username or password.");

        setPasswordFor(testUser, PASSWORD);
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
        userAttributesPage.form().unlockUser();

        testRealmAccountPage.navigateTo();

        setPasswordFor(testUser, PASSWORD);

        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountPage);
    }

    private void waitForFeedbackText(String text) {
        waitGui().until().element(By.className("kc-feedback-text"))
                .text().contains(text);
    }
}
