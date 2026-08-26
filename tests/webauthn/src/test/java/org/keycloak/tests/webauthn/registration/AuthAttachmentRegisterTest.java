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
package org.keycloak.tests.webauthn.registration;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.WebAuthnRealmData;
import org.keycloak.tests.webauthn.AbstractWebAuthnVirtualTest;

import com.webauthn4j.data.AuthenticatorAttachment;
import com.webauthn4j.data.UserVerificationRequirement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_BLE;
import static org.keycloak.tests.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_INTERNAL;
import static org.keycloak.tests.webauthn.authenticators.DefaultVirtualAuthOptions.DEFAULT_USB;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@KeycloakIntegrationTest
public class AuthAttachmentRegisterTest extends AbstractWebAuthnVirtualTest {

    @Test
    public void authenticatorAttachmentCrossPlatform() {
        getVirtualAuthManager().useAuthenticator(DEFAULT_USB.getOptions());
        assertAuthenticatorAttachment(true, AuthenticatorAttachment.CROSS_PLATFORM);
    }

    @Test
    @Disabled
    public void authenticatorAttachmentCrossPlatformInternal() {
        getVirtualAuthManager().useAuthenticator(DEFAULT_INTERNAL.getOptions());
        assertAuthenticatorAttachment(true, AuthenticatorAttachment.CROSS_PLATFORM);
    }

    @Test
    public void authenticatorAttachmentPlatform() {
        managedRealm.updateWithCleanup(r ->
                r.webAuthn(isPasswordless(), wAuthN -> wAuthN
                        .authenticatorAttachment(AuthenticatorAttachment.PLATFORM.getValue())
                        .userVerificationRequirement(UserVerificationRequirement.DISCOURAGED.getValue())
                        .timeout(3)
                )
        );

        // It shouldn't be possible to register the authenticator
        getVirtualAuthManager().useAuthenticator(DEFAULT_BLE.getOptions());

        WebAuthnRealmData realmData = new WebAuthnRealmData(managedRealm.admin().toRepresentation(), isPasswordless());
        assertEquals(AuthenticatorAttachment.PLATFORM.getValue(), realmData.getAuthenticatorAttachment());
        assertEquals(UserVerificationRequirement.DISCOURAGED.getValue(), realmData.getUserVerificationRequirement());

        registerDefaultUser(false);

        // Instead of returning an error it seems that selenium webauthn just hangs
        // So we cannot test this correctly
        webAuthnRegisterPage.assertCurrent();

        // click authentication again does nothing
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.assertCurrent();

        // it timeouts after create timeout
        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString("The Passkey operation was not allowed or timed out."));
    }

    @Test
    public void authenticatorAttachmentPlatformInternal() {
        getVirtualAuthManager().useAuthenticator(DEFAULT_INTERNAL.getOptions());
        assertAuthenticatorAttachment(true, AuthenticatorAttachment.PLATFORM);
    }

    private void assertAuthenticatorAttachment(boolean shouldSuccess, AuthenticatorAttachment attachment) {
        managedRealm.updateWithCleanup(r ->
                r.webAuthn(isPasswordless(), WAuthN -> WAuthN.authenticatorAttachment(attachment.getValue()))
        );

        WebAuthnRealmData realmData = new WebAuthnRealmData(managedRealm.admin().toRepresentation(), isPasswordless());
        assertEquals(attachment.getValue(), realmData.getAuthenticatorAttachment());

        registerDefaultUser(shouldSuccess);

        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
    }
}
