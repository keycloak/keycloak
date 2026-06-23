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

package org.keycloak.tests.conformance.vci;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.keycloak.OID4VCConstants;
import org.keycloak.VCFormat;
import org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderConfig;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedEcdhKeyProviderFactory;
import org.keycloak.keys.JavaKeystoreKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCGeneratedIdMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCIssuedAtTimeClaimMapper;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.DisplayObject;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.conformance.containers.OpenIdConformanceSuite;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;

import static org.keycloak.OID4VCConstants.OID4VCI_ENABLED_ATTRIBUTE_KEY;
import static org.keycloak.models.Constants.CREATE_DEFAULT_CLIENT_SCOPES;
import static org.keycloak.models.Constants.DEFAULT_ROLES_ROLE_PREFIX;

public class VciConformanceRealmConfig implements RealmConfig {

    public static final String REALM = "oid4vci";
    public static final String HOLDER = "alice";
    public static final String PASSWORD = "password";
    public static final String CLIENT = "oid4vci-client";
    public static final String CLIENT2 = "oid4vci-client2";
    public static final String SD_JWT_SCOPE = "conformance_sd_jwt_vc";
    public static final String CREDENTIAL_CONFIGURATION_ID = "conformance_sd_jwt_vc";
    public static final String CONFORMANCE_CALLBACK = OpenIdConformanceSuite.INTERNAL_BASE_URI + "/test/a/keycloak/callback";
    public static final String TRUST_IDP_ALIAS = "conformance-client-attester";

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        realm.name(REALM)
                .eventsEnabled(true)
                .eventsListeners("jboss-logging")
                .verifiableCredentialsEnabled(true)
                .attribute(CREATE_DEFAULT_CLIENT_SCOPES, "true")
                // The conformance suite wallet requests DEF-compressed encrypted credential responses
                .attribute(OID4VCIssuerWellKnownProvider.ATTR_REQUEST_ZIP_ALGS, "DEF")
                .defaultSignatureAlgorithm(Algorithm.ES256)
                .clientScopes(createSdJwtCredentialScope())
                .users(UserBuilder.create()
                        .username(HOLDER)
                        .enabled(true)
                        .email("alice@example.test")
                        .emailVerified(true)
                        .firstName("Alice")
                        .lastName("Wonderland")
                        .password(PASSWORD)
                        .attribute("did", "did:key:alice")
                        .attribute("address_street_address", "221B Baker Street")
                        .attribute("address_locality", "London")
                        .realmRoles(DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM)
                        .verifiableCredential(SD_JWT_SCOPE)
                        .build())
                .clients(conformanceClient(CLIENT, false), conformanceClient(CLIENT2, true))
                .update(rep -> {
                    MultivaluedHashMap<String, ComponentExportRepresentation> components = rep.getComponents();
                    if (components == null) {
                        components = new MultivaluedHashMap<>();
                        rep.setComponents(components);
                    }
                    components.add(KeyProvider.class.getName(), conformanceSigningKeyProvider());
                    components.add(KeyProvider.class.getName(), conformanceEcdhEncryptionKeyProvider());
                    rep.setIdentityProviders(List.of(attesterTrustIdentityProvider()));
                });
        return realm;
    }

    public static JsonNode attesterJwks() {
        return VciAttesterKey.privateJwks();
    }

    private IdentityProviderRepresentation attesterTrustIdentityProvider() {
        IdentityProviderRepresentation trust = new IdentityProviderRepresentation();
        trust.setAlias(TRUST_IDP_ALIAS);
        trust.setProviderId(DefaultTrustIdentityProviderFactory.PROVIDER_ID);
        trust.setEnabled(true);
        trust.setConfig(Map.of(DefaultTrustIdentityProviderConfig.TRUSTED_JWKS, VciAttesterKey.publicJwks().toString()));
        return trust;
    }

    private ClientBuilder conformanceClient(String clientId, boolean wildcardRedirect) {
        return ClientBuilder.create(clientId)
                .serviceAccountsEnabled(false)
                .directAccessGrantsEnabled(false)
                .authenticatorType(AttestationBasedClientAuthenticator.PROVIDER_ID)
                .attribute(AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_CONFIG_TRUST_IDPS, TRUST_IDP_ALIAS)
                .attribute(OID4VCIConstants.OID4VCI_ATTESTER_TRUST_IDPS_ATTR, TRUST_IDP_ALIAS)
                .defaultClientScopes("basic", "profile", "roles")
                .optionalClientScopes(SD_JWT_SCOPE, "email")
                .attribute(OID4VCI_ENABLED_ATTRIBUTE_KEY, "true")
                .redirectUris(CONFORMANCE_CALLBACK + (wildcardRedirect ? "*" : ""))
                .webOrigins(OpenIdConformanceSuite.INTERNAL_BASE_URI.toString());
    }

    private CredentialScopeRepresentation createSdJwtCredentialScope() {
        CredentialScopeRepresentation scope = new CredentialScopeRepresentation(SD_JWT_SCOPE)
                .setIncludeInTokenScope(true)
                .setExpiryInSeconds(300)
                .setCredentialConfigurationId(CREDENTIAL_CONFIGURATION_ID)
                .setCredentialIdentifier(CREDENTIAL_CONFIGURATION_ID)
                .setFormat(VCFormat.SD_JWT_VC)
                .setSigningAlg(Algorithm.ES256)
                .setVct("https://credentials.example.com/SD-JWT-Credential");

        scope.setDisplay(JsonSerialization.valueAsString(List.of(new DisplayObject().setName(CREDENTIAL_CONFIGURATION_ID).setLocale("en-EN"))));
        scope.setProtocolMappers(protocolMappers(SD_JWT_SCOPE));

        Map<String, String> attributes = Optional.ofNullable(scope.getAttributes()).orElseGet(HashMap::new);
        attributes.put(CredentialScopeModel.VC_BINDING_REQUIRED, "true");
        attributes.put(CredentialScopeModel.VC_BINDING_REQUIRED_PROOF_TYPES, "jwt");
        attributes.put(CredentialScopeModel.VC_CRYPTOGRAPHIC_BINDING_METHODS, CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT);
        scope.setAttributes(attributes);
        return scope;
    }

    private ComponentExportRepresentation conformanceSigningKeyProvider() {
        ComponentExportRepresentation keyProvider = new ComponentExportRepresentation();
        keyProvider.setName("oid4vci-conformance-signing-key");
        keyProvider.setId(UUID.randomUUID().toString());
        keyProvider.setProviderId(JavaKeystoreKeyProviderFactory.ID);
        keyProvider.setConfig(new MultivaluedHashMap<>(Map.of(
                Attributes.PRIORITY_KEY, List.of("0"),
                Attributes.ENABLED_KEY, List.of("true"),
                Attributes.ACTIVE_KEY, List.of("true"),
                Attributes.ALGORITHM_KEY, List.of(Algorithm.ES256),
                Attributes.KEY_USE, List.of(KeyUse.SIG.name()),
                JavaKeystoreKeyProviderFactory.KEYSTORE_KEY, List.of(VciTestSigningKey.keyStorePath()),
                JavaKeystoreKeyProviderFactory.KEYSTORE_PASSWORD_KEY, List.of(VciTestSigningKey.PASSWORD),
                JavaKeystoreKeyProviderFactory.KEYSTORE_TYPE_KEY, List.of("PKCS12"),
                JavaKeystoreKeyProviderFactory.KEY_ALIAS_KEY, List.of(VciTestSigningKey.KEY_ALIAS),
                JavaKeystoreKeyProviderFactory.KEY_PASSWORD_KEY, List.of(VciTestSigningKey.PASSWORD))));
        return keyProvider;
    }

    // The conformance suite wallet requests ECDH-ES encrypted credential responses
    private ComponentExportRepresentation conformanceEcdhEncryptionKeyProvider() {
        ComponentExportRepresentation keyProvider = new ComponentExportRepresentation();
        keyProvider.setName("oid4vci-conformance-ecdh-encryption-key");
        keyProvider.setId(UUID.randomUUID().toString());
        keyProvider.setProviderId(GeneratedEcdhKeyProviderFactory.ID);
        keyProvider.setConfig(new MultivaluedHashMap<>(Map.of(
                Attributes.PRIORITY_KEY, List.of("0"),
                Attributes.ENABLED_KEY, List.of("true"),
                Attributes.ACTIVE_KEY, List.of("true"),
                GeneratedEcdhKeyProviderFactory.ECDH_ALGORITHM_KEY, List.of(Algorithm.ECDH_ES),
                GeneratedEcdhKeyProviderFactory.ECDH_ELLIPTIC_CURVE_KEY, List.of("P-256"))));
        return keyProvider;
    }

    private List<ProtocolMapperRepresentation> protocolMappers(String scopeName) {
        return List.of(
                mapper("did-mapper", "oid4vc-subject-id-mapper", Map.of("claim.name", OID4VCConstants.CLAIM_NAME_SUBJECT_ID, "userAttribute", "did")),
                mapper("email-mapper", "oid4vc-user-attribute-mapper", Map.of("claim.name", "email", "userAttribute", "email")),
                mapper("first-name-mapper", "oid4vc-user-attribute-mapper", Map.of("claim.name", "firstName", "userAttribute", "firstName")),
                mapper("last-name-mapper", "oid4vc-user-attribute-mapper", Map.of("claim.name", "lastName", "userAttribute", "lastName")),
                mapper("address-street-mapper", "oid4vc-user-attribute-mapper",
                        Map.of("claim.name", "address.street_address", "userAttribute", "address_street_address")),
                mapper("address-locality-mapper", "oid4vc-user-attribute-mapper",
                        Map.of("claim.name", "address.locality", "userAttribute", "address_locality")),
                mapper("generated-id-mapper", "oid4vc-generated-id-mapper", Map.of(OID4VCGeneratedIdMapper.CLAIM_NAME, "jti")),
                mapper("static-scope-mapper", "oid4vc-static-claim-mapper", Map.of("claim.name", "scope-name", "staticValue", scopeName)),
                mapper("issued-at-mapper", "oid4vc-issued-at-time-claim-mapper", Map.of(
                        OID4VCIssuedAtTimeClaimMapper.CLAIM_NAME, "iat",
                        OID4VCIssuedAtTimeClaimMapper.TRUNCATE_TO_TIME_UNIT_KEY, "HOURS",
                        OID4VCIssuedAtTimeClaimMapper.VALUE_SOURCE, "COMPUTE")),
                mapper("not-before-mapper", "oid4vc-issued-at-time-claim-mapper", Map.of(
                        OID4VCIssuedAtTimeClaimMapper.CLAIM_NAME, "nbf",
                        OID4VCIssuedAtTimeClaimMapper.VALUE_SOURCE, "COMPUTE")));
    }

    private ProtocolMapperRepresentation mapper(String name, String type, Map<String, String> config) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(name);
        mapper.setId(UUID.randomUUID().toString());
        mapper.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
        mapper.setProtocolMapper(type);
        mapper.setConfig(config);
        return mapper;
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI, Profile.Feature.CLIENT_AUTH_ABCA)
                    .option("hostname", OpenIdConformanceSuite.KEYCLOAK_BASE_URI.toString());
        }
    }
}
