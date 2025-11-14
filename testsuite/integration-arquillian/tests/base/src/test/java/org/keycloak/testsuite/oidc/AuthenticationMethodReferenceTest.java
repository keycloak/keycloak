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

package org.keycloak.testsuite.oidc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.forms.LevelOfAssuranceFlowTest;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Ben Cresitello-Dittmar
 * Test for the OIDC authentication method reference (AMR) feature and protocol mapper.
 */
public class AuthenticationMethodReferenceTest extends AbstractOIDCScopeTest{

    // config
    private static String AMR_VALUE_KEY = Constants.AUTHENTICATION_EXECUTION_REFERENCE_VALUE;
    private static String AMR_MAX_AGE_KEY = Constants.AUTHENTICATION_EXECUTION_REFERENCE_MAX_AGE;
    private static Integer DEFAULT_MAX_AGE = 120;
    private static String CLIENT_ID = "test-app";
    private static String CLIENT_SECRET = "password";
    private static String PASSWORD = "password";
    private static String TOTP_SECRET = "totpsecret";

    // pages
    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    protected LoginPage loginPage;

    private TimeBasedOTP totp = new TimeBasedOTP();
    private static String passwordUserId;
    private static String totpUserId;

    /**
     * Create the AMR protocol mapper and add it to the test OIDC client.
     * @param testRealm The realm read from /testrealm.json.
     */
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        // setup user
        UserRepresentation user = createTestUser("test-user", PASSWORD, null);
        UserRepresentation totpUser = createTestUser("totp-user", PASSWORD, TOTP_SECRET);
        testRealm.getUsers().add(user);
        testRealm.getUsers().add(totpUser);
        passwordUserId = user.getId();
        totpUserId = totpUser.getId();

        // setup amr scope
        List<ClientScopeRepresentation> newScopes = createScopes("oidc-amr-mapper", "oidc-acr-mapper");

        List<ClientScopeRepresentation> scopes = testRealm.getClientScopes();
        if (scopes == null){
            testRealm.setClientScopes(new ArrayList<>());
        }
        for (ClientScopeRepresentation newScope : newScopes){
            testRealm.getClientScopes().add(newScope);
        }

