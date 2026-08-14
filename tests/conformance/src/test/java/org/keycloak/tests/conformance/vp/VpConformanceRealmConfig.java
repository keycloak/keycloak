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

package org.keycloak.tests.conformance.vp;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.broker.oid4vp.OID4VPIdentityProviderConfig;
import org.keycloak.broker.oid4vp.OID4VPIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.JavaKeystoreKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.conformance.ConformanceSigningKey;
import org.keycloak.tests.conformance.containers.OpenIdConformanceSuite;

public class VpConformanceRealmConfig implements RealmConfig {

    public static final String REALM = "oid4vp-verifier";
    public static final String CLIENT_ID = "wallet-mock";
    public static final String IDP_ALIAS = "oid4vp";

    // The suite presents a PID SD-JWT VC and discloses given_name and family_name. given_name is the
    // principal because the presented credential carries no stable subject claim.
    static final String DCQL_QUERY = """
            {
              "credentials": [
                {
                  "id": "pid_sd_jwt",
                  "format": "dc+sd-jwt",
                  "meta": { "vct_values": ["urn:eudi:pid:1"] },
                  "claims": [{ "path": ["given_name"] }, { "path": ["family_name"] }]
                }
              ],
              "credential_sets": [{ "options": [["pid_sd_jwt"]], "required": true }]
            }
            """;

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        return realm.name(REALM)
                .clients(ClientBuilder.create(CLIENT_ID).publicClient().redirectUris("*"))
                .update(rep -> {
                    rep.setIdentityProviders(List.of(identityProvider()));
                    MultivaluedHashMap<String, ComponentExportRepresentation> components = new MultivaluedHashMap<>();
                    components.add(KeyProvider.class.getName(), verifierSigningKeyProvider());
                    rep.setComponents(components);
                });
    }

    private IdentityProviderRepresentation identityProvider() {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(IDP_ALIAS);
        idp.setProviderId(OID4VPIdentityProviderFactory.PROVIDER_ID);
        idp.setEnabled(true);
        idp.setFirstBrokerLoginFlowAlias("first broker login");
        idp.setConfig(idpConfig());
        return idp;
    }

    // Overridable so the encrypted direct_post.jwt variant can enable response encryption.
    protected Map<String, String> idpConfig() {
        return Map.of(
                OID4VPIdentityProviderConfig.TRUSTED_ISSUER_JWKS, VpVerifierKey.publicJwks().toString(),
                OID4VPIdentityProviderConfig.DCQL_QUERY, DCQL_QUERY,
                OID4VPIdentityProviderConfig.PRINCIPAL_ATTRIBUTE, "given_name");
    }

    // The verifier's request-signing key, holding the CA-issued certificate the suite trusts.
    private ComponentExportRepresentation verifierSigningKeyProvider() {
        ComponentExportRepresentation keyProvider = new ComponentExportRepresentation();
        keyProvider.setName("oid4vp-verifier-signing-key");
        keyProvider.setId(UUID.randomUUID().toString());
        keyProvider.setProviderId(JavaKeystoreKeyProviderFactory.ID);
        keyProvider.setConfig(new MultivaluedHashMap<>(Map.of(
                Attributes.PRIORITY_KEY, List.of("100"),
                Attributes.ENABLED_KEY, List.of("true"),
                Attributes.ACTIVE_KEY, List.of("true"),
                Attributes.ALGORITHM_KEY, List.of(Algorithm.ES256),
                Attributes.KEY_USE, List.of(KeyUse.SIG.name()),
                JavaKeystoreKeyProviderFactory.KEYSTORE_KEY, List.of(VpVerifierKey.keyStorePath()),
                JavaKeystoreKeyProviderFactory.KEYSTORE_PASSWORD_KEY, List.of(VpVerifierKey.keyStorePassword()),
                JavaKeystoreKeyProviderFactory.KEYSTORE_TYPE_KEY, List.of("PKCS12"),
                JavaKeystoreKeyProviderFactory.KEY_ALIAS_KEY, List.of(VpVerifierKey.keyAlias()),
                JavaKeystoreKeyProviderFactory.KEY_PASSWORD_KEY, List.of(VpVerifierKey.keyStorePassword()))));
        return keyProvider;
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VP)
                    .option("hostname", OpenIdConformanceSuite.KEYCLOAK_BASE_URI.toString())
                    .spiOption("keys", "java-keystore", "keystores-path", ConformanceSigningKey.keystoresBaseDir());
        }
    }
}
