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

import com.webauthn4j.data.AttestationConveyancePreference;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.updaters.AbstractWebAuthnRealmUpdater;
import org.keycloak.testsuite.webauthn.utils.WebAuthnDataWrapper;
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.virtualauthenticator.Credential;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.models.Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class AttestationConveyanceRegisterTest extends AbstractWebAuthnVirtualTest {

    @Test
    public void attestationDefaultValue() {
        WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
        assertThat(realmData.getAttestationConveyancePreference(), is(DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED));

        registerDefaultUser();
        displayErrorMessageIfPresent();

        final String credentialType = getCredentialType();

        getTestingClient().server(TEST_REALM_NAME).run(session -> {
            final WebAuthnDataWrapper dataWrapper = new WebAuthnDataWrapper(session, USERNAME, credentialType);
            assertThat(dataWrapper, notNullValue());

            final WebAuthnCredentialData data = dataWrapper.getWebAuthnData();
            assertThat(data, notNullValue());
            assertThat(data.getAttestationStatementFormat(), is(AttestationConveyancePreference.NONE.getValue()));
        });
    }

    @Ignore("invalid cert path")
    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class)
    public void attestationConveyancePreferenceNone() {
        assertAttestationConveyance(true, AttestationConveyancePreference.NONE);
    }

    @Ignore("invalid cert path")
    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class)
    public void attestationConveyancePreferenceIndirect() {
        assertAttestationConveyance(true, AttestationConveyancePreference.INDIRECT);
    }

    @Ignore("invalid cert path")
    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class)
    public void attestationConveyancePreferenceDirect() {
        getVirtualAuthManager().useAuthenticator(DEFAULT.getOptions().setHasResidentKey(true).setIsUserConsenting(true).setHasUserVerification(true));
        assertAttestationConveyance(true, AttestationConveyancePreference.DIRECT);
    }

    protected void assertAttestationConveyance(boolean shouldSuccess, AttestationConveyancePreference attestation) {
        Credential credential = getDefaultResidentKeyCredential();

        getVirtualAuthManager().useAuthenticator(getDefaultAuthenticatorOptions().setHasResidentKey(true));
        getVirtualAuthManager().getCurrent().getAuthenticator().addCredential(credential);

        try (AbstractWebAuthnRealmUpdater updater = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyAttestationConveyancePreference(attestation.getValue())
                .update()) {

            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(realmData.getAttestationConveyancePreference(), is(attestation.getValue()));

            registerDefaultUser(shouldSuccess);
            displayErrorMessageIfPresent();

            final boolean isErrorCurrent = webAuthnErrorPage.isCurrent();
            assertThat(isErrorCurrent, is(!shouldSuccess));

            final String credentialType = getCredentialType();

            getTestingClient().server(TEST_REALM_NAME).run(session -> {
                final WebAuthnDataWrapper dataWrapper = new WebAuthnDataWrapper(session, USERNAME, credentialType);
                assertThat(dataWrapper, notNullValue());

                final WebAuthnCredentialData data = dataWrapper.getWebAuthnData();
                assertThat(data, notNullValue());
                assertThat(data.getAttestationStatementFormat(), is(attestation.getValue()));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
