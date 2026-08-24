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

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
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
// This test should be ignored on Firefox: See https://github.com/keycloak/keycloak/issues/10368
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
        managedRealm.updateWithCleanup(r -> {
                    if (isPasswordless()) {
                        r.webAuthnPolicyPasswordlessAuthenticatorAttachment(AuthenticatorAttachment.PLATFORM.getValue());
                        r.webAuthnPolicyPasswordlessUserVerificationRequirement(UserVerificationRequirement.DISCOURAGED.getValue());
                        r.webAuthnPolicyPasswordlessCreateTimeout(3);
                    } else {
                        r.webAuthnPolicyAuthenticatorAttachment(AuthenticatorAttachment.PLATFORM.getValue());
                        r.webAuthnPolicyUserVerificationRequirement(UserVerificationRequirement.DISCOURAGED.getValue());
                        r.webAuthnPolicyCreateTimeout(3);
                    }
                    return r;
                }
        );

        // It shouldn't be possible to register the authenticator
        getVirtualAuthManager().useAuthenticator(DEFAULT_BLE.getOptions());

        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        if (isPasswordless()) {
            assertEquals(AuthenticatorAttachment.PLATFORM.getValue(), rep.getWebAuthnPolicyPasswordlessAuthenticatorAttachment());
            assertEquals(UserVerificationRequirement.DISCOURAGED.getValue(), rep.getWebAuthnPolicyPasswordlessUserVerificationRequirement());
        } else {
            assertEquals(AuthenticatorAttachment.PLATFORM.getValue(), rep.getWebAuthnPolicyAuthenticatorAttachment());
            assertEquals(UserVerificationRequirement.DISCOURAGED.getValue(), rep.getWebAuthnPolicyUserVerificationRequirement());
        }

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
        managedRealm.updateWithCleanup(r -> isPasswordless()
                ? r.webAuthnPolicyPasswordlessAuthenticatorAttachment(attachment.getValue())
                : r.webAuthnPolicyAuthenticatorAttachment(attachment.getValue())
        );
        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        assertEquals(attachment.getValue(), isPasswordless()
                ? rep.getWebAuthnPolicyPasswordlessAuthenticatorAttachment()
                : rep.getWebAuthnPolicyAuthenticatorAttachment()
        );

        registerDefaultUser(shouldSuccess);

        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
    }
}
