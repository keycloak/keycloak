/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.broker;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.IDToken;
import org.keycloak.testsuite.broker.util.UserSessionStatusServlet.UserSessionStatus;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.thoughtworks.selenium.SeleneseTestBase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author pedroigor
 */
public abstract class AbstractIdentityProviderTest {

    @ClassRule
    public static BrokerKeyCloakRule brokerServerRule = new BrokerKeyCloakRule();

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    private WebDriver driver;

    @WebResource
    private LoginPage loginPage;

    @WebResource
    private LoginUpdateProfilePage updateProfilePage;

    private KeycloakSession session;

    @Before
    public void onBefore() {
        this.session = brokerServerRule.startSession();
        removeTestUsers();
        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();
    }

    @After
    public void onAfter() {
        brokerServerRule.stopSession(this.session, true);
    }

    @Test
    public void testSuccessfulAuthentication() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();

        identityProviderModel.setUpdateProfileFirstLogin(true);

        assertSuccessfulAuthentication(identityProviderModel);
    }

    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();

        identityProviderModel.setUpdateProfileFirstLogin(false);

        assertSuccessfulAuthentication(identityProviderModel);
    }

    @Test
    public void testDisabled() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();

        identityProviderModel.setEnabled(false);

        this.driver.navigate().to("http://localhost:8081/test-app/");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/login"));

        try {
            this.driver.findElement(By.className(getProviderId()));
            fail("Provider [" + getProviderId() + "] not disabled.");
        } catch (NoSuchElementException nsee) {

        }
    }

    @Test
    public void testUserAlreadyExistsWhenUpdatingProfile() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();

        identityProviderModel.setUpdateProfileFirstLogin(true);

        this.driver.navigate().to("http://localhost:8081/test-app/");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/login"));

        // choose the identity provider
        this.loginPage.clickSocial(getProviderId());

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));

        // log in to identity provider
        this.loginPage.login("test-user", "password");

        doAfterProviderAuthentication();

        this.updateProfilePage.assertCurrent();
        this.updateProfilePage.update("Test", "User", "psilva@redhat.com");

        WebElement element = this.driver.findElement(By.className("kc-feedback-text"));

        assertNotNull(element);

        assertEquals("Email already exists", element.getText());

        this.updateProfilePage.assertCurrent();
        this.updateProfilePage.update("Test", "User", "test-user@redhat.com");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));

        UserModel federatedUser = getFederatedUser();

        assertNotNull(federatedUser);
    }

    @Test
    public void testUserAlreadyExistsWhenNotUpdatingProfile() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();

        identityProviderModel.setUpdateProfileFirstLogin(false);

        this.driver.navigate().to("http://localhost:8081/test-app/");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/login"));

        // choose the identity provider
        this.loginPage.clickSocial(getProviderId());

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));

        // log in to identity provider
        this.loginPage.login("pedroigor", "password");

        doAfterProviderAuthentication();

        WebElement element = this.driver.findElement(By.className("kc-feedback-text"));

        assertNotNull(element);

        assertEquals("User with email already exists. Please login to account management to link the account.", element.getText());
    }

    private void assertSuccessfulAuthentication(IdentityProviderModel identityProviderModel) {
        driver.navigate().to("http://localhost:8081/test-app");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/login"));

        // choose the identity provider
        this.loginPage.clickSocial(getProviderId());

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));

        // log in to identity provider
        this.loginPage.login("test-user", "password");

        doAfterProviderAuthentication();

        if (identityProviderModel.isUpdateProfileFirstLogin()) {
            String userEmail = "new@email.com";
            String userFirstName = "New first";
            String userLastName = "New last";

            // update profile
            this.updateProfilePage.assertCurrent();
            this.updateProfilePage.update(userFirstName, userLastName, userEmail);
        }

        // authenticated and redirected to app
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));

        UserModel federatedUser = getFederatedUser();

        assertNotNull(federatedUser);

        doAssertFederatedUser(federatedUser);

        RealmModel realm = getRealm();

        Set<FederatedIdentityModel> federatedIdentities = this.session.users().getFederatedIdentities(federatedUser, realm);

        assertEquals(1, federatedIdentities.size());

        FederatedIdentityModel federatedIdentityModel = federatedIdentities.iterator().next();

        assertEquals(getProviderId(), federatedIdentityModel.getIdentityProvider());
        assertEquals(federatedUser.getUsername(), federatedIdentityModel.getUserName());

        driver.navigate().to("http://localhost:8081/test-app/logout");
        driver.navigate().to("http://localhost:8081/test-app");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/login"));
    }

    protected UserModel getFederatedUser() {
        UserSessionStatus userSessionStatus = retrieveSessionStatus();
        IDToken idToken = userSessionStatus.getIdToken();
        KeycloakSession samlServerSession = brokerServerRule.startSession();
        RealmModel brokerRealm = samlServerSession.realms().getRealm("realm-with-broker");

        return samlServerSession.users().getUserById(idToken.getSubject(), brokerRealm);
    }

    protected void doAfterProviderAuthentication() {

    }

    protected abstract String getProviderId();

    protected IdentityProviderModel getIdentityProviderModel() {
        IdentityProviderModel identityProviderModel = getRealm().getIdentityProviderById(getProviderId());

        assertNotNull(identityProviderModel);

        return identityProviderModel;
    }

    private RealmModel getRealm() {
        return this.session.realms().getRealm("realm-with-broker");
    }

    protected void doAssertFederatedUser(UserModel federatedUser) {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();

        if (identityProviderModel.isUpdateProfileFirstLogin()) {
            String userEmail = "new@email.com";
            String userFirstName = "New first";
            String userLastName = "New last";

            assertEquals(userEmail, federatedUser.getEmail());
            assertEquals(userFirstName, federatedUser.getFirstName());
            assertEquals(userLastName, federatedUser.getLastName());
        } else {
            assertEquals("test-user@localhost", federatedUser.getEmail());
            assertEquals("Test", federatedUser.getFirstName());
            assertEquals("User", federatedUser.getLastName());
        }
    }

    private UserSessionStatus retrieveSessionStatus() {
        UserSessionStatus sessionStatus = null;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String pageSource = this.driver.getPageSource();

            sessionStatus = objectMapper.readValue(pageSource.getBytes(), UserSessionStatus.class);
        } catch (IOException ignore) {
            ignore.printStackTrace();
        }

        return sessionStatus;
    }

    private void removeTestUsers() {
        RealmModel realm = getRealm();
        List<UserModel> users = this.session.users().getUsers(realm);

        for (UserModel user : users) {
            Set<FederatedIdentityModel> identities = this.session.users().getFederatedIdentities(user, realm);

            for (FederatedIdentityModel fedIdentity : identities) {
                this.session.users().removeFederatedIdentity(realm, user, fedIdentity.getIdentityProvider());
            }

            if (!user.getUsername().equals("pedroigor")) {
                this.session.users().removeUser(realm, user);
            }
        }
    }
}
