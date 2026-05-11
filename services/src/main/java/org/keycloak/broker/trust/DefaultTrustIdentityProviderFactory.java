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

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

public class DefaultTrustIdentityProviderFactory extends AbstractIdentityProviderFactory<DefaultTrustIdentityProvider> {

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
        ProviderConfigProperty trustedJwksUrl = new ProviderConfigProperty();
        trustedJwksUrl.setName(DefaultTrustIdentityProviderConfig.TRUSTED_JWKS_URL);
        trustedJwksUrl.setLabel("Trusted JWKS URL");
        trustedJwksUrl.setHelpText("External JWKS URL containing trusted signing keys.");
        trustedJwksUrl.setType(ProviderConfigProperty.STRING_TYPE);

        ProviderConfigProperty trustedJwks = new ProviderConfigProperty();
        trustedJwks.setName(DefaultTrustIdentityProviderConfig.TRUSTED_JWKS);
        trustedJwks.setLabel("Trusted JWKS");
        trustedJwks.setHelpText("Hardcoded JWKS containing trusted signing keys.");
        trustedJwks.setType(ProviderConfigProperty.TEXT_TYPE);

        return List.of(trustedJwksUrl, trustedJwks);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
