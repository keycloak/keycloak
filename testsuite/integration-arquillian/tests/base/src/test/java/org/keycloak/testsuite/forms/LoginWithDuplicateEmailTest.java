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
package org.keycloak.testsuite.forms;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class LoginWithDuplicateEmailTest extends AbstractTestRealmKeycloakTest {

    protected static final String PASSWORD = "password";

    protected static final String PASSWORD_3 = "passwordanother";

    protected static final String USERNAME_3 = "login-test3";

    protected static final String USERNAME_2 = "login-test2";

    protected static final String USERNAME_1 = "login-test1";

    protected static final String EMAIL = "loginduplicate@test.com";

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

        testRealm.setLoginWithEmailAllowed(true);
        testRealm.setDuplicateEmailsAllowed(true);

        UserRepresentation user = UserBuilder.create().id(USERNAME_1).username(USERNAME_1).email(EMAIL).enabled(true).password(PASSWORD).createdTimestamp(1050L).build();
        userId = user.getId();

        UserRepresentation user2 = UserBuilder.create().id(USERNAME_2).username(USERNAME_2).email(EMAIL).enabled(true).password(PASSWORD).createdTimestamp(1060L).build();

        user2Id = user2.getId();

        UserRepresentation user3 = UserBuilder.create().id(USERNAME_3).username(USERNAME_3).email(EMAIL).enabled(true).password(PASSWORD_3).createdTimestamp(1055L).build();

        user3Id = user3.getId();

        RealmBuilder.edit(testRealm).user(user).user(user2).user(user3);
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    private static String userId;

    private static String user2Id;

    private static String user3Id;

    @Test
    public void loginInvalidPassword() {
        loginPage.open();
        loginPage.login(EMAIL, "invalid");

        loginPage.assertCurrent();

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        // first user is selected if no password is valid
        events.expectLogin().user(userId).session((String) null).error("invalid_user_credentials").detail(Details.USERNAME, EMAIL).removeDetail(Details.CONSENT).assertEvent();
    }

    private void setUserEnabled(String userName, boolean enabled) {
        UserRepresentation rep = adminClient.realm("test").users().get(userName).toRepresentation();
        rep.setEnabled(enabled);
        adminClient.realm("test").users().get(userName).update(rep);
    }

    @Test
    public void loginUserWithDuplicatePassword() {
        loginPage.open();
        loginPage.login(EMAIL, PASSWORD);

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        // first user is selected as he is older
        events.expectLogin().user(userId).detail(Details.USERNAME, EMAIL).assertEvent();
    }

    @Test
    public void loginUserWithUniquePassword() {
        loginPage.open();
        loginPage.login(EMAIL, PASSWORD_3);

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        // third user selected by his password
        events.expectLogin().user(user3Id).detail(Details.USERNAME, EMAIL).assertEvent();
    }

    @Test
    public void loginWithOneDisabledUser() {
        // disable first user to see that second one is used
        setUserEnabled(USERNAME_1, false);

        try {
            loginPage.open();
            loginPage.login(EMAIL, PASSWORD);

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

            // second user is selected as first one is disabled
            events.expectLogin().user(user2Id).detail(Details.USERNAME, EMAIL).assertEvent();
        } finally {
            setUserEnabled(USERNAME_1, true);
        }
    }

    @Test
    public void loginAllDisabledUsersForPassword() {
        // disable all users with the password we login with
        setUserEnabled(USERNAME_1, false);
        setUserEnabled(USERNAME_2, false);

        try {
            loginPage.open();
            loginPage.login(EMAIL, PASSWORD);

            loginPage.assertCurrent();

            Assert.assertEquals("Invalid username or password.", loginPage.getError());

            // user 3 is here, it was selected because it is the only enabled
            events.expectLogin().user(user3Id).session((String) null).error("invalid_user_credentials").detail(Details.USERNAME, EMAIL).removeDetail(Details.CONSENT).assertEvent();
        } finally {
            setUserEnabled(USERNAME_1, true);
            setUserEnabled(USERNAME_2, true);
        }
    }

    @Test
    public void loginAllDisabledUsers() {
        // disable all users for the email
        setUserEnabled(USERNAME_1, false);
        setUserEnabled(USERNAME_2, false);
        setUserEnabled(USERNAME_3, false);

        try {
            loginPage.open();
            loginPage.login(EMAIL, PASSWORD);

            loginPage.assertCurrent();

            Assert.assertEquals("Account is disabled, contact admin.", loginPage.getError());

            events.expectLogin().user(userId).session((String) null).error("user_disabled").detail(Details.USERNAME, EMAIL).removeDetail(Details.CONSENT).assertEvent();
        } finally {
            setUserEnabled(USERNAME_1, true);
            setUserEnabled(USERNAME_2, true);
            setUserEnabled(USERNAME_3, true);
        }
    }

}
