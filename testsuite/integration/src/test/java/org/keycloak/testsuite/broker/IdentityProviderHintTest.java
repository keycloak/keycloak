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

package org.keycloak.testsuite.broker;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import static org.junit.Assert.assertTrue;

/**
 * @author pedroigor
 */
public class IdentityProviderHintTest {

    @ClassRule
    public static BrokerKeyCloakRule keycloakRule = new BrokerKeyCloakRule();

    @ClassRule
    public static AbstractKeycloakRule samlServerRule = new AbstractKeycloakRule() {

        @Override
        protected void configureServer(KeycloakServer server) {
            server.getConfig().setPort(8082);
        }

        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            server.importRealm(getClass().getResourceAsStream("/broker-test/test-broker-realm-with-kc-oidc.json"));
        }

        @Override
        protected String[] getTestRealms() {
            return new String[] { "realm-with-oidc-identity-provider" };
        }
    };

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    private WebDriver driver;

    @WebResource
    private LoginPage loginPage;

    @WebResource
    private OAuthGrantPage grantPage;

    @Test
    public void testSuccessfulRedirect() {
        this.driver.navigate().to("http://localhost:8081/test-app?kc_idp_hint=kc-oidc-idp");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));

        // log in to identity provider
        this.loginPage.login("test-user", "password");

         // authenticated and redirected to app
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));
        assertTrue(this.driver.getPageSource().contains("idToken"));
    }
    
    @Test
    public void testSuccessfulRedirectToProviderHiddenOnLoginPage() {
        this.driver.navigate().to("http://localhost:8081/test-app?kc_idp_hint=kc-oidc-idp-hidden");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));
    }


    // KEYCLOAK-5260
    @Test
    public void testSuccessfulRedirectToProviderAfterLoginPageShown() {
        this.driver.navigate().to("http://localhost:8081/test-app");
        String loginPageUrl = driver.getCurrentUrl();
        assertTrue(loginPageUrl.startsWith("http://localhost:8081/auth/"));

        // Manually add "kc_idp_hint" to URL . Should redirect to provider
        loginPageUrl = loginPageUrl + "&kc_idp_hint=kc-oidc-idp-hidden";
        this.driver.navigate().to(loginPageUrl);
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));

        // Redirect from the app with the "kc_idp_hint". Should redirect to provider
        this.driver.navigate().to("http://localhost:8081/test-app?kc_idp_hint=kc-oidc-idp-hidden");
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));

        // Now redirect should't happen
        this.driver.navigate().to("http://localhost:8081/test-app");
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/"));
    }


    @Test
    public void testInvalidIdentityProviderHint() {
        this.driver.navigate().to("http://localhost:8081/test-app?kc_idp_hint=invalid-idp-id");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));

        System.out.println(driver.getPageSource());
        assertTrue(driver.getTitle().equals("Log in to realm-with-broker"));
    }

    private AuthenticationExecutionInfoRepresentation findExecution(RealmResource realm) {
            for (AuthenticationExecutionInfoRepresentation e : realm.flows().getExecutions("browser")) {
                if (e.getProviderId().equals("identity-provider-redirector")) {
                    return e;
                }
            }
        return null;
    }
}
