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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.authentication.authenticators.browser.CookieAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
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
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ben Cresitello-Dittmar
 * Test for the OIDC authentication method reference (AMR) feature and protocol mapper.
 */
@KeycloakIntegrationTest
public class AuthenticationMethodReferenceTest extends AbstractOIDCScopeTest {

    @InjectRealm(config = AuthenticationMethodReferenceRealmConfig.class,lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectUser(config = PasswordUserConfig.class)
    ManagedUser passwordUser;

    @InjectClient(config = AmrClientConfig.class)
    ManagedClient client;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    // config
    private static final String AMR_VALUE_KEY = Constants.AUTHENTICATION_EXECUTION_REFERENCE_VALUE;
    private static final String AMR_MAX_AGE_KEY = Constants.AUTHENTICATION_EXECUTION_REFERENCE_MAX_AGE;
    private static final Integer DEFAULT_MAX_AGE = 120;
    private static final String CLIENT_ID = "amr-client";
    private static final String CLIENT_SECRET = "password";
    private static final String PASSWORD = "password";
    private static final String TOTP_SECRET = "totpsecret";

    private final TimeBasedOTP totp = new TimeBasedOTP();

    @BeforeEach
    public void clientSetup() {
        oauth.client(CLIENT_ID, CLIENT_SECRET);
    }
    /**
     * Test the AMR protocol mapper if no authenticator references are configured in the authentication flow.
     * Expected: AMR = []
     */
    @Test
    public void testAmrNone() {
        List<String> expectedAmrs = new ArrayList<>();
        oauth.openLoginForm();
        authenticatePassword("test-user");
        Tokens tokens = assertLogin(passwordUser.getId());

        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);

        logout(passwordUser.getId(), tokens);
    }

    /**
     * Test the AMR protocol mapper if only the password form authenticator has a reference configured.
     * Expected: AMR = ["password"]
     */
    @Test
    public void testAmrPassword() {
        setAmr("browser", "auth-username-password-form", "password", DEFAULT_MAX_AGE);

        List<String> expectedAmrs = new ArrayList<>() {{
            add("password");
        }};
        oauth.openLoginForm();
        authenticatePassword("test-user");
        Tokens tokens = assertLogin(passwordUser.getId());

        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);

