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

package org.keycloak.testsuite.forms;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.RecoveryAuthnCodesFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.common.Profile;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.AuthenticationFlowBindings;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.account.AccountRestClient;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.authentication.PushButtonAuthenticatorFactory;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.DeleteCredentialPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.pages.PushTheButtonPage;
import org.keycloak.testsuite.pages.SelectAuthenticatorPage;
import org.keycloak.testsuite.pages.SetupRecoveryAuthnCodesPage;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.RealmRepUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.JsonSerialization;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.actions.AppInitiatedActionDeleteCredentialTest.getKcActionParamForDeleteCredential;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for Level Of Assurance conditions in authentication flow.
 *
 * @author <a href="mailto:sebastian.zoescher@prime-sign.com">Sebastian Zoescher</a>
 */
public class LevelOfAssuranceFlowTest extends AbstractChangeImportedUserPasswordsTest {

    private final static String FLOW_ALIAS = "browser -  Level of Authentication FLow";

    private static final String CLIENT_ID = "test-app";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    protected LoginConfigTotpPage totpSetupPage;

    @Page
    protected SetupRecoveryAuthnCodesPage setupRecoveryAuthnCodesPage;

    @Page
    protected SelectAuthenticatorPage selectAuthenticatorPage;

    private TimeBasedOTP totp = new TimeBasedOTP();

    @Page
    protected PushTheButtonPage pushTheButtonPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected DeleteCredentialPage deleteCredentialPage;

    @Page
    protected AppPage appPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        try {
            testRealm.setOtpPolicyCodeReusable(true);
            findTestApp(testRealm).setAttributes(Collections.singletonMap(Constants.ACR_LOA_MAP, getAcrToLoaMappingForClient()));
            UserRepresentation user = RealmRepUtil.findUser(testRealm, "test-user@localhost");
            UserBuilder.edit(user)
                    .totpSecret("totpSecret")
                    .otpEnabled();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void beforeTest() {
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        UserRepresentation userRep = user.toRepresentation();
        user.remove();

        userRep.setId(null);
        UserBuilder.edit(userRep)
                .password(generatePassword("test-user@localhost"))
                .totpSecret("totpSecret")
                .otpEnabled();
        Response response = testRealm().users().create(userRep);
        Assert.assertEquals(201, response.getStatus());
        response.close();
    }

    private String getAcrToLoaMappingForClient() throws IOException {
        Map<String, Integer> acrLoaMap = new HashMap<>();
        acrLoaMap.put("copper", 0);
        acrLoaMap.put("silver", 1);
        acrLoaMap.put("gold", 2);
        return JsonSerialization.writeValueAsString(acrLoaMap);
    }

    @Before
    public void setupFlow() {
        configureStepUpFlow(testingClient);
        canBeOtpCodesReusable(true);
    }

    @After
    public void tearDown() {
        canBeOtpCodesReusable(false);
    }

    // Fixing this test with not reusable OTP codes would bring additional unwanted workarounds; not scope of this test
    private void canBeOtpCodesReusable(boolean state) {
        new RealmAttributeUpdater(testRealm())
                .setOtpPolicyCodeReusable(state)
                .update();
    }

    public static void configureStepUpFlow(KeycloakTestingClient testingClient) {
        configureStepUpFlow(testingClient, ConditionalLoaAuthenticator.DEFAULT_MAX_AGE, 0, 0);
    }

    private static void configureStepUpFlow(KeycloakTestingClient testingClient, int maxAge1, int maxAge2, int maxAge3) {
        testingClient.server(TEST_REALM_NAME).run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(FLOW_ALIAS));
        testingClient.server(TEST_REALM_NAME)
                .run(session -> FlowUtil.inCurrentRealm(session).selectFlow(FLOW_ALIAS).inForms(forms -> forms.clear()
                        // level 1 authentication
                        .addSubFlowExecution("level1-subflow", AuthenticationFlow.BASIC_FLOW, Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "1");
                                        config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(maxAge1));
                                    });

