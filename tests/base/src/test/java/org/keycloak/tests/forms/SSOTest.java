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
package org.keycloak.tests.forms;

import jakarta.ws.rs.core.Response;

import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@KeycloakIntegrationTest
@DatabaseTest
public class SSOTest {

    @InjectRealm(config = TestRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver;

    @InjectWebDriver(ref = "driver2", lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver2;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectOAuthClient(ref="oauth2", webDriverRef = "driver2")
    OAuthClient oauth2;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage(ref = "loginpage2", webDriverRef = "driver2")
    protected LoginPage loginPage2;

    @InjectPage
    protected LoginPasswordUpdatePage updatePasswordPage;

    @InjectEvents
    protected Events events;

    @InjectUser(ref="testUser", config = SSOTestUserConfig.class)
    ManagedUser testUser;

    @InjectUser(ref="johnDoh", config = JohnDohUserConfig.class)
    ManagedUser johnDohUser;

    @Test
    public void loginSuccess() {

        // Initial login for SSO
        oauth.doLogin(testUser.getUsername(), testUser.getPassword());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);
        String sessionId1 = loginEvent.getSessionId();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        events.skip(); // skip CODE_TO_TOKEN event
        IDToken idToken = oauth.parseToken(tokenResponse.getIdToken(), IDToken.class);
        assertEquals("1", idToken.getAcr());
        Long authTime = idToken.getAuth_time();

        driver.open(oauth.getRedirectUri());

        // Perform Same browser SSO login, no credentials required - SSO cookie
        oauth.openLoginForm();
        driver.waiting().waitForOAuthCallback();

        EventRepresentation loginEvent2 = events.poll();
        EventAssertion.assertSuccess(loginEvent2)
                .type(EventType.LOGIN)
                .clientId("test-app");
        String sessionId2 = loginEvent2.getSessionId();

        assertEquals(sessionId1, sessionId2);

        String code2 = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse2 = oauth.doAccessTokenRequest(code2);
        events.skip(); // skip CODE_TO_TOKEN event
        IDToken idToken2 = oauth.parseToken(tokenResponse2.getIdToken(), IDToken.class);
        Assertions.assertEquals("0", idToken2.getAcr());

        // auth time hasn't changed as we authenticated through SSO cookie
        Assertions.assertEquals(authTime, idToken2.getAuth_time());

        // Expire session and logout (removes cookie)
        managedRealm.admin().deleteSession(sessionId1, false);

        oauth.doLogin(testUser.getUsername(), testUser.getPassword());

        // Verify new session created
        EventRepresentation loginEvent3 = events.poll();
        EventAssertion.assertSuccess(loginEvent3).type(EventType.LOGIN);
        String sessionId3 = loginEvent3.getSessionId();
        assertNotEquals(sessionId1, sessionId3);
    }

    @Test
    public void multipleSessions() {

        oauth.doLogin(testUser.getUsername(), testUser.getPassword());
        String code1 = oauth.parseLoginResponse().getCode();
        Assertions.assertNotNull(code1);
        EventRepresentation login1 = events.poll();
        EventAssertion.assertSuccess(login1).type(EventType.LOGIN);
        String sessionId1 = login1.getSessionId();

        oauth2.doLogin(testUser.getUsername(), testUser.getPassword());
        String code2 = oauth2.parseLoginResponse().getCode();
        Assertions.assertNotNull(code2);

        EventRepresentation login2 = events.poll();
        EventAssertion.assertSuccess(login2).type(EventType.LOGIN);
        String sessionId2 = login2.getSessionId();

        assertNotEquals(sessionId1, sessionId2);

        AccessTokenResponse tokenResponse1 = oauth.doAccessTokenRequest(code1);
        events.skip(); // Skip CODE_TO_TOKEN event
        oauth.logoutForm()
                .idTokenHint(tokenResponse1.getIdToken())
                .postLogoutRedirectUri(oauth.getRedirectUri())
                .open();

        EventRepresentation logoutEvent1 = events.poll();
        EventAssertion.assertSuccess(logoutEvent1)
                .type(EventType.LOGOUT)
                .sessionId(sessionId1);

        oauth.openLoginForm();
        loginPage.assertCurrent();

        oauth2.openLoginForm();
        driver2.waiting().waitForOAuthCallback();
        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .sessionId(sessionId2)
                .withoutDetails("test-user@localhost");

        assertThat(driver2.getCurrentUrl(), Matchers.startsWith(oauth2.getRedirectUri()));
        AccessTokenResponse tokenResponse2 = oauth2.doAccessTokenRequest(code2);
        events.skip(); // Skip CODE_TO_TOKEN event

        oauth2.logoutForm()
                .idTokenHint(tokenResponse2.getIdToken())
                .postLogoutRedirectUri(oauth2.getRedirectUri())
                .open();

        EventRepresentation logoutEvent2 = events.poll();
        EventAssertion.assertSuccess(logoutEvent2)
                .type(EventType.LOGOUT)
                .sessionId(sessionId2);

        oauth2.openLoginForm();
        driver2.waiting().waitForTitle("Sign in to test");
        loginPage2.assertCurrent();
    }


    @Test
    public void loginWithRequiredActionAddedInTheMeantime() {

        // SSO login
        oauth.doLogin(testUser.getUsername(), testUser.getPassword());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);
        String sessionId = loginEvent.getSessionId();
        String userId = loginEvent.getUserId();

        UserRepresentation userRep = managedRealm.admin().users().get(userId).toRepresentation();
        userRep.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        managedRealm.admin().users().get(userId).update(userRep);

        // Attempt SSO login. update-password form is shown
        oauth.openLoginForm();
        updatePasswordPage.assertCurrent();
        updatePasswordPage.changePassword(testUser.getPassword(), testUser.getPassword());
        driver.waiting().waitForOAuthCallback();

        // Poll both password update events (order may vary due to identical timestamps)
        EventRepresentation event1 = events.poll();
        EventRepresentation event2 = events.poll();

        EventRepresentation updatePasswordEvent;
        EventRepresentation updateCredentialEvent;

        if (EventType.UPDATE_PASSWORD == EventType.valueOf(event1.getType())) {
            updatePasswordEvent = event1;
            updateCredentialEvent = event2;
        } else {
            updatePasswordEvent = event2;
            updateCredentialEvent = event1;
        }

        EventAssertion.assertSuccess(updatePasswordEvent)
                .type(EventType.UPDATE_PASSWORD)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE);

