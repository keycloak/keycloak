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
package org.keycloak.protocol.oid4vc.presentation;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class OID4VPIdentityProviderFactory extends AbstractIdentityProviderFactory<OID4VPIdentityProvider> implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "oid4vp";

    @Override
    public String getName() {
        return "OpenID for Verifiable Presentations";
    }

    @Override
    public OID4VPIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new OID4VPIdentityProvider(session, new OID4VPIdentityProviderConfig(model));
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty walletScheme = new ProviderConfigProperty();
        walletScheme.setName(OID4VPIdentityProviderConfig.WALLET_SCHEME);
        walletScheme.setLabel("Wallet URL Scheme");
        walletScheme.setHelpText("Custom wallet URL scheme prefix (for example, openid4vp:// or haip-vp://).");
        walletScheme.setType(ProviderConfigProperty.STRING_TYPE);
        walletScheme.setDefaultValue(OID4VPConstants.DEFAULT_WALLET_SCHEME);

        ProviderConfigProperty requestObjectLifespan = new ProviderConfigProperty();
        requestObjectLifespan.setName(OID4VPIdentityProviderConfig.REQUEST_OBJECT_LIFESPAN);
        requestObjectLifespan.setLabel("Request Object Lifespan");
        requestObjectLifespan.setHelpText("Lifetime of generated OID4VP request objects in seconds.");
        requestObjectLifespan.setType(ProviderConfigProperty.STRING_TYPE);
        requestObjectLifespan.setDefaultValue(Integer.toString(OID4VPIdentityProviderConfig.DEFAULT_REQUEST_OBJECT_LIFESPAN));

        return List.of(walletScheme, requestObjectLifespan);
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new OID4VPIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.OID4VC_VP);
    }
}
