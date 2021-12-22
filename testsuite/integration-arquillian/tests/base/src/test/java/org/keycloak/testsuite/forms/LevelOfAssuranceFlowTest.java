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
import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.Constants;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.authentication.PushButtonAuthenticatorFactory;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.pages.PushTheButtonPage;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.util.JsonSerialization;

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
            Map<String, Integer> acrLoaMap = new HashMap<>();
            acrLoaMap.put("copper", 0);
            acrLoaMap.put("silver", 1);
            acrLoaMap.put("gold", 2);
            findTestApp(testRealm).setAttributes(Collections.singletonMap(Constants.ACR_LOA_MAP, JsonSerialization.writeValueAsString(acrLoaMap)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setupFlow() {
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
        authenticateWithButton();
        assertLoggedInWithAcr("gold");
        // step-up to level 3 needs password authentication because level 2 is not stored in user session
        openLoginFormWithAcrClaim(true, "3");
        authenticateWithPassword();
        authenticateWithButton();
        assertLoggedInWithAcr("3");
    }

    @Test
    public void reauthenticationWithNoAcr() {
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");
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

    private void openLoginFormWithAcrClaim(boolean essential, String... acrValues) {
        ClaimsRepresentation.ClaimValue<String> acrClaim = new ClaimsRepresentation.ClaimValue<>();
        acrClaim.setEssential(essential);
        acrClaim.setValues(Arrays.asList(acrValues));

        ClaimsRepresentation claims = new ClaimsRepresentation();
        claims.setIdTokenClaims(Collections.singletonMap("acr", acrClaim));

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
}