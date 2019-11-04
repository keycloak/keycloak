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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.GreenMailRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequiredActionResetPasswordTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setResetPasswordAllowed(Boolean.TRUE);
        ActionUtil.addRequiredActionForUser(testRealm, "test-user@localhost", RequiredAction.UPDATE_PASSWORD.name());
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
    protected LoginPasswordUpdatePage changePasswordPage;


    @Test
    public void tempPassword() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        changePasswordPage.assertCurrent();
        assertFalse(changePasswordPage.isCancelDisplayed());

        changePasswordPage.changePassword("new-password", "new-password");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        oauth.openLogout();

        events.expectLogout(loginEvent.getSessionId()).assertEvent();

        loginPage.open();
        loginPage.login("test-user@localhost", "new-password");

        events.expectLogin().assertEvent();
    }

}
