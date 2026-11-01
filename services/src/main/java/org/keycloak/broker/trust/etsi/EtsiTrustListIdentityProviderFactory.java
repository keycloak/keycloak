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

package org.keycloak.broker.trust.etsi;

import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class EtsiTrustListIdentityProviderFactory extends AbstractIdentityProviderFactory<EtsiTrustListIdentityProvider>
        implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "etsi-trust-list";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
            .property()
            .name(EtsiTrustListIdentityProviderConfig.TRUST_LIST_URL)
            .label("Trust list URL")
            .helpText("URL of the signed ETSI TS 119 602 List of Trusted Entities (LoTE).")
            .type(ProviderConfigProperty.STRING_TYPE)
            .required(true)
            .add()
            .property()
            .name(EtsiTrustListIdentityProviderConfig.SIGNING_CERTIFICATES)
            .label("Trust list signing certificates")
            .helpText("PEM bundle of the certificates allowed to sign the trust list. Configure several certificates for a key rotation.")
            .type(ProviderConfigProperty.TEXT_TYPE)
            .required(true)
            .add()
            .property()
            .name(EtsiTrustListIdentityProviderConfig.LOTE_TYPE)
            .label("LoTE type")
            .helpText("Required LoTE type of the trust list, for example http://uri.etsi.org/19602/LoTEType/EUWalletProvidersList.")
            .type(ProviderConfigProperty.STRING_TYPE)
            .required(true)
            .add()
            .property()
            .name(EtsiTrustListIdentityProviderConfig.SERVICE_TYPES)
            .label("Service types")
            .helpText("Comma-separated service type identifiers whose certificates are trusted, for example http://uri.etsi.org/19602/SvcType/WalletSolution/Issuance.")
            .type(ProviderConfigProperty.STRING_TYPE)
            .required(true)
            .add()
            .property()
            .name(EtsiTrustListIdentityProviderConfig.SERVICE_STATUSES)
            .label("Service statuses")
            .helpText("Comma-separated service statuses. Services with one of these statuses are trusted. Services without a status are always trusted.")
            .type(ProviderConfigProperty.STRING_TYPE)
            .add()
            .property()
            .name(EtsiTrustListIdentityProviderConfig.REFRESH_INTERVAL)
            .label("Refresh interval")
            .helpText("Maximum number of seconds between two downloads of the trust list. This value decides how soon a withdrawn certificate stops being trusted.")
            .type(ProviderConfigProperty.STRING_TYPE)
            .defaultValue(String.valueOf(EtsiTrustListIdentityProviderConfig.DEFAULT_REFRESH_INTERVAL))
            .add()
            .build();

    @Override
    public String getName() {
        return "ETSI Trust List";
    }

    @Override
    public EtsiTrustListIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new EtsiTrustListIdentityProvider(session, new EtsiTrustListIdentityProviderConfig(model));
    }

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, String config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new EtsiTrustListIdentityProviderConfig();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI) || Profile.isFeatureEnabled(Profile.Feature.OID4VC_VP)
                || Profile.isFeatureEnabled(Profile.Feature.CLIENT_AUTH_ABCA);
    }
}
