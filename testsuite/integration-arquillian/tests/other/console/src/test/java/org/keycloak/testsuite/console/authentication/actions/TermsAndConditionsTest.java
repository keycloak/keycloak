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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.auth.page.login.LoginError;
import org.keycloak.testsuite.auth.page.login.Registration;
import org.keycloak.testsuite.auth.page.login.TermsAndConditions;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.authentication.RequiredActions;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 
 */
public class TermsAndConditionsTest extends AbstractConsoleTest {

    private static final String TERMS_TEXT = "Terms and conditions to be defined";
    
    private static final String REALM = "TermsAndConditions";
    
    private static final String BART = "Bart";
    
    private static final String BART_PASS = "Ay caramba!";
    
    private static final String HOMER = "Homer";
    
    private static final String HOMER_PASS = "Mmm donuts.";

    private static final String FLANDERS = "Flanders";

    private static final String FLANDERS_PASS = "Okily Dokily";
            
    @Page
    private TermsAndConditions termsAndConditionsPage;
    
    @Page
    private Registration registrationPage;

    @Page
    protected LoginError errorPage;
    
    @Override
    public void beforeConsoleTest() {
        // no operation - we don't need 'admin' user for this test.
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(REALM);
        testRealmAdminConsolePage.setAdminRealm(REALM);
        termsAndConditionsPage.setAuthRealm(testRealmPage);
    }
    
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setRealm(REALM);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }
    
    @Test
    public void testExistingUser() {
        // create user
        String userId = createUser(REALM, HOMER, HOMER_PASS);
        
        // test t&c - log in and make sure t&c is not displayed
        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.form().login(HOMER, HOMER_PASS); 
        testRealmAdminConsolePage.logOut();

        // enable terms
        setRequiredActionEnabled(REALM, RequiredActions.TERMS_AND_CONDITIONS, true, false);
        setRequiredActionEnabled(REALM, userId, RequiredActions.TERMS_AND_CONDITIONS, true);

        // test t&c - log in and accept
        testRealmLoginPage.form().login(HOMER, HOMER_PASS); 
        Assert.assertEquals(TERMS_TEXT, termsAndConditionsPage.getText());
        termsAndConditionsPage.declineTerms();
 
        testRealmLoginPage.form().login(HOMER, HOMER_PASS); 
        Assert.assertEquals(TERMS_TEXT, termsAndConditionsPage.getText());
      
        termsAndConditionsPage.acceptTerms();        
        testRealmAdminConsolePage.logOut();
      
        testRealmLoginPage.form().login(HOMER, HOMER_PASS);
        testRealmAdminConsolePage.logOut();
        
        // disable terms
        setRequiredActionEnabled(REALM, RequiredActions.TERMS_AND_CONDITIONS, false, false);
    }
    
    @Test
    public void testAdminCreatedUser() {
        // enable terms
        setRequiredActionEnabled(REALM, RequiredActions.TERMS_AND_CONDITIONS, true, false);
        
        // create user
        String userId = createUser(REALM, BART, BART_PASS);
        setRequiredActionEnabled(REALM, userId, RequiredActions.TERMS_AND_CONDITIONS, true);
        
        // test t&c
        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.form().login(BART, BART_PASS); 
        Assert.assertTrue(termsAndConditionsPage.isCurrent());
        
        // disable terms
        setRequiredActionEnabled(REALM, RequiredActions.TERMS_AND_CONDITIONS, false, false);
    }
    
    @Test
    public void testSelfRegisteredUser() {
        // enable self-registration
        RealmResource realmResource = adminClient.realm(REALM);
        RealmRepresentation realmRepresentation = realmResource.toRepresentation();
        realmRepresentation.setRegistrationAllowed(true);
        realmResource.update(realmRepresentation);
        
        // enable terms
        setRequiredActionEnabled(REALM, RequiredActions.TERMS_AND_CONDITIONS, true, true);
        
        // self-register
        CredentialRepresentation mrBurnsPassword = new CredentialRepresentation();
        mrBurnsPassword.setType(CredentialRepresentation.PASSWORD);
        mrBurnsPassword.setValue("Excellent.");
        
        List<CredentialRepresentation> credentials = new ArrayList<CredentialRepresentation>();
        credentials.add(mrBurnsPassword);
        
        UserRepresentation mrBurns = new UserRepresentation();
        mrBurns.setUsername("mrburns");
        mrBurns.setFirstName("Montgomery");
        mrBurns.setLastName("Burns");
        mrBurns.setEmail("mburns@keycloak.org");
        mrBurns.setCredentials(credentials);
        
        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.form().register();
        
        registrationPage.register(mrBurns);
        
        // test t&c
        Assert.assertTrue(termsAndConditionsPage.isCurrent());
        
        // disable terms
        setRequiredActionEnabled(REALM, RequiredActions.TERMS_AND_CONDITIONS, false, false);
    }

    @Test
    public void testTermsAndConditionsOnAccountPage() {
        String userId = createUser(REALM, FLANDERS, FLANDERS_PASS);

        setRequiredActionEnabled(REALM, RequiredActions.TERMS_AND_CONDITIONS, true, false);
        setRequiredActionEnabled(REALM, userId, RequiredActions.TERMS_AND_CONDITIONS, true);

        // login and decline the terms -- an error page should be shown
        testRealmAccountPage.navigateTo();
        loginPage.form().login(FLANDERS, FLANDERS_PASS);
        termsAndConditionsPage.assertCurrent();
        termsAndConditionsPage.declineTerms();

        // check an error page after declining the terms
        errorPage.assertCurrent();
        assertEquals("No access", errorPage.getErrorMessage());

        // follow the link "back to application"
        errorPage.backToApplication();

        // login again and accept the terms for now
        loginPage.form().login(FLANDERS, FLANDERS_PASS);
        termsAndConditionsPage.assertCurrent();
        termsAndConditionsPage.acceptTerms();
        testRealmAccountPage.assertCurrent();
        testRealmAccountPage.logOut();

        // disable terms
        setRequiredActionEnabled(REALM, RequiredActions.TERMS_AND_CONDITIONS, false, false);
    }
}