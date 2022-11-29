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

package org.keycloak.testsuite.webauthn.registration;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.utils.PropertyRequirement;
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.virtualauthenticator.Credential;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.WebAuthnConstants.OPTION_REQUIRED;
import static org.keycloak.models.Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@IgnoreBrowserDriver(FirefoxDriver.class)
public class ResidentKeyRegisterTest extends AbstractWebAuthnVirtualTest {

    @Test
    public void residentKeyNotRequiredNoRK() {
        assertResidentKey(true, PropertyRequirement.NO, false);
    }

    @Test
    public void residentKeyNotRequiredPresent() {
        assertResidentKey(true, PropertyRequirement.NO, true);
    }

    @Test
    public void residentKeyRequiredCorrect() {
        assertResidentKey(true, PropertyRequirement.YES, true);
    }

    @Test
    public void residentKeyRequiredWrong() {
        assertResidentKey(false, PropertyRequirement.YES, false);
    }

    private void assertResidentKey(boolean shouldSuccess, PropertyRequirement requirement, boolean hasResidentKey) {
        final String userVerification;

        if (hasResidentKey) {
            getVirtualAuthManager().useAuthenticator(DEFAULT_RESIDENT_KEY.getOptions());
            userVerification = OPTION_REQUIRED;
        } else {
            userVerification = DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        }

        try (Closeable u = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRpEntityName("localhost")
                .setWebAuthnPolicyRequireResidentKey(requirement.getValue())
                .setWebAuthnPolicyUserVerificationRequirement(userVerification)
                .update()) {

            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(realmData.getRpEntityName(), is("localhost"));
            assertThat(realmData.getRequireResidentKey(), is(requirement.getValue()));
            assertThat(realmData.getUserVerificationRequirement(), is(userVerification));

            registerDefaultUser(shouldSuccess);

            displayErrorMessageIfPresent();

            if (!shouldSuccess) {
                assertThat(webAuthnErrorPage.isCurrent(), is(true));
                return;
            } else {
                assertThat(webAuthnErrorPage.isCurrent(), is(false));
            }

            final List<Credential> credentials = getVirtualAuthManager().getCurrent().getAuthenticator().getCredentials();
            assertThat(credentials, notNullValue());
            assertThat(credentials, not(Matchers.empty()));

            if (PropertyRequirement.YES.equals(requirement)) {
                final String userId = ApiUtil.findUserByUsername(testRealm(), USERNAME).getId();
                final Credential credential = credentials.get(0);
                assertThat(credential.isResidentCredential(), is(hasResidentKey));
                assertThat(new String(credential.getUserHandle()), is(userId));
            }

            logout();
            authenticateDefaultUser();

        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
