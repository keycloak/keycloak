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
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.updaters.UserAttributeUpdater;
import org.keycloak.testsuite.util.GreenMailRule;

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
    protected VerifyEmailPage verifyEmailPage;

    @Test
    public void executeUnknownAction() {
        oauth.kcAction("nosuch").openLoginForm();

        loginPage.login("test-user@localhost", "password");

        assertTrue(appPage.isCurrent());

        String kcActionStatus = oauth.getCurrentQuery().get("kc_action_status");
        assertEquals("error", kcActionStatus);
    }

    @Test
    public void executeUnsupportedAction() {
        oauth.kcAction(TermsAndConditions.PROVIDER_ID).openLoginForm();

        loginPage.login("test-user@localhost", "password");

        assertTrue(appPage.isCurrent());

        String kcActionStatus = oauth.getCurrentQuery().get("kc_action_status");
        assertEquals("error", kcActionStatus);
    }

    @Test
    public void executeDisabledAction() {
        RequiredActionProviderRepresentation configureTotp = testRealm().flows().getRequiredAction("CONFIGURE_TOTP");
        configureTotp.setEnabled(false);
        try {
            testRealm().flows().updateRequiredAction("CONFIGURE_TOTP", configureTotp);

            oauth.kcAction(UserModel.RequiredAction.CONFIGURE_TOTP.name()).openLoginForm();

            loginPage.login("test-user@localhost", "password");

            assertTrue(appPage.isCurrent());

            String kcActionStatus = oauth.getCurrentQuery().get("kc_action_status");
            assertEquals("error", kcActionStatus);
        } finally {
            configureTotp.setEnabled(true);
            testRealm().flows().updateRequiredAction("CONFIGURE_TOTP", configureTotp);
        }
    }

    @Test
    public void executeActionWithVerifyEmailUnsupportedAIA() throws IOException {
        RealmResource realm = testRealm();
        RequiredActionProviderRepresentation model = realm.flows().getRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL.name());
        int prevPriority = model.getPriority();

        try (UserAttributeUpdater userUpdater = UserAttributeUpdater
                .forUserByUsername(realm, "test-user@localhost")
                .setRequiredActions(UserModel.RequiredAction.VERIFY_EMAIL).update()) {
            // Set max priority for verify email (AIA not supported) to be executed before update password
            model.setPriority(1);
            realm.flows().updateRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL.name(), model);

            oauth.kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).openLoginForm();
            loginPage.login("test-user@localhost", "password");

            // the update password should be displayed
            passwordUpdatePage.assertCurrent();
            passwordUpdatePage.changePassword("password", "password");

            // once the AIA password is executed the verify profile should be displayed for the login
            verifyEmailPage.assertCurrent();
        } finally {
            model.setPriority(prevPriority);
            realm.flows().updateRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL.name(), model);
        }
    }
}
