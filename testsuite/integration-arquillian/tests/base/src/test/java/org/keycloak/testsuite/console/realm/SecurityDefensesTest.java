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
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.console.page.realm.SecurityDefenses;
import org.openqa.selenium.By;

import java.util.Date;

import static org.jboss.arquillian.graphene.Graphene.waitGui;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;

/**
 *
 * @author Filip Kiss
 * @author mhajas
 */
public class SecurityDefensesTest extends AbstractRealmTest {

    @Page
    private SecurityDefenses securityDefenses;

    @Page
    private Account testRealmAccount;

    @Before
    public void beforeSecurityDefensesTest() {
        configure().realmSettings();
        tabs().securityDefenses();
        testRealmAccount.setAuthRealm("test");
    }

    @Test
    public void maxLoginFailuresTest() {
        int secondsToWait = 3;

        securityDefenses.goToBruteForceDetection();
        securityDefenses.bruteForceDetection().form().setProtectionEnabled(true);
        securityDefenses.bruteForceDetection().form().setMaxLoginFailures("1");
        securityDefenses.bruteForceDetection().form().setWaitIncrementSelect(SecurityDefenses.TimeSelectValues.SECONDS);
        securityDefenses.bruteForceDetection().form().setWaitIncrementInput(String.valueOf(secondsToWait));
        securityDefenses.bruteForceDetection().form().save();
        assertFlashMessageSuccess();

        testRealmAccount.navigateTo();

        UserRepresentation user = createUserRepresentation("test", "test@email.test", "test", "user", true);
        setPasswordFor(user, PASSWORD + "-mismatch");

        testRealmLogin.form().login(user);
        waitForFeedbackText("Invalid username or password.");
        Date startTime = new Date();
        Date endTime = new Date(startTime.getTime() + secondsToWait*1000);

        testRealmLogin.form().login(user);
        waitGui().until().element(By.className("instruction"))
                .text().contains("Account is temporarily disabled, contact admin or try again later.");
        endTime = new Date(endTime.getTime() + secondsToWait*1000);
        testRealmAccount.navigateTo();
        testRealmLogin.form().login(user);
        endTime = new Date(endTime.getTime() + secondsToWait*1000);

        while(new Date().compareTo(endTime) < 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        testRealmLogin.form().login(user);
        waitForFeedbackText("Invalid username or password.");
    }


    @Test
    public void quickLoginCheck() {
        int secondsToWait = 3;

        securityDefenses.goToBruteForceDetection();
        securityDefenses.bruteForceDetection().form().setProtectionEnabled(true);
        securityDefenses.bruteForceDetection().form().setMaxLoginFailures("100");
        securityDefenses.bruteForceDetection().form().setQuickLoginCheckInput("1500");
        securityDefenses.bruteForceDetection().form().setMinQuickLoginWaitSelect(SecurityDefenses.TimeSelectValues.SECONDS);
        securityDefenses.bruteForceDetection().form().setMinQuickLoginWaitInput(String.valueOf(secondsToWait));
        securityDefenses.bruteForceDetection().form().save();
        assertFlashMessageSuccess();

        testRealmAccount.navigateTo();

        UserRepresentation user = createUserRepresentation("test", "test@email.test", "test", "user", true);
        setPasswordFor(user, PASSWORD + "-mismatch");

        testRealmLogin.form().login(user);
        testRealmLogin.form().login(user);
        Date startTime = new Date();
        Date endTime = new Date(startTime.getTime() + secondsToWait*1000);
        testRealmLogin.form().login(user);
        waitGui().until().element(By.className("instruction"))
                .text().contains("Account is temporarily disabled, contact admin or try again later.");
        endTime = new Date(endTime.getTime() + secondsToWait*1000);

        testRealmAccount.navigateTo();
        testRealmLogin.form().login(user);

        while(new Date().compareTo(endTime) < 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        testRealmLogin.form().login(user);
        waitForFeedbackText("Invalid username or password.");
    }

    @Test
    public void maxWaitLoginFailures() {
        int secondsToWait = 5;

        securityDefenses.goToBruteForceDetection();
        securityDefenses.bruteForceDetection().form().setProtectionEnabled(true);
        securityDefenses.bruteForceDetection().form().setMaxLoginFailures("1");
        securityDefenses.bruteForceDetection().form().setMaxWaitSelect(SecurityDefenses.TimeSelectValues.SECONDS);
        securityDefenses.bruteForceDetection().form().setMaxWaitInput(String.valueOf(secondsToWait));
        securityDefenses.bruteForceDetection().form().save();

        testRealmAccount.navigateTo();

        UserRepresentation user = createUserRepresentation("test", "test@email.test", "test", "user", true);
        setPasswordFor(user, PASSWORD + "-mismatch");

        testRealmLogin.form().login(user);
        waitForFeedbackText("Invalid username or password.");
        Date startTime = new Date();

        testRealmLogin.form().login(user);
        waitGui().until().element(By.className("instruction"))
                .text().contains("Account is temporarily disabled, contact admin or try again later.");
        testRealmAccount.navigateTo();
        testRealmLogin.form().login(user);
        Date endTime = new Date(new Date().getTime() + secondsToWait*1000);
        waitForFeedbackText("Account is temporarily disabled, contact admin or try again later.");

        while(new Date().compareTo(endTime) < 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        testRealmLogin.form().login(user);
        waitForFeedbackText("Invalid username or password.");
    }

    @Test
    public void failureResetTime() {
        int secondsToWait = 3;

        securityDefenses.goToBruteForceDetection();
        securityDefenses.bruteForceDetection().form().setProtectionEnabled(true);
        securityDefenses.bruteForceDetection().form().setMaxLoginFailures("2");
        securityDefenses.bruteForceDetection().form().setFailureResetTimeSelect(SecurityDefenses.TimeSelectValues.SECONDS);
        securityDefenses.bruteForceDetection().form().setFailureResetTimeInput(String.valueOf(secondsToWait));
        securityDefenses.bruteForceDetection().form().save();
        assertFlashMessageSuccess();

        testRealmAccount.navigateTo();

        UserRepresentation user = createUserRepresentation("test", "test@email.test", "test", "user", true);
        setPasswordFor(user, PASSWORD + "-mismatch");

        testRealmLogin.form().login(user);
        waitForFeedbackText("Invalid username or password.");
        Date endTime = new Date(new Date().getTime() + secondsToWait*1000);

        while(new Date().compareTo(endTime) < 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        testRealmLogin.form().login(user);
        waitForFeedbackText("Invalid username or password.");
        testRealmLogin.form().login(user);
        waitForFeedbackText("Invalid username or password.");
    }

    private void waitForFeedbackText(String text) {
        waitGui().until().element(By.className("kc-feedback-text"))
                .text().contains(text);
    }
}
