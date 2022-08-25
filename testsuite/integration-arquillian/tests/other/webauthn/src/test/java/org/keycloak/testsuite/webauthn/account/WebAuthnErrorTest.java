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
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.pages.WebAuthnAuthenticatorsList;
import org.keycloak.testsuite.webauthn.pages.WebAuthnErrorPage;
import org.keycloak.testsuite.webauthn.pages.WebAuthnLoginPage;
import org.keycloak.testsuite.webauthn.updaters.WebAuthnRealmAttributeUpdater;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class WebAuthnErrorTest extends AbstractWebAuthnAccountTest {

    @Page
    protected WebAuthnLoginPage webAuthnLoginPage;

    @Page
    protected WebAuthnErrorPage webAuthnErrorPage;

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class)
    public void errorPageWithTimeout() throws IOException {
        final int timeoutSec = 3;
        final String authenticatorLabel = "authenticator";
        addWebAuthnCredential(authenticatorLabel);

        try (RealmAttributeUpdater u = new WebAuthnRealmAttributeUpdater(testRealmResource())
                .setWebAuthnPolicyCreateTimeout(timeoutSec)
                .update()) {

            RealmRepresentation realm = testRealmResource().toRepresentation();
            assertThat(realm, notNullValue());
            assertThat(realm.getWebAuthnPolicyCreateTimeout(), is(timeoutSec));

            final int webAuthnCount = webAuthnCredentialType.getUserCredentialsCount();
            assertThat(webAuthnCount, is(1));

            getWebAuthnManager().getCurrent().getAuthenticator().removeAllCredentials();

            setUpWebAuthnFlow("webAuthnFlow");
            logout();

            signingInPage.navigateTo();
            loginToAccount();

            webAuthnLoginPage.assertCurrent();

            final WebAuthnAuthenticatorsList authenticators = webAuthnLoginPage.getAuthenticators();
            assertThat(authenticators.getCount(), is(1));
            assertThat(authenticators.getLabels(), Matchers.contains(authenticatorLabel));

            webAuthnLoginPage.clickAuthenticate();

            //Should fail after this time
            WaitUtils.pause((timeoutSec + 1) * 1000);

            webAuthnErrorPage.assertCurrent();
            assertThat(webAuthnErrorPage.getError(), is("Failed to authenticate by the Security key."));
        }
    }
}
