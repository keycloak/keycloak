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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.*;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.UserBuilder;

import java.util.*;

/**
 * @author Ben Cresitello-Dittmar
 * Test for the OIDC authentication context class reference (ACR) to authentication
 * flow feature mapping feature
 */
public class AcrAuthFlowTest extends AbstractOIDCScopeTest{

    // config
    private static String CLIENT_ID = "test-app";
    private static String CLIENT_SECRET = "password";
    private static String PASSWORD = "password";
    private static String TOTP_SECRET = "totpsecret";

    private static String PASSWORD_FLOW_ALIAS = "password-flow";

    private static String PASSWORD_OTP_FLOW_ALIAS = "password-otp-flow";

    // pages
    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    protected LoginPage loginPage;

    private TimeBasedOTP totp = new TimeBasedOTP();
    private static String userId;

    /**
     * Create the ACR protocol mapper and add it to the test OIDC client.
     * @param testRealm The realm read from /testrealm.json.
     */
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        // setup user
        UserRepresentation user = createTestUser("test-user", PASSWORD, TOTP_SECRET);
        testRealm.getUsers().add(user);
        userId = user.getId();

        // setup acr scope
        ClientScopeRepresentation scope = createScope();

        List<ClientScopeRepresentation> scopes = testRealm.getClientScopes();
        if (scopes == null){
            testRealm.setClientScopes(new ArrayList<>());
        }
        testRealm.getClientScopes().add(scope);

        // update client and default scopes
        testRealm.setDefaultDefaultClientScopes(Collections.singletonList(scope.getName()));
        testRealm.getClients().stream().filter(c -> c.getClientId().equals(CLIENT_ID)).findFirst().orElseThrow().setDefaultClientScopes(Collections.singletonList(scope.getName()));
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
     * Helper function to create the ACR scope and protocol mapper.
     * @return The created scope object
     */
    private ClientScopeRepresentation createScope(){
        ProtocolMapperRepresentation protocolMapper = createMapper();
        return new ClientScopeRepresentation(){{
            setId(KeycloakModelUtils.generateId());
            setName("acr-test-scope");
            setProtocol("openid-connect");
            setAttributes(new HashMap<>() {{
                put(ClientScopeModel.INCLUDE_IN_TOKEN_SCOPE, "false");
                put(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, "false");
            }});
            setProtocolMappers(Collections.singletonList(protocolMapper));
        }};
    }

    /**
     * Helper function to create the acr protocol mapper.
     * @return The created protocol mapper
     */
    private ProtocolMapperRepresentation createMapper(){
        return new ProtocolMapperRepresentation(){{
            setId(KeycloakModelUtils.generateId());
            setName("acr-test-mapper");
            setProtocol("openid-connect");
            setProtocolMapper("oidc-acr-flow-mapper");
            setConfig(new HashMap<>() {{
                put("id.token.claim", "true");
                put("access.token.claim", "true");
            }});
        }};
    }


    /**
     * Setup for the test cases
     */
    @Before
    public void setupTest() {
        oauth.clientId(CLIENT_ID);
        createPasswordFlow();
        createOTPFlow();

        // needed otherwise multiple OTP tests will fail due to token reuse
        new RealmAttributeUpdater(testRealm())
                .setOtpPolicyCodeReusable(true)
                .update();
    }

    /**
     * Reset clients post test
     */
    @After
    public void cleanupTest() {
        clearAcrMap(CLIENT_ID);
    }

    /**
     * Clear the configured acr auth flow map for the realm and given client
     * @param clientId The client to update
     */
    private void clearAcrMap(String clientId) {
        ClientRepresentation c = testRealm().clients().findByClientId(clientId).stream().findFirst().orElseThrow();
        Map<String, String> clientAttrs = c.getAttributes();

        RealmRepresentation r = testRealm().toRepresentation();
        Map<String, String> realmAttrs = r.getAttributes();

        try {
            clientAttrs.put(Constants.ACR_FLOW_MAP, serialize(Collections.emptyMap()));
            c.setAttributes(clientAttrs);
            realmAttrs.put(Constants.ACR_FLOW_MAP, serialize(Collections.emptyMap()));
            r.setAttributes(realmAttrs);
        } catch (JsonProcessingException e){
            throw new RuntimeException("failed to clear acr map");
        }

        testRealm().clients().get(c.getId()).update(c);
        testRealm().update(r);
    }

