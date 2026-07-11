/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LoginUpdateProfileEditUsernameAllowedPage;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequiredActionMultipleActionsTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        ActionUtil.addRequiredActionForUser(testRealm, "test-user@localhost", RequiredAction.UPDATE_PROFILE.name());
        ActionUtil.addRequiredActionForUser(testRealm, "test-user@localhost", RequiredAction.UPDATE_PASSWORD.name());
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginPasswordUpdatePage changePasswordPage;

    @Page
    protected LoginUpdateProfileEditUsernameAllowedPage updateProfilePage;

    @Test
    public void updateProfileAndPassword() throws Exception {
        oauth.openLoginForm();
        loginPage.login("test-user@localhost", "password");

        String codeId = null;
        if (changePasswordPage.isCurrent()) {
            codeId = updatePassword(codeId);

            updateProfilePage.assertCurrent();
            updateProfile(codeId);
        } else if (updateProfilePage.isCurrent()) {
            codeId = updateProfile(codeId);

            changePasswordPage.assertCurrent();
            updatePassword(codeId);
        } else {
            Assertions.fail("Expected to update password and profile before login");
        }

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventAssertion.expectLoginSuccess(events.poll()).sessionId(codeId);
    }

    public String updatePassword(String codeId) {
        changePasswordPage.changePassword("new-password", "new-password");

        EventRepresentation eventRep1 = EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_PASSWORD).details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).getEvent();

        EventRepresentation eventRep2 = EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_CREDENTIAL).details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).getEvent();

        Assertions.assertEquals(eventRep1.getDetails().get(Details.CODE_ID), eventRep2.getDetails().get(Details.CODE_ID));
        return eventRep2.getDetails().get(Details.CODE_ID);
    }

    public String updateProfile(String codeId) {
        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last")
                .email("new@email.com").submit();

        EventRepresentation eventRep = EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_PROFILE)
                .details(Details.UPDATED_FIRST_NAME, "New first")
                .details(Details.UPDATED_LAST_NAME, "New last")
                .details(Details.PREVIOUS_EMAIL, "test-user@localhost")
                .details(Details.UPDATED_EMAIL, "new@email.com").getEvent();
        return eventRep.getDetails().get(Details.CODE_ID);
    }

}
