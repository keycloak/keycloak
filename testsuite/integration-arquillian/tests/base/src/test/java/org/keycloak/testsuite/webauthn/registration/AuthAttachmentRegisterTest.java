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

import java.io.Closeable;
import java.io.IOException;

import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;

import com.webauthn4j.data.AuthenticatorAttachment;
import com.webauthn4j.data.UserVerificationRequirement;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.firefox.FirefoxDriver;

import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_BLE;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_INTERNAL;
import static org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_USB;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
public class AuthAttachmentRegisterTest extends AbstractWebAuthnVirtualTest {

    @Test
    public void authenticatorAttachmentCrossPlatform() {
        getVirtualAuthManager().useAuthenticator(DEFAULT_USB.getOptions());
        assertAuthenticatorAttachment(true, AuthenticatorAttachment.CROSS_PLATFORM);
    }

    @Test
    @Ignore
    public void authenticatorAttachmentCrossPlatformInternal() {
        getVirtualAuthManager().useAuthenticator(DEFAULT_INTERNAL.getOptions());
        assertAuthenticatorAttachment(true, AuthenticatorAttachment.CROSS_PLATFORM);
    }

    @Test
    public void authenticatorAttachmentPlatform() throws IOException {
        try (Closeable u = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyAuthenticatorAttachment(AuthenticatorAttachment.PLATFORM.getValue())
                .setWebAuthnPolicyUserVerificationRequirement(UserVerificationRequirement.DISCOURAGED.getValue())
                .setWebAuthnPolicyCreateTimeout(3)
                .update()) {

            // It shouldn't be possible to register the authenticator
            getVirtualAuthManager().useAuthenticator(DEFAULT_BLE.getOptions());

            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(realmData.getAuthenticatorAttachment(), is(AuthenticatorAttachment.PLATFORM.getValue()));
            assertThat(realmData.getUserVerificationRequirement(), is(UserVerificationRequirement.DISCOURAGED.getValue()));

            registerDefaultUser(false);

            // Instead of returning an error it seems that selenium webauthn just hangs
            // So we cannot test this correctly
            webAuthnRegisterPage.assertCurrent();

            // click authentication again does nothing
            webAuthnRegisterPage.clickRegister();
            webAuthnRegisterPage.clickRegister();
            webAuthnRegisterPage.assertCurrent();

            // it timeouts after create timeout
            WaitUtils.waitUntilPageIsCurrent(webAuthnErrorPage);
            assertThat(webAuthnErrorPage.getError(), containsString("The operation either timed out or was not allowed."));
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
