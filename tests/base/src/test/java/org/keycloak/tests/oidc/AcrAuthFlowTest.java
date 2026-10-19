/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.oidc;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalUserConfiguredAuthenticatorFactory;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.services.clientpolicy.condition.AcrCondition;
import org.keycloak.services.clientpolicy.condition.AcrConditionFactory;
import org.keycloak.services.clientpolicy.executor.AuthenticationFlowSelectorExecutor;
import org.keycloak.services.clientpolicy.executor.AuthenticationFlowSelectorExecutorFactory;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.AuthenticationExecutionExportBuilder;
import org.keycloak.testframework.realm.AuthenticationFlowBuilder;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginConfigTotpPage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.ClientPoliciesUtil;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author <a href="mailto:ggrazian@redhat.com">Giuseppe Graziano</a>
 */
@KeycloakIntegrationTest
public class AcrAuthFlowTest extends AbstractOIDCScopeTest {

    @InjectRealm(config = AcrAuthFlowRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectUser(config = AcrUserConfig.class)
    ManagedUser user;

    @InjectUser(ref = "no-otp-user", config = NoOtpUserConfig.class)
    ManagedUser noOtpUser;

    @InjectClient(config = AcrClientConfig.class)
    ManagedClient client;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginConfigTotpPage loginConfigTotpPage;

    @InjectPage
    RegisterPage registerPage;

    // config
    private static String TOTP_SECRET = "totpsecret";
    private static String PASSWORD_FLOW_ALIAS = "password-flow";
    private static String PASSWORD_OTP_FLOW_ALIAS = "password-otp-flow";

    private TimeBasedOTP totp = new TimeBasedOTP();

    /**
     * Test the ACR auth flow map for the password auth flow
     * Expected: ACR = "acr-password"
     */
    @Test
    public void testAuthFlow() {
        setAcrClientPolicy("acr-password", PASSWORD_FLOW_ALIAS, 2);
        setAcrClientPolicy("acr-otp", PASSWORD_OTP_FLOW_ALIAS, 3);

        loginWithAcr(new ArrayList<>() {{
            add("acr-password");
        }});

        authenticatePassword(user.getPassword());
        Tokens tokens = assertLoginWithAcr(user.getId(), "acr-password");

        logout(user.getId(), tokens);
    }

    @Test
    public void testAuthFlowWithoutLoaConfig() {
        setAcrClientPolicy("acr-password", PASSWORD_FLOW_ALIAS);

        loginWithAcr(new ArrayList<>() {{
            add("acr-password");
        }});

        authenticatePassword(user.getPassword());
        Tokens tokens = assertLoginWithAcr(user.getId(), "default");

        logout(user.getId(), tokens);
    }


    /**
     * Test the ACR auth flow mapping feature for an alternate otp auth flow
     * Expected: ACR = "acr-otp"
     */
    @Test
    public void testAuthFlowOTP() {

        setAcrClientPolicy("acr-password", PASSWORD_FLOW_ALIAS, 2);
        setAcrClientPolicy("acr-otp", PASSWORD_OTP_FLOW_ALIAS, 3);

        loginWithAcr(new ArrayList<>() {{
            add("acr-otp");
        }});

        authenticatePassword(user.getPassword());
        authenticateTOTP(TOTP_SECRET);
        Tokens tokens = assertLoginWithAcr(user.getId(), "acr-otp");

        logout(user.getId(), tokens);
    }

    /**
     * Test fallback to default flow when no valid mapping is found. Ensure acr is default value 1
     * Expected: ACR = default with the default acr-loa mapping behavior
     */
    @Test
    public void testNoMapping() {

        loginWithAcr(new ArrayList<>() {{
            add("acr-password");
        }});

        authenticatePassword(user.getPassword());
        authenticateTOTP(TOTP_SECRET);
        Tokens tokens = assertLoginWithAcr(user.getId(), "default");

        logout(user.getId(), tokens);
    }

    /**
     * Test sessions when using ACR flow mapping
     * <p>
     * Expected: Re-authentication forces user to redo authenticators for newly specified flow
     */
    @Test
    public void testSessionReAuth() {
        Tokens tokens;

        setAcrClientPolicy("acr-password", PASSWORD_FLOW_ALIAS, 2);
        setAcrClientPolicy("acr-otp", PASSWORD_OTP_FLOW_ALIAS, 3);

        // initial login
        loginWithAcr(new ArrayList<>() {{
            add("acr-password");
        }});
        authenticatePassword(user.getPassword());
        assertLoginWithAcr(user.getId(), "acr-password");

        // ensure re-auth forced with different acr
        loginWithAcr(new ArrayList<>() {{
            add("acr-otp");
        }});
        authenticatePassword(user.getPassword());
        authenticateTOTP(TOTP_SECRET);
        tokens = assertLoginWithAcr(user.getId(), "acr-otp");

        logout(user.getId(), tokens);
    }

    @Test
    public void test2FASetupBypassViaSessionRestart() {
        startForcedOtpSetup();

        restartAuthenticationSession();
        authenticatePassword(noOtpUser.getUsername(), noOtpUser.getPassword());
        loginConfigTotpPage.assertCurrent();
    }

    @Test
    public void test2FASetupBypassViaSessionRestartDuringReAuth() {
        setAcrClientPolicy("acr-otp", PASSWORD_OTP_FLOW_ALIAS, 3);

        // Log in with password first, so the restart below goes through the re-authentication branch
        loginWithAcr(List.of("acr-password"));
        authenticatePassword(noOtpUser.getUsername(), noOtpUser.getPassword());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());
        Assertions.assertEquals(1, noOtpUser.admin().getUserSessions().size());

        loginWithAcr(List.of("acr-otp"));
        authenticatePassword(noOtpUser.getUsername(), noOtpUser.getPassword());
        loginConfigTotpPage.assertCurrent();

        restartAuthenticationSession();
        Assertions.assertTrue(noOtpUser.admin().getUserSessions().isEmpty());
        authenticatePassword(noOtpUser.getUsername(), noOtpUser.getPassword());
        loginConfigTotpPage.assertCurrent();
    }

