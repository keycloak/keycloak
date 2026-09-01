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
import org.keycloak.protocol.oid4vc.issuance.TimeClaimNormalizer;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCGeneratedIdMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCIssuedAtTimeClaimMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCStaticClaimMapper;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.DisplayObject;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.conformance.OpenIdConformanceServer;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;

import static org.keycloak.OID4VCConstants.CRYPTOGRAPHIC_BINDING_METHOD_COSE_KEY;
import static org.keycloak.OID4VCConstants.OID4VCI_ENABLED_ATTRIBUTE_KEY;
import static org.keycloak.models.Constants.CREATE_DEFAULT_CLIENT_SCOPES;
import static org.keycloak.models.Constants.DEFAULT_ROLES_ROLE_PREFIX;

/**
 * Shared building blocks for the OID4VCI conformance realm. The pieces here are identical for HAIP and non-HAIP:
 * realm defaults, the SD-JWT and mDoc credential scopes with their protocol mappers, the holder user, the account
 * app client, the signing and encryption key providers, and the common (auth-agnostic) conformance client shape.
 *
 * The HAIP and non-HAIP realm configs compose these helpers and add only what differs (client authentication,
 * client policies, and trust anchors). Kept as a utility rather than a base class so each config reads as a flat
 * recipe and the two variants never share behaviour implicitly.
 */
public final class VciConformanceRealmUtil {

    public static final String REALM = "oid4vci";
    public static final String HOLDER = "alice";
    public static final String PASSWORD = "password";
    public static final String CLIENT = "oid4vci-client";
    public static final String CLIENT2 = "oid4vci-client2";
    public static final String APP_CLIENT = "oid4vci-app";
    public static final String SD_JWT_SCOPE = "conformance_sd_jwt_vc";
    public static final String CREDENTIAL_CONFIGURATION_ID = "conformance_sd_jwt_vc";
    public static final String MDOC_SCOPE = "conformance_mso_mdoc";
    public static final String MDOC_CREDENTIAL_CONFIGURATION_ID = "conformance_mso_mdoc";
    public static final String MDOC_DOC_TYPE = "org.iso.18013.5.1.mDL";
    public static final String MDOC_NAMESPACE = "org.iso.18013.5.1";
    // The credential_format plan variant value the conformance suite uses for ISO mdoc, see VCI1FinalCredentialFormat
    public static final String MDOC_CREDENTIAL_FORMAT_VARIANT = "mdoc";
    public static final String CONFORMANCE_CALLBACK = OpenIdConformanceServer.INTERNAL_BASE_URI + "/test/a/keycloak/callback";

    // FAPI2 requires the TLS layer to only offer BCP195 (RFC 9325) recommended cipher suites for TLS 1.2. The
    // default JVM cipher list includes non-recommended suites, which the conformance suite TLS checks reject, so
    // the test server is restricted to the recommended AEAD suites. The TLS 1.3 suites must be listed too, as
    // restricting the cipher list would otherwise disable TLS 1.3 entirely and the suite checks that TLS 1.3 is
    // negotiable where offered. Both the HAIP and non-HAIP servers run the FAPI2-family TLS checks, so they share
    // this list.
    public static final String TLS_PROTOCOLS = "TLSv1.3,TLSv1.2";
    public static final String BCP195_CIPHERS = String.join(",",
            "TLS_AES_128_GCM_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

    /**
     * Maps the conformance suite {@code credential_format} plan variant to the matching credential configuration id
     * provisioned on the realm, defaulting to SD-JWT VC.
     */
    public static String credentialConfigurationId(String credentialFormatVariant) {
        return MDOC_CREDENTIAL_FORMAT_VARIANT.equals(credentialFormatVariant)
                ? MDOC_CREDENTIAL_CONFIGURATION_ID
                : CREDENTIAL_CONFIGURATION_ID;
    }

    public static JsonNode attesterJwks() {
        return VciAttesterKey.privateJwks();
    }

    /**
     * Applies the realm configuration shared by every OID4VCI conformance variant: realm-level attributes, the two
     * credential scopes with their protocol mappers, and the holder user. Callers add the clients, client policies,
     * trust anchors and key providers that differ per variant.
     */
    public static RealmBuilder applyCommon(RealmBuilder realm) {
        return realm.name(REALM)
                .eventsEnabled(true)
                .eventsListeners("jboss-logging")
                .verifiableCredentialsEnabled(true)
                .attribute(CREATE_DEFAULT_CLIENT_SCOPES, "true")
                // The conformance suite wallet requests DEF-compressed encrypted credential responses
                .attribute(OID4VCIssuerWellKnownProvider.ATTR_REQUEST_ZIP_ALGS, "DEF")
                // Randomize credential time claims (iat/exp/nbf) so two credentials from the same dataset do not
                // carry the precise issuance time, which the suite's unlinkability check (RFC 9901 10.1) warns on.
                .attribute(OID4VCIConstants.TIME_CLAIMS_STRATEGY, TimeClaimNormalizer.Strategy.RANDOMIZE.name())
                .attribute(OID4VCIConstants.TIME_RANDOMIZE_WINDOW_SECONDS, "300")
                .defaultSignatureAlgorithm(Algorithm.ES256)
                .clientScopes(createSdJwtCredentialScope(), createMdocCredentialScope())
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
                        .verifiableCredential(MDOC_SCOPE)
                        .build());
    }

