/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.webauthn.registration.passwordless;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.webauthn.registration.WebAuthnOtherSettingsTest;

/**
 * Migrated from {@code org.keycloak.testsuite.webauthn.registration.passwordless.PwdLessOtherSettingsTest}.
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@KeycloakIntegrationTest
public class PwdLessOtherSettingsTest extends WebAuthnOtherSettingsTest {

    @Override
    public boolean isPasswordless() {
        return true;
    }

    @Override
    protected void switchExecutionInBrowserFormToPasswordless() {
        // These tests register a passwordless-only credential, so use a login flow that goes straight from the password form to passwordless webauthn
        managedRealm.updateWithCleanup(r -> r.browserFlow("passwordless-only"));
    }
}
