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
 *
 */

package org.keycloak.testsuite.forms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalUserConfiguredAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.auth.page.login.OneTimeCode;
import org.keycloak.testsuite.broker.SocialLoginTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.util.FederatedIdentityBuilder;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.hamcrest.CoreMatchers.is;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GITHUB;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GITLAB;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GOOGLE;

/**
 * Test for various scenarios with user re-authentication
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ReAuthenticationTest extends AbstractTestRealmKeycloakTest {

    @ArquillianResource
    protected OAuthClient oauth;

    @Drone
    protected WebDriver driver;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginUsernameOnlyPage loginUsernameOnlyPage;

    @Page
    protected PasswordPage passwordPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    protected OneTimeCode oneTimeCodePage;

    @Page
    protected AppPage appPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    private RealmRepresentation loadTestRealm() {
        RealmRepresentation res = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        res.setBrowserFlow("browser");
        res.setRememberMe(true);

        // Add some sample dummy GitHub, Gitlab & Google social providers to the testing realm. Those are dummy providers for test if they are visible (clickable)
        // on the login pages
        List<IdentityProviderRepresentation> idps = new ArrayList<>();
        for (SocialLoginTest.Provider provider : Arrays.asList(GITHUB, GOOGLE)) {
            SocialLoginTest socialLoginTest = new SocialLoginTest();
            idps.add(socialLoginTest.buildIdp(provider));
        }
        res.setIdentityProviders(idps);

        return res;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        log.debug("Adding test realm for import from testrealm.json");
        testRealms.add(loadTestRealm());
    }


    @Test
    public void usernamePasswordFormReauthentication() {
        // Add fake github link to user account
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        FederatedIdentityRepresentation fedLink = FederatedIdentityBuilder.create()
                .identityProvider("github")
                .userId("123")
                .userName("test")
                .build();
        user.addFederatedIdentity("github", fedLink);

        // Login user
        loginPage.open();
        loginPage.assertCurrent();
        assertUsernameFieldAndOtherFields(true);
        assertSocialButtonsPresent(true, true);
        loginPage.login("test-user@localhost", "password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Set time offset
        setTimeOffset(10);

        // Request re-authentication
        oauth.maxAge("1");
        loginPage.open();
        loginPage.assertCurrent();

        // Username input hidden as well as register and rememberMe. Info message should be shown
        assertUsernameFieldAndOtherFields(false);
        assertInfoMessageAboutReAuthenticate(true);

        // Assert github link present as it is linked to user account. Google link should be hidden
        assertSocialButtonsPresent(true, false);

        // Try bad password and assert things still hidden
        loginPage.login("bad-password");
        loginPage.assertCurrent();
        Assert.assertEquals("Invalid password.", loginPage.getInputError());
        assertUsernameFieldAndOtherFields(false);
        assertInfoMessageAboutReAuthenticate(false);

        loginPage.login("password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Remove link
        user.removeFederatedIdentity("github");
    }

    // Case when user press the link "Restart login" during re-authentication
    @Test
    public void usernamePasswordFormReauthenticationWithResetFlow() {
        // Login user
        loginPage.open();
        loginPage.assertCurrent();
        assertUsernameFieldAndOtherFields(true);
        assertSocialButtonsPresent(true, true);
        loginPage.login("test-user@localhost", "password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Set time offset
        setTimeOffset(10);

        // Request re-authentication
        oauth.maxAge("1");
        loginPage.open();
        loginPage.assertCurrent();

        // Username input hidden as well as register and rememberMe. Info message should be shown
        assertUsernameFieldAndOtherFields(false);
        assertInfoMessageAboutReAuthenticate(true);

        // Assert none of github link and google link present. As none of the providers is linked to user account
        assertSocialButtonsPresent(false, false);

        // Try click "Reset password" . This will start login page from the beginning due SSO logout
        Assert.assertEquals("test-user@localhost", loginPage.getAttemptedUsername());
        loginPage.clickResetLogin();

        // Username field should be back. Attempted username should not be shown
        loginPage.assertCurrent();
        assertUsernameFieldAndOtherFields(true);
        assertInfoMessageAboutReAuthenticate(false);

        // Both social buttons should be present
        assertSocialButtonsPresent(true, true);

        // Successfully login as different user. It should be possible due previous SSO session was removed
        loginPage.login("john-doh@localhost", "password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    // Re-authentication with user form separate to the password form. The username form would be skipped
    @Test
    @AuthServerContainerExclude(REMOTE)
    public void identityFirstFormReauthentication() {
        // Set identity-first as realm flow
        setupIdentityFirstFlow();

        // Login user
        loginPage.open();
        loginUsernameOnlyPage.assertCurrent();
        assertUsernameFieldAndOtherFields(true);
        assertSocialButtonsPresent(true, true);
        loginUsernameOnlyPage.login("test-user@localhost");
        passwordPage.assertCurrent();
        passwordPage.login("password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Set time offset
        setTimeOffset(10);

        // Request re-authentication
        oauth.maxAge("1");
        loginPage.open();

        // User directly on the password page. Info message should be shown here
        passwordPage.assertCurrent();
        Assert.assertEquals("test-user@localhost", passwordPage.getAttemptedUsername());
        assertInfoMessageAboutReAuthenticate(true);

        passwordPage.login("bad-password");
        Assert.assertEquals("Invalid password.", passwordPage.getPasswordError());
        passwordPage.login("password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Revert flows
        BrowserFlowTest.revertFlows(testRealm(), "browser - identity first");
    }

    // Re-authentication with user form separate to the password form. The username form is shown due the user linked with "github"
    @Test
    @AuthServerContainerExclude(REMOTE)
    public void identityFirstFormReauthenticationWithGithubLink() {
        // Set identity-first as realm flow
        setupIdentityFirstFlow();

        // Add fake federated link to the user
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        FederatedIdentityRepresentation fedLink = FederatedIdentityBuilder.create()
                .identityProvider("github")
                .userId("123")
                .userName("test")
                .build();
        user.addFederatedIdentity("github", fedLink);

        // Login user
        loginPage.open();
        loginUsernameOnlyPage.assertCurrent();
        loginUsernameOnlyPage.login("test-user@localhost");
        passwordPage.assertCurrent();
        passwordPage.login("password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // See that user can re-authenticate with the github link present on the page as user has link to github social provider
        setTimeOffset(10);
        oauth.maxAge("1");

        loginPage.open();

        // Username input hidden as well as register and rememberMe. Info message should be present
        loginPage.assertCurrent();
        assertUsernameFieldAndOtherFields(false);
        assertInfoMessageAboutReAuthenticate(true);

        // Check there is NO password field
        Assert.assertThat(true, is(driver.findElements(By.id("password")).isEmpty()));

        // Github present, Google hidden
        assertSocialButtonsPresent(true, false);

        // Confirm login with password
        loginUsernameOnlyPage.clickSubmitButton();

        // Login with password. Info message should not be there anymore
        passwordPage.assertCurrent();
        passwordPage.login("password");
        assertInfoMessageAboutReAuthenticate(false);
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Remove link and flow
        user.removeFederatedIdentity("github");
        BrowserFlowTest.revertFlows(testRealm(), "browser - identity first");
    }

    private void setupIdentityFirstFlow() {
        String newFlowAlias = "browser - identity first";
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID)
                ).defineAsBrowserFlow() // Activate this new flow
        );
    }


    private void assertUsernameFieldAndOtherFields(boolean expectPresent) {
        Assert.assertThat(expectPresent, is(loginPage.isUsernameInputPresent()));
        Assert.assertThat(expectPresent, is(loginPage.isRegisterLinkPresent()));
        Assert.assertThat(expectPresent, is(loginPage.isRememberMeCheckboxPresent()));
    }

    private void assertSocialButtonsPresent(boolean expectGithubPresent, boolean expectGooglePresent) {
        Assert.assertThat(expectGithubPresent, is(loginPage.isSocialButtonPresent("github")));
        Assert.assertThat(expectGooglePresent, is(loginPage.isSocialButtonPresent("google")));
    }

    private void assertInfoMessageAboutReAuthenticate(boolean expectPresent) {
        Matcher<String> expectedInfo = expectPresent ? is("Please re-authenticate to continue") : Matchers.nullValue(String.class);
        Assert.assertThat(loginPage.getInfoMessage(), expectedInfo);
    }

}
