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
import org.junit.ClassRule;
import org.junit.Rule;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.broker.util.UserSessionStatusServlet;
import org.keycloak.testsuite.broker.util.UserSessionStatusServlet.UserSessionStatus;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author pedroigor
 */
public abstract class AbstractIdentityProviderTest {

    @ClassRule
    public static AbstractKeycloakRule brokerServerRule = new AbstractKeycloakRule() {

        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            server.importRealm(getClass().getResourceAsStream("/broker-test/test-realm-with-broker.json"));
            URL url = getClass().getResource("/broker-test/test-app-keycloak.json");
            deployApplication("test-app", "/test-app", UserSessionStatusServlet.class, url.getPath(), "manager");
        }
    };

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    private WebDriver driver;

    @WebResource
    private LoginPage loginPage;

    @WebResource
    private LoginUpdateProfilePage updateProfilePage;

    protected void assertSuccessfulAuthentication(String providerId) {
        this.driver.navigate().to("http://localhost:8081/test-app/");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/login"));

        // choose the identity provider
        this.loginPage.clickSocial(providerId);

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/realms/realm-with-saml-identity-provider/protocol/saml"));

        // log in to identity provider
        this.loginPage.login("saml.user", "password");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/broker/realm-with-broker/" + providerId));

        // update profile
        this.updateProfilePage.assertCurrent();

        String userEmail = "new@email.com";
        String userFirstName = "New first";
        String userLastName = "New last";

        this.updateProfilePage.update(userFirstName, userLastName, userEmail);

        // authenticated and redirected to app
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app/"));

        KeycloakSession samlServerSession = brokerServerRule.startSession();
        RealmModel brokerRealm = samlServerSession.realms().getRealm("realm-with-broker");

        UserModel federatedUser = samlServerSession.users().getUserByEmail(userEmail, brokerRealm);

        // user created
        assertNotNull(federatedUser);
        assertEquals(userFirstName, federatedUser.getFirstName());
        assertEquals(userLastName, federatedUser.getLastName());

        driver.navigate().to("http://localhost:8081/test-app/logout");
        driver.navigate().to("http://localhost:8081/test-app/");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/login"));

        // choose the identity provider
        this.loginPage.clickSocial(providerId);

        // already authenticated in saml idp and redirected to app
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app/"));
    }

    private UserSessionStatus retrieveSessionStatus() {
        UserSessionStatus sessionStatus = null;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String pageSource = this.driver.getPageSource();

            sessionStatus = objectMapper.readValue(pageSource.getBytes(), UserSessionStatus.class);

            assertNotNull(retrieveSessionStatus());
        } catch (IOException e) {
            throw new RuntimeException("Could not retrieve session status.", e);
        }

        return sessionStatus;
    }

}