        // update client and default scopes
        List<String> scopeNames = newScopes.stream().map(ClientScopeRepresentation::getName).collect(Collectors.toList());
        testRealm.setDefaultDefaultClientScopes(scopeNames);
        testRealm.getClients().stream().filter(c -> c.getClientId().equals(CLIENT_ID)).findFirst().orElseThrow().setDefaultClientScopes(scopeNames);
    }

    /**
     * Helper function to create a test user, optionally with OTP configured
     * @param username The username of the user to create
     * @param password The password to set on the user
     * @param totpSecret If set, will configure a totp authenticator with this secret
     * @return
     */
    private UserRepresentation createTestUser(String username, String password, String totpSecret){
        UserBuilder builder = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username(username)
                .enabled(true)
                .email(username + "@email.com")
                .firstName(username)
                .lastName(username)
                .password(password);

        if (totpSecret != null){
            builder.totpSecret(totpSecret)
                    .otpEnabled();
        }

        return builder.build();
    }

    /**
     * Helper function to create the AMR scope and protocol mapper.
     * @return The created scope object
     */
    private List<ClientScopeRepresentation> createScopes(String ...mappers){
        List<ClientScopeRepresentation> scopes = new ArrayList<>();
        for (String mapper : mappers){
            ProtocolMapperRepresentation protocolMapper = createMapper(mapper);
            ClientScopeRepresentation scope = new ClientScopeRepresentation();
            scope.setId(KeycloakModelUtils.generateId());
            scope.setName(mapper);
            scope.setProtocol("openid-connect");
            scope.setAttributes(new HashMap<>() {{
                put(ClientScopeModel.INCLUDE_IN_TOKEN_SCOPE, "false");
                put(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, "false");
            }});
            scope.setProtocolMappers(Collections.singletonList(protocolMapper));
            scopes.add(scope);
        }

        return scopes;
    }

    /**
     * Helper function to create the amr protocol mapper.
     * @return The created protocol mapper
     */
    private ProtocolMapperRepresentation createMapper(String mapper){
        ProtocolMapperRepresentation protocolMapper = new ProtocolMapperRepresentation();
        protocolMapper.setId(KeycloakModelUtils.generateId());
        protocolMapper.setName(mapper);
        protocolMapper.setProtocol("openid-connect");
        protocolMapper.setProtocolMapper(mapper);
        protocolMapper.setConfig(new HashMap<>() {{
            put("id.token.claim", "true");
            put("access.token.claim", "true");
        }});
        return protocolMapper;
    }

    /**
     * Set the OIDC client before each test.
     */
    @Before
    public void configureClient() {
        oauth.clientId(CLIENT_ID);
    }

    /**
     * Clear the configured authenticator reference values in the authentication flow after each test
     */
    @After
    public void cleanup() {
        clearAmr("browser");

        // reset default browser flow
        setBrowserFlow("browser");

        // allow otp code reuse
        new RealmAttributeUpdater(testRealm())
                .setOtpPolicyCodeReusable(true)
                .update();
    }

    /**
     * Helper function to clear the amr configuration from the given flow
     * @param flowAlias
     */
    private void clearAmr(String flowAlias){
        getAuthenticatorConfigs(flowAlias).forEach(c -> {
            Map<String, String> configVals = c.getConfig();
            configVals.put(AMR_VALUE_KEY, null);
            configVals.put(AMR_MAX_AGE_KEY, null);
            c.setConfig(configVals);

            testRealm().flows().updateAuthenticatorConfig(c.getId(), c);
        });
    }

    /**
     * Get all authenticator configs for a given authentication flow
     * @param flowAlias The alias of the flow
     * @return A list of authenticator configs from the specified flow
     */
    private List<AuthenticatorConfigRepresentation> getAuthenticatorConfigs(String flowAlias){
        return testRealm().flows().getExecutions(flowAlias).stream().filter(e -> e.getAuthenticationConfig() != null).map(e -> testRealm().flows().getAuthenticatorConfig(e.getAuthenticationConfig())).collect(Collectors.toList());
    }

    /**
     * Test the AMR protocol mapper if no authenticator references are configured in the authentication flow
     * Expected: AMR = []
     */
    @Test
    public void testAmrNone() {
        List<String> expectedAmrs = new ArrayList<>();
        authenticatePassword("test-user", PASSWORD);
        Tokens tokens = assertLogin(passwordUserId);

        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);

        logout(passwordUserId, tokens);
    }

    /**
     * Test the AMR protocol mapper if only the password form authenticator have an authenticator reference configured
     * Expected: AMR = ["password"]
     */
    @Test
    public void testAmrPassword() {
        setAmr("browser", "auth-username-password-form", "password");

        List<String> expectedAmrs = new ArrayList<>(){{
            add("password");
        }};
        authenticatePassword("test-user", PASSWORD);
        Tokens tokens = assertLogin(passwordUserId);

        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);

        logout(passwordUserId, tokens);
    }

    /**
     * Test the AMR protocol mapper if the password form and totp form have an authenticator reference configured
     * Expected: AMR = ["password", "totp"]
     */
    @Test
    public void testAmrPasswordTotp() {
        setAmr("browser", "auth-username-password-form", "password");
        setAmr("browser", "auth-otp-form", "totp");

        List<String> expectedAmrs = new ArrayList<>(){{
            add("password");
            add("totp");
        }};
        authenticatePassword("totp-user", PASSWORD);
        authenticateTOTP(TOTP_SECRET);
        Tokens tokens = assertLogin(totpUserId);


        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);

        logout(totpUserId, tokens);
    }

    /**
     * Test the AMR protocol mapper if the max age of the stored amr value has been passed
     * Expected: AMR = ["password"]
     */
    @Test
    public void testAmrPastMaxAge() {
        setAmr("browser", "auth-username-password-form", "password", 10);

        List<String> expectedAmrs = new ArrayList<>();
        authenticatePassword("test-user", PASSWORD);

        // server time forward by 20 seconds to ensure max age is exceeded
        setTimeOffset(20);

        Tokens tokens = assertLogin(passwordUserId);

        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);

        logout(passwordUserId, tokens);
    }

    /**
     * Test the AMR protocol mapper if the max age of the stored amr value has been not been passed
     * Expected: AMR = ["password"]
     */
    @Test
    public void testAmrWithinMaxAge() {
        Tokens tokens;

        setAmr("browser", "auth-username-password-form", "password", 60);
        List<String> expectedAmrs = new ArrayList<>(){{
            add("password");
        }};

        // perform initial login
        authenticatePassword("test-user", PASSWORD);
        tokens = assertLogin(passwordUserId);
        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);
        getLogger().info(tokens.accessToken.getId());

        // re-open login page to login with cookie - ensure amr values persist
        oauth.openLoginForm();
        tokens = assertLogin(passwordUserId);
        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);
        getLogger().info(tokens.accessToken.getId());

        logout(passwordUserId, tokens);
    }

    /**
     * Test the AMR protocol mapper during step up authentication
     */
    @Test
    public void testAmrStepUp() {
        Tokens tokens;
        List<String> expectedAmrs = new ArrayList<>(){{
            add("password");
        }};

        // configure acr loa
        configureStepUpFlow("browser step-up");
        setBrowserFlow("browser step-up");
        configureRealmAcrMap(new HashMap<>(){{
            put("silver", 1);
            put("gold", 2);
        }});

        // configure amr
        setAmr("browser step-up", "auth-username-password-form", "password");
        setAmr("browser step-up", "auth-otp-form", "totp");

        // login at level 1
        LevelOfAssuranceFlowTest.openLoginFormWithAcrClaim(oauth,true, "silver");
        authenticatePassword("totp-user", PASSWORD);
        tokens = assertLogin(totpUserId);
        assertAcr(tokens.idToken, "silver");
        assertAcr(tokens.accessToken, "silver");
        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);

        // step-up to level 2
        expectedAmrs.add("totp");
        LevelOfAssuranceFlowTest.openLoginFormWithAcrClaim(oauth, true, "gold");
        authenticateTOTP(TOTP_SECRET);
        tokens = assertLogin(totpUserId);
        assertAcr(tokens.idToken, "gold");
        assertAcr(tokens.accessToken, "gold");
        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);

        logout(totpUserId, tokens);
    }

    private void setAmr(String flowAlias, String providerId, String amrValue){
        setAmr(flowAlias, providerId, amrValue, DEFAULT_MAX_AGE);
    }

    /**
     * Helper function to set the authenticator reference config on the specified provider in the given flow
     * @param flowAlias The authentication flow to search
     * @param providerId The provider ID of the authentication execution within the specified flow
     * @param amrValue The authenticator reference value to set on the authenticator config
     * @param maxAge The max age the authenticator reference value is valid
     */
    private void setAmr(String flowAlias, String providerId, String amrValue, Integer maxAge){
        AuthenticationExecutionInfoRepresentation execution = testRealm().flows().getExecutions(flowAlias).stream().filter(e -> e.getProviderId() != null && e.getProviderId().equals(providerId)).findFirst().orElseThrow();

        if (execution.getAuthenticationConfig() == null){
            // create config if it doesn't exist
            AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
            config.setAlias(KeycloakModelUtils.generateId());
            config.setConfig(new HashMap<>(){{
                put(AMR_VALUE_KEY, amrValue);
                put(AMR_MAX_AGE_KEY, maxAge.toString());
            }});

            testRealm().flows().newExecutionConfig(execution.getId(), config);
        } else {
            // update existing config
            AuthenticatorConfigRepresentation config = testRealm().flows().getAuthenticatorConfig(execution.getAuthenticationConfig());
            Map<String, String> newConfig = config.getConfig();
            newConfig.put(AMR_VALUE_KEY, amrValue);
            newConfig.put(AMR_MAX_AGE_KEY, maxAge.toString());
            config.setConfig(newConfig);
            testRealm().flows().updateAuthenticatorConfig(config.getId(), config);
        }
    }

    /**
     * Helper function to set the browser flow for the realm
     * @param flowAlias The alias of the flow to set as the browser flow
     */
    private void setBrowserFlow(String flowAlias){
        testingClient.server(TEST_REALM_NAME)
                .run(session -> FlowUtil.inCurrentRealm(session).selectFlow(flowAlias).defineAsBrowserFlow());
    }

    /**
     * Helper function to configure the realm acr loa map
     * @param acrLoaMap The map to set
     */
    private void configureRealmAcrMap(Map<String, Integer> acrLoaMap){
        RealmRepresentation realmRep = testRealm().toRepresentation();
        try {
            realmRep.getAttributes().put(Constants.ACR_LOA_MAP, JsonSerialization.writeValueAsString(acrLoaMap));
        } catch (IOException e){
            throw new RuntimeException("failed to parse acr loa map");
        }
        testRealm().update(realmRep);
    }

    /**
     * Helper function to configure a step-up flow.
     * Flow: acr=1 -> password, acr=2 -> totp
     */
    private void configureStepUpFlow(String newFlowAlias) {
        testingClient.server(TEST_REALM_NAME).run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server(TEST_REALM_NAME)
                .run(session -> FlowUtil.inCurrentRealm(session).selectFlow(newFlowAlias).inForms(forms -> forms.clear()
                        // level 1 authentication
                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "1");
                                        config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(60));
                                    });

                            // username input for level 1
                            subFlow.addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID);
                        })

                        // level 2 authentication
                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "2");
                                        config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(60));
                                    });

                            // password required for level 2
                            subFlow.addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, OTPFormAuthenticatorFactory.PROVIDER_ID);
                        })
                ));
    }

    /**
     * Helper function to log out the specified user
     * @param userId The keycloak identifier of the user
     * @param tokens The OIDC tokens received during login
     */
    private void logout(String userId, Tokens tokens){
        // Logout
        oauth.doLogout(tokens.refreshToken);
        events.expectLogout(tokens.idToken.getSessionState())
                .client(CLIENT_ID)
                .user(userId)
                .removeDetail(Details.REDIRECT_URI).assertEvent();
    }

    /**
     * Helper function to authenticate with a username and password
     * @param username The username to log in with
     * @param password The password to log in with
     */
    private void authenticatePassword(String username, String password){
        loginPage.open();
        loginPage.login(username, password);
    }

    /**
     * Helper function to authenticate with a TOTP token
     * @param totpSecret The secret to use to generate the TOTP token
     */
    private void authenticateTOTP(String totpSecret){
        org.junit.Assert.assertTrue(loginTotpPage.isCurrent());
        setOtpTimeOffset(TimeBasedOTP.DEFAULT_INTERVAL_SECONDS, totp);

        loginTotpPage.login(totp.generateTOTP(totpSecret));
    }

    /**
     * Helper function to assert login completed successfully for the specified user
     * @param userId The keycloak ID of the user to check
     * @return The tokens from a successful login
     */
    private Tokens assertLogin(String userId){
        EventRepresentation loginEvent = events.expectLogin()
                .user(userId)
                .assertEvent();

        return sendTokenRequest(loginEvent, userId, "openid", CLIENT_ID);
    }

    /**
     * Helper function to assert the token contains the specified amr values
     * @param token The token to check (either access or ID)
     * @param expectedValues The expected amr values in the token
     */
    private void assertAmr(IDToken token, List<String> expectedValues) {
        getLogger().infof("Got claims %s", token.getOtherClaims().toString());
        List<String> amr = (List<String>) token.getOtherClaims().get("amr");
        Assert.assertNotNull(amr);

        // sort otherwise order may be different
        Collections.sort(amr);
        Collections.sort(expectedValues);

        Assert.assertArrayEquals(expectedValues.toArray(), amr.toArray());
    }

    private void assertAcr(IDToken token, String acr){
        Assert.assertEquals(acr, token.getAcr());
    }

}