                            // username input for level 1
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID);
                        })

                        // level 2 authentication
                        .addSubFlowExecution("level2-subflow", AuthenticationFlow.BASIC_FLOW, Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "2");
                                        config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(maxAge2));
                                    });

                            // password required for level 2
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, OTPFormAuthenticatorFactory.PROVIDER_ID);
                        })

                        // level 3 authentication
                        .addSubFlowExecution("level3-subflow", AuthenticationFlow.BASIC_FLOW, Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "3");
                                        config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(maxAge3));
                                    });

                            // simply push button for level 3
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, PushButtonAuthenticatorFactory.PROVIDER_ID);
                        })

                ).defineAsBrowserFlow());
    }

    private void reconfigureStepUpFlow(int maxAge1, int maxAge2, int maxAge3) {
        BrowserFlowTest.revertFlows(testRealm(), FLOW_ALIAS);
        configureStepUpFlow(testingClient, maxAge1, maxAge2, maxAge3);
    }

    private static void configureFlowsWithRecoveryCodes(KeycloakTestingClient testingClient) {
        testingClient.server(TEST_REALM_NAME)
                .run(session -> {
                    FlowUtil.inCurrentRealm(session).selectFlow(FLOW_ALIAS).inForms(forms ->
                                    // Remove "OTP" required execution
                                    forms.selectFlow("level2-subflow")
                                            .removeExecution(1)
                                            .addAuthenticatorExecution(Requirement.ALTERNATIVE, OTPFormAuthenticatorFactory.PROVIDER_ID)
                                            .addAuthenticatorExecution(Requirement.ALTERNATIVE, RecoveryAuthnCodesFormAuthenticatorFactory.PROVIDER_ID)
                    );
                });
    }

    @After
    public void after() {
        BrowserFlowTest.revertFlows(testRealm(), FLOW_ALIAS);
    }

    @Test
    public void loginWithoutAcr() {
        oauth.openLoginForm();
        // Authentication without specific LOA results in level 1 authentication
        authenticateWithUsernamePassword();
        assertLoggedInWithAcr("silver");
    }

    @Test
    public void loginWithAcr1() {
        // username input for level 1
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsernamePassword();
        assertLoggedInWithAcr("silver");

    }

    @Test
    public void loginWithAcr2() {
        // username and password input for level 2
        openLoginFormWithAcrClaim(true, "gold");
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");
    }

    @Test
    public void loginWithAcr3() {
        // username, password input and finally push button for level 3
        openLoginFormWithAcrClaim(true, "3");
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        authenticateWithButton();
        // ACR 3 is returned because it was requested, although there is no mapping for it
        assertLoggedInWithAcr("3");
    }

    @Test
    public void stepupAuthentication() {
        // logging in to level 1
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsernamePassword();
        assertLoggedInWithAcr("silver");
        // doing step-up authentication to level 2
        openLoginFormWithAcrClaim(true, "gold");
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");
        // step-up to level 3 needs password authentication because level 2 is not stored in user session
        openLoginFormWithAcrClaim(true, "3");
        authenticateWithTotp();
        authenticateWithButton();
        assertLoggedInWithAcr("3");
    }

    @Test
    public void stepupAuthenticationNoAuthSessionCookie() {
        // logging in to level 1
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsernamePassword();
        assertLoggedInWithAcr("silver");
        // doing step-up authentication to level 2
        openLoginFormWithAcrClaim(true, "gold");
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");
        // going back to login again, otp should be presented as by default max-age is 0
        openLoginFormWithAcrClaim(true, "gold");
        // remove the auth session cookie emulating a browser restart in which this cookie is lost
        driver.manage().deleteCookieNamed(CookieType.AUTH_SESSION_ID.getName());
        openLoginFormWithAcrClaim(true, "gold");
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");
    }

    @Test
    public void stepupToUnknownEssentialAcrFails() {
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsernamePassword();
        assertLoggedInWithAcr("silver");
        // step-up to unknown acr
        openLoginFormWithAcrClaim(true, "uranium");
        assertErrorPage("Invalid parameter: claims");
    }

    @Test
    public void reauthenticationWithNoAcr() {
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsernamePassword();
        assertLoggedInWithAcr("silver");
        oauth.openLoginForm();
        assertLoggedInWithAcr("silver"); // Return silver without need to re-authenticate due maxAge for "silver" condition did not timed-out yet
    }

    @Test
    public void reauthenticationWithReachedAcr() {
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsernamePassword();
        assertLoggedInWithAcr("silver");
        openLoginFormWithAcrClaim(true, "silver");
        assertLoggedInWithAcr("silver"); // Return previous level due maxAge for "silver" condition did not timed-out yet
    }

    @Test
    public void reauthenticationWithOptionalUnknownAcr() {
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsernamePassword();
        assertLoggedInWithAcr("silver");
        openLoginFormWithAcrClaim(false, "iron");
        assertLoggedInWithAcr("silver"); // Return silver without need to re-authenticate due maxAge for "silver" condition did not timed-out yet
    }

    @Test
    public void essentialClaimNotReachedFails() {
        openLoginFormWithAcrClaim(true, "4");
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        authenticateWithButton();
        assertErrorPage("Authentication requirements not fulfilled");
    }

    @Test
    public void optionalClaimNotReachedSucceeds() {
        openLoginFormWithAcrClaim(false, "4");
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        authenticateWithButton();
        // the reached loa is 3, but there is no mapping for it, and it was not explicitly
        // requested, so the highest known and reached ACR is returned
        assertLoggedInWithAcr("gold");
    }

    @Test
    public void essentialUnknownClaimFails() {
        openLoginFormWithAcrClaim(true, "uranium");
        assertErrorPage("Invalid parameter: claims");
    }

    @Test
    public void optionalUnknownClaimSucceeds() {
        openLoginFormWithAcrClaim(false, "iron");
        authenticateWithUsernamePassword();
        assertLoggedInWithAcr("silver");
    }

    @Test
    public void acrValuesQueryParameter() {
        oauth.loginForm().param("acr_values", "gold 3").open();
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");
    }

    @Test
    public void multipleEssentialAcrValues() {
        openLoginFormWithAcrClaim(true, "gold", "3");
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");
    }

    @Test
    public void multipleOptionalAcrValues() {
        openLoginFormWithAcrClaim(false, "gold", "3");
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");
    }


    @Test
    public void testRealmAcrLoaMapping() throws IOException {
        // Setup realm acr-to-loa mapping
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, Integer> acrLoaMap = new HashMap<>();
        acrLoaMap.put("realm:copper", 0);
        acrLoaMap.put("realm:silver", 1);
        acrLoaMap.put("realm:gold", 2);
        realmRep.getAttributes().put(Constants.ACR_LOA_MAP, JsonSerialization.writeValueAsString(acrLoaMap));
        testRealm().update(realmRep);

        // Remove acr-to-loa mapping from the client. It should use realm acr-to-loa mapping
        ClientResource testClient = ApiUtil.findClientByClientId(testRealm(), CLIENT_ID);
        ClientRepresentation testClientRep = testClient.toRepresentation();
        testClientRep.getAttributes().put(Constants.ACR_LOA_MAP, "{}");
        testClient.update(testClientRep);

        openLoginFormWithAcrClaim(true, "realm:gold");
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        assertLoggedInWithAcr("realm:gold");

        // Add "acr-to-loa" back to the client. Client mapping will be used instead of realm mapping
        testClientRep.getAttributes().put(Constants.ACR_LOA_MAP, getAcrToLoaMappingForClient());
        testClient.update(testClientRep);

        openLoginFormWithAcrClaim(true, "realm:gold");
        assertErrorPage("Invalid parameter: claims");

        openLoginFormWithAcrClaim(true, "gold");
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");

        // Rollback
        realmRep.getAttributes().remove(Constants.ACR_LOA_MAP);
        testRealm().update(realmRep);
    }

    @Test
    public void testClientDefaultAcrValues() {
        ClientResource testClient = ApiUtil.findClientByClientId(testRealm(), CLIENT_ID);
        ClientRepresentation testClientRep = testClient.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setAttributeMultivalued(Constants.DEFAULT_ACR_VALUES, Arrays.asList("silver", "gold"));
        testClient.update(testClientRep);

        // Should request client to authenticate with silver
        oauth.openLoginForm();
        authenticateWithUsernamePassword();
        assertLoggedInWithAcr("silver");

        // Re-configure to level gold
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setAttributeMultivalued(Constants.DEFAULT_ACR_VALUES, Arrays.asList("gold"));
        testClient.update(testClientRep);
        oauth.openLoginForm();
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");

        // Value from essential ACR from claims parameter should have preference over the client default
        openLoginFormWithAcrClaim(true, "silver");
        assertLoggedInWithAcr("silver");

        // Value from non-essential ACR from claims parameter should have preference over the client default
        openLoginFormWithAcrClaim(false, "silver");
        assertLoggedInWithAcr("silver");

        // Revert
        testClientRep.getAttributes().put(Constants.DEFAULT_ACR_VALUES, null);
        testClient.update(testClientRep);
    }

    @Test
    public void testClientDefaultAcrValuesValidation() throws IOException {
        // Setup realm acr-to-loa mapping
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, Integer> acrLoaMap = new HashMap<>();
        acrLoaMap.put("realm:copper", 0);
        acrLoaMap.put("realm:silver", 1);
        realmRep.getAttributes().put(Constants.ACR_LOA_MAP, JsonSerialization.writeValueAsString(acrLoaMap));
        testRealm().update(realmRep);

        // Value "foo" not used in any ACR-To-Loa mapping
        ClientResource testClient = ApiUtil.findClientByClientId(testRealm(), CLIENT_ID);
        ClientRepresentation testClientRep = testClient.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setAttributeMultivalued(Constants.DEFAULT_ACR_VALUES, Arrays.asList("silver", "2", "foo"));
        try {
            testClient.update(testClientRep);
            Assert.fail("Should not successfully update client");
        } catch (BadRequestException bre) {
            // Expected
        }

        // Value "5" too big
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setAttributeMultivalued(Constants.DEFAULT_ACR_VALUES, Arrays.asList("silver", "2", "5"));
        try {
            testClient.update(testClientRep);
            Assert.fail("Should not successfully update client");
        } catch (BadRequestException bre) {
            // Expected
        }

        // Should be fine
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setAttributeMultivalued(Constants.DEFAULT_ACR_VALUES, Arrays.asList("silver", "2"));
        testClient.update(testClientRep);

        // Revert
        testClientRep.getAttributes().put(Constants.DEFAULT_ACR_VALUES, null);
        testClient.update(testClientRep);
        realmRep.getAttributes().remove(Constants.ACR_LOA_MAP);
        testRealm().update(realmRep);
    }

    @Test
    public void testClientMinimumAcrValueValidation() throws IOException {
        // Setup realm acr-to-loa mapping
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, Integer> acrLoaMap = new HashMap<>();
        acrLoaMap.put("realm:copper", 0);
        acrLoaMap.put("realm:silver", 1);
        realmRep.getAttributes().put(Constants.ACR_LOA_MAP, JsonSerialization.writeValueAsString(acrLoaMap));
        testRealm().update(realmRep);

        // Value "foo" not used in any ACR-To-Loa mapping
        ClientResource testClient = ApiUtil.findClientByClientId(testRealm(), CLIENT_ID);
        ClientRepresentation testClientRep = testClient.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue("foo");
        Assert.assertThrows(BadRequestException.class, () -> {
            testClient.update(testClientRep);
        });

        // Realm value should not be considered either
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue("realm:silver");
        Assert.assertThrows(BadRequestException.class, () -> {
            testClient.update(testClientRep);
        });

        // Value from client map should be OK
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue("silver");
        testClient.update(testClientRep);

        // Cleanup
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue(null);
        testClient.update(testClientRep);
        realmRep.getAttributes().remove(Constants.ACR_LOA_MAP);
        testRealm().update(realmRep);
    }

    // After initial authentication with "acr=2", there will be further re-authentication requests sent in different intervals
    // without "acr" parameter. User should be always re-authenticated due SSO, but with different acr levels due their gradual expirations
    @Test
    public void testMaxAgeConditionWithSSO() {
        reconfigureStepUpFlow(300, 300, 200);

        // Authentication
        openLoginFormWithAcrClaim(true, "3");
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        authenticateWithButton();
        assertLoggedInWithAcr("3");

        // Re-auth 1: Should be automatically authenticated and still return "3"
        oauth.openLoginForm();
        assertLoggedInWithAcr("3");

        // Time offset to 210
        setTimeOffset(210);

        // Re-auth 2: Should return level 2 (gold) due level 3 expired
        oauth.openLoginForm();
        assertLoggedInWithAcr("gold");

        // Time offset to 310
        setTimeOffset(310);

        // Re-auth 3: Should return level 0 (copper) due levels 1 and 2 expired
        oauth.openLoginForm();
        assertLoggedInWithAcr("copper");
    }

    // After initial authentication with "acr=2", there will be further re-authentication requests sent in different intervals
    // asking for "acr=2" . User should be asked for re-authentication with various authenticators in various cases.
    @Test
    public void testMaxAgeConditionWithAcr() {
        reconfigureStepUpFlow(300, 200, 200);

        // Authentication
        openLoginFormWithAcrClaim(true, "3");
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        authenticateWithButton();
        assertLoggedInWithAcr("3");

        // Re-auth 1: Should be automatically authenticated and still return "3"
        openLoginFormWithAcrClaim(true, "3");
        assertLoggedInWithAcr("3");

        // Time offset to 210
        setTimeOffset(210);

        // Re-auth 2: Should ask user for re-authentication with level2 and level3. Level1 did not yet expired and should be automatic
        openLoginFormWithAcrClaim(true, "3");
        authenticateWithTotp();
        authenticateWithButton();
        assertLoggedInWithAcr("3");

        // Time offset to 310
        setTimeOffset(310);

        // Re-auth 3: Should ask user for re-authentication with level1. Level2 and Level3 did not yet expired and should be automatic
        openLoginFormWithAcrClaim(true, "3");
        reauthenticateWithPassword();
        assertLoggedInWithAcr("3");
    }

    // Authenticate with LoA=3 and then send request with "prompt=login" to force re-authentication
    @Test
    public void testMaxAgeConditionWithForcedReauthentication() {
        reconfigureStepUpFlow(300, 300, 300);

        // Authentication
        openLoginFormWithAcrClaim(true, "3");
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        authenticateWithButton();
        assertLoggedInWithAcr("3");

        // Send request with prompt=login . User should be asked to re-authenticate with level 1
        oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).open();
        reauthenticateWithPassword();
        assertLoggedInWithAcr("silver");

        // Request with prompt=login together with "acr=2" . User should be asked to re-authenticate with level 2
        oauth.loginForm().claims(claims(true, "gold")).prompt("login").open();
        reauthenticateWithPassword();
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");

        // Request with "acr=3", but without prompt. User should be automatically authenticated
        openLoginFormWithAcrClaim(true, "3");
        assertLoggedInWithAcr("3");
    }


    @Test
    public void testChangingLoaConditionConfiguration() {
        // Authentication
        oauth.openLoginForm();
        authenticateWithUsernamePassword();
        assertLoggedInWithAcr("silver");

        setTimeOffset(120);


        // Change condition configuration to 60
        reconfigureStepUpFlow(60, 0, 0);

        // Re-authenticate without "acr". Should return 0 (copper) due the SSO. Level "silver" should not be returned due it is expired
        // based on latest condition configuration
        oauth.openLoginForm();
        assertLoggedInWithAcr("copper");

        // Re-authenticate with requested ACR=1 (silver). User should be asked to re-authenticate
        openLoginFormWithAcrClaim(true, "silver");
        reauthenticateWithPassword();
        assertLoggedInWithAcr("silver");
    }


    // Backwards compatibility with Keycloak 17 when condition was configured with option "Store Loa in User Session"
    @Test
    public void testBackwardsCompatibilityForLoaConditionConfig() {
        // Reconfigure to the format of Keycloak 17 with option "Store Loa in User Session"
        BrowserFlowTest.revertFlows(testRealm(), FLOW_ALIAS);
        testingClient.server(TEST_REALM_NAME).run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(FLOW_ALIAS));
        testingClient.server(TEST_REALM_NAME)
                .run(session -> FlowUtil.inCurrentRealm(session).selectFlow(FLOW_ALIAS).inForms(forms -> forms.clear()
                        // level 1 authentication
                        .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "1");
                                        config.getConfig().put(ConditionalLoaAuthenticator.STORE_IN_USER_SESSION, "true");
                                    });

                            // username input for level 1
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID);
                        })

                        // level 2 authentication
                        .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "2");
                                        config.getConfig().put(ConditionalLoaAuthenticator.STORE_IN_USER_SESSION, "false");
                                    });

                            // password required for level 2
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, OTPFormAuthenticatorFactory.PROVIDER_ID);
                        })

                        // level 3 authentication
                        .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "3");
                                        config.getConfig().put(ConditionalLoaAuthenticator.STORE_IN_USER_SESSION, "false");
                                    });

                            // simply push button for level 3
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, PushButtonAuthenticatorFactory.PROVIDER_ID);
                        })

                ).defineAsBrowserFlow());

        // Tests that re-authentication always needed for levels 2 and 3
        stepupAuthentication();
    }


    @Test
    @DisableFeature(value = Profile.Feature.STEP_UP_AUTHENTICATION, skipRestart = true)
    public void testDisableStepupFeatureTest() {
        BrowserFlowTest.revertFlows(testRealm(), FLOW_ALIAS);

        // Login normal way - should return 1 (backwards compatibility before step-up was introduced)
        loginPage.open();
        authenticateWithUsernamePassword();
        authenticateWithTotp(); // OTP required due the user has it
        assertLoggedInWithAcr("1");

        // SSO login - should return 0 (backwards compatibility before step-up was introduced)
        oauth.openLoginForm();
        assertLoggedInWithAcr("0");

        // Flow is needed due the "after()" method
        testingClient.server(TEST_REALM_NAME).run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(FLOW_ALIAS));
    }

    @Test
    @DisableFeature(value = Profile.Feature.STEP_UP_AUTHENTICATION, skipRestart = true)
    public void testDisableStepupFeatureInNewRealm() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("new-realm");
        adminClient.realms().create(rep);
        RealmResource newRealm = adminClient.realms().realm("new-realm");
        try {
            // Test client scope was not created in the new realm when feature is disabled
            boolean acrScopeExists = newRealm.clientScopes().findAll().stream()
                    .anyMatch(clientScope -> OIDCLoginProtocolFactory.ACR_SCOPE.equals(clientScope.getName()));
            assertThat(false, is(acrScopeExists));
        } finally {
            newRealm.remove();
        }
    }

    @Test
    public void testWithMultipleOTPCodes() throws Exception {
        // Get regular authentication. Only level1 required.
        oauth.openLoginForm();
        // Authentication without specific LOA results in level 1 authentication
        authenticateWithUsernamePassword();
        TokenCtx token1 = assertLoggedInWithAcr("silver");

        // Add "kc_action" for setup another OTP. Existing OTP authentication should be required. No offer for recovery-codes as they are different level
        oauth.loginForm().kcAction(UserModel.RequiredAction.CONFIGURE_TOTP.name()).open();
        loginTotpPage.assertCurrent();
        loginTotpPage.assertOtpCredentialSelectorAvailability(false);

        authenticateWithTotp();
        totpSetupPage.assertCurrent();
        totpSetupPage.configure(totp.generateTOTP(totpSetupPage.getTotpSecret()), "totp2-label");
        events.expectRequiredAction(EventType.UPDATE_TOTP).detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE).assertEvent();
        TokenCtx token2 = assertLoggedInWithAcr("gold");

        // Trying to add another OTP by "kc_action". Level 2 should be required and user can choose between 2 OTP codes
        oauth.loginForm().kcAction(UserModel.RequiredAction.CONFIGURE_TOTP.name()).open();
        loginTotpPage.assertCurrent();
        loginTotpPage.assertOtpCredentialSelectorAvailability(true);
        List<String> availableOtps = loginTotpPage.getAvailableOtpCredentials();
        Assert.assertNames(availableOtps, OTPFormAuthenticator.UNNAMED, "totp2-label");

        // Removing 2nd OTP by account REST API with regular token. Should fail as acr=2 is required
        String otpCredentialId;
        try (AccountRestClient accountRestClient = AccountRestClient
                .builder(suiteContext)
                .accessToken(token1.accessToken)
                .build()) {
            otpCredentialId = accountRestClient.getCredentialByUserLabel("totp2-label").getId();
            try (SimpleHttpResponse response = accountRestClient.removeCredential(otpCredentialId)) {
                Assert.assertEquals(403, response.getStatus());
            }
        }

        // Removing 2nd OTP by account REST API with level2 token. Should work as acr=2 is required
        try (AccountRestClient accountRestClient = AccountRestClient
                .builder(suiteContext)
                .accessToken(token2.accessToken)
                .build()) {
            otpCredentialId = accountRestClient.getCredentialByUserLabel("totp2-label").getId();
            try (SimpleHttpResponse response = accountRestClient.removeCredential(otpCredentialId)) {
                Assert.assertEquals(204, response.getStatus());
            }
            Assert.assertNull(accountRestClient.getCredentialByUserLabel("totp2-label"));
        }
    }

    @Test
    public void testDeleteCredentialAction() throws Exception {
        // Login level1
        oauth.openLoginForm();
        authenticateWithUsernamePassword();
        TokenCtx token1 = assertLoggedInWithAcr("silver");

        // Setup another OTP (requires login with existing OTP)
        oauth.loginForm().kcAction(UserModel.RequiredAction.CONFIGURE_TOTP.name()).open();
        authenticateWithTotp();
        totpSetupPage.assertCurrent();
        totpSetupPage.configure(totp.generateTOTP(totpSetupPage.getTotpSecret()), "totp2-label");
        events.expectRequiredAction(EventType.UPDATE_TOTP).detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE).assertEvent();
        TokenCtx token2 = assertLoggedInWithAcr("gold");

        String otp2CredentialId = getCredentialIdByLabel("totp2-label");

        // Delete OTP credential requires level2. Re-authentication is required (because of max_age=0 for level2 evaluated during re-authentication)
        oauth.loginForm().kcAction(getKcActionParamForDeleteCredential(otp2CredentialId)).open();
        loginTotpPage.assertCurrent();
        authenticateWithTotp();

        deleteCredentialPage.assertCurrent();
        deleteCredentialPage.assertCredentialInMessage("totp2-label");
        deleteCredentialPage.confirm();

        events.expectRequiredAction(EventType.REMOVE_TOTP).detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE).assertEvent();
        events.expectRequiredAction(EventType.REMOVE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE).assertEvent();
        assertLoggedInWithAcr("gold");
    }

    @Test
    public void testWithOTPAndRecoveryCodesAtLevel2() {
        configureFlowsWithRecoveryCodes(testingClient);
        try {
            // Get regular authentication. Only level1 required.
            oauth.openLoginForm();
            authenticateWithUsernamePassword();
            TokenCtx token1 = assertLoggedInWithAcr("silver");

            // Trying to delete existing OTP. Should require authentication with this OTP
            String otpCredentialId = getCredentialIdByType(OTPCredentialModel.TYPE);
            oauth.loginForm().kcAction(getKcActionParamForDeleteCredential(otpCredentialId)).open();
            Assert.assertEquals("Strong authentication required to continue", loginPage.getInfoMessage());
            authenticateWithTotp();

            deleteCredentialPage.assertCurrent();
            deleteCredentialPage.assertCredentialInMessage("otp");
            deleteCredentialPage.confirm();
            events.expectRequiredAction(EventType.REMOVE_TOTP).detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE).assertEvent();
            events.expectRequiredAction(EventType.REMOVE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE).assertEvent();
            assertLoggedInWithAcr("gold");

            // Trying to add OTP. No 2nd factor should be required as user doesn't have any
            oauth.loginForm().kcAction(UserModel.RequiredAction.CONFIGURE_TOTP.name()).open();
            totpSetupPage.assertCurrent();
            String totp2Secret = totpSetupPage.getTotpSecret();
            totpSetupPage.configure(totp.generateTOTP(totp2Secret), "totp2-label");
            events.expectRequiredAction(EventType.UPDATE_TOTP).detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE).assertEvent();
            events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE).assertEvent();
            assertLoggedInWithAcr("silver");

            // set time offset for OTP as it is not permitted to authenticate with same OTP code multiple times
            setOtpTimeOffset(TimeBasedOTP.DEFAULT_INTERVAL_SECONDS, totp);

            // Add "kc_action" for setup recovery codes. OTP should be required
            oauth.loginForm().kcAction(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name()).open();
            loginTotpPage.assertCurrent();
            loginTotpPage.login(totp.generateTOTP(totp2Secret));
            setupRecoveryAuthnCodesPage.assertCurrent();
            setupRecoveryAuthnCodesPage.clickSaveRecoveryAuthnCodesButton();
            events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).assertEvent();
            assertLoggedInWithAcr("gold");

            // Removing recovery-code credential. User required to authenticate with 2nd-factor. He can choose between OTP or recovery-codes
            String recoveryCodesId = getCredentialIdByType(RecoveryAuthnCodesCredentialModel.TYPE);
            oauth.loginForm().kcAction(getKcActionParamForDeleteCredential(recoveryCodesId)).open();
            loginTotpPage.assertCurrent();
            loginTotpPage.clickTryAnotherWayLink();
            selectAuthenticatorPage.assertCurrent();
            Assert.assertEquals(Arrays.asList(SelectAuthenticatorPage.AUTHENTICATOR_APPLICATION, SelectAuthenticatorPage.RECOVERY_AUTHN_CODES), selectAuthenticatorPage.getAvailableLoginMethods());
            selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.AUTHENTICATOR_APPLICATION);
            loginTotpPage.assertCurrent();
            loginTotpPage.login(totp.generateTOTP(totp2Secret));

            deleteCredentialPage.assertCurrent();
            deleteCredentialPage.assertCredentialInMessage("Recovery codes");
            deleteCredentialPage.confirm();
            events.expectRequiredAction(EventType.REMOVE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, RecoveryAuthnCodesCredentialModel.TYPE).assertEvent();
            assertLoggedInWithAcr("gold");
        } finally {
            setOtpTimeOffset(0, totp);
        }
    }

    @Test
    public void testLoginWithMinimumAcrWithoutAcrValues() {
        ClientResource testClient = ApiUtil.findClientByClientId(testRealm(), CLIENT_ID);
        ClientRepresentation testClientRep = testClient.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue("gold");
        testClient.update(testClientRep);

        // Should request client to authenticate with gold
        oauth.openLoginForm();
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");

        // Revert
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue(null);
        testClient.update(testClientRep);
    }

    @Test
    public void testLoginWithMinimumAcrWithLowerAcrValues() {
        ClientResource testClient = ApiUtil.findClientByClientId(testRealm(), CLIENT_ID);
        ClientRepresentation testClientRep = testClient.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue("gold");
        testClient.update(testClientRep);

        // Should request client to authenticate with gold, even if the client sends silver
        oauth.loginForm().param("acr_values", "silver").open();
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");

        // Revert
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue(null);
        testClient.update(testClientRep);
    }

    @Test
    public void testLoginWithMinimumAcrWithHigherAcrValues() {
        ClientResource testClient = ApiUtil.findClientByClientId(testRealm(), CLIENT_ID);
        ClientRepresentation testClientRep = testClient.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue("gold");
        testClient.update(testClientRep);

        // Should request client to authenticate with gold, even if the client sends silver
        oauth.loginForm().param("acr_values", "3").open();
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        authenticateWithButton();
        assertLoggedInWithAcr("3");

        // Revert
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue(null);
        testClient.update(testClientRep);
    }

    @Test
    public void testEssentialAcrMinimumOk() {
        ClientResource testClient = ApiUtil.findClientByClientId(testRealm(), CLIENT_ID);
        ClientRepresentation testClientRep = testClient.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue("gold");
        testClient.update(testClientRep);

        // username, password input and finally push button for gold
        openLoginFormWithAcrClaim(true, "gold");

        authenticateWithUsernamePassword();
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");

        // Revert
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue(null);
        testClient.update(testClientRep);
    }

    @Test
    public void testEssentialAcrMinimumTooLow() {
        ClientResource testClient = ApiUtil.findClientByClientId(testRealm(), CLIENT_ID);
        ClientRepresentation testClientRep = testClient.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue("gold");
        testClient.update(testClientRep);

        // requesting a too low essential acr should fail
        openLoginFormWithAcrClaim(true, "silver");
        assertErrorPage("Invalid parameter: claims");

        // Revert
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue(null);
        testClient.update(testClientRep);
    }

    @Test
    public void testNonEssentialAcrMinimumUpgrade() {
        ClientResource testClient = ApiUtil.findClientByClientId(testRealm(), CLIENT_ID);
        ClientRepresentation testClientRep = testClient.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue("gold");
        testClient.update(testClientRep);

        // requesting a too low non-essential ACR should be upgraded
        openLoginFormWithAcrClaim(false, "silver");
        authenticateWithUsernamePassword();
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");

        // Revert
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setMinimumAcrValue(null);
        testClient.update(testClientRep);
    }

    @Test
    public void testClientOverrideLOAFlow() throws Exception {
        reconfigureStepUpFlow(300, 300, 300);

        String flowId = testRealm().flows().getFlows().stream().filter(flow -> FLOW_ALIAS.equals(flow.getAlias())).findFirst().get().getId();
        try (RealmAttributeUpdater realmUpdater = new RealmAttributeUpdater(testRealm()).setBrowserFlow(DefaultAuthenticationFlows.BROWSER_FLOW).update(); 
             ClientAttributeUpdater updater = ClientAttributeUpdater.forClient(adminClient, TEST_REALM_NAME, CLIENT_ID)
                .setAttribute(Constants.MINIMUM_ACR_VALUE, "gold")
                .setAuthenticationFlowBindingOverrides(Collections.singletonMap(AuthenticationFlowBindings.BROWSER_BINDING, flowId))
                .update()) {
            openLoginFormWithAcrClaim(true, "gold");
            authenticateWithUsernamePassword();
            authenticateWithTotp();
            assertLoggedInWithAcr("gold");
            appPage.assertCurrent();
            openLoginFormWithAcrClaim(true, "gold");
            appPage.assertCurrent();
        }
    }

    private String getCredentialIdByLabel(String credentialLabel) {
        return ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost").credentials()
                .stream()
                .filter(credential -> "totp2-label".equals(credential.getUserLabel()))
                .map(CredentialRepresentation::getId)
                .findFirst().orElseThrow(() -> new IllegalStateException("Did not found credential with label " + credentialLabel));
    }

    private String getCredentialIdByType(String credentialType) {
        return ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost").credentials()
                .stream()
                .filter(credential -> credentialType.equals(credential.getType()))
                .map(CredentialRepresentation::getId)
                .findFirst().orElseThrow(() -> new IllegalStateException("Did not found credential with OTP type on the user"));
    }

    public void openLoginFormWithAcrClaim(boolean essential, String... acrValues) {
        openLoginFormWithAcrClaim(oauth, essential, acrValues);
    }

    public static void openLoginFormWithAcrClaim(OAuthClient oauth, boolean essential, String... acrValues) {
        oauth.loginForm().claims(claims(essential, acrValues)).open();
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
    
    private void authenticateWithUsernamePassword() {
        loginPage.assertCurrent();
        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
    }

    private void reauthenticateWithPassword() {
        loginPage.assertCurrent();
        Assert.assertEquals("test-user@localhost", loginPage.getAttemptedUsername());
        loginPage.login(getPassword("test-user@localhost"));
    }

    private void authenticateWithTotp() {
        loginTotpPage.assertCurrent();
        loginTotpPage.login(totp.generateTOTP("totpSecret"));
    }

    private void authenticateWithButton() {
        pushTheButtonPage.assertCurrent();
        pushTheButtonPage.submit();
    }

    private TokenCtx assertLoggedInWithAcr(String acr) {
        EventRepresentation loginEvent = events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        IDToken idToken = oauth.verifyIDToken(tokenResponse.getIdToken());
        Assert.assertEquals(acr, idToken.getAcr());
        return new TokenCtx(tokenResponse.getAccessToken(), idToken);
    }

    private void assertErrorPage(String expectedError) {
        assertThat(true, is(errorPage.isCurrent()));
        Assert.assertEquals(expectedError, errorPage.getError());
        events.clear();
    }

    private class TokenCtx {
        private String accessToken;
        private IDToken idToken;

        private TokenCtx(String accessToken, IDToken idToken) {
            this.accessToken = accessToken;
            this.idToken = idToken;
        }
    }
}
