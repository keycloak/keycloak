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
package org.keycloak.testsuite.forms;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.util.Time;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {
        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            UserModel user = manager.getSession().users().addUser(appRealm, "login-test");
            user.setEmail("login@test.com");
            user.setEnabled(true);

            userId = user.getId();

            UserCredentialModel creds = new UserCredentialModel();
            creds.setType(CredentialRepresentation.PASSWORD);
            creds.setValue("password");

            user.updateCredential(creds);
        }
    });

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected ErrorPage errorPage;
    
    @WebResource
    protected LoginPasswordUpdatePage updatePasswordPage;

    private static String userId;

    @Test
    public void testBrowserSecurityHeaders() {
        Client client = ClientBuilder.newClient();
        Response response = client.target(oauth.getLoginFormUrl()).request().get();
        Assert.assertEquals(200, response.getStatus());
        for (Map.Entry<String, String> entry : BrowserSecurityHeaders.defaultHeaders.entrySet()) {
            String headerName = BrowserSecurityHeaders.headerAttributeMap.get(entry.getKey());
            String headerValue = response.getHeaderString(headerName);
            Assert.assertNotNull(headerValue);
            Assert.assertEquals(headerValue, entry.getValue());
        }
        response.close();
    }

    @Test
    public void loginInvalidPassword() {
        loginPage.open();
        loginPage.login("login-test", "invalid");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assert.assertEquals("login-test", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().user(userId).session((String) null).error("invalid_user_credentials")
                .detail(Details.USERNAME, "login-test")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    public void loginMissingPassword() {
        loginPage.open();
        loginPage.missingPassword("login-test");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assert.assertEquals("login-test", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().user(userId).session((String) null).error("invalid_user_credentials")
                .detail(Details.USERNAME, "login-test")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    public void loginInvalidPasswordDisabledUser() {
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                session.users().getUserByUsername("login-test", appRealm).setEnabled(false);
            }
        });

        try {
            loginPage.open();
            loginPage.login("login-test", "invalid");

            loginPage.assertCurrent();

            // KEYCLOAK-1741 - assert form field values kept
            Assert.assertEquals("login-test", loginPage.getUsername());
            Assert.assertEquals("", loginPage.getPassword());

            Assert.assertEquals("Account is disabled, contact admin.", loginPage.getError());

            events.expectLogin().user(userId).session((String) null).error("user_disabled")
                    .detail(Details.USERNAME, "login-test")
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
        } finally {
            keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    session.users().getUserByUsername("login-test", appRealm).setEnabled(true);
                }
            });
        }
    }

    @Test
    public void loginDisabledUser() {
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                session.users().getUserByUsername("login-test", appRealm).setEnabled(false);
            }
        });

        try {
            loginPage.open();
            loginPage.login("login-test", "password");

            loginPage.assertCurrent();

            // KEYCLOAK-1741 - assert form field values kept
            Assert.assertEquals("login-test", loginPage.getUsername());
            Assert.assertEquals("", loginPage.getPassword());

            Assert.assertEquals("Account is disabled, contact admin.", loginPage.getError());

            events.expectLogin().user(userId).session((String) null).error("user_disabled")
                    .detail(Details.USERNAME, "login-test")
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
        } finally {
            keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    session.users().getUserByUsername("login-test", appRealm).setEnabled(true);
                }
            });
        }
    }

    @Test
    public void loginInvalidUsername() {
        loginPage.open();
        loginPage.login("invalid", "password");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assert.assertEquals("invalid", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().user((String) null).session((String) null).error("user_not_found")
                .detail(Details.USERNAME, "invalid")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    public void loginMissingUsername() {
        loginPage.open();
        loginPage.missingUsername();

        loginPage.assertCurrent();

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().user((String) null).session((String) null).error("user_not_found")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    public void loginSuccess() {
        loginPage.open();
        loginPage.login("login-test", "password");
        
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginPromptNone() {
        driver.navigate().to(oauth.getLoginFormUrl().toString() + "&prompt=none");

        assertFalse(loginPage.isCurrent());
        assertTrue(appPage.isCurrent());

        loginPage.open();
        loginPage.login("login-test", "password");
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();

        driver.navigate().to(oauth.getLoginFormUrl().toString() + "&prompt=none");
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().user(userId).removeDetail(Details.USERNAME).assertEvent();
    }
    
    @Test
    public void loginWithForcePasswordChangePolicy() {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setPasswordPolicy(new PasswordPolicy("forceExpiredPasswordChange(1)"));
            }
        });
        
        try {
            // Setting offset to more than one day to force password update
            // elapsedTime > timeToExpire
            Time.setOffset(86405);
            
            loginPage.open();

            loginPage.login("login-test", "password");
            
            updatePasswordPage.assertCurrent();
            
            updatePasswordPage.changePassword("updatedPassword", "updatedPassword");

            events.expectRequiredAction(EventType.UPDATE_PASSWORD).user(userId).detail(Details.USERNAME, "login-test").assertEvent();
            
            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
            
        } finally {
            keycloakRule.update(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    appRealm.setPasswordPolicy(new PasswordPolicy(null));
                    
                    UserModel user = manager.getSession().users().getUserByUsername("login-test", appRealm);
                    UserCredentialModel cred = new UserCredentialModel();
                    cred.setType(CredentialRepresentation.PASSWORD);
                    cred.setValue("password");
                    user.updateCredential(cred);
                }
            });
            Time.setOffset(0);
        }
    }
    
    @Test
    public void loginWithoutForcePasswordChangePolicy() {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setPasswordPolicy(new PasswordPolicy("forceExpiredPasswordChange(1)"));
            }
        });
        
        try {
            // Setting offset to less than one day to avoid forced password update
            // elapsedTime < timeToExpire
            Time.setOffset(86205);
            
            loginPage.open();

            loginPage.login("login-test", "password");
            
            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

            events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
            
        } finally {
            keycloakRule.update(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    appRealm.setPasswordPolicy(new PasswordPolicy(null));
                }
            });
            Time.setOffset(0);
        }
    }
    
    @Test
    public void loginNoTimeoutWithLongWait() {
        try {
            loginPage.open();

            Time.setOffset(1700);

            loginPage.login("login-test", "password");

            events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();
        } finally {
            Time.setOffset(0);
        }
    }

    @Test
    public void loginTimeout() {
        try {
            loginPage.open();

            Time.setOffset(1850);

            loginPage.login("login-test", "password");

            events.expectLogin().clearDetails().detail(Details.CODE_ID, AssertEvents.isCodeId()).user((String) null).session((String) null).error("expired_code").assertEvent().getSessionId();
        } finally {
            Time.setOffset(0);
        }
    }

    @Test
    public void loginLoginHint() {
        String loginFormUrl = oauth.getLoginFormUrl() + "&login_hint=login-test";
        driver.navigate().to(loginFormUrl);

        Assert.assertEquals("login-test", loginPage.getUsername());
        loginPage.login("password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginWithEmailSuccess() {
        loginPage.open();
        loginPage.login("login@test.com", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).assertEvent();
    }

    @Test
    public void loginWithRememberMe() {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setRememberMe(true);
            }
        });

        try {
            loginPage.open();
            assertFalse(loginPage.isRememberMeChecked());
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login-test", "password");

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
            Event loginEvent = events.expectLogin().user(userId)
                    .detail(Details.USERNAME, "login-test")
                    .detail(Details.REMEMBER_ME, "true")
                    .assertEvent();
            String sessionId = loginEvent.getSessionId();

            // Expire session
            keycloakRule.removeUserSession(sessionId);

            // Assert rememberMe checked and username/email prefilled
            loginPage.open();
            assertTrue(loginPage.isRememberMeChecked());
            Assert.assertEquals("login-test", loginPage.getUsername());

            loginPage.setRememberMe(false);
        } finally {
            keycloakRule.update(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    appRealm.setRememberMe(false);
                }
            });
        }
    }

    // KEYCLOAK-1037
    @Test
    public void loginExpiredCode() {
        try {
            loginPage.open();
            Time.setOffset(5000);
            keycloakRule.update(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                   manager.getSession().sessions().removeExpiredUserSessions(appRealm);
                }
            });

            loginPage.login("login@test.com", "password");

            //loginPage.assertCurrent();
            loginPage.assertCurrent();

            //Assert.assertEquals("Login timeout. Please login again.", loginPage.getError());

            events.expectLogin().user((String) null).session((String) null).error("expired_code").clearDetails()
                    .detail(Details.RESTART_AFTER_TIMEOUT, "true")
                    .client((String) null)
                    .assertEvent();

        } finally {
            Time.setOffset(0);
        }
    }

}
