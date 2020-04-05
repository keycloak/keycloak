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
import org.junit.Before;
import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.events.Details;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testsuite.WebAuthnAssume;
import org.keycloak.testsuite.actions.AbstractAppInitiatedActionTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.pages.webauthn.WebAuthnRegisterPage;
import org.keycloak.testsuite.util.FlowUtil;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.common.Profile.Feature.WEB_AUTHN;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.ALTERNATIVE;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@EnableFeature(value = WEB_AUTHN, skipRestart = true, onlyForProduct = true)
@AuthServerContainerExclude(REMOTE)
public class AppInitiatedActionWebAuthnTest extends AbstractAppInitiatedActionTest {

    @Page
    LoginUsernameOnlyPage usernamePage;

    @Page
    PasswordPage passwordPage;

    @Page
    WebAuthnRegisterPage registerPage;

    public AppInitiatedActionWebAuthnTest() {
        super(WebAuthnRegisterFactory.PROVIDER_ID);
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        RequiredActionProviderRepresentation action = new RequiredActionProviderRepresentation();
        action.setAlias(WebAuthnRegisterFactory.PROVIDER_ID);
        action.setProviderId(WebAuthnRegisterFactory.PROVIDER_ID);
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
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> {
            FlowUtil.inCurrentRealm(session)
                    .selectFlow(newFlowAlias)
                    .inForms(forms -> forms
                            .clear()
                            .addAuthenticatorExecution(REQUIRED, UsernameFormFactory.PROVIDER_ID)
                            .addSubFlowExecution(REQUIRED, subFlow -> subFlow
                                    .addAuthenticatorExecution(ALTERNATIVE, PasswordFormFactory.PROVIDER_ID)
                                    .addAuthenticatorExecution(ALTERNATIVE, WebAuthnAuthenticatorFactory.PROVIDER_ID)))
                    .defineAsBrowserFlow();
        });
    }

    @Before
    public void verifyEnvironment() {
        WebAuthnAssume.assumeChrome();
    }

    @Test
    public void cancelSetupWebAuthn() {
        loginUser();

        doAIA();

        registerPage.assertCurrent();
        registerPage.cancelAIA();

        assertKcActionStatus("cancelled");
    }

    private void loginUser() {
        usernamePage.open();
        usernamePage.assertCurrent();
        usernamePage.login("test-user@localhost");

        passwordPage.assertCurrent();
        passwordPage.login("password");

        events.expectLogin()
                .detail(Details.USERNAME, "test-user@localhost")
                .assertEvent();
    }
}