    @Test
    public void test2FASetupBypassViaRestartCookie() {
        startForcedOtpSetup();

        // Without AUTH_SESSION_ID the session is rebuilt from the KC_RESTART cookie
        driver.driver().manage().deleteCookieNamed(CookieType.AUTH_SESSION_ID.getName());
        driver.open(driver.getCurrentUrl());
        authenticatePassword(noOtpUser.getUsername(), noOtpUser.getPassword());
        loginConfigTotpPage.assertCurrent();
    }

    @Test
    public void test2FASetupEnforcedLoaSurvivesRestart() {
        startForcedOtpSetup();

        restartAuthenticationSession();
        authenticatePassword(noOtpUser.getUsername(), noOtpUser.getPassword());
        configureTOTP();

        AccessTokenResponse response = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("acr-otp", oauth.verifyToken(response.getAccessToken()).getAcr());
    }

    private void startForcedOtpSetup() {
        setAcrClientPolicy("acr-otp", PASSWORD_OTP_FLOW_ALIAS, 3);
        loginWithAcr(List.of("acr-otp"));
        authenticatePassword(noOtpUser.getUsername(), noOtpUser.getPassword());
        loginConfigTotpPage.assertCurrent();
    }

    private void restartAuthenticationSession() {
        // Swap the current login-actions sub-path for "restart", keeping the client_id/tab_id already in the URL
        driver.open(driver.getCurrentUrl().replaceFirst("/login-actions/[^/?]+", "/login-actions/restart"));
    }

    private void configureTOTP() {
        loginConfigTotpPage.assertCurrent();
        loginConfigTotpPage.configure(totp.generateTOTP(loginConfigTotpPage.getTotpSecret()));
    }

    private void setAcrClientPolicy(String acr, String alias) {
        setAcrClientPolicy(acr, alias, null);
    }

