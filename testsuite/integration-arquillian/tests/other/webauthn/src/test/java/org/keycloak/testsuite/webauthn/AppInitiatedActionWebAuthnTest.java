/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.webauthn;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnPasswordlessAuthenticatorFactory;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.events.Details;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testsuite.actions.AbstractAppInitiatedActionTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.keycloak.testsuite.webauthn.authenticators.UseVirtualAuthenticators;
import org.keycloak.testsuite.webauthn.authenticators.VirtualAuthenticatorManager;
import org.keycloak.testsuite.webauthn.pages.WebAuthnRegisterPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.ALTERNATIVE;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;
import static org.keycloak.testsuite.util.BrowserDriverUtil.isDriverFirefox;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@AuthServerContainerExclude(REMOTE)
public class AppInitiatedActionWebAuthnTest extends AbstractAppInitiatedActionTest implements UseVirtualAuthenticators {

    private VirtualAuthenticatorManager virtualManager;

    protected final String WEB_AUTHN_REGISTER_PROVIDER = isPasswordless() ? WebAuthnPasswordlessRegisterFactory.PROVIDER_ID : WebAuthnRegisterFactory.PROVIDER_ID;
    protected final String DEFAULT_USERNAME = "test-user@localhost";
    protected final String DEFAULT_PASSWORD = "password";

    @Page
    LoginUsernameOnlyPage usernamePage;

    @Page
    PasswordPage passwordPage;

    @Page
    WebAuthnRegisterPage webAuthnRegisterPage;

    @Before
    @Override
    public void setUpVirtualAuthenticator() {
        if (!isDriverFirefox(driver)) {
            virtualManager = AbstractWebAuthnVirtualTest.createDefaultVirtualManager(driver, DefaultVirtualAuthOptions.DEFAULT.getOptions());
        }
    }

    @After
    @Override
    public void removeVirtualAuthenticator() {
        if (!isDriverFirefox(driver)) {
            virtualManager.removeAuthenticator();
        }
    }

    @Override
    public String getAiaAction() {
        return WEB_AUTHN_REGISTER_PROVIDER;
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    protected boolean isPasswordless() {
        return false;
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        RequiredActionProviderRepresentation action = new RequiredActionProviderRepresentation();
        action.setAlias(WEB_AUTHN_REGISTER_PROVIDER);
        action.setProviderId(WEB_AUTHN_REGISTER_PROVIDER);
        action.setEnabled(true);
        action.setDefaultAction(true);
        action.setPriority(10);

        List<RequiredActionProviderRepresentation> actions = new ArrayList<>();
        actions.add(action);
        testRealm.setRequiredActions(actions);
    }

    @Before
    public void setUpWebAuthnFlow() {
        final String newFlowAlias = "browserWebAuthnAIA";
        final String webAuthnAuthProvider = isPasswordless() ? WebAuthnPasswordlessAuthenticatorFactory.PROVIDER_ID : WebAuthnAuthenticatorFactory.PROVIDER_ID;

        testingClient.server(TEST_REALM_NAME).run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server(TEST_REALM_NAME).run(session -> {
            FlowUtil.inCurrentRealm(session)
                    .selectFlow(newFlowAlias)
                    .inForms(forms -> forms
                            .clear()
                            .addAuthenticatorExecution(REQUIRED, UsernameFormFactory.PROVIDER_ID)
                            .addSubFlowExecution(REQUIRED, subFlow -> subFlow
                                    .addAuthenticatorExecution(ALTERNATIVE, PasswordFormFactory.PROVIDER_ID)
                                    .addAuthenticatorExecution(ALTERNATIVE, webAuthnAuthProvider)))
                    .defineAsBrowserFlow();
        });
    }

    @Test
    public void cancelSetupWebAuthn() {
        loginUser();

        doAIA();

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.cancelAIA();

        waitForPageToLoad();

        assertKcActionStatus(CANCELLED);
    }

    @Test
    public void proceedSetupWebAuthn() {
        loginUser();

        doAIA();

        final Supplier<Integer> getCredentialCount = () -> Optional.ofNullable(ApiUtil.findUserByUsernameId(testRealm(), DEFAULT_USERNAME))
                .map(UserResource::credentials)
                .map(List::size)
                .orElse(0);

        final int credentialsCount = getCredentialCount.get();

        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential("authenticator1");

        assertKcActionStatus(SUCCESS);

        assertThat(getCredentialCount.get(), is(credentialsCount + 1));
    }

    private void loginUser() {
        usernamePage.open();
        usernamePage.assertCurrent();
        usernamePage.login(DEFAULT_USERNAME);

        passwordPage.assertCurrent();
        passwordPage.login(DEFAULT_PASSWORD);

        events.expectLogin()
                .detail(Details.USERNAME, DEFAULT_USERNAME)
                .assertEvent();
    }
}
