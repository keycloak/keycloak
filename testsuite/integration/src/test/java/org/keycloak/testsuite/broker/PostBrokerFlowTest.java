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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.authentication.authenticators.broker.IdpEmailVerificationAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.testsuite.pages.IdpConfirmLinkPage;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PostBrokerFlowTest extends AbstractIdentityProviderTest {

    private static final int PORT = 8082;

    private static String POST_BROKER_FLOW_ID;

    private static final String APP_REALM_ID = "realm-with-broker";

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

            RealmModel realmWithBroker = getRealm(session);

            // Disable "idp-email-verification" authenticator in firstBrokerLogin flow. Disable updateProfileOnFirstLogin page
            AbstractFirstBrokerLoginTest.setExecutionRequirement(realmWithBroker, DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_HANDLE_EXISTING_SUBFLOW,
                    IdpEmailVerificationAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.DISABLED);
            setUpdateProfileFirstLogin(realmWithBroker, IdentityProviderRepresentation.UPFLM_OFF);

            // Add post-broker flow with OTP authenticator to the realm
            AuthenticationFlowModel postBrokerFlow = new AuthenticationFlowModel();
            postBrokerFlow.setAlias("post-broker");
            postBrokerFlow.setDescription("post-broker flow with OTP");
            postBrokerFlow.setProviderId("basic-flow");
            postBrokerFlow.setTopLevel(true);
            postBrokerFlow.setBuiltIn(false);
            postBrokerFlow = realmWithBroker.addAuthenticationFlow(postBrokerFlow);

            POST_BROKER_FLOW_ID = postBrokerFlow.getId();

            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(postBrokerFlow.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator("auth-otp-form");
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);
            realmWithBroker.addAuthenticatorExecution(execution);

        }

        @Override
        protected String[] getTestRealms() {
            return new String[] { "realm-with-oidc-identity-provider", "realm-with-saml-idp-basic" };
        }
    };


    @WebResource
    protected IdpConfirmLinkPage idpConfirmLinkPage;

    @WebResource
    protected LoginTotpPage loginTotpPage;

    @WebResource
    protected LoginConfigTotpPage totpPage;

    private TimeBasedOTP totp = new TimeBasedOTP();


    @Override
    protected String getProviderId() {
        return "kc-oidc-idp";
    }


    @Test
    public void testPostBrokerLoginWithOTP() {
        // enable post-broker flow
        IdentityProviderModel identityProvider = getIdentityProviderModel();
        setPostBrokerFlowForProvider(identityProvider, getRealm(), true);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        // login with broker and assert that OTP needs to be set.
        loginIDP("test-user");
        totpPage.assertCurrent();
        String totpSecret = totpPage.getTotpSecret();
        totpPage.configure(totp.generateTOTP(totpSecret));

        assertFederatedUser("test-user", "test-user@localhost", "test-user", getProviderId());

        driver.navigate().to("http://localhost:8081/test-app/logout");

        // Login again and assert that OTP needs to be provided.
        loginIDP("test-user");
        loginTotpPage.assertCurrent();
        loginTotpPage.login(totp.generateTOTP(totpSecret));

        assertFederatedUser("test-user", "test-user@localhost", "test-user", getProviderId());

        driver.navigate().to("http://localhost:8081/test-app/logout");

        // Disable post-broker and ensure that OTP is not required anymore
        setPostBrokerFlowForProvider(identityProvider, getRealm(), false);
        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        loginIDP("test-user");
        assertFederatedUser("test-user", "test-user@localhost", "test-user", getProviderId());
        driver.navigate().to("http://localhost:8081/test-app/logout");
    }


    @Test
    public void testBrokerReauthentication_samlBrokerWithOTPRequired() throws Exception {
        RealmModel realmWithBroker = getRealm();

        // Enable OTP just for SAML provider
        IdentityProviderModel samlIdentityProvider = realmWithBroker.getIdentityProviderByAlias("kc-saml-idp-basic");
        setPostBrokerFlowForProvider(samlIdentityProvider, realmWithBroker, true);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        // ensure TOTP setup is required during SAML broker firstLogin and during reauthentication for link OIDC broker too
        reauthenticateOIDCWithSAMLBroker(true, false);

        // Disable TOTP for SAML provider
        realmWithBroker = getRealm();
        samlIdentityProvider = realmWithBroker.getIdentityProviderByAlias("kc-saml-idp-basic");
        setPostBrokerFlowForProvider(samlIdentityProvider, realmWithBroker, false);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();
    }

    @Test
    public void testBrokerReauthentication_oidcBrokerWithOTPRequired() throws Exception {

        // Enable OTP just for OIDC provider
        IdentityProviderModel oidcIdentityProvider = getIdentityProviderModel();
        setPostBrokerFlowForProvider(oidcIdentityProvider, getRealm(), true);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        // ensure TOTP setup is not required during SAML broker firstLogin, but during reauthentication for link OIDC broker
        reauthenticateOIDCWithSAMLBroker(false, true);

        // Disable TOTP for SAML provider
        oidcIdentityProvider = getIdentityProviderModel();
        setPostBrokerFlowForProvider(oidcIdentityProvider, getRealm(), false);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();
    }

    @Test
    public void testBrokerReauthentication_bothBrokerWithOTPRequired() throws Exception {
        RealmModel realmWithBroker = getRealm();

        // Enable OTP for both OIDC and SAML provider
        IdentityProviderModel samlIdentityProvider = realmWithBroker.getIdentityProviderByAlias("kc-saml-idp-basic");
        setPostBrokerFlowForProvider(samlIdentityProvider, realmWithBroker, true);

        IdentityProviderModel oidcIdentityProvider = getIdentityProviderModel();
        setPostBrokerFlowForProvider(oidcIdentityProvider, getRealm(), true);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        // ensure TOTP setup is required during SAML broker firstLogin and during reauthentication for link OIDC broker too
        reauthenticateOIDCWithSAMLBroker(true, true);

        // Disable TOTP for both SAML and OIDC provider
        realmWithBroker = getRealm();
        samlIdentityProvider = realmWithBroker.getIdentityProviderByAlias("kc-saml-idp-basic");
        setPostBrokerFlowForProvider(samlIdentityProvider, realmWithBroker, false);

        oidcIdentityProvider = getIdentityProviderModel();
        setPostBrokerFlowForProvider(oidcIdentityProvider, getRealm(), false);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();
    }


    private void reauthenticateOIDCWithSAMLBroker(boolean samlBrokerTotpEnabled, boolean oidcBrokerTotpEnabled) {
        // First login as "testuser" with SAML broker
        driver.navigate().to("http://localhost:8081/test-app");
        this.loginPage.clickSocial("kc-saml-idp-basic");
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));
        Assert.assertEquals("Log in to realm-with-saml-idp-basic", this.driver.getTitle());
        this.loginPage.login("test-user", "password");

        // Ensure user needs to setup TOTP if SAML broker requires that
        String totpSecret = null;
        if (samlBrokerTotpEnabled) {
            totpPage.assertCurrent();
            totpSecret = totpPage.getTotpSecret();
            totpPage.configure(totp.generateTOTP(totpSecret));
        }

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));
        driver.navigate().to("http://localhost:8081/test-app/logout");

        // login through OIDC broker now
        loginIDP("test-user");

        this.idpConfirmLinkPage.assertCurrent();
        Assert.assertEquals("User with email test-user@localhost already exists. How do you want to continue?", this.idpConfirmLinkPage.getMessage());
        this.idpConfirmLinkPage.clickLinkAccount();

        // assert reauthentication with login page. On login page is link to kc-saml-idp-basic as user has it linked already
        Assert.assertEquals("Log in to " + APP_REALM_ID, this.driver.getTitle());
        Assert.assertEquals("Authenticate as test-user to link your account with " + getProviderId(), this.loginPage.getInfoMessage());

        // reauthenticate with SAML broker. OTP authentication is required as well
        this.loginPage.clickSocial("kc-saml-idp-basic");
        Assert.assertEquals("Log in to realm-with-saml-idp-basic", this.driver.getTitle());
        this.loginPage.login("test-user", "password");

        if (samlBrokerTotpEnabled) {
            // User already set TOTP during first login with SAML broker
            loginTotpPage.assertCurrent();
            loginTotpPage.login(totp.generateTOTP(totpSecret));
        } else if (oidcBrokerTotpEnabled) {
            // User needs to set TOTP as first login with SAML broker didn't require that
            totpPage.assertCurrent();
            totpSecret = totpPage.getTotpSecret();
            totpPage.configure(totp.generateTOTP(totpSecret));
        }

        // authenticated and redirected to app. User is linked with both identity providers
        assertFederatedUser("test-user", "test-user@localhost", "test-user", getProviderId(), "kc-saml-idp-basic");
    }

    private void setPostBrokerFlowForProvider(IdentityProviderModel identityProvider, RealmModel realm, boolean enable) {
        if (enable) {
            identityProvider.setPostBrokerLoginFlowId(POST_BROKER_FLOW_ID);
        } else {
            identityProvider.setPostBrokerLoginFlowId(null);
        }
        realm.updateIdentityProvider(identityProvider);
    }

    private void assertFederatedUser(String expectedUsername, String expectedEmail, String expectedFederatedUsername, String... expectedLinkedProviders) {
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));
        UserModel federatedUser = getFederatedUser();

        assertNotNull(federatedUser);
        assertEquals(expectedUsername, federatedUser.getUsername());
        assertEquals(expectedEmail, federatedUser.getEmail());

        RealmModel realmWithBroker = getRealm();
        Set<FederatedIdentityModel> federatedIdentities = this.session.users().getFederatedIdentities(federatedUser, realmWithBroker);

        List<String> expectedProvidersList = Arrays.asList(expectedLinkedProviders);
        assertEquals(expectedProvidersList.size(), federatedIdentities.size());
        for (FederatedIdentityModel federatedIdentityModel : federatedIdentities) {
            String providerAlias = federatedIdentityModel.getIdentityProvider();
            Assert.assertTrue(expectedProvidersList.contains(providerAlias));
            assertEquals(expectedFederatedUsername, federatedIdentityModel.getUserName());
        }
    }

}
