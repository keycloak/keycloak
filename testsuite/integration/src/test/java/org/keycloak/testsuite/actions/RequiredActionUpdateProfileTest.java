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

import org.junit.*;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfileEditUsernameAllowedPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequiredActionUpdateProfileTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    @Rule
    public WebRule webRule = new WebRule(this);

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected LoginUpdateProfileEditUsernameAllowedPage updateProfilePage;

    @Before
    public void before() {
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel defaultRealm, RealmModel appRealm) {
                UserModel user = manager.getSession().users().getUserByUsername("test-user@localhost", appRealm);
                user.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
                UserModel anotherUser = manager.getSession().users().getUserByEmail("john-doh@localhost", appRealm);
                anotherUser.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
            }
        });
    }

    @Test
    public void updateProfile() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "New last", "new@email.com", "test-user@localhost");

        String sessionId = events.expectRequiredAction(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, "test-user@localhost").detail(Details.UPDATED_EMAIL, "new@email.com").assertEvent().getSessionId();
        events.expectRequiredAction(EventType.UPDATE_PROFILE).session(sessionId).assertEvent();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().session(sessionId).assertEvent();

        // assert user is really updated in persistent store
        UserRepresentation user = keycloakRule.getUser("test", "test-user@localhost");
        Assert.assertEquals("New first", user.getFirstName());
        Assert.assertEquals("New last", user.getLastName());
        Assert.assertEquals("new@email.com", user.getEmail());
        Assert.assertEquals("test-user@localhost", user.getUsername());
    }

    @Test
    public void updateUsername() {
        loginPage.open();

        loginPage.login("john-doh@localhost", "password");

        String userId = keycloakRule.getUser("test", "john-doh@localhost").getId();

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "New last", "john-doh@localhost", "new");

        String sessionId = events
                .expectLogin()
                .event(EventType.UPDATE_PROFILE)
                .detail(Details.USERNAME, "john-doh@localhost")
                .user(userId)
                .session(AssertEvents.isUUID())
                .removeDetail(Details.CONSENT)
                .assertEvent()
                .getSessionId();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().detail(Details.USERNAME, "john-doh@localhost").user(userId).session(sessionId).assertEvent();

        // assert user is really updated in persistent store
        UserRepresentation user = keycloakRule.getUser("test", "new");
        Assert.assertEquals("New first", user.getFirstName());
        Assert.assertEquals("New last", user.getLastName());
        Assert.assertEquals("john-doh@localhost", user.getEmail());
        Assert.assertEquals("new", user.getUsername());
    }

    @Test
    public void updateProfileMissingFirstName() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("", "New last", "new@email.com", "new");

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assert.assertEquals("", updateProfilePage.getFirstName());
        Assert.assertEquals("New last", updateProfilePage.getLastName());
        Assert.assertEquals("new@email.com", updateProfilePage.getEmail());

        Assert.assertEquals("Please specify first name.", updateProfilePage.getError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileMissingLastName() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "", "new@email.com", "new");

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assert.assertEquals("New first", updateProfilePage.getFirstName());
        Assert.assertEquals("", updateProfilePage.getLastName());
        Assert.assertEquals("new@email.com", updateProfilePage.getEmail());

        Assert.assertEquals("Please specify last name.", updateProfilePage.getError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileMissingEmail() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "New last", "", "new");

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assert.assertEquals("New first", updateProfilePage.getFirstName());
        Assert.assertEquals("New last", updateProfilePage.getLastName());
        Assert.assertEquals("", updateProfilePage.getEmail());

        Assert.assertEquals("Please specify email.", updateProfilePage.getError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileInvalidEmail() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "New last", "invalidemail", "invalid");

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assert.assertEquals("New first", updateProfilePage.getFirstName());
        Assert.assertEquals("New last", updateProfilePage.getLastName());
        Assert.assertEquals("invalidemail", updateProfilePage.getEmail());

        Assert.assertEquals("Invalid email address.", updateProfilePage.getError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileMissingUsername() {
        loginPage.open();

        loginPage.login("john-doh@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "New last", "new@email.com", "");

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assert.assertEquals("New first", updateProfilePage.getFirstName());
        Assert.assertEquals("New last", updateProfilePage.getLastName());
        Assert.assertEquals("new@email.com", updateProfilePage.getEmail());
        Assert.assertEquals("", updateProfilePage.getUsername());

        Assert.assertEquals("Please specify username.", updateProfilePage.getError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileDuplicateUsername() {
        loginPage.open();

        loginPage.login("john-doh@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "New last", "new@email.com", "test-user@localhost");

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assert.assertEquals("New first", updateProfilePage.getFirstName());
        Assert.assertEquals("New last", updateProfilePage.getLastName());
        Assert.assertEquals("new@email.com", updateProfilePage.getEmail());
        Assert.assertEquals("test-user@localhost", updateProfilePage.getUsername());

        Assert.assertEquals("Username already exists.", updateProfilePage.getError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileDuplicatedEmail() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "New last", "keycloak-user@localhost", "test-user@localhost");

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assert.assertEquals("New first", updateProfilePage.getFirstName());
        Assert.assertEquals("New last", updateProfilePage.getLastName());
        Assert.assertEquals("keycloak-user@localhost", updateProfilePage.getEmail());

        Assert.assertEquals("Email already exists.", updateProfilePage.getError());

        events.assertEmpty();
    }

}