        logout(passwordUser.getId(), tokens);
    }

    /**
     * Test the AMR protocol mapper if both password and totp forms have a reference configured.
     * Expected: AMR = ["password", "totp"]
     */
    @Test
    public void testAmrPasswordTotp() {
        setAmr("browser", "auth-username-password-form", "password", DEFAULT_MAX_AGE);
        setAmr("browser", "auth-otp-form", "totp", DEFAULT_MAX_AGE);

        List<String> expectedAmrs = new ArrayList<>() {{
            add("password");
            add("totp");
        }};
        oauth.openLoginForm();
        authenticatePassword("totp-user");
        authenticateTOTP();
        String totpUserId = getTotpUserId();
        Tokens tokens = assertLogin(totpUserId);

        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);

        logout(totpUserId, tokens);
    }

    /**
     * Test the AMR protocol mapper when the max age of the stored amr value has been exceeded.
     * Expected: AMR = []
     */
    @Test
    public void testAmrPastMaxAge() {
        setAmr("browser", "auth-username-password-form", "password", 10);

        List<String> expectedAmrs = new ArrayList<>();
        oauth.openLoginForm();
        authenticatePassword("test-user");

        EventRepresentation loginEvent = EventAssertion.expectLoginSuccess(events.poll())
                .userId(passwordUser.getId()).getEvent();

        timeOffSet.set(20);

        Tokens tokens = sendTokenRequest(loginEvent, passwordUser.getId(), "openid", CLIENT_ID);

        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);

        logout(passwordUser.getId(), tokens);
    }

    /**
     * Test the AMR protocol mapper when the max age of the stored amr value has not been exceeded.
     * Expected: AMR = ["password"]
     */
    @Test
    public void testAmrWithinMaxAge() {
        Tokens tokens;

        setAmr("browser", "auth-username-password-form", "password", 60);
        List<String> expectedAmrs = new ArrayList<>() {{
            add("password");
        }};

        oauth.openLoginForm();
        authenticatePassword("test-user");
        tokens = assertLogin(passwordUser.getId());
        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);

        oauth.openLoginForm();
        tokens = assertLogin(passwordUser.getId());
        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);

        logout(passwordUser.getId(), tokens);
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
        configureStepUpFlow();
        setBrowserFlow();
        configureRealmAcrMap(new HashMap<>(){{
            put("silver", 1);
            put("gold", 2);
        }});

        // configure amr
        setAmr("browser step-up level 1", "auth-username-password-form", "password", DEFAULT_MAX_AGE);
        setAmr("browser step-up level 2", "auth-otp-form", "totp", DEFAULT_MAX_AGE);

        // login at level 1
        oauth.loginForm().claims(claims(true, "silver")).open();
        authenticatePassword("totp-user");
        tokens = assertLogin(getTotpUserId());
        assertAcr(tokens.idToken, "silver");
        assertAcr(tokens.accessToken, "silver");
        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);

        // step-up to level 2
        expectedAmrs.add("totp");
        oauth.loginForm().claims(claims(true, "gold")).open();
        authenticateTOTP();
        tokens = assertLogin(getTotpUserId());
        assertAcr(tokens.idToken, "gold");
        assertAcr(tokens.accessToken, "gold");
        assertAmr(tokens.idToken, expectedAmrs);
        assertAmr(tokens.accessToken, expectedAmrs);

        logout(getTotpUserId(), tokens);
    }

    /**
     * Helper function to set the browser flow for the realm
     */
    private void setBrowserFlow(){
        managedRealm.updateWithCleanup(realm -> realm.browserFlow("browser step-up"));
    }

    /**
     * Helper function to configure the realm acr loa map
     * @param acrLoaMap The map to set
     */
    private void configureRealmAcrMap(Map<String, Integer> acrLoaMap){
        RealmRepresentation realmRep = managedRealm.admin().toRepresentation();
        try {
            realmRep.getAttributes().put(Constants.ACR_LOA_MAP, JsonSerialization.writeValueAsString(acrLoaMap));
        } catch (IOException e){
            throw new RuntimeException("failed to parse acr loa map");
        }
        managedRealm.admin().update(realmRep);
    }

    /**
     * Helper function to configure a step-up flow.
     * Flow: acr=1 -> password, acr=2 -> totp
     */
    private void configureStepUpFlow() {
        // Set LEVEL and MAX_AGE configs for ConditionalLoaAuthenticator executions
        setLoaConfig("browser step-up level 1", ConditionalLoaAuthenticatorFactory.PROVIDER_ID, "1", 60);
        setLoaConfig("browser step-up level 2", ConditionalLoaAuthenticatorFactory.PROVIDER_ID, "2", 60);
    }

    private void setLoaConfig(String flowAlias, String providerId, String level, Integer maxAge) {
        AuthenticationExecutionInfoRepresentation execution = managedRealm.admin().flows().getExecutions(flowAlias)
                .stream()
                .filter(e -> e.getProviderId() != null && e.getProviderId().equals(providerId))
                .findFirst()
                .orElseThrow();

        AuthenticatorConfigRepresentation config = execution.getAuthenticationConfig() != null
                ? managedRealm.admin().flows().getAuthenticatorConfig(execution.getAuthenticationConfig())
                : new AuthenticatorConfigRepresentation();

        config.setAlias(config.getAlias() != null ? config.getAlias() : KeycloakModelUtils.generateId());
        Map<String, String> configValues = config.getConfig() != null ? config.getConfig() : new HashMap<>();
        configValues.put(ConditionalLoaAuthenticator.LEVEL, level);
        configValues.put(ConditionalLoaAuthenticator.MAX_AGE, maxAge.toString());
        config.setConfig(configValues);

        if (execution.getAuthenticationConfig() == null) {
            managedRealm.admin().flows().newExecutionConfig(execution.getId(), config);
        } else {
            managedRealm.admin().flows().updateAuthenticatorConfig(config.getId(), config);
        }
    }

    private void assertAcr(IDToken token, String acr){
        Assertions.assertEquals(acr, token.getAcr());
    }

    public static ClaimsRepresentation claims(boolean essential, String... acrValues) {
        //in order to test both values and value
        //setValue only for essential false and only one value
        ClaimsRepresentation.ClaimValue<String> acrClaim = new ClaimsRepresentation.ClaimValue<>();
        acrClaim.setEssential(essential);
        if (essential || acrValues.length > 1) {
            acrClaim.setValues(Arrays.asList(acrValues));
        } else {
            acrClaim.setValue(acrValues[0]);
        }

        ClaimsRepresentation claims = new ClaimsRepresentation();
        claims.setIdTokenClaims(Collections.singletonMap(IDToken.ACR, acrClaim));
        return claims;
    }

    private void setAmr(String flowAlias, String providerId, String amrValue, Integer maxAge) {
        AuthenticationExecutionInfoRepresentation execution = managedRealm.admin().flows().getExecutions(flowAlias)
                .stream()
                .filter(e -> e.getProviderId() != null && e.getProviderId().equals(providerId))
                .findFirst()
                .orElseThrow();

        AuthenticatorConfigRepresentation config = execution.getAuthenticationConfig() != null
                ? managedRealm.admin().flows().getAuthenticatorConfig(execution.getAuthenticationConfig())
                : new AuthenticatorConfigRepresentation();

        config.setAlias(config.getAlias() != null ? config.getAlias() : KeycloakModelUtils.generateId());
        Map<String, String> configValues = config.getConfig() != null ? config.getConfig() : new HashMap<>();
        configValues.put(AMR_VALUE_KEY, amrValue);
        configValues.put(AMR_MAX_AGE_KEY, maxAge.toString());
        config.setConfig(configValues);

        if (execution.getAuthenticationConfig() == null) {
            managedRealm.admin().flows().newExecutionConfig(execution.getId(), config);
        } else {
            managedRealm.admin().flows().updateAuthenticatorConfig(config.getId(), config);
        }
    }

    private void authenticatePassword(String username) {
        loginPage.assertCurrent();
        loginPage.fillLogin(username, AuthenticationMethodReferenceTest.PASSWORD);
        loginPage.submit();
    }

    private void authenticateTOTP() {
        loginTotpPage.assertCurrent();
        setOtpTimeOffset(totp);
        loginTotpPage.login(totp.generateTOTP(AuthenticationMethodReferenceTest.TOTP_SECRET));
    }

    private void setOtpTimeOffset(TimeBasedOTP otp) {
        timeOffSet.set(TimeBasedOTP.DEFAULT_INTERVAL_SECONDS);
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, TimeBasedOTP.DEFAULT_INTERVAL_SECONDS);
        otp.setCalendar(calendar);
    }

    private Tokens assertLogin(String userId) {
        EventRepresentation loginEvent = EventAssertion.expectLoginSuccess(events.poll())
                .userId(userId).getEvent();
        return sendTokenRequest(loginEvent, userId, "openid", CLIENT_ID);
    }

    private void logout(String userId, Tokens tokens) {
        oauth.doLogout(tokens.refreshToken);
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGOUT)
                .sessionId(tokens.idToken.getSessionId())
                .clientId(CLIENT_ID)
                .userId(userId)
                .withoutDetails(Details.REDIRECT_URI);
    }

    private void assertAmr(IDToken token, List<String> expectedValues) {
        List<String> amr = (List<String>) token.getOtherClaims().get("amr");
        Assertions.assertNotNull(amr);

        Collections.sort(amr);
        Collections.sort(expectedValues);

        Assertions.assertArrayEquals(expectedValues.toArray(), amr.toArray());
    }


    private String getTotpUserId() {
        return AdminApiUtil.findUserByUsername(managedRealm.admin(), "totp-user").getId();
    }

    private static class AuthenticationMethodReferenceRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .users(
                            UserBuilder.create("totp-user")
                                    .email("totp-user@email.com")
                                    .firstName("totp-user")
                                    .lastName("totp-user")
                                    .password(PASSWORD)
                                    .totpSecret(TOTP_SECRET)
                    )
                    .clientScopes(
                            ClientScopeBuilder.create()
                                    .name("oidc-amr-mapper")
                                    .protocol("openid-connect")
                                    .attribute(ClientScopeModel.INCLUDE_IN_TOKEN_SCOPE, "false")
                                    .attribute(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, "false")
                                    .mappers(ProtocolMapperBuilder.create()
                                            .name("oidc-amr-mapper")
                                            .protocol("openid-connect")
                                            .protocolMapper("oidc-amr-mapper")
                                            .config("id.token.claim", "true")
                                            .config("access.token.claim", "true")
                                            .build()),
                            ClientScopeBuilder.create()
                                    .name("oidc-acr-mapper")
                                    .protocol("openid-connect")
                                    .attribute(ClientScopeModel.INCLUDE_IN_TOKEN_SCOPE, "false")
                                    .attribute(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, "false")
                                    .mappers(ProtocolMapperBuilder.create()
                                            .name("oidc-acr-mapper")
                                            .protocol("openid-connect")
                                            .protocolMapper("oidc-acr-mapper")
                                            .config("id.token.claim", "true")
                                            .config("access.token.claim", "true")
                                            .build())
                    )
                    .defaultClientScopes("oidc-amr-mapper", "oidc-acr-mapper")
                    .authenticationFlows(
                            AuthenticationFlowBuilder.create("browser step-up", "browser based authentication", "basic-flow", true, false)
                                    .authenticationExecutions(
                                            AuthenticationExecutionExportBuilder.authenticator(CookieAuthenticatorFactory.PROVIDER_ID,
                                                    AuthenticationExecutionModel.Requirement.ALTERNATIVE.name(), 10, false),
                                            AuthenticationExecutionExportBuilder.alias("browser step-up forms",
                                                    AuthenticationExecutionModel.Requirement.ALTERNATIVE.name(), 20, false)
                                    ),
                            AuthenticationFlowBuilder.create("browser step-up forms", "browser step-up forms", "basic-flow", false, false)
                                    .authenticationExecutions(
                                            AuthenticationExecutionExportBuilder.alias("browser step-up level 1",
                                                    AuthenticationExecutionModel.Requirement.CONDITIONAL.name(), 10, false),
                                            AuthenticationExecutionExportBuilder.alias("browser step-up level 2",
                                                    AuthenticationExecutionModel.Requirement.CONDITIONAL.name(), 20, false)
                                    ),
                            AuthenticationFlowBuilder.create("browser step-up level 1", "Level 1 authentication", "basic-flow", false, false)
                                    .authenticationExecutions(
                                            AuthenticationExecutionExportBuilder.authenticator(ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                                    AuthenticationExecutionModel.Requirement.REQUIRED.name(), 10, false),
                                            AuthenticationExecutionExportBuilder.authenticator(UsernamePasswordFormFactory.PROVIDER_ID,
                                                    AuthenticationExecutionModel.Requirement.REQUIRED.name(), 20, false)
                                    ),
                            AuthenticationFlowBuilder.create("browser step-up level 2", "Level 2 authentication", "basic-flow", false, false)
                                    .authenticationExecutions(
                                            AuthenticationExecutionExportBuilder.authenticator(ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                                    AuthenticationExecutionModel.Requirement.REQUIRED.name(), 10, false),
                                            AuthenticationExecutionExportBuilder.authenticator(OTPFormAuthenticatorFactory.PROVIDER_ID,
                                                    AuthenticationExecutionModel.Requirement.REQUIRED.name(), 20, false)
                                    )
                    );
        }
    }

    private static class AmrClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId(CLIENT_ID)
                    .secret(CLIENT_SECRET)
                    .redirectUris("*")
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(true)
                    .defaultClientScopes("oidc-amr-mapper", "oidc-acr-mapper");
        }
    }

    private static class PasswordUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("test-user")
                    .email("test-user@email.com")
                    .firstName("test-user")
                    .lastName("test-user")
                    .password(PASSWORD);
        }
    }

}
