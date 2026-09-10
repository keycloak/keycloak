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

package org.keycloak.tests.conformance.vci.haip;

import java.util.List;
import java.util.Map;

import org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderConfig;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.json.RawJsonValue;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientPolicyConditionRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.clientpolicy.executor.PKCEEnforcerExecutorFactory;
import org.keycloak.services.clientpolicy.executor.RejectImplicitGrantExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureParContentsExecutorFactory;
import org.keycloak.testframework.conformance.OpenIdConformanceServer;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.conformance.ConformanceSigningKey;
import org.keycloak.tests.conformance.vci.VciAttesterKey;
import org.keycloak.tests.conformance.vci.VciConformanceRealmUtil;
import org.keycloak.tests.conformance.vci.nonhaip.NonHaipVciConformanceRealmConfig;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.keycloak.OID4VCConstants.OID4VCI_ENABLED_ATTRIBUTE_KEY;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.CLIENT;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.CLIENT2;

/**
 * HAIP variant of the OID4VCI conformance realm. It composes the shared realm building blocks from
 * {@link VciConformanceRealmUtil} and adds the High Assurance Interoperability Profile specifics: attestation-based
 * client authentication with DPoP sender constraining, the client attestation trust anchors, and the HAIP client
 * profile/policy (SecurePAR, no implicit grant, PKCE enforced). The non-HAIP variant lives in
 * {@link NonHaipVciConformanceRealmConfig}.
 */
public class HaipVciConformanceRealmConfig implements RealmConfig {

    public static final String HAIP_PLAN = "oid4vci-1_0-issuer-haip-test-plan";

    public static final String HAIP_CLIENT_PROFILE = "oid4vc-haip-profile";
    public static final String TRUST_IDP_ALIAS = "conformance-client-attester";
    public static final String X509_TRUST_IDP_ALIAS = "conformance-attester-x509";
    // The attester leaf certificate is generated with the emailProtection extended key usage.
    private static final String ATTESTER_ATTESTATION_EKU = "1.3.6.1.5.5.7.3.4";

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        VciConformanceRealmUtil.applyCommon(realm)
                .clients(conformanceClient(CLIENT, false), conformanceClient(CLIENT2, true), VciConformanceRealmUtil.appClient())
                .clientProfile(haipClientProfile())
                .clientPolicy(haipClientPolicy())
                .update(rep -> {
                    VciConformanceRealmUtil.applyKeyProviders(rep);
                    rep.setIdentityProviders(List.of(attesterTrustIdentityProvider(), attesterX509TrustIdentityProvider()));
                });
        return realm;
    }

    // Attestation-based client authentication against the trust IDPs plus DPoP sender constraining.
    private ClientBuilder conformanceClient(String clientId, boolean wildcardRedirect) {
        return VciConformanceRealmUtil.baseConformanceClient(clientId, wildcardRedirect)
                .authenticatorType(AttestationBasedClientAuthenticator.PROVIDER_ID)
                .attribute(AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_CONFIG_TRUST_IDPS, TRUST_IDP_ALIAS)
                .attribute(OID4VCIConstants.OID4VCI_ATTESTER_TRUST_IDPS_ATTR, TRUST_IDP_ALIAS + "," + X509_TRUST_IDP_ALIAS)
                // HAIP requires DPoP sender constraining, so the token endpoint must reject a request without a
                // DPoP proof (the holder-of-key conformance module checks exactly this)
                .attribute(OIDCConfigAttributes.DPOP_BOUND_ACCESS_TOKENS, "true");
    }

    private IdentityProviderRepresentation attesterTrustIdentityProvider() {
        IdentityProviderRepresentation trust = new IdentityProviderRepresentation();
        trust.setAlias(TRUST_IDP_ALIAS);
        trust.setProviderId(DefaultTrustIdentityProviderFactory.PROVIDER_ID);
        trust.setEnabled(true);
        trust.setConfig(Map.of(DefaultTrustIdentityProviderConfig.TRUSTED_JWKS, VciAttesterKey.publicJwks().toString()));
        return trust;
    }

    // Trusts the key attestation x5c certificate chain against the attester CA. Key attestations carry an x5c
    // header, so Keycloak must be able to validate the chain against a configured X.509 trust domain.
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

    private ClientProfileRepresentation haipClientProfile() {
        ClientProfileRepresentation profile = new ClientProfileRepresentation();
        profile.setName(HAIP_CLIENT_PROFILE);
        profile.setDescription("Enforces the OpenID4VC High Assurance Interoperability Profile 1.0");
        profile.setExecutors(List.of(
                executor(SecureParContentsExecutorFactory.PROVIDER_ID, JsonNodeFactory.instance.objectNode()),
                executor(RejectImplicitGrantExecutorFactory.PROVIDER_ID,
                        JsonNodeFactory.instance.objectNode().put("auto-configure", false)),
                executor(PKCEEnforcerExecutorFactory.PROVIDER_ID,
                        JsonNodeFactory.instance.objectNode().put("auto-configure", false))));
        return profile;
    }

    private ClientPolicyRepresentation haipClientPolicy() {
        ClientPolicyRepresentation policy = new ClientPolicyRepresentation();
        policy.setName("oid4vc-haip-policy");
        policy.setDescription("Enables the oid4vc-haip-profile for OID4VCI clients");
        policy.setEnabled(true);

        ClientPolicyConditionRepresentation condition = new ClientPolicyConditionRepresentation();
        condition.setConditionProviderId("client-attributes");
        ObjectNode config = JsonNodeFactory.instance.objectNode();
        config.put("attributes", JsonSerialization.valueAsString(List.of(Map.of(
                "key", OID4VCI_ENABLED_ATTRIBUTE_KEY,
                "value", String.valueOf(true)))));
        condition.setConfiguration(RawJsonValue.of(config));

        policy.setConditions(List.of(condition));
        policy.setProfiles(List.of(HAIP_CLIENT_PROFILE));
        return policy;
    }

    private ClientPolicyExecutorRepresentation executor(String providerId, JsonNode config) {
        ClientPolicyExecutorRepresentation executor = new ClientPolicyExecutorRepresentation();
        executor.setExecutorProviderId(providerId);
        executor.setConfiguration(RawJsonValue.of(config));
        return executor;
    }

    public static class ConsentRequiredRealmConfig extends HaipVciConformanceRealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return super.configure(realm).update(rep -> rep.getClients().stream()
                    .filter(client -> CLIENT.equals(client.getClientId()) || CLIENT2.equals(client.getClientId()))
                    .forEach(client -> client.setConsentRequired(true)));
        }
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI, Profile.Feature.OID4VC_MDOC, Profile.Feature.CLIENT_AUTH_ABCA)
                    .option("https-protocols", VciConformanceRealmUtil.TLS_PROTOCOLS)
                    .option("https-cipher-suites", VciConformanceRealmUtil.BCP195_CIPHERS)
                    .option("hostname", OpenIdConformanceServer.KEYCLOAK_BASE_URI.toString())
                    .spiOption("keys", "java-keystore", "keystores-path", ConformanceSigningKey.keystoresBaseDir());
        }
    }
}
