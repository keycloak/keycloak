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
package org.keycloak.broker.oidc;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

import java.util.Map;

/**
 * @author <a href="https://www.n-k.de">Niko Köbler</a>
 */
public class EntraOIDCIdentityProviderFactory extends AbstractIdentityProviderFactory<EntraOIDCIdentityProvider> {

    public static final String PROVIDER_ID = "entraid-oidc";

    @Override
    public String getName() {
        return "Microsoft Entra ID OpenID Connect";
    }

    @Override
    public EntraOIDCIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new EntraOIDCIdentityProvider(session, new EntraOIDCIdentityProviderConfig(model));
    }

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, String config) {
        return OIDCIdentityProviderFactory.parseOIDCConfig(session, config);
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new OIDCIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
