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

import com.webauthn4j.data.UserVerificationRequirement;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@IgnoreBrowserDriver(FirefoxDriver.class)
public class UserVerificationRegisterTest extends AbstractWebAuthnVirtualTest {

    @Test
    public void discouragedAny() {
        assertUserVerification(true, UserVerificationRequirement.DISCOURAGED,
                auth -> auth.setHasUserVerification(true).setIsUserVerified(true));
    }

    @Test
    public void discouraged() {
        assertUserVerification(false, UserVerificationRequirement.DISCOURAGED,
                auth -> auth.setHasUserVerification(true).setIsUserVerified(false));
    }

    @Test
    public void discouragedNoVerification() {
        assertUserVerification(true, UserVerificationRequirement.DISCOURAGED,
                auth -> auth.setHasUserVerification(false));
    }

    @Test
    public void preferredNoVerification() {
        assertUserVerification(true, UserVerificationRequirement.PREFERRED,
                auth -> auth.setHasUserVerification(false));
    }

    @Test
    public void preferredVerificationWrong() {
        assertUserVerification(false, UserVerificationRequirement.PREFERRED,
                auth -> auth.setHasUserVerification(true).setIsUserVerified(false));
    }

    @Test
    public void preferredVerificationCorrect() {
        assertUserVerification(true, UserVerificationRequirement.PREFERRED,
                auth -> auth.setHasUserVerification(true).setIsUserVerified(true));
    }

    @Test
    public void requiredWrong() {
        assertUserVerification(false, UserVerificationRequirement.REQUIRED,
                auth -> auth.setHasUserVerification(true).setIsUserVerified(false));
    }

    @Test
    public void requiredWrongNoVerification() {
        assertUserVerification(false, UserVerificationRequirement.REQUIRED,
                auth -> auth.setHasUserVerification(false));
    }

    @Test
    public void required() {
        assertUserVerification(true, UserVerificationRequirement.REQUIRED,
                auth -> auth.setHasUserVerification(true).setIsUserVerified(true));
    }

    private void assertUserVerification(boolean shouldSuccess,
                                        UserVerificationRequirement requirement,
                                        Consumer<VirtualAuthenticatorOptions> authenticator) {
        VirtualAuthenticatorOptions options = getDefaultAuthenticatorOptions();
        authenticator.accept(options);
        getVirtualAuthManager().useAuthenticator(options);
        WaitUtils.pause(200);

        try (Closeable u = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyUserVerificationRequirement(requirement.getValue())
                .update()) {

            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(realmData.getUserVerificationRequirement(), containsString(requirement.getValue()));

            registerDefaultUser(shouldSuccess);

            displayErrorMessageIfPresent();

            assertThat(webAuthnErrorPage.isCurrent(), is(!shouldSuccess));
        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
