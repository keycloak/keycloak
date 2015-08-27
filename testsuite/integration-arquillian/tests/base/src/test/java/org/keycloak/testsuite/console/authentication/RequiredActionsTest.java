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
package org.keycloak.testsuite.console.authentication;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.auth.page.login.Registration;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.authentication.RequiredActions;
import org.keycloak.testsuite.console.page.realm.LoginSettings;
import org.openqa.selenium.By;

import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;

/**
 * @author Petr Mensik
 * @author mhajas
 */
public class RequiredActionsTest extends AbstractConsoleTest {

    @Page
    private RequiredActions requiredActions;

    @Page
    private LoginSettings loginSettings;

    @Page
    private Registration testRealmRegistration;

    @Before
    public void beforeRequiredActionsTest() {
        configure().authentication();
        requiredActions.tabs().requiredActions();
        testRealmRegistration.setAuthRealm("test");
    }

    @Test
    public void requiredActionsTest() {
        requiredActions.clickTermsAndConditionEnabled();
        assertFlashMessageSuccess();

        requiredActions.clickTermsAndConditionDefaultAction();
        assertFlashMessageSuccess();

        requiredActions.clickVerifyEmailEnabled();
        assertFlashMessageSuccess();

        requiredActions.clickVerifyEmailDefaultAction();
        assertFlashMessageSuccess();

        requiredActions.clickUpdatePasswordEnabled();
        assertFlashMessageSuccess();

        requiredActions.clickUpdatePasswordDefaultAction();
        assertFlashMessageSuccess();

        requiredActions.clickConfigureTotpEnabled();
        assertFlashMessageSuccess();

        requiredActions.clickConfigureTotpDefaultAction();
        assertFlashMessageSuccess();

        requiredActions.clickUpdateProfileEnabled();
        assertFlashMessageSuccess();

        requiredActions.clickUpdateProfileDefaultAction();
        assertFlashMessageSuccess();
    }

    @Test
    public void termsAndConditionsDefaultActionTest() {
        requiredActions.clickTermsAndConditionEnabled();
        requiredActions.clickTermsAndConditionDefaultAction();

        allowTestRealmUserRegistration();

        navigateToTestRealmRegistration();

        registerTestUser();

        driver.findElement(By.xpath("//div[@id='kc-header-wrapper' and text()[contains(.,'Terms and Conditions')]]"));
    }

    @Test
    public void configureTotpDefaultActionTest() {
        requiredActions.clickConfigureTotpDefaultAction();

        allowTestRealmUserRegistration();

        navigateToTestRealmRegistration();

        registerTestUser();

        driver.findElement(By.xpath("//div[@id='kc-header-wrapper' and text()[contains(.,'Mobile Authenticator Setup')]]"));
    }

    private void allowTestRealmUserRegistration() {
        loginSettings.navigateTo();
        loginSettings.form().setRegistrationAllowed(true);
        loginSettings.form().save();
    }

    private void navigateToTestRealmRegistration() {
        testRealmAdminConsole.navigateTo();
        testRealmLogin.form().register();
    }

    private void registerTestUser() {
        UserRepresentation user = createUserRepresentation("testUser", "testUser@email.test", "test", "user", true);
        setPasswordFor(user, PASSWORD);

        testRealmRegistration.register(user);
    }
}