    /**
     * Helper function to create an authentication flow with the password authenticator
     */
    private void createPasswordFlow(){
        testingClient.server(TEST_REALM_NAME).run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(PASSWORD_FLOW_ALIAS));
        testingClient.server(TEST_REALM_NAME)
                .run(session -> FlowUtil.inCurrentRealm(session).selectFlow(PASSWORD_FLOW_ALIAS)
                        // remove cookie, kerberos, and idp from browser flow
                        .removeExecution(2).removeExecution(1).removeExecution(0)
                        .inForms(forms -> forms.clear()
                            .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID, null)
                ));
    }

    /**
     * Helper function to create an authentication flow with the password and otp authenticators
     */
    private void createOTPFlow(){
        testingClient.server(TEST_REALM_NAME).run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(PASSWORD_OTP_FLOW_ALIAS));
        testingClient.server(TEST_REALM_NAME)
                .run(session -> FlowUtil.inCurrentRealm(session).selectFlow(PASSWORD_OTP_FLOW_ALIAS)
                        // remove cookie, kerberos, and idp from browser flow
                        .removeExecution(2).removeExecution(1).removeExecution(0)
                        .inForms(forms -> forms.clear()
                            .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID, null)
                            .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, OTPFormAuthenticatorFactory.PROVIDER_ID, null)
                ));
    }

    /**
     * Test the ACR auth flow map for the password auth flow
     * Expected: ACR = "acr-password"
     */
    @Test
    public void testAuthFlow() {
        setClientAcrMap(CLIENT_ID, new HashMap<>(){{
            put("acr-password", findFlowByAlias(PASSWORD_FLOW_ALIAS));
            put("acr-otp", findFlowByAlias(PASSWORD_OTP_FLOW_ALIAS));
        }});

        loginWithAcr(new ArrayList<>(){{
            add("acr-password");
        }});

        authenticatePassword("test-user", PASSWORD);
        Tokens tokens = assertLoginWithAcr(userId, "acr-password");

        logout(userId, tokens);
    }

    /**
     * Test the ACR auth flow mapping feature for an alternate otp auth flow
     * Expected: ACR = "acr-otp"
     */
    @Test
    public void testAuthFlowAlt() {
        setClientAcrMap(CLIENT_ID, new HashMap<>(){{
            put("acr-password", findFlowByAlias(PASSWORD_FLOW_ALIAS));
            put("acr-otp", findFlowByAlias(PASSWORD_OTP_FLOW_ALIAS));
        }});

        loginWithAcr(new ArrayList<>(){{
            add("acr-otp");
        }});

        authenticatePassword("test-user", PASSWORD);
        authenticateTOTP(TOTP_SECRET);
        Tokens tokens = assertLoginWithAcr(userId, "acr-otp");

        logout(userId, tokens);
    }

    /**
     * Test the ACR auth flow mapping feature when an invalid auth flow is configured for the client
     * Expected: Error
     */
    @Test
    public void testInvalidMapping() {
        setClientAcrMap(CLIENT_ID, new HashMap<>(){{
            put("acr-password", "INVALID FLOW");
            put("acr-otp", findFlowByAlias(PASSWORD_OTP_FLOW_ALIAS));
        }});

        loginWithAcr(new ArrayList<>(){{
            add("acr-password");
        }});

        Assert.assertTrue(errorPage.isCurrent());
    }

    /**
     * Test fallback to default flow when no valid mapping is found. Ensure acr is not set
     * Expected: ACR = null
     */
    @Test
    public void testNoMapping() {
        setClientAcrMap(CLIENT_ID, new HashMap<>());

        loginWithAcr(new ArrayList<>(){{
            add("acr-password");
        }});

        authenticatePassword("test-user", PASSWORD);
        authenticateTOTP(TOTP_SECRET);
        Tokens tokens = assertLoginWithAcr(userId, null);

        logout(userId, tokens);
    }

    /**
     * Test success when acr claim value is essential
     * Expected: ACR = acr-password
     */
    @Test
    public void testPassEssential() {
        setClientAcrMap(CLIENT_ID, new HashMap<>(){{
            put("acr-password", findFlowByAlias(PASSWORD_FLOW_ALIAS));
        }});

        loginWithAcr(new ArrayList<>(){{
            add("acr-password");
        }}, true);

        authenticatePassword("test-user", PASSWORD);
        Tokens tokens = assertLoginWithAcr(userId, "acr-password");

        logout(userId, tokens);
    }

    /**
     * Test error when acr claim value is essential and flow cannot be found
     * Expected: Invalid parameter error
     */
    @Test
    public void testFailEssential() {
        setClientAcrMap(CLIENT_ID, new HashMap<>());

        loginWithAcr(new ArrayList<>(){{
            add("acr-password");
        }}, true);

        Assert.assertTrue(errorPage.isCurrent());
        Assert.assertEquals("Invalid parameter: claims", errorPage.getError());
    }

    /**
     * Test sessions when using ACR flow mapping
     *
     * Expected: Re-authentication forces user to redo authenticators for newly specified flow
     */
    @Test
    public void testSessionReAuth() {
        Tokens tokens;

        setClientAcrMap(CLIENT_ID, new HashMap<>(){{
            put("acr-password", findFlowByAlias(PASSWORD_FLOW_ALIAS));
            put("acr-otp", findFlowByAlias(PASSWORD_OTP_FLOW_ALIAS));
        }});

        // initial login
        loginWithAcr(new ArrayList<>(){{
            add("acr-password");
        }});
        authenticatePassword("test-user", PASSWORD);
        assertLoginWithAcr(userId, "acr-password");

        // ensure re-auth forced with different acr
        loginWithAcr(new ArrayList<>(){{
            add("acr-otp");
        }});
        authenticatePassword("test-user", PASSWORD);
        authenticateTOTP(TOTP_SECRET);
        tokens = assertLoginWithAcr(userId, "acr-otp");

        logout(userId, tokens);
    }

    /**
     * Test the ACR flow configuration at the realm level
     * Expected: ACR = "acr-otp"
     */
    @Test
    public void testRealmConfig() {
        setRealmAcrMap(new HashMap<>(){{
            put("acr-password", findFlowByAlias(PASSWORD_FLOW_ALIAS));
            put("acr-otp", findFlowByAlias(PASSWORD_OTP_FLOW_ALIAS));
        }});
        loginWithAcr(new ArrayList<>(){{
            add("acr-otp");
        }});

        authenticatePassword("test-user", PASSWORD);
        authenticateTOTP(TOTP_SECRET);
        Tokens tokens = assertLoginWithAcr(userId, "acr-otp");

        logout(userId, tokens);
    }

    /**
     * Test the ACR flow configuration at the client level supersedes the realm config
     * Expected: ACR = "acr-1"
     */
    @Test
    public void testClientPrecedence() {
        setClientAcrMap(CLIENT_ID, new HashMap<>(){{
            put("acr-1", findFlowByAlias(PASSWORD_FLOW_ALIAS));
        }});
        setRealmAcrMap(new HashMap<>(){{
            put("acr-1", findFlowByAlias(PASSWORD_OTP_FLOW_ALIAS));
        }});

        loginWithAcr(new ArrayList<>(){{
            add("acr-1");
        }});

        authenticatePassword("test-user", PASSWORD);
        Tokens tokens = assertLoginWithAcr(userId, "acr-1");

        logout(userId, tokens);
    }

    private void loginWithAcr(List<String> acrValues){
        loginWithAcr(acrValues, false);
    }

    /**
     * Helper function to open the authentication page, requesting the specified acrValues. Optionally, specify the acr
     * claim as essential.
     * @param acrValues The acr values to include in the authorization request
     * @param essential Specify that the acr claim is essential in the request
     */
    private void loginWithAcr(List<String> acrValues, boolean essential){
        ClaimsRepresentation.ClaimValue<String> acrClaim = new ClaimsRepresentation.ClaimValue<>();
        acrClaim.setEssential(essential);
        acrClaim.setValues(acrValues);

        ClaimsRepresentation claims = new ClaimsRepresentation();
        claims.setIdTokenClaims(Collections.singletonMap(IDToken.ACR, acrClaim));

        oauth.claims(claims);
        oauth.openLoginForm();
    }

    /**
     * Helper function to fetch the authentication flow ID based on the alias
     * @param alias The alias to search for
     * @return The flow ID
     */
    private String findFlowByAlias(String alias){
        return testRealm().flows().getFlows().stream().filter(f -> f.getAlias().equals(alias)).findFirst().orElseThrow().getId();
    }

    /**
     * Set the acr auth flow map configuration on the specified client
     * @param clientId The client to set the configuration on
     * @param acrMap The acr to auth flow map to set
     */
    private void setClientAcrMap(String clientId, Map<String, String> acrMap) {
        ClientRepresentation c = testRealm().clients().findByClientId(clientId).stream().findFirst().orElseThrow();
        Map<String, String> attrs = c.getAttributes();

        try {
            attrs.put(Constants.ACR_FLOW_MAP, serialize(acrMap));
        } catch (JsonProcessingException e){
            throw new RuntimeException("Failed to serialize acr auth flow map");
        }
        c.setAttributes(attrs);

        testRealm().clients().get(c.getId()).update(c);
    }

    /**
     * Set the acr auth flow map configuration on the realm
     * @param acrMap The acr to auth flow map to set
     */
    private void setRealmAcrMap(Map<String, String> acrMap) {
        try {
            new RealmAttributeUpdater(testRealm())
                    .setAttribute(Constants.ACR_FLOW_MAP, serialize(acrMap))
                    .update();
        } catch (JsonProcessingException e){
            throw new RuntimeException("Failed to serialize acr auth flow map");
        }
    }


        /**
         * Helper function to log out the specified user
         * @param userId The keycloak identifier of the user
         * @param tokens The OIDC tokens received during login
         */
    private void logout(String userId, Tokens tokens){
        // Logout
        oauth.doLogout(tokens.refreshToken, CLIENT_SECRET);
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
        Assert.assertTrue(loginPage.isCurrent());
        loginPage.login(username, password);
    }

    /**
     * Helper function to authenticate with a TOTP token
     * @param totpSecret The secret to use to generate the TOTP token
     */
    private void authenticateTOTP(String totpSecret){
        Assert.assertTrue(loginTotpPage.isCurrent());
        setOtpTimeOffset(TimeBasedOTP.DEFAULT_INTERVAL_SECONDS, totp);

        loginTotpPage.login(totp.generateTOTP(totpSecret));
    }

    /**
     * Helper function to assert login completed successfully for the specified user
     * @param userId The keycloak ID of the user to check
     * @param expectedAcr The value expected in the 'acr' claim of the resulting token
     * @return The tokens from a successful login
     */
    private Tokens assertLoginWithAcr(String userId, String expectedAcr){
        EventRepresentation loginEvent = events.expectLogin()
                .user(userId)
                .assertEvent();

        Tokens tokens = sendTokenRequest(loginEvent, userId, "openid", CLIENT_ID);
        assertAcr(tokens.idToken, expectedAcr);
        assertAcr(tokens.accessToken, expectedAcr);

        return tokens;
    }

    /**
     * Helper function to serialize a Map<String, String> object.
     *
     * @param map A map to serialize to JSON
     * @return The JSON serialized map
     * @throws JsonProcessingException
     */
    private static String serialize(Map<String, String> map) throws JsonProcessingException {
        return (new ObjectMapper()).writeValueAsString(map);
    }

    /**
     * Helper function to assert the token contains the specified acr value
     * @param token The token to check (either access or ID)
     * @param expectedAcr The expected acr values in the token
     */
    private void assertAcr(IDToken token, String expectedAcr) {
        getLogger().infof("Expected acr = %s", expectedAcr);
        String acr = token.getAcr();
        getLogger().infof("Response acr = %s", acr);
        if (expectedAcr != null) {
            Assert.assertNotNull(acr);
        }

        Assert.assertEquals(acr, expectedAcr);
    }

}
