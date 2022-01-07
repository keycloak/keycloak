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

package org.keycloak.testsuite.webauthn;

import org.junit.After;
import org.junit.Before;
import org.keycloak.common.Profile;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.keycloak.testsuite.webauthn.authenticators.UseVirtualAuthenticators;
import org.keycloak.testsuite.webauthn.authenticators.VirtualAuthenticatorManager;
import org.keycloak.testsuite.webauthn.updaters.AbstractWebAuthnRealmUpdater;
import org.keycloak.testsuite.webauthn.updaters.PasswordLessRealmAttributeUpdater;
import org.keycloak.testsuite.webauthn.updaters.WebAuthnRealmAttributeUpdater;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

/**
 * Abstract class for WebAuthn tests which use Virtual Authenticators
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@EnableFeature(value = Profile.Feature.WEB_AUTHN, skipRestart = true, onlyForProduct = true)
public abstract class AbstractWebAuthnVirtualTest extends AbstractTestRealmKeycloakTest implements UseVirtualAuthenticators {

    protected static final String ALL_ZERO_AAGUID = "00000000-0000-0000-0000-000000000000";
    protected static final String ALL_ONE_AAGUID = "11111111-1111-1111-1111-111111111111";

    private VirtualAuthenticatorManager virtualAuthenticatorManager;

    @Before
    @Override
    public void setUpVirtualAuthenticator() {
        this.virtualAuthenticatorManager = createDefaultVirtualManager(driver, getDefaultAuthenticatorOptions());
        clearEventQueue();
    }

    @After
    @Override
    public void removeVirtualAuthenticator() {
        virtualAuthenticatorManager.removeAuthenticator();
        clearEventQueue();
    }

    public VirtualAuthenticatorOptions getDefaultAuthenticatorOptions() {
        return DefaultVirtualAuthOptions.DEFAULT.getOptions();
    }

    public VirtualAuthenticatorManager getVirtualAuthManager() {
        return virtualAuthenticatorManager;
    }

    public void setVirtualAuthManager(VirtualAuthenticatorManager manager) {
        this.virtualAuthenticatorManager = manager;
    }

    public AbstractWebAuthnRealmUpdater getWebAuthnRealmUpdater() {
        return isPasswordless() ? new PasswordLessRealmAttributeUpdater(testRealm()) : new WebAuthnRealmAttributeUpdater(testRealm());
    }

    public String getCredentialType() {
        return isPasswordless() ? WebAuthnCredentialModel.TYPE_PASSWORDLESS : WebAuthnCredentialModel.TYPE_TWOFACTOR;
    }

    public boolean isPasswordless() {
        return false;
    }

    protected void clearEventQueue() {
        getTestingClient().testing().clearEventQueue();
    }

    public static VirtualAuthenticatorManager createDefaultVirtualManager(WebDriver webDriver, VirtualAuthenticatorOptions options) {
        VirtualAuthenticatorManager manager = new VirtualAuthenticatorManager(webDriver);
        manager.useAuthenticator(options);
        return manager;
    }
}
