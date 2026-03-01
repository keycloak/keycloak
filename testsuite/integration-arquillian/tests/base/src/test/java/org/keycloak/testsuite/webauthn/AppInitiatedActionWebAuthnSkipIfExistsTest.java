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
package org.keycloak.testsuite.webauthn;

import java.io.IOException;

import org.keycloak.models.Constants;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;

import org.junit.Test;
import org.openqa.selenium.firefox.FirefoxDriver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

/**
 *
 * @author rmartinc
 */
public class AppInitiatedActionWebAuthnSkipIfExistsTest extends AppInitiatedActionWebAuthnTest {

    @Override
    public String getAiaAction() {
        return WEB_AUTHN_REGISTER_PROVIDER + ":" + Constants.KC_ACTION_PARAMETER_SKIP_IF_EXISTS;
    }

    public String getCredentialType() {
        return isPasswordless() ? WebAuthnCredentialModel.TYPE_PASSWORDLESS : WebAuthnCredentialModel.TYPE_TWOFACTOR;
    }

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
    public void processSetupTwice() throws IOException {
        testWebAuthnLogoutOtherSessions(false);
        final long credentialsCount = ApiUtil.findUserByUsernameId(testRealm(), DEFAULT_USERNAME)
                .credentials()
                .stream()
                .filter(c -> c.getType().equals(getCredentialType()))
                .count();
        assertThat(credentialsCount, greaterThan(0L));

        // do a second AIA that should be skiped
        doAIA();
        assertKcActionStatus(SUCCESS);

        assertThat(ApiUtil.findUserByUsernameId(testRealm(), DEFAULT_USERNAME)
                .credentials()
                .stream()
                .filter(c -> c.getType().equals(getCredentialType()))
                .count(), is(credentialsCount));
    }
}
