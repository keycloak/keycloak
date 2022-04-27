/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.ui.account2;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.auth.page.login.UpdateEmailPage;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.PersonalInfoPage;

@EnableFeature(Profile.Feature.UPDATE_EMAIL)
public class UpdateEmailTest extends BaseAccountPageTest {

    @Page
    private PersonalInfoPage personalInfoPage;

    @Page
    private UpdateEmailPage updateEmailPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    protected AbstractLoggedInPage getAccountPage() {
        return personalInfoPage;
    }

    @Before
    public void setup() {
        enableUpdateEmailRequiredAction();
    }

    @After
    public void clean() {
        disableUpdateEmailRequiredAction();
        disableRegistration();
    }

    @Test
    public void updateEmailLinkVisibleWithUpdateEmailActionEnabled() {
        refreshPageAndWaitForLoad();
        personalInfoPage.assertUpdateEmailLinkVisible(true);
    }

    @Test
    public void updateEmailLinkNotVisibleWithoutUpdateEmailActionEnabled() {
        disableUpdateEmailRequiredAction();
        refreshPageAndWaitForLoad();
        personalInfoPage.assertUpdateEmailLinkVisible(false);
    }

    @Test
    public void updateEmailLinkVisibleWithUpdateEmailActionEnabledAndRegistrationEmailAsUsernameAndEditUsernameNotAllowed() {
        enableRegistration(true, false);
        refreshPageAndWaitForLoad();
        personalInfoPage.assertUpdateEmailLinkVisible(false);
    }

    @Test
    public void updateUserInfoWithRegistrationEnabled() {
        enableRegistration(false, true);
        refreshPageAndWaitForLoad();

        assertTrue(personalInfoPage.valuesEqual(testUser));
        personalInfoPage.assertSaveDisabled(false);

        UserRepresentation newInfo = new UserRepresentation();
        newInfo.setUsername(testUser.getUsername());
        newInfo.setEmail(testUser.getEmail());
        newInfo.setFirstName("New First");
        newInfo.setLastName("New Last");

        personalInfoPage.setValues(newInfo, true);
        assertTrue(personalInfoPage.valuesEqual(newInfo));
        personalInfoPage.assertSaveDisabled(false);
        personalInfoPage.clickSave();
        personalInfoPage.alert().assertSuccess();
        personalInfoPage.assertSaveDisabled(false);

        personalInfoPage.navigateTo();
        personalInfoPage.valuesEqual(newInfo);
    }

    @Test
    public void aiaCancellationSucceeds() {
        refreshPageAndWaitForLoad();
        personalInfoPage.assertUpdateEmailLinkVisible(true);
        personalInfoPage.clickUpdateEmailLink();
        Assert.assertTrue(updateEmailPage.isCurrent());
        updateEmailPage.clickCancelAIA();
        Assert.assertTrue(personalInfoPage.isCurrent());
    }

    @Test
    public void updateEmailSucceeds() {
        personalInfoPage.navigateTo();
        personalInfoPage.assertUpdateEmailLinkVisible(true);
        personalInfoPage.clickUpdateEmailLink();
        Assert.assertTrue(updateEmailPage.isCurrent());
        updateEmailPage.changeEmail("new-email@example.org");
        events.expectAccount(EventType.UPDATE_EMAIL).detail(Details.UPDATED_EMAIL, "new-email@example.org");
        Assert.assertEquals("new-email@example.org", testRealmResource().users().get(testUser.getId()).toRepresentation().getEmail());
    }

    private void disableUpdateEmailRequiredAction() {
        RequiredActionProviderRepresentation updateEmail = testRealmResource().flows().getRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name());
        updateEmail.setEnabled(false);
        testRealmResource().flows().updateRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name(), updateEmail);
    }

    private void enableUpdateEmailRequiredAction() {
        RequiredActionProviderRepresentation updateEmail = testRealmResource().flows().getRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name());
        updateEmail.setEnabled(true);
        testRealmResource().flows().updateRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name(), updateEmail);
    }

    private void enableRegistration(boolean emailAsUsername, boolean usernameEditionAllowed) {
        RealmRepresentation realmRepresentation = testRealmResource().toRepresentation();
        realmRepresentation.setRegistrationAllowed(true);
        realmRepresentation.setRegistrationEmailAsUsername(emailAsUsername);
        realmRepresentation.setEditUsernameAllowed(usernameEditionAllowed);
        testRealmResource().update(realmRepresentation);
    }

    private void disableRegistration() {
        RealmRepresentation realmRepresentation = testRealmResource().toRepresentation();
        realmRepresentation.setRegistrationAllowed(false);
        realmRepresentation.setRegistrationEmailAsUsername(false);
        realmRepresentation.setEditUsernameAllowed(false);
        testRealmResource().update(realmRepresentation);
    }

}
