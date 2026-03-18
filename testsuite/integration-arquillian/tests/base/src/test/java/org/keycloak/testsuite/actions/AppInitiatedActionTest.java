/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.actions;

import java.io.IOException;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.TermsAndConditionsPage;
import org.keycloak.testsuite.updaters.UserAttributeUpdater;
import org.keycloak.testsuite.util.GreenMailRule;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.actions.AbstractAppInitiatedActionTest.SUCCESS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:wadahiro@gmail.com">Hiroyuki Wada</a>
 */
public class AppInitiatedActionTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected TermsAndConditionsPage termsAndConditionsPage;

    @Test
    public void executeUnknownAction() {
        oauth.loginForm().kcAction("nosuch").open();

        loginPage.login("test-user@localhost", "password");

        assertTrue(appPage.isCurrent());

        String kcActionStatus = oauth.parseLoginResponse().getKcActionStatus();
        assertEquals("error", kcActionStatus);
    }

    // Issue 37526
    @Test
    public void executeUnknownActionAfterBeingAuthenticated() {
        oauth.loginForm().doLogin("test-user@localhost", "password");
        appPage.assertCurrent();

        oauth.loginForm().kcAction("nosuch").open();
        appPage.assertCurrent();

        String kcActionStatus = oauth.parseLoginResponse().getKcActionStatus();
        assertEquals("error", kcActionStatus);
    }

    @Test
    public void executeUnsupportedAction() {
        oauth.loginForm().kcAction(TermsAndConditions.PROVIDER_ID).open();

        loginPage.login("test-user@localhost", "password");

        assertTrue(appPage.isCurrent());

        String kcActionStatus = oauth.parseLoginResponse().getKcActionStatus();
        assertEquals("error", kcActionStatus);
    }

    @Test
    public void executeDisabledAction() {
        RequiredActionProviderRepresentation configureTotp = testRealm().flows().getRequiredAction("CONFIGURE_TOTP");
        configureTotp.setEnabled(false);
        try {
            testRealm().flows().updateRequiredAction("CONFIGURE_TOTP", configureTotp);

            oauth.loginForm().kcAction(UserModel.RequiredAction.CONFIGURE_TOTP.name()).open();

            loginPage.login("test-user@localhost", "password");

            assertTrue(appPage.isCurrent());

            String kcActionStatus = oauth.parseLoginResponse().getKcActionStatus();
            assertEquals("error", kcActionStatus);
        } finally {
            configureTotp.setEnabled(true);
            testRealm().flows().updateRequiredAction("CONFIGURE_TOTP", configureTotp);
        }
    }

    @Test
    public void executeActionWithTermsAndConditionsUnsupportedAIA() throws IOException {
        RealmResource realm = testRealm();
        RequiredActionProviderRepresentation termsAndConditions = realm.flows().getRequiredAction(TermsAndConditions.PROVIDER_ID);
        int prevPriority = termsAndConditions.getPriority();
        boolean prevEnabled = termsAndConditions.isEnabled();

        try (UserAttributeUpdater userUpdater = UserAttributeUpdater
                .forUserByUsername(realm, "test-user@localhost")
                .setRequiredActions(UserModel.RequiredAction.TERMS_AND_CONDITIONS).update()) {
            // Set max priority for terms and conditions (AIA not supported) to be executed before update password
            termsAndConditions.setPriority(1);
            termsAndConditions.setEnabled(true);
            realm.flows().updateRequiredAction(TermsAndConditions.PROVIDER_ID, termsAndConditions);

            oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();
            loginPage.login("test-user@localhost", "password");

            // Terms and conditions are displayed first (They are not AIA, but displayed as a regular action as they were first)
            termsAndConditionsPage.assertCurrent();
            termsAndConditionsPage.acceptTerms();

            // the update password should be displayed as an AIA
            passwordUpdatePage.assertCurrent();
            assertTrue(passwordUpdatePage.isCancelDisplayed());
            passwordUpdatePage.changePassword("password", "password");

            // once the AIA password is executed the terms and conditions should be displayed for the login
            appPage.assertCurrent();
            assertEquals(SUCCESS, oauth.parseLoginResponse().getKcActionStatus());
        } finally {
            termsAndConditions.setPriority(prevPriority);
            termsAndConditions.setEnabled(prevEnabled);
            realm.flows().updateRequiredAction(TermsAndConditions.PROVIDER_ID, termsAndConditions);
        }
    }
}