        EventAssertion.assertSuccess(updateCredentialEvent)
                .type(EventType.UPDATE_CREDENTIAL)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE);

        EventRepresentation loginEvent2 = events.poll();
        EventAssertion.assertSuccess(loginEvent2)
                .type(EventType.LOGIN)
                .clientId("test-app");
        String sessionId2 = loginEvent2.getSessionId();
        assertEquals(sessionId, sessionId2);
    }

    @Test
    public void failIfUsingCodeFromADifferentSession() {

        // first client user login
        oauth.doLogin(testUser.getUsername(), testUser.getPassword());
        String firstCode = oauth.parseLoginResponse().getCode();

        // second client user login
        oauth2.doLogin(johnDohUser.getUsername(), johnDohUser.getPassword());
        String secondCode = oauth2.parseLoginResponse().getCode();

        String[] firstCodeParts = firstCode.split("\\.");
        String[] secondCodeParts = secondCode.split("\\.");
        secondCodeParts[1] = firstCodeParts[1];
        String tamperedCode = String.join(".", secondCodeParts);

        AccessTokenResponse  tokenResponse = oauth2.doAccessTokenRequest(tamperedCode);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), tokenResponse.getStatusCode());
    }

    private static class SSOTestUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder config) {
            return config.username("test-user@localhost")
                    .password("test-user@localhost")
                    .name("Sso", "User")
                    .email("sso@user.com")
                    .emailVerified(true);
        }
    }

    private static class JohnDohUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder config) {
            return config.username("john-doh@localhost")
                    .password("password")
                    .name("John", "Doh")
                    .email("john@doh.com")
                    .emailVerified(true);
        }
    }

    private static class TestRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name("test");
        }
    }
}
