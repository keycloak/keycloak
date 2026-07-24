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

package org.keycloak.broker.oid4vp;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class OID4VPIdentityProviderFactory extends AbstractIdentityProviderFactory<OID4VPIdentityProvider>
        implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "oid4vp";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
            .property()
                .name(OID4VPIdentityProviderConfig.DCQL_QUERY)
                .label("DCQL Query (JSON)")
                .helpText("Digital Credentials Query Language query describing the requested credential and claims.")
                .type(ProviderConfigProperty.TEXT_TYPE)
                .required(true)
                .add()
            .property()
                .name(OID4VPIdentityProviderConfig.TRUSTED_ISSUER_JWKS)
                .label("Trusted Issuer JWKS")
                .helpText("Inline JWK Set of trusted credential issuer keys used to verify the credential signature.")
                .type(ProviderConfigProperty.TEXT_TYPE)
                .required(true)
                .add()
            .property()
                .name(OID4VPIdentityProviderConfig.PRINCIPAL_ATTRIBUTE)
                .label("Principal Attribute")
                .helpText("Credential claim used as the user principal, i.e. its id and username.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .required(true)
                .add()
            .property()
                .name(OID4VPIdentityProviderConfig.SIGNING_KEY_ID)
                .label("Signing Key ID")
                .helpText("Kid of the realm ES256 key used to sign request objects and derive the client identifier. "
                        + "When empty the active ES256 realm key is used. The referenced key may be passive or "
                        + "disabled, so it can be reserved for this provider without serving regular realm signing. "
                        + "Mind the certificate requirements of the wallet ecosystem: typically a leaf certificate "
                        + "issued by a CA the wallets trust rather than self-signed, with extensions such as basic "
                        + "constraints CA=false and digitalSignature key usage, e.g. supplied through a Java keystore "
                        + "key provider.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
            .property()
                .name(OID4VPIdentityProviderConfig.RESPONSE_MODE)
                .label("Response Mode")
                .helpText("How the wallet returns the presentation. direct_post.jwt encrypts it with a fresh ephemeral "
                        + "verifier key published in the request object, as required for HAIP compliance.")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(Stream.of(ResponseMode.values())
                        .map(ResponseMode::value).toList())
                .defaultValue(ResponseMode.DIRECT_POST.value())
                .required(true)
                .add()
            .property()
                .name(OID4VPIdentityProviderConfig.WALLET_SCHEME)
                .label("Wallet URL Scheme")
                .helpText("Custom URL scheme used in the wallet link (defaults to openid4vp://).")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(OID4VPIdentityProviderConfig.DEFAULT_WALLET_SCHEME)
                .required(true)
                .add()
            .build();

    @Override
    public String getName() {
        return "OpenID4VP (Wallet Login)";
    }

    @Override
    public OID4VPIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new OID4VPIdentityProvider(session, new OID4VPIdentityProviderConfig(model));
    }

    @Override
    public OID4VPIdentityProviderConfig createConfig() {
        return new OID4VPIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.OID4VC_VP);
    }
}
