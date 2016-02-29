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
package org.keycloak.testsuite.console.authentication.actions;

import java.util.concurrent.TimeUnit;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.auth.page.login.TermsAndConditions;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.authentication.RequiredActions;
import org.keycloak.testsuite.console.page.users.User;
import org.keycloak.testsuite.console.page.users.CreateUser;

import org.keycloak.testsuite.console.page.users.UserAttributesForm;
import org.keycloak.testsuite.util.WaitUtils;

/**
 *
 */
public class TermsAndConditionsTest extends AbstractConsoleTest {

    private static final String USERNAME = "user1";
    
    private static final String PASSWORD = "user1";
    
    private static final String TERMS_TEXT = "Terms and conditions to be defined";
    
    @Page
    private RequiredActions requiredActionsPage;

    @Page
    private TermsAndConditions termsAndConditionsPage;

    @Page
    CreateUser createUser;
    
    @Page
    User user;

    /**
     * Increase 'implicitWait'. If smaller then logout from Test Realm Admin Console
     * may not succeed.
     */
    @Override
    protected void driverSettings() {
        driver.manage().timeouts().pageLoadTimeout(WaitUtils.PAGELOAD_TIMEOUT, TimeUnit.MILLISECONDS);
        driver.manage().timeouts().implicitlyWait(10000, TimeUnit.MILLISECONDS);
        driver.manage().window().maximize();
    }
    
    @Before
    public void createUserAccount() throws Exception {
        requiredActionsPage.navigateTo();
        requiredActionsPage.setTermsAndConditionEnabled(true);
        requiredActionsPage.setTermsAndConditionDefaultAction(true);

        assertAlertSuccess();
        
        createUser.navigateTo();
        UserAttributesForm userForm = createUser.form();
        userForm.setUsername(USERNAME);
        userForm.addRequiredAction("Terms and Conditions");
        userForm.save();
        
        user.tabs().credentials();
        user.setPassword(PASSWORD);
        
        assertAlertSuccess();
        
        adminConsolePage.logOut();
    }

    /**
     * Tests verifies that 'Terms and Conditions' page and related workflow is
     * correct. It does that in the following steps.
     * 
     * 1/ Decline terms
     * 2/ Make sure that KC will ask the user to accept them once more.
     * 3/ Accept terms & update password
     * 4/ Login once more and make sure terms will not be displayed as they
     *    where accepted in step 3/
     */
    @Test
    public void termsAndConditionsDefaultActionTest() throws Exception {
        testRealmAdminConsolePage.navigateTo();
        
        testRealmLoginPage.form().login(USERNAME, PASSWORD); 
        Assert.assertEquals(TERMS_TEXT, termsAndConditionsPage.getText());
        termsAndConditionsPage.declineTerms();

        testRealmLoginPage.form().login(USERNAME, PASSWORD); 
        Assert.assertEquals(TERMS_TEXT, termsAndConditionsPage.getText());
        
        termsAndConditionsPage.acceptTerms();        
        Assert.assertTrue(driver.getTitle().equals("Update password"));
        updatePasswordPage.updatePasswords(PASSWORD, PASSWORD);
        testRealmAdminConsolePage.logOut();
        
        testRealmLoginPage.form().login(USERNAME, PASSWORD);
        testRealmAdminConsolePage.logOut();
    }

}
