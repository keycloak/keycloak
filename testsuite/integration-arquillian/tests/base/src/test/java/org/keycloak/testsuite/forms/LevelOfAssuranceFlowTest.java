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
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.authentication.PushButtonAuthenticatorFactory;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.pages.PushTheButtonPage;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RealmRepUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

import static org.hamcrest.CoreMatchers.is;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

/**
 * Tests for Level Of Assurance conditions in authentication flow.
 *
 * @author <a href="mailto:sebastian.zoescher@prime-sign.com">Sebastian Zoescher</a>
 */
@AuthServerContainerExclude(REMOTE)
public class LevelOfAssuranceFlowTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginTotpPage loginTotpPage;

    private TimeBasedOTP totp = new TimeBasedOTP();

    @Page
    protected PushTheButtonPage pushTheButtonPage;

    @Page
    protected ErrorPage errorPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        try {
            findTestApp(testRealm).setAttributes(Collections.singletonMap(Constants.ACR_LOA_MAP, getAcrToLoaMappingForClient()));
            UserRepresentation user = RealmRepUtil.findUser(testRealm, "test-user@localhost");
            UserBuilder.edit(user)
                    .totpSecret("totpSecret")
                    .otpEnabled();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    }

    public static void configureStepUpFlow(KeycloakTestingClient testingClient) {
        configureStepUpFlow(testingClient, ConditionalLoaAuthenticator.DEFAULT_MAX_AGE, 0, 0);
    }

    private static void configureStepUpFlow(KeycloakTestingClient testingClient, int maxAge1, int maxAge2, int maxAge3) {
        final String newFlowAlias = "browser -  Level of Authentication FLow";
        testingClient.server(TEST_REALM_NAME).run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server(TEST_REALM_NAME)
                .run(session -> FlowUtil.inCurrentRealm(session).selectFlow(newFlowAlias).inForms(forms -> forms.clear()
                        // level 1 authentication
                        .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "1");
                                        config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(maxAge1));
                                    });

                            // username input for level 1
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID);
                        })

                        // level 2 authentication
                        .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "2");
                                        config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(maxAge2));
                                    });

                            // password required for level 2
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, OTPFormAuthenticatorFactory.PROVIDER_ID);
                        })

                        // level 3 authentication
                        .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
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
        BrowserFlowTest.revertFlows(testRealm(), "browser -  Level of Authentication FLow");
        configureStepUpFlow(testingClient, maxAge1, maxAge2, maxAge3);
    }

    @After
    public void after() {
        BrowserFlowTest.revertFlows(testRealm(), "browser -  Level of Authentication FLow");
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
        oauth.claims(null);
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
        driver.navigate().to(UriBuilder.fromUri(oauth.getLoginFormUrl())
            .queryParam("acr_values", "gold 3")
            .build().toString());
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
        ClientResource testClient = ApiUtil.findClientByClientId(testRealm(), "test-app");
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
        ClientResource testClient = ApiUtil.findClientByClientId(testRealm(), "test-app");
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
        ClientResource testClient = ApiUtil.findClientByClientId(testRealm(), "test-app");
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
        oauth.claims(null);
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
        oauth.claims(null);
        oauth.prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN);
        oauth.openLoginForm();
        reauthenticateWithPassword();
        assertLoggedInWithAcr("silver");

        // Request with prompt=login together with "acr=2" . User should be asked to re-authenticate with level 2
        openLoginFormWithAcrClaim(true, "gold");
        reauthenticateWithPassword();
        authenticateWithTotp();
        assertLoggedInWithAcr("gold");

        // Request with "acr=3", but without prompt. User should be automatically authenticated
        oauth.prompt(null);
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
        BrowserFlowTest.revertFlows(testRealm(), "browser -  Level of Authentication FLow");
        final String newFlowAlias = "browser -  Level of Authentication FLow";
        testingClient.server(TEST_REALM_NAME).run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server(TEST_REALM_NAME)
                .run(session -> FlowUtil.inCurrentRealm(session).selectFlow(newFlowAlias).inForms(forms -> forms.clear()
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
        BrowserFlowTest.revertFlows(testRealm(), "browser -  Level of Authentication FLow");

        // Login normal way - should return 1 (backwards compatibility before step-up was introduced)
        loginPage.open();
        authenticateWithUsernamePassword();
        authenticateWithTotp(); // OTP required due the user has it
        assertLoggedInWithAcr("1");

        // SSO login - should return 0 (backwards compatibility before step-up was introduced)
        oauth.openLoginForm();
        assertLoggedInWithAcr("0");

        // Flow is needed due the "after()" method
        testingClient.server(TEST_REALM_NAME).run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow("browser -  Level of Authentication FLow"));
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
            Assert.assertThat(false, is(acrScopeExists));
        } finally {
            newRealm.remove();
        }
    }


    public void openLoginFormWithAcrClaim(boolean essential, String... acrValues) {
        openLoginFormWithAcrClaim(oauth, essential, acrValues);
    }

    public static void openLoginFormWithAcrClaim(OAuthClient oauth, boolean essential, String... acrValues) {
        ClaimsRepresentation.ClaimValue<String> acrClaim = new ClaimsRepresentation.ClaimValue<>();
        acrClaim.setEssential(essential);
        acrClaim.setValues(Arrays.asList(acrValues));

        ClaimsRepresentation claims = new ClaimsRepresentation();
        claims.setIdTokenClaims(Collections.singletonMap(IDToken.ACR, acrClaim));

        oauth.claims(claims);
        oauth.openLoginForm();
    }

    private void authenticateWithUsernamePassword() {
        loginPage.assertCurrent();
        loginPage.login("test-user@localhost", "password");
    }

    private void reauthenticateWithPassword() {
        loginPage.assertCurrent();
        Assert.assertEquals("test-user@localhost", loginPage.getAttemptedUsername());
        loginPage.login("password");
    }

    private void authenticateWithTotp() {
        loginTotpPage.assertCurrent();
        loginTotpPage.login(totp.generateTOTP("totpSecret"));
    }

    private void authenticateWithButton() {
        pushTheButtonPage.assertCurrent();
        pushTheButtonPage.submit();
    }

    private void assertLoggedInWithAcr(String acr) {
        EventRepresentation loginEvent = events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        IDToken idToken = sendTokenRequestAndGetIDToken(loginEvent);
        Assert.assertEquals(acr, idToken.getAcr());
    }

    private void assertErrorPage(String expectedError) {
        Assert.assertThat(true, is(errorPage.isCurrent()));
        Assert.assertEquals(expectedError, errorPage.getError());
        events.clear();
    }
}