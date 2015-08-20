/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.actions;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.KeycloakRule.KeycloakSetup;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequiredActionMultipleActionsTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel defaultRealm, RealmModel appRealm) {
            UserModel user = manager.getSession().users().getUserByUsername("test-user@localhost", appRealm);
            user.addRequiredAction(RequiredAction.UPDATE_PROFILE);
            user.addRequiredAction(RequiredAction.UPDATE_PASSWORD);
        }

    });

    @Rule
    public WebRule webRule = new WebRule(this);

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected LoginPasswordUpdatePage changePasswordPage;

    @WebResource
    protected LoginUpdateProfilePage updateProfilePage;

    @Test
    public void updateProfileAndPassword() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        String sessionId = null;
        if (changePasswordPage.isCurrent()) {
            sessionId = updatePassword(sessionId);

            updateProfilePage.assertCurrent();
            updateProfile(sessionId);
        } else if (updateProfilePage.isCurrent()) {
            sessionId = updateProfile(sessionId);

            changePasswordPage.assertCurrent();
            updatePassword(sessionId);
        } else {
            Assert.fail("Expected to update password and profile before login");
        }

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().session(sessionId).assertEvent();
    }

    public String updatePassword(String sessionId) {
        changePasswordPage.changePassword("new-password", "new-password");

        AssertEvents.ExpectedEvent expectedEvent = events.expectRequiredAction(EventType.UPDATE_PASSWORD);
        if (sessionId != null) {
            expectedEvent.session(sessionId);
        }
        return expectedEvent.assertEvent().getSessionId();
    }

    public String updateProfile(String sessionId) {
        updateProfilePage.update("New first", "New last", "new@email.com");

        AssertEvents.ExpectedEvent expectedEvent = events.expectRequiredAction(EventType.UPDATE_PROFILE);
        if (sessionId != null) {
            expectedEvent.session(sessionId);
        }
        sessionId = expectedEvent.assertEvent().getSessionId();
        events.expectRequiredAction(EventType.UPDATE_EMAIL).session(sessionId).detail(Details.PREVIOUS_EMAIL, "test-user@localhost").detail(Details.UPDATED_EMAIL, "new@email.com").assertEvent();
        return sessionId;
    }

}
