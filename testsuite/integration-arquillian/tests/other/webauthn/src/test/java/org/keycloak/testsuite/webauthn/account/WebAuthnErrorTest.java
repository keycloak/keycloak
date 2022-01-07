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

package org.keycloak.testsuite.webauthn.account;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.pages.WebAuthnErrorPage;
import org.keycloak.testsuite.webauthn.pages.WebAuthnLoginPage;
import org.keycloak.testsuite.webauthn.updaters.WebAuthnRealmAttributeUpdater;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class WebAuthnErrorTest extends AbstractWebAuthnAccountTest {

    @Page
    protected WebAuthnLoginPage webAuthnLoginPage;

    @Page
    protected WebAuthnErrorPage webAuthnErrorPage;

    @Test
    public void errorPageWithPossibleAuthenticators() throws IOException {
        final int timeoutSec = 3;

        addWebAuthnCredential("authenticator#1");
        addWebAuthnCredential("authenticator#2");

        try (RealmAttributeUpdater u = new WebAuthnRealmAttributeUpdater(testRealmResource())
                .setWebAuthnPolicyCreateTimeout(timeoutSec)
                .update()) {

            RealmRepresentation realm = testRealmResource().toRepresentation();
            assertThat(realm, notNullValue());
            assertThat(realm.getWebAuthnPolicyCreateTimeout(), is(timeoutSec));

            final int webAuthnCount = webAuthnCredentialType.getUserCredentialsCount();
            assertThat(webAuthnCount, is(2));

            getWebAuthnManager().getCurrent().getAuthenticator().removeAllCredentials();

            setUpWebAuthnFlow("webAuthnFlow");
            logout();

            signingInPage.navigateTo();
            loginToAccount();

            webAuthnLoginPage.assertCurrent();
            webAuthnLoginPage.clickAuthenticate();

            //Should fail after this time
            WaitUtils.pause((timeoutSec + 1) * 1000);

            webAuthnErrorPage.assertCurrent();
            assertThat(webAuthnErrorPage.getError(), containsString("Failed to authenticate by the Security key."));
            assertThat(webAuthnErrorPage.getAuthenticatorsCount(), is(2));
            assertThat(webAuthnErrorPage.getAuthenticators(), Matchers.contains("authenticator#1", "authenticator#2"));
        }
    }

    private void setUpWebAuthnFlow(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID)
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, WebAuthnAuthenticatorFactory.PROVIDER_ID)
                )
                .defineAsBrowserFlow() // Activate this new flow
        );
    }
}
