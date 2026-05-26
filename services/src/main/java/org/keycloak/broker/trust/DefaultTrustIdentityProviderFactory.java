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

package org.keycloak.broker.trust;

import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

import static org.keycloak.broker.oidc.OIDCIdentityProviderConfig.USE_JWKS_URL;

public class DefaultTrustIdentityProviderFactory extends AbstractIdentityProviderFactory<DefaultTrustIdentityProvider> implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "default-trust";

    @Override
    public String getName() {
        return "Default Trust";
    }

    @Override
    public DefaultTrustIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new DefaultTrustIdentityProvider(session, new DefaultTrustIdentityProviderConfig(model));
    }

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, String config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new DefaultTrustIdentityProviderConfig();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty useJwksUrl = new ProviderConfigProperty();
        useJwksUrl.setName(USE_JWKS_URL);
        useJwksUrl.setLabel("Use JWKS URL");
        useJwksUrl.setHelpText("If enabled, trusted signing keys are downloaded from the JWKS URL. "
                + "If disabled, the configured JSON Web Key Set is used.");
        useJwksUrl.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        useJwksUrl.setDefaultValue(Boolean.TRUE.toString());

        ProviderConfigProperty trustedJwksUrl = new ProviderConfigProperty();
        trustedJwksUrl.setName(DefaultTrustIdentityProviderConfig.TRUSTED_JWKS_URL);
        trustedJwksUrl.setLabel("JWKS URL");
        trustedJwksUrl.setHelpText("External JWKS URL containing trusted signing keys.");
        trustedJwksUrl.setType(ProviderConfigProperty.STRING_TYPE);

        ProviderConfigProperty trustedJwks = new ProviderConfigProperty();
        trustedJwks.setName(DefaultTrustIdentityProviderConfig.TRUSTED_JWKS);
        trustedJwks.setLabel("JSON Web Key Set");
        trustedJwks.setHelpText("Hardcoded JWKS containing trusted signing keys.");
        trustedJwks.setType(ProviderConfigProperty.TEXT_TYPE);

        return List.of(useJwksUrl, trustedJwksUrl, trustedJwks);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI) || Profile.isFeatureEnabled(Profile.Feature.CLIENT_AUTH_ABCA);
    }
}
