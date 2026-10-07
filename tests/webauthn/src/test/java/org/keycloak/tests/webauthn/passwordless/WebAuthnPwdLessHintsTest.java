/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.webauthn.passwordless;

import java.util.List;

import org.keycloak.models.Constants;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.tests.webauthn.WebAuthnHintsTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;

@KeycloakIntegrationTest
public class WebAuthnPwdLessHintsTest extends WebAuthnHintsTest {

    @InjectPage
    protected LoginUsernamePage usernamePage;

    @Override
    public boolean isPasswordless() {
        return true;
    }

    /**
     * The passkey button on the username form builds its own options, in passkeys.ftl, so it needs its own check.
     * Mediation is "none" so the page does not call get() on load, before the capture script is in place.
     */
    @Test
    public void passkeysButtonHints() {
        managedRealm.updateWithCleanup(r -> r.browserFlow("passkeys-username")
                .webAuthn(true, builder -> builder
                        .rpEntityName("localhost")
                        .residentKey(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED)
                        .userVerificationRequirement(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED)
                        .passkeysEnabled(Boolean.TRUE)
                        .mediation("none")
                        .hints(List.of("hybrid", "client-device"))
                ));

        oAuthClient.openLoginForm();
        usernamePage.assertCurrent();
        ((JavascriptExecutor) driver.driver()).executeScript(captureOptions("get"));
        // passkeys.ftl renders the same button id, so the same page object clicks it.
        webAuthnLoginPage.clickAuthenticate();

        Assertions.assertEquals("hybrid,client-device", getCapturedHints());
    }

    @Override
    protected void switchExecutionInBrowserFormToPasswordless() {
        // These tests register a passwordless credential only, so the login flow has to go straight from the
        // password form to passwordless WebAuthn.
        managedRealm.updateWithCleanup(r -> r.browserFlow("passwordless-only"));
    }
}
