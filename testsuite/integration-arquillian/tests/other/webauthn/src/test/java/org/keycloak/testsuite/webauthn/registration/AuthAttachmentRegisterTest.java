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

import com.webauthn4j.data.AuthenticatorAttachment;
import com.webauthn4j.data.UserVerificationRequirement;
import org.junit.Test;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.Closeable;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_BLE;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_INTERNAL;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_USB;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@IgnoreBrowserDriver(FirefoxDriver.class)
public class AuthAttachmentRegisterTest extends AbstractWebAuthnVirtualTest {

    @Test
    public void authenticatorAttachmentCrossPlatform() {
        getVirtualAuthManager().useAuthenticator(DEFAULT_USB.getOptions());
        assertAuthenticatorAttachment(true, AuthenticatorAttachment.CROSS_PLATFORM);
    }

    @Test
    public void authenticatorAttachmentCrossPlatformInternal() {
        getVirtualAuthManager().useAuthenticator(DEFAULT_INTERNAL.getOptions());
        assertAuthenticatorAttachment(true, AuthenticatorAttachment.CROSS_PLATFORM);
    }

    @Test
    public void authenticatorAttachmentPlatform() throws IOException {
        try (Closeable u = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyAuthenticatorAttachment(AuthenticatorAttachment.PLATFORM.getValue())
                .setWebAuthnPolicyUserVerificationRequirement(UserVerificationRequirement.DISCOURAGED.getValue())
                .update()) {

            // It shouldn't be possible to register the authenticator
            getVirtualAuthManager().useAuthenticator(DEFAULT_BLE.getOptions());

            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(realmData.getAuthenticatorAttachment(), is(AuthenticatorAttachment.PLATFORM.getValue()));
            assertThat(realmData.getUserVerificationRequirement(), is(UserVerificationRequirement.DISCOURAGED.getValue()));

            registerDefaultUser(false);

            webAuthnRegisterPage.assertCurrent();

            webAuthnRegisterPage.clickRegister();

            webAuthnErrorPage.assertCurrent();
            assertThat(webAuthnErrorPage.getError(), containsString("A request is already pending."));
        }
    }

    @Test
    public void authenticatorAttachmentPlatformInternal() {
        getVirtualAuthManager().useAuthenticator(DEFAULT_INTERNAL.getOptions());
        assertAuthenticatorAttachment(true, AuthenticatorAttachment.PLATFORM);
    }

    private void assertAuthenticatorAttachment(boolean shouldSuccess, AuthenticatorAttachment attachment) {
        try (Closeable u = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyAuthenticatorAttachment(attachment.getValue())
                .update()) {

            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(realmData.getAuthenticatorAttachment(), is(attachment.getValue()));

            registerDefaultUser(shouldSuccess);

            displayErrorMessageIfPresent();

            assertThat(webAuthnErrorPage.isCurrent(), is(!shouldSuccess));
        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
