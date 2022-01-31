/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import org.junit.Test;
import org.keycloak.WebAuthnConstants;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.WebAuthnConstants.OPTION_REQUIRED;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY;
import static org.keycloak.testsuite.webauthn.utils.PropertyRequirement.YES;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class WebAuthnPropertyTest extends AbstractWebAuthnVirtualTest {

    @Test
    public void residentKey() throws IOException {
        getVirtualAuthManager().useAuthenticator(DEFAULT_RESIDENT_KEY.getOptions());

        try (Closeable c = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRpEntityName("localhost")
                .setWebAuthnPolicyRequireResidentKey(YES.getValue())
                .setWebAuthnPolicyUserVerificationRequirement(OPTION_REQUIRED)
                .update()) {

            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(realmData, notNullValue());
            assertThat(realmData.getRpEntityName(), is("localhost"));
            assertThat(realmData.getRequireResidentKey(), is(YES.getValue()));
            assertThat(realmData.getUserVerificationRequirement(), is(OPTION_REQUIRED));

            registerDefaultUser();

            UserRepresentation user = userResource().toRepresentation();
            assertThat(user, notNullValue());

            logout();

            events.clear();

            authenticateDefaultUser();

            // confirm that authentication is successfully completed
            events.expectLogin()
                    .user(user.getId())
                    .detail(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true")
                    .assertEvent();
        }
    }

    @Test
    public void timeout() throws IOException {
        final Integer TIMEOUT = 3; //seconds

        registerDefaultUser();
        logout();

        getVirtualAuthManager().removeAuthenticator();

        try (Closeable u = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyCreateTimeout(TIMEOUT)
                .update()) {

            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(realmData.getCreateTimeout(), is(TIMEOUT));

            authenticateDefaultUser(false);
            WaitUtils.pause((TIMEOUT + 2) * 1000);
            webAuthnErrorPage.assertCurrent();
            assertThat(webAuthnErrorPage.getError(), containsString("Failed to authenticate by the Security key."));
        }
    }

    @Test
    public void changeAuthenticatorProperties() throws IOException {
        getVirtualAuthManager().useAuthenticator(DEFAULT_RESIDENT_KEY.getOptions());

        try (Closeable c = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRpEntityName("localhost")
                .setWebAuthnPolicyRequireResidentKey(YES.getValue())
                .setWebAuthnPolicyUserVerificationRequirement(OPTION_REQUIRED)
                .update()) {

            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(realmData, notNullValue());
            assertThat(realmData.getRpEntityName(), is("localhost"));
            assertThat(realmData.getRequireResidentKey(), is(YES.getValue()));
            assertThat(realmData.getUserVerificationRequirement(), is(OPTION_REQUIRED));

            registerDefaultUser();

            logout();

            getVirtualAuthManager().useAuthenticator(DEFAULT.getOptions());

            WaitUtils.pause(500);

            authenticateDefaultUser(false);
            webAuthnErrorPage.assertCurrent();
        }
    }

    @Test
    public void requiredActionRegistration() {
        registerDefaultUser();

        logout();

        loginPage.open();
        loginPage.assertCurrent();
        loginPage.login(USERNAME, PASSWORD);
        webAuthnLoginPage.assertCurrent();

        final String credType = isPasswordless() ? WebAuthnCredentialModel.TYPE_PASSWORDLESS : WebAuthnCredentialModel.TYPE_TWOFACTOR;
        final String credentialId = userResource().credentials()
                .stream()
                .filter(Objects::nonNull)
                .filter(f -> credType.equals(f.getType()))
                .map(CredentialRepresentation::getId)
                .findFirst()
                .orElse(null);

        assertThat(credentialId, notNullValue());
        userResource().removeCredential(credentialId);

        driver.navigate().refresh();

        // required action
        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential("something");

        appPage.assertCurrent();
    }
}