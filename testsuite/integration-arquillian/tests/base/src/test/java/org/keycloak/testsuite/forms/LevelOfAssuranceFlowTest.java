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
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.authentication.PushButtonAuthenticatorFactory;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.pages.PushTheButtonPage;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.OAuthClient;
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
    protected LoginUsernameOnlyPage loginUsernameOnlyPage;

    @Page
    protected PasswordPage passwordPage;

    @Page
    protected PushTheButtonPage pushTheButtonPage;

    @Page
    protected ErrorPage errorPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        try {
            findTestApp(testRealm).setAttributes(Collections.singletonMap(Constants.ACR_LOA_MAP, getAcrToLoaMappingForClient()));
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
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID);
                        })

                        // level 2 authentication
                        .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "2"));

                            // password required for level 2
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID);
                        })

                        // level 3 authentication
                        .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "3"));

                            // simply push button for level 3
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, PushButtonAuthenticatorFactory.PROVIDER_ID);
                        })

                ).defineAsBrowserFlow());
    }

    @After
    public void after() {
        BrowserFlowTest.revertFlows(testRealm(), "browser -  Level of Authentication FLow");
    }

    @Test
    public void loginWithoutAcr() {
        oauth.openLoginForm();
        // Authentication without specific LOA results in level 1 authentication
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");
    }

    @Test
    public void loginWithAcr1() {
        // username input for level 1
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");

    }

    @Test
    public void loginWithAcr2() {
        // username and password input for level 2
        openLoginFormWithAcrClaim(true, "gold");
        authenticateWithUsername();
        authenticateWithPassword();
        assertLoggedInWithAcr("gold");
    }

    @Test
    public void loginWithAcr3() {
        // username, password input and finally push button for level 3
        openLoginFormWithAcrClaim(true, "3");
        authenticateWithUsername();
        authenticateWithPassword();
        authenticateWithButton();
        // ACR 3 is returned because it was requested, although there is no mapping for it
        assertLoggedInWithAcr("3");
    }

    @Test
    public void stepupAuthentication() {
        // logging in to level 1
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");
        // doing step-up authentication to level 2
        openLoginFormWithAcrClaim(true, "gold");
        authenticateWithPassword();
        assertLoggedInWithAcr("gold");
        // step-up to level 3 needs password authentication because level 2 is not stored in user session
        openLoginFormWithAcrClaim(true, "3");
        authenticateWithPassword();
        authenticateWithButton();
        assertLoggedInWithAcr("3");
    }

    @Test
    public void stepupToUnknownEssentialAcrFails() {
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");
        // step-up to unknown acr
        openLoginFormWithAcrClaim(true, "uranium");
        assertErrorPage("Invalid parameter: claims");
    }

    @Test
    public void reauthenticationWithNoAcr() {
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");
        oauth.claims(null);
        oauth.openLoginForm();
        assertLoggedInWithAcr("0");
    }

    @Test
    public void reauthenticationWithReachedAcr() {
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");
        openLoginFormWithAcrClaim(true, "silver");
        assertLoggedInWithAcr("0");
    }

    @Test
    public void reauthenticationWithOptionalUnknownAcr() {
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");
        openLoginFormWithAcrClaim(false, "iron");
        assertLoggedInWithAcr("0");
    }

    @Test
    public void essentialClaimNotReachedFails() {
        openLoginFormWithAcrClaim(true, "4");
        authenticateWithUsername();
        authenticateWithPassword();
        authenticateWithButton();
        assertErrorPage("Authentication requirements not fulfilled");
    }

    @Test
    public void optionalClaimNotReachedSucceeds() {
        openLoginFormWithAcrClaim(false, "4");
        authenticateWithUsername();
        authenticateWithPassword();
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
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");
    }

    @Test
    public void acrValuesQueryParameter() {
        driver.navigate().to(UriBuilder.fromUri(oauth.getLoginFormUrl())
            .queryParam("acr_values", "gold 3")
            .build().toString());
        authenticateWithUsername();
        authenticateWithPassword();
        assertLoggedInWithAcr("gold");
    }

    @Test
    public void multipleEssentialAcrValues() {
        openLoginFormWithAcrClaim(true, "gold", "3");
        authenticateWithUsername();
        authenticateWithPassword();
        assertLoggedInWithAcr("gold");
    }

    @Test
    public void multipleOptionalAcrValues() {
        openLoginFormWithAcrClaim(false, "gold", "3");
        authenticateWithUsername();
        authenticateWithPassword();
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
        authenticateWithUsername();
        authenticateWithPassword();
        assertLoggedInWithAcr("realm:gold");

        // Add "acr-to-loa" back to the client. Client mapping will be used instead of realm mapping
        testClientRep.getAttributes().put(Constants.ACR_LOA_MAP, getAcrToLoaMappingForClient());
        testClient.update(testClientRep);

        openLoginFormWithAcrClaim(true, "realm:gold");
        assertErrorPage("Invalid parameter: claims");

        openLoginFormWithAcrClaim(true, "gold");
        authenticateWithPassword();
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
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");

        // Re-configure to level gold
        OIDCAdvancedConfigWrapper.fromClientRepresentation(testClientRep).setAttributeMultivalued(Constants.DEFAULT_ACR_VALUES, Arrays.asList("gold"));
        testClient.update(testClientRep);
        oauth.openLoginForm();
        authenticateWithPassword();
        assertLoggedInWithAcr("gold");

        // Value from essential ACR should have preference
        openLoginFormWithAcrClaim(true, "silver");
        assertLoggedInWithAcr("0");

        // Value from non-essential ACR should have preference
        openLoginFormWithAcrClaim(false, "silver");
        assertLoggedInWithAcr("0");

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

    private void authenticateWithUsername() {
        loginUsernameOnlyPage.assertCurrent();
        loginUsernameOnlyPage.login("test-user@localhost");
    }

    private void authenticateWithPassword() {
        passwordPage.assertCurrent();
        passwordPage.login("password");
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