    private void setAcrClientPolicy(String acr, String alias, Integer loa) {

        try {
            ClientProfilesRepresentation clientProfiles = managedRealm.admin().clientPoliciesProfilesResource().getProfiles(false);
            AuthenticationFlowSelectorExecutor.Configuration aliasConfiguration = new AuthenticationFlowSelectorExecutor.Configuration();
            aliasConfiguration.setAuthFlowAlias(alias);
            if (loa != null) {
                aliasConfiguration.setAuthFlowLoa(loa);
            }
            ClientPoliciesUtil.ClientProfilesBuilder clientProfilesBuilder = new ClientPoliciesUtil.ClientProfilesBuilder()
                    .addProfile(
                            (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(alias, "")
                                    .addExecutor(AuthenticationFlowSelectorExecutorFactory.PROVIDER_ID, aliasConfiguration)
                                    .toRepresentation()
                    );
            clientProfiles.getProfiles().forEach(clientProfilesBuilder::addProfile);
            String json = clientProfilesBuilder.toString();

            clientProfiles = JsonSerialization.readValue(json, ClientProfilesRepresentation.class);
            managedRealm.admin().clientPoliciesProfilesResource().updateProfiles(clientProfiles);


            ClientPoliciesRepresentation clientPolicies = managedRealm.admin().clientPoliciesPoliciesResource().getPolicies(false);

            AcrCondition.Configuration acrConfiguration = new AcrCondition.Configuration();
            acrConfiguration.setAcrProperty(acr);

            // register policies
            ClientPoliciesUtil.ClientPoliciesBuilder clientPoliciesBuilder = new ClientPoliciesUtil.ClientPoliciesBuilder()
                    .addPolicy(
                            (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(alias, "", Boolean.TRUE)
                                    .addCondition(AcrConditionFactory.PROVIDER_ID,
                                            acrConfiguration)
                                    .addProfile(alias)
                                    .toRepresentation()
                    );

            clientPolicies.getPolicies().forEach(clientPoliciesBuilder::addPolicy);
            json = clientPoliciesBuilder.toString();

            clientPolicies = json == null ? null : JsonSerialization.readValue(json, ClientPoliciesRepresentation.class);
            managedRealm.admin().clientPoliciesPoliciesResource().updatePolicies(clientPolicies);
        } catch (Exception e) {
            Assertions.fail();
        }
    }


    @Test
    public void testAuthFlowEssential() {
        setAcrClientPolicy("acr-password", PASSWORD_FLOW_ALIAS, 2);
        setAcrClientPolicy("acr-otp", PASSWORD_OTP_FLOW_ALIAS, 3);

        loginWithAcr(List.of("acr-password"), true);

        authenticatePassword(user.getPassword());
        Tokens tokens = assertLoginWithAcr(user.getId(), "acr-password");

        logout(user.getId(), tokens);
    }

    @Test
    public void testAuthFlowOTPEssential() {
        setAcrClientPolicy("acr-password", PASSWORD_FLOW_ALIAS, 2);
        setAcrClientPolicy("acr-otp", PASSWORD_OTP_FLOW_ALIAS, 3);

        loginWithAcr(List.of("acr-otp"), true);

        authenticatePassword(user.getPassword());
        authenticateTOTP(TOTP_SECRET);
        Tokens tokens = assertLoginWithAcr(user.getId(), "acr-otp");

        logout(user.getId(), tokens);
    }

    /**
     * The level of the selected flow applies also when the flow contains a level condition which is skipped.
     * Expected: ACR = "acr-otp" for a user without OTP
     */
    @Test
    public void testAuthFlowWithSkippedLevelConditionEssential() {
        String flowAlias = "selected-step-up-flow";
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(flowAlias));
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(flowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.CONDITIONAL, subflow -> subflow
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ConditionalUserConfiguredAuthenticatorFactory.PROVIDER_ID)
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                        config -> {
                                            config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "2");
                                            config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(ConditionalLoaAuthenticator.DEFAULT_MAX_AGE));
                                        })
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, OTPFormAuthenticatorFactory.PROVIDER_ID))));
        setAcrClientPolicy("acr-otp", flowAlias, 3);

        loginWithAcr(List.of("acr-otp"), true);
        authenticatePassword("no-otp-user", noOtpUser.getPassword());
        Tokens tokens = assertLoginWithAcr(noOtpUser.getId(), "acr-otp");

        logout(noOtpUser.getId(), tokens);
    }

    /**
     * The level of the selected flow applies only to logins completing that flow, a registration does not reach it.
     * Expected: essential ACR requiring the level is rejected and no user is created
     */
    @Test
    public void testRegistrationDoesNotReachAuthFlowLoaEssential() {
        setAcrClientPolicy("acr-password", PASSWORD_FLOW_ALIAS, 2);
        setAcrClientPolicy("acr-otp", PASSWORD_OTP_FLOW_ALIAS, 3);
        managedRealm.updateWithCleanup(r -> r.registrationAllowed(true));

        loginWithAcr(List.of("acr-otp"), true);
        loginPage.clickRegister();
        registerPage.assertCurrent();
        registerPage.register("First", "Last", "registered@example.com", "registered", "password");

        errorPage.assertCurrent();
        Assertions.assertEquals("Authentication requirements not fulfilled", errorPage.getError());
        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .error(Errors.GENERIC_AUTHENTICATION_ERROR)
                .details(Details.AUTHENTICATION_ERROR_DETAIL, "Forced level of authentication did not meet the requirements. Requested level: 3, Fulfilled level: 1");
        Assertions.assertTrue(managedRealm.admin().users().searchByUsername("registered", true).isEmpty());
    }

    private void loginWithAcr(List<String> acrValues) {
        loginWithAcr(acrValues, false);
    }

    /**
     * Helper function to open the authentication page, requesting the specified acrValues. Optionally, specify the acr
     * claim as essential.
     *
     * @param acrValues The acr values to include in the authorization request
     * @param essential Specify that the acr claim is essential in the request
     */
    private void loginWithAcr(List<String> acrValues, boolean essential) {
        ClaimsRepresentation.ClaimValue<String> acrClaim = new ClaimsRepresentation.ClaimValue<>();
        acrClaim.setEssential(essential);
        acrClaim.setValues(acrValues);

        ClaimsRepresentation claims = new ClaimsRepresentation();
        claims.setIdTokenClaims(Collections.singletonMap(IDToken.ACR, acrClaim));

        oauth.client(client.getClientId(), "password");
        oauth.loginForm().claims(claims).open();
    }

    /**
     * Helper function to log out the specified user
     *
     * @param userId The keycloak identifier of the user
     * @param tokens The OIDC tokens received during login
     */
    private void logout(String userId, Tokens tokens) {
        // Logout
        oauth.doLogout(tokens.refreshToken);
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGOUT)
                .sessionId(tokens.idToken.getSessionState())
                .clientId(client.getClientId())
                .userId(userId)
                .withoutDetails(Details.REDIRECT_URI);
    }

    /**
     * Helper function to authenticate with a username and password
     *
     * @param password The password to log in with
     */
    private void authenticatePassword(String password) {
        authenticatePassword("test-user", password);
    }

    private void authenticatePassword(String username, String password) {
        loginPage.assertCurrent();
        loginPage.fillLogin(username, password);
        loginPage.submit();
    }

    /**
     * Helper function to authenticate with a TOTP token
     *
     * @param totpSecret The secret to use to generate the TOTP token
     */
    private void authenticateTOTP(String totpSecret) {
        loginTotpPage.assertCurrent();
        setOtpTimeOffset(TimeBasedOTP.DEFAULT_INTERVAL_SECONDS, totp);

        loginTotpPage.login(totp.generateTOTP(totpSecret));
    }
    
    private void setOtpTimeOffset(int offsetSeconds, TimeBasedOTP otp) {
        timeOffSet.set(offsetSeconds);
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, offsetSeconds);
        otp.setCalendar(calendar);
    }

    /**
     * Helper function to assert login completed successfully for the specified user
     *
     * @param userId      The keycloak ID of the user to check
     * @param expectedAcr The value expected in the 'acr' claim of the resulting token
     * @return The tokens from a successful login
     */
    private Tokens assertLoginWithAcr(String userId, String expectedAcr) {
        EventRepresentation loginEvent = EventAssertion.expectLoginSuccess(events.poll())
                .userId(userId).getEvent();

        Tokens tokens = sendTokenRequest(loginEvent, userId, "openid", client.getClientId());
        assertAcr(tokens.idToken, expectedAcr);
        assertAcr(tokens.accessToken, expectedAcr);

        return tokens;
    }

    /**
     * Helper function to assert the token contains the specified acr value
     *
     * @param token       The token to check (either access or ID)
     * @param expectedAcr The expected acr values in the token
     */
    private void assertAcr(IDToken token, String expectedAcr) {
        String acr = token.getAcr();
        if (expectedAcr != null) {
            Assertions.assertNotNull(acr);
        }

        Assertions.assertEquals(expectedAcr, acr);
    }

    private static class AcrAuthFlowRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name("test")
                    .authenticationFlows(
                            AuthenticationFlowBuilder.create(PASSWORD_FLOW_ALIAS, "Browser based authentication", AuthenticationFlow.BASIC_FLOW, true, false)
                                    .authenticationExecutions(AuthenticationExecutionExportBuilder.alias(
                                            PASSWORD_FLOW_ALIAS + " forms", AuthenticationExecutionModel.Requirement.ALTERNATIVE.name(), 30, false)),
                            AuthenticationFlowBuilder.create(PASSWORD_FLOW_ALIAS + " forms", "Username, password, otp and other auth forms.", AuthenticationFlow.BASIC_FLOW, false, false)
                                    .authenticationExecutions(AuthenticationExecutionExportBuilder.authenticator(
                                            UsernamePasswordFormFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.REQUIRED.name(), 10, false)),
                            AuthenticationFlowBuilder.create(PASSWORD_OTP_FLOW_ALIAS, "Browser based authentication", AuthenticationFlow.BASIC_FLOW, true, false)
                                    .authenticationExecutions(AuthenticationExecutionExportBuilder.alias(
                                            PASSWORD_OTP_FLOW_ALIAS + " forms", AuthenticationExecutionModel.Requirement.ALTERNATIVE.name(), 30, false)),
                            AuthenticationFlowBuilder.create(PASSWORD_OTP_FLOW_ALIAS + " forms", "Username, password, otp and other auth forms.", AuthenticationFlow.BASIC_FLOW, false, false)
                                    .authenticationExecutions(
                                            AuthenticationExecutionExportBuilder.authenticator(UsernamePasswordFormFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.REQUIRED.name(), 10, false),
                                            AuthenticationExecutionExportBuilder.authenticator(OTPFormAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.REQUIRED.name(), 20, false)))
                    .clientScopes(ClientScopeBuilder.create()
                            .name("acr-test-scope")
                            .protocol("openid-connect")
                            .attribute(ClientScopeModel.INCLUDE_IN_TOKEN_SCOPE, "false")
                            .attribute(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, "false")
                            .mappers(ProtocolMapperBuilder.create()
                                    .name("acr-test-mapper")
                                    .protocol("openid-connect")
                                    .protocolMapper("oidc-acr-mapper")
                                    .config("id.token.claim", "true")
                                    .config("access.token.claim", "true")
                                    .build()))
                    .otpCodeReusable(true)
                    .defaultClientScopes("acr-test-scope");
        }
    }

    private static class AcrUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("test-user")
                    .email("test-user@email.com")
                    .firstName("test-user")
                    .lastName("test-user")
                    .password("password")
                    .totpSecret(TOTP_SECRET);
        }
    }

    private static class NoOtpUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("no-otp-user")
                    .email("no-otp-user@email.com")
                    .firstName("no-otp-user")
                    .lastName("no-otp-user")
                    .password("password");
        }
    }

    private static class AcrClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("acr-client")
                    .secret("password")
                    .redirectUris("*")
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(true)
                    .attribute(Constants.ACR_LOA_MAP, "{\"default\":1,\"acr-password\":2,\"acr-otp\":3}")
                    .defaultClientScopes("acr-test-scope");
        }
    }
}
