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

import org.keycloak.admin.client.Keycloak;
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
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.clientpolicy.condition.AcrCondition;
import org.keycloak.services.clientpolicy.condition.AcrConditionFactory;
import org.keycloak.services.clientpolicy.executor.AuthenticationFlowSelectorExecutor;
import org.keycloak.services.clientpolicy.executor.AuthenticationFlowSelectorExecutorFactory;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:ggrazian@redhat.com">Giuseppe Graziano</a>
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

        try {
            findTestApp(testRealm).setAttributes(Collections.singletonMap(Constants.ACR_LOA_MAP, getAcrToLoaMappingForClient()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private String getAcrToLoaMappingForClient() throws IOException {
        Map<String, Integer> acrLoaMap = new HashMap<>();
        acrLoaMap.put("default", 1);
        acrLoaMap.put("acr-password", 2);
        acrLoaMap.put("acr-otp", 3);
        return JsonSerialization.writeValueAsString(acrLoaMap);
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
            setProtocolMapper("oidc-acr-mapper");
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
        try {
            ClientPoliciesRepresentation clientPolicies = JsonSerialization.readValue("{}", ClientPoliciesRepresentation.class);
            adminClient.realm(TEST_REALM_NAME).clientPoliciesPoliciesResource().updatePolicies(clientPolicies);

            ClientProfilesRepresentation clientProfilesRepresentation = JsonSerialization.readValue("{}", ClientProfilesRepresentation.class);

            adminClient.realm(TEST_REALM_NAME).clientPoliciesProfilesResource().updateProfiles(clientProfilesRepresentation);
        }
        catch (Exception e) {
            Assert.fail();
        }

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
        setAcrClientPolicy(adminClient, TEST_REALM_NAME, "acr-password", PASSWORD_FLOW_ALIAS, 2);
        setAcrClientPolicy(adminClient, TEST_REALM_NAME, "acr-otp", PASSWORD_OTP_FLOW_ALIAS, 3);

        loginWithAcr(new ArrayList<>(){{
            add("acr-password");
        }});

        authenticatePassword("test-user", PASSWORD);
        Tokens tokens = assertLoginWithAcr(userId, "acr-password");

        logout(userId, tokens);
    }

    @Test
    public void testAuthFlowWithoutLoaConfig() {
        setAcrClientPolicy(adminClient, TEST_REALM_NAME, "acr-password", PASSWORD_FLOW_ALIAS);

        loginWithAcr(new ArrayList<>(){{
            add("acr-password");
        }});

        authenticatePassword("test-user", PASSWORD);
        Tokens tokens = assertLoginWithAcr(userId, "default");

        logout(userId, tokens);
    }


    /**
     * Test the ACR auth flow mapping feature for an alternate otp auth flow
     * Expected: ACR = "acr-otp"
     */
    @Test
    public void testAuthFlowOTP() {

        setAcrClientPolicy(adminClient, TEST_REALM_NAME, "acr-password", PASSWORD_FLOW_ALIAS, 2);
        setAcrClientPolicy(adminClient, TEST_REALM_NAME, "acr-otp", PASSWORD_OTP_FLOW_ALIAS, 3);

        loginWithAcr(new ArrayList<>(){{
            add("acr-otp");
        }});

        authenticatePassword("test-user", PASSWORD);
        authenticateTOTP(TOTP_SECRET);
        Tokens tokens = assertLoginWithAcr(userId, "acr-otp");

        logout(userId, tokens);
    }

    /**
     * Test fallback to default flow when no valid mapping is found. Ensure acr is default value 1
     * Expected: ACR = default with the default acr-loa mapping behavior
     */
    @Test
    public void testNoMapping() {

        loginWithAcr(new ArrayList<>(){{
            add("acr-password");
        }});

        authenticatePassword("test-user", PASSWORD);
        authenticateTOTP(TOTP_SECRET);
        Tokens tokens = assertLoginWithAcr(userId, "default");

        logout(userId, tokens);
    }

    /**
     * Test sessions when using ACR flow mapping
     *
     * Expected: Re-authentication forces user to redo authenticators for newly specified flow
     */
    @Test
    public void testSessionReAuth() {
        Tokens tokens;

        setAcrClientPolicy(adminClient, TEST_REALM_NAME, "acr-password", PASSWORD_FLOW_ALIAS, 2);
        setAcrClientPolicy(adminClient, TEST_REALM_NAME, "acr-otp", PASSWORD_OTP_FLOW_ALIAS, 3);

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

    private void setAcrClientPolicy(Keycloak adminClient, String realm, String acr, String alias) {
        setAcrClientPolicy(adminClient, realm, acr, alias, null);
    }

    public static void setAcrClientPolicy(Keycloak adminClient, String realm, String acr, String alias, Integer loa) {

        try {

            ClientProfilesRepresentation clientProfiles = adminClient.realm(realm).clientPoliciesProfilesResource().getProfiles(false);
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
            adminClient.realm(realm).clientPoliciesProfilesResource().updateProfiles(clientProfiles);
            
            
            ClientPoliciesRepresentation clientPolicies = adminClient.realm(realm).clientPoliciesPoliciesResource().getPolicies(false);

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

            clientPolicies = json==null ? null : JsonSerialization.readValue(json, ClientPoliciesRepresentation.class);
            adminClient.realm(realm).clientPoliciesPoliciesResource().updatePolicies(clientPolicies);
        }
        catch (Exception e) {
            Assert.fail();
        }
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

        oauth.loginForm().claims(claims).open();
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
