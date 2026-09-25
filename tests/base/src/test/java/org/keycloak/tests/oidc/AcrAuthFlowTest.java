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


import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.AccessToken;
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
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.tests.account.custom.CustomAuthFlowOTPTest.LoginTotpPage;
import org.keycloak.tests.utils.ClientPoliciesUtil;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.oauth.RefreshTokenTest.assertScopes;

/**
 * @author <a href="mailto:ggrazian@redhat.com">Giuseppe Graziano</a>
 */
@KeycloakIntegrationTest
public class AcrAuthFlowTest {

    @InjectRealm(config = AcrAuthFlowRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectOAuthClient(config = AcrClientConfig.class)
    OAuthClient oauth;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @InjectEvents
    Events events;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;


    // config
    private static String CLIENT_ID = "test-app";
    private static String CLIENT_SECRET = "password";
    private static String PASSWORD = "password";
    private static String TOTP_SECRET = "totpsecret";
    private static String PASSWORD_FLOW_ALIAS = "password-flow";
    private static String PASSWORD_OTP_FLOW_ALIAS = "password-otp-flow";

    // pages
    @InjectPage
    protected LoginTotpPage loginTotpPage;

    @InjectPage
    protected LoginPage loginPage;

    private TimeBasedOTP totp = new TimeBasedOTP();

    private String userId;

    protected Logger log = Logger.getLogger(this.getClass());


    static class Tokens {
        final IDToken idToken;
        final AccessToken accessToken;
        final String refreshToken;

        private Tokens(IDToken idToken, AccessToken accessToken, String refreshToken) {
            this.idToken = idToken;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }


    protected Tokens sendTokenRequest(EventRepresentation loginEvent, String userId, String expectedScope, String clientId) {
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.client(clientId, "password").doAccessTokenRequest(code);
        Assertions.assertEquals(200, response.getStatusCode());

        // Test scopes
        log.info("expectedScopes = " + expectedScope);
        log.info("responseScopes = " + response.getScope());
        assertScopes(expectedScope, response.getScope());

        IDToken idToken = oauth.verifyIDToken(response.getIdToken());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());

        // Test scope in the access token
        assertScopes(expectedScope, accessToken.getScope());

        EventRepresentation codeToTokenEvent = EventAssertion.expectCodeToTokenSuccess(events.poll())
                .sessionId(sessionId)
                .userId(userId)
                .clientId(clientId)
                .details(Details.CODE_ID, codeId)
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .details(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID).getEvent();

        // Test scope in the event
        assertScopes(expectedScope, codeToTokenEvent.getDetails().get(Details.SCOPE));

        return new Tokens(idToken, accessToken, response.getRefreshToken());
    }

    private static String getAcrToLoaMappingForClient() throws IOException {
        Map<String, Integer> acrLoaMap = new HashMap<>();
        acrLoaMap.put("default", 1);
        acrLoaMap.put("acr-password", 2);
        acrLoaMap.put("acr-otp", 3);
        return JsonSerialization.writeValueAsString(acrLoaMap);
    }

    /**
     * Helper function to create a test user, optionally with OTP configured
     *
     * @param username   The username of the user to create
     * @param password   The password to set on the user
     * @param totpSecret If set, will configure a totp authenticator with this secret
     * @return
     */

    private static UserRepresentation createTestUser(String username, String password, String totpSecret) {
        UserBuilder builder = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username(username)
                .enabled(true)
                .email(username + "@email.com")
                .firstName(username)
                .lastName(username)
                .password(password);

        if (totpSecret != null) {
            builder.totpSecret(totpSecret);
        }

        return builder.build();
    }

    /**
     * Helper function to create the ACR scope and protocol mapper.
     *
     * @return The created scope object
     */
    private static ClientScopeRepresentation createScope() {
        ProtocolMapperRepresentation protocolMapper = createMapper();
        return new ClientScopeRepresentation() {{
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
     *
     * @return The created protocol mapper
     */
    private static ProtocolMapperRepresentation createMapper() {
        return new ProtocolMapperRepresentation() {{
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
    @BeforeEach
    public void setupTest() {
        oauth.client(CLIENT_ID);
        createPasswordFlow();
        createOTPFlow();

        userId = managedRealm.admin().users().search("test-user").stream()
                .findFirst()
                .orElseThrow()
                .getId();

        // Configure ACR LOA mapping on test-app client
        managedRealm.admin().clients().findByClientId(CLIENT_ID).stream()
                .findFirst()
                .ifPresent(client -> {
                    try {
                        client.setAttributes(Collections.singletonMap(
                                Constants.ACR_LOA_MAP, getAcrToLoaMappingForClient()));
                        managedRealm.admin().clients().get(client.getId()).update(client);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        // needed otherwise multiple OTP tests will fail due to token reuse
        RealmRepresentation realmRep = managedRealm.admin().toRepresentation();
        realmRep.setOtpPolicyCodeReusable(true);
        managedRealm.admin().update(realmRep);
    }

    /**
     * Reset clients post test
     */
    @AfterEach
    public void cleanupTest() {
        try {
            ClientPoliciesRepresentation clientPolicies = JsonSerialization.readValue("{}", ClientPoliciesRepresentation.class);
            managedRealm.admin().clientPoliciesPoliciesResource().updatePolicies(clientPolicies);

            ClientProfilesRepresentation clientProfilesRepresentation = JsonSerialization.readValue("{}", ClientProfilesRepresentation.class);

            managedRealm.admin().clientPoliciesProfilesResource().updateProfiles(clientProfilesRepresentation);
        } catch (Exception e) {
            Assertions.fail();
        }

    }

    /**
     * Helper function to create an authentication flow with the password authenticator
     */
    private void createPasswordFlow() {
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(PASSWORD_FLOW_ALIAS));
        runOnServer
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
    private void createOTPFlow() {
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(PASSWORD_OTP_FLOW_ALIAS));
        runOnServer
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
        setAcrClientPolicy(adminClient, managedRealm.getName(), "acr-password", PASSWORD_FLOW_ALIAS, 2);
        setAcrClientPolicy(adminClient, managedRealm.getName(), "acr-otp", PASSWORD_OTP_FLOW_ALIAS, 3);

        loginWithAcr(new ArrayList<>() {{
            add("acr-password");
        }});

        authenticatePassword(PASSWORD);
        Tokens tokens = assertLoginWithAcr(userId, "acr-password");

        logout(userId, tokens);
    }

    @Test
    public void testAuthFlowWithoutLoaConfig() {
        setAcrClientPolicy(adminClient, managedRealm.getName(), "acr-password", PASSWORD_FLOW_ALIAS);

        loginWithAcr(new ArrayList<>() {{
            add("acr-password");
        }});

        authenticatePassword(PASSWORD);
        Tokens tokens = assertLoginWithAcr(userId, "default");

        logout(userId, tokens);
    }


    /**
     * Test the ACR auth flow mapping feature for an alternate otp auth flow
     * Expected: ACR = "acr-otp"
     */
    @Test
    public void testAuthFlowOTP() {

        setAcrClientPolicy(adminClient, managedRealm.getName(), "acr-password", PASSWORD_FLOW_ALIAS, 2);
        setAcrClientPolicy(adminClient, managedRealm.getName(), "acr-otp", PASSWORD_OTP_FLOW_ALIAS, 3);

        loginWithAcr(new ArrayList<>() {{
            add("acr-otp");
        }});

        authenticatePassword(PASSWORD);
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

        loginWithAcr(new ArrayList<>() {{
            add("acr-password");
        }});

        authenticatePassword(PASSWORD);
        authenticateTOTP(TOTP_SECRET);
        Tokens tokens = assertLoginWithAcr(userId, "default");

        logout(userId, tokens);
    }

    /**
     * Test sessions when using ACR flow mapping
     * <p>
     * Expected: Re-authentication forces user to redo authenticators for newly specified flow
     */
    @Test
    public void testSessionReAuth() {
        Tokens tokens;

        setAcrClientPolicy(adminClient, managedRealm.getName(), "acr-password", PASSWORD_FLOW_ALIAS, 2);
        setAcrClientPolicy(adminClient, managedRealm.getName(), "acr-otp", PASSWORD_OTP_FLOW_ALIAS, 3);

        // initial login
        loginWithAcr(new ArrayList<>() {{
            add("acr-password");
        }});
        authenticatePassword(PASSWORD);
        assertLoginWithAcr(userId, "acr-password");

        // ensure re-auth forced with different acr
        loginWithAcr(new ArrayList<>() {{
            add("acr-otp");
        }});
        authenticatePassword(PASSWORD);
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

            clientPolicies = json == null ? null : JsonSerialization.readValue(json, ClientPoliciesRepresentation.class);
            adminClient.realm(realm).clientPoliciesPoliciesResource().updatePolicies(clientPolicies);
        } catch (Exception e) {
            Assertions.fail();
        }
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

        oauth.loginForm().claims(claims).open();
    }

    /**
     * Helper function to fetch the authentication flow ID based on the alias
     *
     * @param alias The alias to search for
     * @return The flow ID
     */
    private String findFlowByAlias(String alias) {
        return managedRealm.admin().flows().getFlows().stream().filter(f -> f.getAlias().equals(alias)).findFirst().orElseThrow().getId();
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
                .clientId(CLIENT_ID)
                .userId(userId)
                .withoutDetails(Details.REDIRECT_URI);
    }

    /**
     * Helper function to authenticate with a username and password
     *
     * @param password The password to log in with
     */
    private void authenticatePassword(String password) {
        loginPage.assertCurrent();
        loginPage.fillLogin("test-user", password);
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

        Tokens tokens = sendTokenRequest(loginEvent, userId, "openid", CLIENT_ID);
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
        log.infof("Expected acr = %s", expectedAcr);
        String acr = token.getAcr();
        log.infof("Response acr = %s", acr);
        if (expectedAcr != null) {
            Assertions.assertNotNull(acr);
        }

        Assertions.assertEquals(expectedAcr, acr);
    }

    private static class AcrAuthFlowRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            UserBuilder userBuilder = UserBuilder.create("test-user")
                    .email("test-user@email.com")
                    .firstName("test-user")
                    .lastName("test-user")
                    .password(PASSWORD)
                    .totpSecret(TOTP_SECRET);
            ClientScopeRepresentation scope = createScope();

            return realm.name("test")
                    .users(userBuilder)
                    .clientScopes(scope)
                    .update(testRealm -> {
                        testRealm.setDefaultDefaultClientScopes(Collections.singletonList(scope.getName()));
                    });
        }
    }

    private static class AcrClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId(CLIENT_ID)
                    .secret(CLIENT_SECRET)
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(true)
                    .defaultClientScopes("acr-test-scope");
        }
    }
}
