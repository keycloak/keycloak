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
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.console.page.realm.SecurityDefenses;
import org.openqa.selenium.By;

import java.util.Date;

import static org.jboss.arquillian.graphene.Graphene.waitGui;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 *
 * @author Filip Kiss
 * @author mhajas
 */
public class SecurityDefensesTest extends AbstractRealmTest {

    @Page
    private SecurityDefenses.BruteForceDetection bruteForceDetectionPage;

    @Page
    private Account testRealmAccountPage;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmAccountPage.setAuthRealm(TEST);
    }

    @Before
    public void beforeSecurityDefensesTest() {
//        configure().realmSettings();
//        tabs().securityDefenses();
        bruteForceDetectionPage.navigateTo();
    }

    @Test
    public void maxLoginFailuresTest() {
        int secondsToWait = 3;

//        securityDefensesPage.goToBruteForceDetection();
        bruteForceDetectionPage.form().setProtectionEnabled(true);
        bruteForceDetectionPage.form().setMaxLoginFailures("1");
        bruteForceDetectionPage.form().setWaitIncrementSelect(SecurityDefenses.TimeSelectValues.SECONDS);
        bruteForceDetectionPage.form().setWaitIncrementInput(String.valueOf(secondsToWait));
        bruteForceDetectionPage.form().save();
        assertFlashMessageSuccess();

        testRealmAccountPage.navigateTo();

        UserRepresentation user = createUserRepresentation("test", "test@email.test", "test", "user", true);
        setPasswordFor(user, PASSWORD + "-mismatch");

        testRealmLoginPage.form().login(user);
        waitForFeedbackText("Invalid username or password.");
        Date startTime = new Date();
        Date endTime = new Date(startTime.getTime() + secondsToWait * 1000);

        testRealmLoginPage.form().login(user);
        waitGui().until().element(By.className("instruction"))
                .text().contains("Account is temporarily disabled, contact admin or try again later.");
        endTime = new Date(endTime.getTime() + secondsToWait * 1000);
        testRealmAccountPage.navigateTo();
        testRealmLoginPage.form().login(user);
        endTime = new Date(endTime.getTime() + secondsToWait * 1000);

        while (new Date().compareTo(endTime) < 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        testRealmLoginPage.form().login(user);
        waitForFeedbackText("Invalid username or password.");
    }

    @Test
    public void quickLoginCheck() {
        int secondsToWait = 3;

//        securityDefensesPage.goToBruteForceDetection();
        bruteForceDetectionPage.form().setProtectionEnabled(true);
        bruteForceDetectionPage.form().setMaxLoginFailures("100");
        bruteForceDetectionPage.form().setQuickLoginCheckInput("1500");
        bruteForceDetectionPage.form().setMinQuickLoginWaitSelect(SecurityDefenses.TimeSelectValues.SECONDS);
        bruteForceDetectionPage.form().setMinQuickLoginWaitInput(String.valueOf(secondsToWait));
        bruteForceDetectionPage.form().save();
        assertFlashMessageSuccess();

        testRealmAccountPage.navigateTo();

        UserRepresentation user = createUserRepresentation("test", "test@email.test", "test", "user", true);
        setPasswordFor(user, PASSWORD + "-mismatch");

        testRealmLoginPage.form().login(user);
        testRealmLoginPage.form().login(user);
        Date startTime = new Date();
        Date endTime = new Date(startTime.getTime() + secondsToWait * 1000);
        testRealmLoginPage.form().login(user);
        waitGui().until().element(By.className("instruction"))
                .text().contains("Account is temporarily disabled, contact admin or try again later.");
        endTime = new Date(endTime.getTime() + secondsToWait * 1000);

        testRealmAccountPage.navigateTo();
        testRealmLoginPage.form().login(user);

        while (new Date().compareTo(endTime) < 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        testRealmLoginPage.form().login(user);
        waitForFeedbackText("Invalid username or password.");
    }

    @Test
    public void maxWaitLoginFailures() {
        int secondsToWait = 5;

//        securityDefensesPage.goToBruteForceDetection();
        bruteForceDetectionPage.form().setProtectionEnabled(true);
        bruteForceDetectionPage.form().setMaxLoginFailures("1");
        bruteForceDetectionPage.form().setMaxWaitSelect(SecurityDefenses.TimeSelectValues.SECONDS);
        bruteForceDetectionPage.form().setMaxWaitInput(String.valueOf(secondsToWait));
        bruteForceDetectionPage.form().save();

        testRealmAccountPage.navigateTo();

        UserRepresentation user = createUserRepresentation("test", "test@email.test", "test", "user", true);
        setPasswordFor(user, PASSWORD + "-mismatch");

        testRealmLoginPage.form().login(user);
        waitForFeedbackText("Invalid username or password.");
        Date startTime = new Date();

        testRealmLoginPage.form().login(user);
        waitGui().until().element(By.className("instruction"))
                .text().contains("Account is temporarily disabled, contact admin or try again later.");
        testRealmAccountPage.navigateTo();
        testRealmLoginPage.form().login(user);
        Date endTime = new Date(new Date().getTime() + secondsToWait * 1000);
        waitForFeedbackText("Account is temporarily disabled, contact admin or try again later.");

        while (new Date().compareTo(endTime) < 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        testRealmLoginPage.form().login(user);
        waitForFeedbackText("Invalid username or password.");
    }

    @Test
    public void failureResetTime() {
        int secondsToWait = 3;

//        securityDefensesPage.goToBruteForceDetection();
        bruteForceDetectionPage.form().setProtectionEnabled(true);
        bruteForceDetectionPage.form().setMaxLoginFailures("2");
        bruteForceDetectionPage.form().setFailureResetTimeSelect(SecurityDefenses.TimeSelectValues.SECONDS);
        bruteForceDetectionPage.form().setFailureResetTimeInput(String.valueOf(secondsToWait));
        bruteForceDetectionPage.form().save();
        assertFlashMessageSuccess();

        testRealmAccountPage.navigateTo();

        UserRepresentation user = createUserRepresentation("test", "test@email.test", "test", "user", true);
        setPasswordFor(user, PASSWORD + "-mismatch");

        testRealmLoginPage.form().login(user);
        waitForFeedbackText("Invalid username or password.");
        Date endTime = new Date(new Date().getTime() + secondsToWait * 1000);

        while (new Date().compareTo(endTime) < 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        testRealmLoginPage.form().login(user);
        waitForFeedbackText("Invalid username or password.");
        testRealmLoginPage.form().login(user);
        waitForFeedbackText("Invalid username or password.");
    }

    private void waitForFeedbackText(String text) {
        waitGui().until().element(By.className("kc-feedback-text"))
                .text().contains(text);
    }
}
