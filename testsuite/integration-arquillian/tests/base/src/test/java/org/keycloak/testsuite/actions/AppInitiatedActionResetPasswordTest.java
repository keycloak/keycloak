/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.GreenMailRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Stan Silvert
 */
public class AppInitiatedActionResetPasswordTest extends AbstractAppInitiatedActionTest {

    public AppInitiatedActionResetPasswordTest() {
        super(UserModel.RequiredAction.UPDATE_PASSWORD.name());
    }
    
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setResetPasswordAllowed(Boolean.TRUE);
    }

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected LoginPasswordUpdatePage changePasswordPage;

    @After
    public void after() {
        ApiUtil.resetUserPassword(testRealm().users().get(findUser("test-user@localhost").getId()), "password", false);
    }

    @Test
    public void resetPassword() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        events.expectLogin().assertEvent();

        doAIA();

        changePasswordPage.assertCurrent();
        assertTrue(changePasswordPage.isCancelDisplayed());

        changePasswordPage.changePassword("new-password", "new-password");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();

        assertKcActionStatus("success");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        oauth.openLogout();

        events.expectLogout(loginEvent.getSessionId()).assertEvent();

        loginPage.open();
        loginPage.login("test-user@localhost", "new-password");

        events.expectLogin().assertEvent();
    }

    @Test
    public void resetPasswordRequiresReAuth() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        events.expectLogin().assertEvent();

        setTimeOffset(350);

        // Should prompt for re-authentication
        doAIA();

        loginPage.assertCurrent();
        loginPage.login("test-user@localhost", "password");

        changePasswordPage.assertCurrent();
        assertTrue(changePasswordPage.isCancelDisplayed());

        changePasswordPage.changePassword("new-password", "new-password");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();
        assertKcActionStatus("success");
    }

    @Test
    public void cancelChangePassword() throws Exception {
        doAIA();
        
        loginPage.login("test-user@localhost", "password");
        
        changePasswordPage.assertCurrent();
        changePasswordPage.cancel();
        
        assertKcActionStatus("cancelled");
    }

    @Test
    public void resetPasswordUserHasUpdatePasswordRequiredAction() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        UserResource userResource = testRealm().users().get(findUser("test-user@localhost").getId());
        UserRepresentation userRep = userResource.toRepresentation();
        userRep.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        userResource.update(userRep);

        events.expectLogin().assertEvent();

        doAIA();

        changePasswordPage.assertCurrent();
        assertFalse(changePasswordPage.isCancelDisplayed());

        changePasswordPage.changePassword("new-password", "new-password");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();

        assertKcActionStatus("success");
    }

}