    /**
     * Common conformance client shape without any client authentication. HAIP and non-HAIP configs add their own
     * authentication (attestation-based vs public + PKCE) on top of this.
     */
    public static ClientBuilder baseConformanceClient(String clientId, boolean wildcardRedirect) {
        return ClientBuilder.create(clientId)
                .serviceAccountsEnabled(false)
                .directAccessGrantsEnabled(false)
                .defaultClientScopes("basic", "profile", "roles")
                .optionalClientScopes(SD_JWT_SCOPE, MDOC_SCOPE, "email")
                .attribute(OID4VCI_ENABLED_ATTRIBUTE_KEY, "true")
                .redirectUris(CONFORMANCE_CALLBACK + (wildcardRedirect ? "*" : ""))
                .webOrigins(OpenIdConformanceServer.INTERNAL_BASE_URI.toString());
    }

    public static ClientBuilder appClient() {
        return ClientBuilder.create(APP_CLIENT)
                .publicClient(true)
                .serviceAccountsEnabled(false)
                .directAccessGrantsEnabled(false)
                .redirectUris(OpenIdConformanceServer.KEYCLOAK_BASE_URI + "/realms/" + REALM + "/account/*")
                .defaultClientScopes("basic", "profile", "roles");
    }

    /**
     * Adds the signing and ECDH encryption key providers used by every variant to the realm representation.
     */
    public static void applyKeyProviders(RealmRepresentation rep) {
        MultivaluedHashMap<String, ComponentExportRepresentation> components = rep.getComponents();
        if (components == null) {
            components = new MultivaluedHashMap<>();
            rep.setComponents(components);
        }
        components.add(KeyProvider.class.getName(), conformanceSigningKeyProvider());
        components.add(KeyProvider.class.getName(), conformanceEcdhEncryptionKeyProvider());
    }

    private static CredentialScopeRepresentation createSdJwtCredentialScope() {
        CredentialScopeRepresentation scope = new CredentialScopeRepresentation(SD_JWT_SCOPE)
                .setIncludeInTokenScope(true)
                .setExpiryInSeconds(300)
                .setCredentialConfigurationId(CREDENTIAL_CONFIGURATION_ID)
                .setCredentialIdentifier(CREDENTIAL_CONFIGURATION_ID)
                .setFormat(VCFormat.SD_JWT_VC)
                .setSigningAlg(Algorithm.ES256)
                // A urn vct (rather than an https URL) means there is no retrievable SD-JWT VC Type Metadata, so
                // the suite does not attempt to fetch it (SD-JWT VC 6.3.1). An https vct would require hosting the
                // type metadata document at that URL for the suite to fetch.
                .setVct("urn:example:sd-jwt-credential");

        scope.setDisplay(JsonSerialization.valueAsString(List.of(new DisplayObject().setName(CREDENTIAL_CONFIGURATION_ID).setLocale("en-US"))));
        scope.setProtocolMappers(protocolMappers(SD_JWT_SCOPE));

        Map<String, String> attributes = Optional.ofNullable(scope.getAttributes()).orElseGet(HashMap::new);
        attributes.put(CredentialScopeModel.VC_BINDING_REQUIRED, "true");
        attributes.put(CredentialScopeModel.VC_BINDING_REQUIRED_PROOF_TYPES, "jwt");
        attributes.put(CredentialScopeModel.VC_CRYPTOGRAPHIC_BINDING_METHODS, CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT);
        scope.setAttributes(attributes);
        return scope;
    }

