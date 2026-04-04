/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.authentication.authenticators.browser;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 * @deprecated Factory is deprecated as passkeys are now integrated with the
 * default username authenticators. It will be removed in future versions
 * when the passkeys feature become supported.
 */
@Deprecated(since = "26.3", forRemoval = true)
public class PasskeysConditionalUIAuthenticatorFactory extends WebAuthnPasswordlessAuthenticatorFactory implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "passkeys-authenticator";

    @Override
    public String getDisplayType() {
        return "Passkeys Conditional UI Authenticator";
    }

    @Override
    public String getHelpText() {
        return "Authenticator for Passkeys with conditional UI. A list of passkeys stored on a device where a browser is running is automatically shown. Due to characteristics of conditional UI, it is used for login-less authentication.";
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new PasskeysConditionalUIAuthenticator(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.PASSKEYS_CONDITIONAL_UI_AUTHENTICATOR);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
