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

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.authentication.authenticators.broker.IdpEmailVerificationAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.openqa.selenium.NoSuchElementException;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCFirstBrokerLoginTest extends AbstractFirstBrokerLoginTest {

    private static final int PORT = 8082;

    @ClassRule
    public static AbstractKeycloakRule samlServerRule = new AbstractKeycloakRule() {

        @Override
        protected void configureServer(KeycloakServer server) {
            server.getConfig().setPort(PORT);
        }

        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            server.importRealm(getClass().getResourceAsStream("/broker-test/test-broker-realm-with-kc-oidc.json"));
            server.importRealm(getClass().getResourceAsStream("/broker-test/test-broker-realm-with-saml.json"));
        }

        @Override
        protected String[] getTestRealms() {
            return new String[] { "realm-with-oidc-identity-provider", "realm-with-saml-idp-basic" };
        }
    };

    @Override
    protected String getProviderId() {
        return "kc-oidc-idp";
    }


    /**
     * Tests that duplication is detected and user wants to link federatedIdentity with existing account. He will confirm link by reauthentication
     * with different broker already linked to his account
     */
    @Test
    public void testLinkAccountByReauthenticationWithDifferentBroker() throws Exception {
        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                setExecutionRequirement(realmWithBroker, DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_HANDLE_EXISTING_SUBFLOW,
                        IdpEmailVerificationAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.DISABLED);

                setUpdateProfileFirstLogin(realmWithBroker, IdentityProviderRepresentation.UPFLM_OFF);
            }

        }, APP_REALM_ID);

        // First link "pedroigor" user with SAML broker and logout
        driver.navigate().to("http://localhost:8081/test-app");
        this.loginPage.clickSocial("kc-saml-idp-basic");
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));
        Assert.assertEquals("Log in to realm-with-saml-idp-basic", this.driver.getTitle());
        this.loginPage.login("pedroigor", "password");

        this.idpConfirmLinkPage.assertCurrent();
        Assert.assertEquals("User with email psilva@redhat.com already exists. How do you want to continue?", this.idpConfirmLinkPage.getMessage());
        this.idpConfirmLinkPage.clickLinkAccount();

        this.loginPage.login("password");
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));
        driver.navigate().to("http://localhost:8081/test-app/logout");


        // login through OIDC broker now
        loginIDP("pedroigor");

        this.idpConfirmLinkPage.assertCurrent();
        Assert.assertEquals("User with email psilva@redhat.com already exists. How do you want to continue?", this.idpConfirmLinkPage.getMessage());
        this.idpConfirmLinkPage.clickLinkAccount();

        // assert reauthentication with login page. On login page is link to kc-saml-idp-basic as user has it linked already
        Assert.assertEquals("Log in to " + APP_REALM_ID, this.driver.getTitle());
        Assert.assertEquals("Authenticate as pedroigor to link your account with " + getProviderId(), this.loginPage.getInfoMessage());

        try {
            this.loginPage.findSocialButton(getProviderId());
            Assert.fail("Not expected to see social button with " + getProviderId());
        } catch (NoSuchElementException expected) {
        }

        // reauthenticate with SAML broker
        this.loginPage.clickSocial("kc-saml-idp-basic");
        Assert.assertEquals("Log in to realm-with-saml-idp-basic", this.driver.getTitle());
        this.loginPage.login("pedroigor", "password");


        // authenticated and redirected to app. User is linked with identity provider
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));
        UserModel federatedUser = getFederatedUser();

        assertNotNull(federatedUser);
        assertEquals("pedroigor", federatedUser.getUsername());
        assertEquals("psilva@redhat.com", federatedUser.getEmail());

        RealmModel realmWithBroker = getRealm();
        Set<FederatedIdentityModel> federatedIdentities = this.session.users().getFederatedIdentities(federatedUser, realmWithBroker);
        assertEquals(2, federatedIdentities.size());

        for (FederatedIdentityModel link : federatedIdentities) {
            Assert.assertEquals("pedroigor", link.getUserName());
            Assert.assertTrue(link.getIdentityProvider().equals(getProviderId()) || link.getIdentityProvider().equals("kc-saml-idp-basic"));
        }

        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                setExecutionRequirement(realmWithBroker, DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_HANDLE_EXISTING_SUBFLOW,
                        IdpEmailVerificationAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.ALTERNATIVE);

            }

        }, APP_REALM_ID);
    }

}