    private static CredentialScopeRepresentation createMdocCredentialScope() {
        CredentialScopeRepresentation scope = new CredentialScopeRepresentation(MDOC_SCOPE)
                .setIncludeInTokenScope(true)
                .setExpiryInSeconds(300)
                .setCredentialConfigurationId(MDOC_CREDENTIAL_CONFIGURATION_ID)
                .setCredentialIdentifier(MDOC_CREDENTIAL_CONFIGURATION_ID)
                .setFormat(VCFormat.MSO_MDOC)
                .setSigningAlg(Algorithm.ES256)
                // mDoc carries the doctype where SD-JWT VC carries the vct
                .setVct(MDOC_DOC_TYPE);

        scope.setDisplay(JsonSerialization.valueAsString(List.of(new DisplayObject().setName(MDOC_CREDENTIAL_CONFIGURATION_ID).setLocale("en-US"))));
        scope.setProtocolMappers(mdocProtocolMappers());

        Map<String, String> attributes = Optional.ofNullable(scope.getAttributes()).orElseGet(HashMap::new);
        attributes.put(CredentialScopeModel.VC_BINDING_REQUIRED, "true");
        attributes.put(CredentialScopeModel.VC_BINDING_REQUIRED_PROOF_TYPES, "jwt");
        attributes.put(CredentialScopeModel.VC_CRYPTOGRAPHIC_BINDING_METHODS, CRYPTOGRAPHIC_BINDING_METHOD_COSE_KEY);
        scope.setAttributes(attributes);
        return scope;
    }

    private static ComponentExportRepresentation conformanceSigningKeyProvider() {
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
    private static ComponentExportRepresentation conformanceEcdhEncryptionKeyProvider() {
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

    private static List<ProtocolMapperRepresentation> protocolMappers(String scopeName) {
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

    // mDoc claims are organised into namespaces, so every mapper pins the ISO 18013-5 namespace. ISO/IEC
    // 18013-5 marks a fixed set of org.iso.18013.5.1 data elements mandatory for an mDL, so a conformant
    // issuer must emit all of them (the suite's EnsureMdocMdlMandatoryDataElementsPresent check enforces this).
    // The holder-specific ones come from user attributes; the rest are static document values.
    private static List<ProtocolMapperRepresentation> mdocProtocolMappers() {
        return List.of(
                mdocMapper("did-mapper", "oid4vc-subject-id-mapper", "id", "did"),
                mdocMapper("given-name-mapper", "oid4vc-user-attribute-mapper", "given_name", "firstName"),
                mdocMapper("family-name-mapper", "oid4vc-user-attribute-mapper", "family_name", "lastName"),
                mdocStaticMapper("birth-date-mapper", "birth_date", "1986-03-22"),
                mdocStaticMapper("issue-date-mapper", "issue_date", "2019-10-20"),
                mdocStaticMapper("expiry-date-mapper", "expiry_date", "2029-10-20"),
                mdocStaticMapper("issuing-country-mapper", "issuing_country", "US"),
                mdocStaticMapper("issuing-authority-mapper", "issuing_authority", "Keycloak Conformance"),
                mdocStaticMapper("document-number-mapper", "document_number", "TEST-1234567"),
                mdocStaticMapper("un-distinguishing-sign-mapper", "un_distinguishing_sign", "USA"),
                mdocStaticMapper("portrait-mapper", "portrait", "conformance-portrait"),
                mdocStaticMapper("driving-privileges-mapper", "driving_privileges", "[]"));
    }

    private static ProtocolMapperRepresentation mdocStaticMapper(String name, String claimName, String value) {
        return mapper(name, OID4VCStaticClaimMapper.MAPPER_ID, Map.of(
                OID4VCMapper.CLAIM_NAME, claimName,
                OID4VCStaticClaimMapper.STATIC_CLAIM_KEY, value,
                OID4VCMapper.MDOC_NAMESPACE, MDOC_NAMESPACE));
    }

    private static ProtocolMapperRepresentation mdocMapper(String name, String type, String claimName, String userAttribute) {
        return mapper(name, type, Map.of(
                OID4VCMapper.CLAIM_NAME, claimName,
                OID4VCMapper.USER_ATTRIBUTE_KEY, userAttribute,
                OID4VCMapper.MDOC_NAMESPACE, MDOC_NAMESPACE));
    }

    private static ProtocolMapperRepresentation mapper(String name, String type, Map<String, String> config) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(name);
        mapper.setId(UUID.randomUUID().toString());
        mapper.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
        mapper.setProtocolMapper(type);
        mapper.setConfig(config);
        return mapper;
    }
}
