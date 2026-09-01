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

package org.keycloak.tests.conformance.vci.nonhaip;

import java.util.List;
import java.util.Map;

import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderConfig;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.conformance.OpenIdConformanceServer;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.conformance.ConformanceSigningKey;
import org.keycloak.tests.conformance.vci.VciAttesterKey;
import org.keycloak.tests.conformance.vci.VciConformanceRealmUtil;
import org.keycloak.tests.conformance.vci.haip.HaipVciConformanceRealmConfig;

import com.fasterxml.jackson.databind.JsonNode;

import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.CLIENT;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.CLIENT2;

/**
 * Non-HAIP variant of the OID4VCI conformance realm. It composes the shared realm building blocks from
 * {@link VciConformanceRealmUtil} and, unlike {@link HaipVciConformanceRealmConfig}, authenticates the conformance
 * clients with private_key_jwt (confidential clients holding a registered public JWKS) instead of attestation-based
 * client authentication, so there are no client attestation trust anchors and no HAIP client profile/policy.
 *
 * The conformance suite's OID4VCI issuer test modules do not permit public clients or client_secret authentication
 * ({@code client_auth_type=none}/{@code client_secret_*} are marked not applicable), so private_key_jwt is the
 * non-HAIP client authentication used here. FAPI2 still mandates sender constraining, so the clients keep DPoP.
 *
 * Pairs with its own {@link ServerConfig}, the neutral OID4VC_VCI server baseline (no CLIENT_AUTH_ABCA), since
 * non-HAIP does not use attestation-based client authentication.
 */
public class NonHaipVciConformanceRealmConfig implements RealmConfig {

    public static final String NON_HAIP_PLAN = "oid4vci-1_0-issuer-test-plan";

    // Key attestations carry an x5c chain, so Keycloak validates them against this X.509 trust domain. Client
    // attestation (HAIP) is not used here, only key attestation on the proof, so there is no JWKS trust IDP.
    public static final String X509_TRUST_IDP_ALIAS = "conformance-attester-x509";
    // The attester leaf certificate is generated with the emailProtection extended key usage.
    private static final String ATTESTER_ATTESTATION_EKU = "1.3.6.1.5.5.7.3.4";

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        VciConformanceRealmUtil.applyCommon(realm)
                .clients(conformanceClient(CLIENT, false, VciClientKey.publicJwks()),
                        conformanceClient(CLIENT2, true, VciClientKey.publicJwks2()),
                        VciConformanceRealmUtil.appClient())
                .update(rep -> {
                    VciConformanceRealmUtil.applyKeyProviders(rep);
                    rep.setIdentityProviders(List.of(attesterX509TrustIdentityProvider()));
                });
        return realm;
    }

    // Non-HAIP client authentication: confidential client using private_key_jwt with the conformance client's own
    // public JWKS registered on the client, plus DPoP sender constraining as FAPI2 requires. The suite holds the
    // matching private JWKS (see VciClientKey / VciSuiteConfig) to sign the client assertions. The attester trust
    // IDP is referenced so key attestations on the proof can be validated.
    private ClientBuilder conformanceClient(String clientId, boolean wildcardRedirect, JsonNode publicJwks) {
        return VciConformanceRealmUtil.baseConformanceClient(clientId, wildcardRedirect)
                .authenticatorType(JWTClientAuthenticator.PROVIDER_ID)
                .attribute(OIDCConfigAttributes.USE_JWKS_STRING, "true")
                .attribute(OIDCConfigAttributes.JWKS_STRING, publicJwks.toString())
                .attribute(OID4VCIConstants.OID4VCI_ATTESTER_TRUST_IDPS_ATTR, X509_TRUST_IDP_ALIAS)
                .attribute(OIDCConfigAttributes.DPOP_BOUND_ACCESS_TOKENS, "true");
    }

    // Trusts the key attestation x5c certificate chain against the attester CA, so Keycloak can validate the chain
    // against a configured X.509 trust domain when a credential configuration requires key attestation.
    private IdentityProviderRepresentation attesterX509TrustIdentityProvider() {
        IdentityProviderRepresentation trust = new IdentityProviderRepresentation();
        trust.setAlias(X509_TRUST_IDP_ALIAS);
        trust.setProviderId(DefaultTrustIdentityProviderFactory.PROVIDER_ID);
        trust.setEnabled(true);
        trust.setConfig(Map.of(
                DefaultTrustIdentityProviderConfig.USE_X509, "true",
                DefaultTrustIdentityProviderConfig.TRUSTED_CERTIFICATES, VciAttesterKey.caCertificatePem(),
                DefaultTrustIdentityProviderConfig.REQUIRED_EXTENDED_KEY_USAGES, ATTESTER_ATTESTATION_EKU));
        return trust;
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI)
                    .option("https-protocols", VciConformanceRealmUtil.TLS_PROTOCOLS)
                    .option("https-cipher-suites", VciConformanceRealmUtil.BCP195_CIPHERS)
                    .option("hostname", OpenIdConformanceServer.KEYCLOAK_BASE_URI.toString())
                    .spiOption("keys", "java-keystore", "keystores-path", ConformanceSigningKey.keystoresBaseDir());
        }
    }
}
