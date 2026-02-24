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
package org.keycloak.tests.admin.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;

/**
 * Utility class for comparing v1 ClientRepresentation against v2 client representations.
 * This class maps fields between the two API versions and reports matches and mismatches.
 */
public class ClientRepresentationComparator {

    public static class ComparisonResult {
        private final List<String> mismatches = new ArrayList<>();
        private final List<String> matches = new ArrayList<>();
        private final List<String> v1OnlyFields = new ArrayList<>();
        private final List<String> v2OnlyFields = new ArrayList<>();

        public void addMismatch(String fieldName, Object v1Value, Object v2Value) {
            mismatches.add(String.format("%s: v1='%s' vs v2='%s'", fieldName, v1Value, v2Value));
        }

        public void addMatch(String fieldName, Object value) {
            matches.add(String.format("%s: '%s'", fieldName, value));
        }

        public void addV1OnlyField(String fieldName, Object value) {
            if (value != null) {
                v1OnlyFields.add(String.format("%s: '%s'", fieldName, value));
            }
        }

        public void addV2OnlyField(String fieldName, Object value) {
            if (value != null) {
                v2OnlyFields.add(String.format("%s: '%s'", fieldName, value));
            }
        }

        public boolean allMatch() {
            return mismatches.isEmpty();
        }

        public List<String> getMismatches() {
            return mismatches;
        }

        public List<String> getMatches() {
            return matches;
        }

        public List<String> getV1OnlyFields() {
            return v1OnlyFields;
        }

        public List<String> getV2OnlyFields() {
            return v2OnlyFields;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (!mismatches.isEmpty()) {
                sb.append("MISMATCHES:\n");
                mismatches.forEach(m -> sb.append("  - ").append(m).append("\n"));
            }
            if (!matches.isEmpty()) {
                sb.append("MATCHES:\n");
                matches.forEach(m -> sb.append("  - ").append(m).append("\n"));
            }
            if (!v1OnlyFields.isEmpty()) {
                sb.append("V1-ONLY FIELDS (not mapped to v2):\n");
                v1OnlyFields.forEach(f -> sb.append("  - ").append(f).append("\n"));
            }
            if (!v2OnlyFields.isEmpty()) {
                sb.append("V2-ONLY FIELDS (not mapped from v1):\n");
                v2OnlyFields.forEach(f -> sb.append("  - ").append(f).append("\n"));
            }
            return sb.toString();
        }
    }

    /**
     * Compares a v1 ClientRepresentation against a v2 BaseClientRepresentation.
     * Maps fields with different names between v1 and v2.
     */
    public static ComparisonResult compare(ClientRepresentation v1, BaseClientRepresentation v2) {
        ComparisonResult result = new ComparisonResult();

        compareBaseFields(result, v1, v2);

        if (v2 instanceof OIDCClientRepresentation oidc) {
            compareOIDCFields(result, v1, oidc);
        } else if (v2 instanceof SAMLClientRepresentation saml) {
            compareSAMLFields(result, v1, saml);
        }

        recordV1OnlyFields(result, v1);

        return result;
    }

    private static void compareBaseFields(ComparisonResult result, ClientRepresentation v1, BaseClientRepresentation v2) {
        compareField(result, "clientId", v1.getClientId(), v2.getClientId());
        compareField(result, "name→displayName", v1.getName(), v2.getDisplayName());
        compareField(result, "description", v1.getDescription(), v2.getDescription());
        compareField(result, "enabled", v1.isEnabled(), v2.getEnabled());
        compareField(result, "baseUrl→appUrl", v1.getBaseUrl(), v2.getAppUrl());
        compareField(result, "protocol", v1.getProtocol(), v2.getProtocol());

        compareCollectionAsSet(result, "redirectUris", v1.getRedirectUris(), v2.getRedirectUris());
    }

    private static void compareOIDCFields(ComparisonResult result, ClientRepresentation v1, OIDCClientRepresentation v2) {
        compareCollectionAsSet(result, "webOrigins", v1.getWebOrigins(), v2.getWebOrigins());

        Set<OIDCClientRepresentation.Flow> expectedFlows = new HashSet<>();
        if (Boolean.TRUE.equals(v1.isStandardFlowEnabled())) {
            expectedFlows.add(OIDCClientRepresentation.Flow.STANDARD);
        }
        if (Boolean.TRUE.equals(v1.isImplicitFlowEnabled())) {
            expectedFlows.add(OIDCClientRepresentation.Flow.IMPLICIT);
        }
        if (Boolean.TRUE.equals(v1.isDirectAccessGrantsEnabled())) {
            expectedFlows.add(OIDCClientRepresentation.Flow.DIRECT_GRANT);
        }
        if (Boolean.TRUE.equals(v1.isServiceAccountsEnabled())) {
            expectedFlows.add(OIDCClientRepresentation.Flow.SERVICE_ACCOUNT);
        }
        compareField(result, "flows→loginFlows", expectedFlows, v2.getLoginFlows());

        String expectedAuthMethod = determineAuthMethod(v1);
        String actualAuthMethod = v2.getAuth() != null ? v2.getAuth().getMethod() : null;
        compareField(result, "clientAuthenticatorType→auth.method", expectedAuthMethod, actualAuthMethod);

        String expectedSecret = v1.getSecret();
        String actualSecret = v2.getAuth() != null ? v2.getAuth().getSecret() : null;
        compareField(result, "secret→auth.secret", expectedSecret, actualSecret);

        result.addV2OnlyField("auth.certificate", v2.getAuth() != null ? v2.getAuth().getCertificate() : null);
        result.addV2OnlyField("serviceAccountRoles", 
            v2.getServiceAccountRoles() != null && !v2.getServiceAccountRoles().isEmpty() ? v2.getServiceAccountRoles() : null);
    }

    private static void compareSAMLFields(ComparisonResult result, ClientRepresentation v1, SAMLClientRepresentation v2) {
        compareField(result, "frontchannelLogout→frontChannelLogout", 
            v1.isFrontchannelLogout(), v2.getFrontChannelLogout());

        Map<String, String> attrs = v1.getAttributes();
        if (attrs != null) {
            compareField(result, "attr[saml_name_id_format]→nameIdFormat", 
                attrs.get("saml_name_id_format"), v2.getNameIdFormat());
            compareField(result, "attr[saml.force.name.id.format]→forceNameIdFormat",
                parseBooleanAttribute(attrs.get("saml.force.name.id.format")), 
                v2.getForceNameIdFormat());
            compareField(result, "attr[saml.authnstatement]→includeAuthnStatement",
                parseBooleanAttribute(attrs.get("saml.authnstatement")), 
                v2.getIncludeAuthnStatement());
            compareField(result, "attr[saml.server.signature]→signDocuments",
                parseBooleanAttribute(attrs.get("saml.server.signature")), 
                v2.getSignDocuments());
            compareField(result, "attr[saml.assertion.signature]→signAssertions",
                parseBooleanAttribute(attrs.get("saml.assertion.signature")), 
                v2.getSignAssertions());
            compareField(result, "attr[saml.client.signature]→clientSignatureRequired",
                parseBooleanAttribute(attrs.get("saml.client.signature")), 
                v2.getClientSignatureRequired());
            compareField(result, "attr[saml.force.post.binding]→forcePostBinding",
                parseBooleanAttribute(attrs.get("saml.force.post.binding")), 
                v2.getForcePostBinding());
            compareField(result, "attr[saml.signature.algorithm]→signatureAlgorithm",
                attrs.get("saml.signature.algorithm"), 
                v2.getSignatureAlgorithm());
            compareField(result, "attr[saml_signature_canonicalization_method]→signatureCanonicalizationMethod",
                attrs.get("saml_signature_canonicalization_method"), 
                v2.getSignatureCanonicalizationMethod());
            compareField(result, "attr[saml.signing.certificate]→signingCertificate",
                attrs.get("saml.signing.certificate"), 
                v2.getSigningCertificate());
            compareField(result, "attr[saml.allow.ecp.flow]→allowEcpFlow",
                parseBooleanAttribute(attrs.get("saml.allow.ecp.flow")), 
                v2.getAllowEcpFlow());
        }
    }

    private static void recordV1OnlyFields(ComparisonResult result, ClientRepresentation v1) {
        result.addV1OnlyField("id", v1.getId());
        result.addV1OnlyField("type", v1.getType());
        result.addV1OnlyField("rootUrl", v1.getRootUrl());
        result.addV1OnlyField("adminUrl", v1.getAdminUrl());
        result.addV1OnlyField("surrogateAuthRequired", v1.isSurrogateAuthRequired());
        result.addV1OnlyField("alwaysDisplayInConsole", v1.isAlwaysDisplayInConsole());
        result.addV1OnlyField("registrationAccessToken", v1.getRegistrationAccessToken());
        result.addV1OnlyField("notBefore", v1.getNotBefore());
        result.addV1OnlyField("bearerOnly", v1.isBearerOnly());
        result.addV1OnlyField("consentRequired", v1.isConsentRequired());
        result.addV1OnlyField("authorizationServicesEnabled", v1.getAuthorizationServicesEnabled());
        result.addV1OnlyField("publicClient", v1.isPublicClient());
        result.addV1OnlyField("fullScopeAllowed", v1.isFullScopeAllowed());
        result.addV1OnlyField("nodeReRegistrationTimeout", v1.getNodeReRegistrationTimeout());
        result.addV1OnlyField("registeredNodes", v1.getRegisteredNodes());
        result.addV1OnlyField("protocolMappers", v1.getProtocolMappers());
        result.addV1OnlyField("defaultClientScopes", v1.getDefaultClientScopes());
        result.addV1OnlyField("optionalClientScopes", v1.getOptionalClientScopes());
        result.addV1OnlyField("authorizationSettings", v1.getAuthorizationSettings());
        result.addV1OnlyField("access", v1.getAccess());
        result.addV1OnlyField("origin", v1.getOrigin());
        result.addV1OnlyField("authenticationFlowBindingOverrides", v1.getAuthenticationFlowBindingOverrides());
    }

    private static String determineAuthMethod(ClientRepresentation v1) {
        if (Boolean.TRUE.equals(v1.isPublicClient())) {
            return "none";
        }
        String authenticatorType = v1.getClientAuthenticatorType();
        if (authenticatorType != null) {
            return authenticatorType;
        }
        return v1.getSecret() != null ? "client-secret" : null;
    }

    private static Boolean parseBooleanAttribute(String value) {
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }

    private static void compareField(ComparisonResult result, String fieldName, Object v1Value, Object v2Value) {
        if (Objects.equals(v1Value, v2Value)) {
            result.addMatch(fieldName, v1Value);
        } else if (v1Value == null && isEmptyCollection(v2Value)) {
            result.addMatch(fieldName, "null/empty");
        } else if (v2Value == null && isEmptyCollection(v1Value)) {
            result.addMatch(fieldName, "null/empty");
        } else {
            result.addMismatch(fieldName, v1Value, v2Value);
        }
    }

    private static boolean isEmptyCollection(Object value) {
        if (value instanceof Collection<?> c) {
            return c.isEmpty();
        }
        if (value instanceof Map<?, ?> m) {
            return m.isEmpty();
        }
        return false;
    }

    private static <T> void compareCollectionAsSet(ComparisonResult result, String fieldName, 
            Collection<T> v1Collection, Collection<T> v2Collection) {
        Set<T> v1Set = v1Collection != null ? new HashSet<>(v1Collection) : new HashSet<>();
        Set<T> v2Set = v2Collection != null ? new HashSet<>(v2Collection) : new HashSet<>();
        
        if (Objects.equals(v1Set, v2Set)) {
            result.addMatch(fieldName, v1Set.isEmpty() ? "empty" : v1Set);
        } else {
            result.addMismatch(fieldName, v1Set, v2Set);
        }
    }

    /**
     * Asserts that v1 and v2 representations match on all comparable fields.
     * @throws AssertionError if there are mismatches
     */
    public static void assertMatches(ClientRepresentation v1, BaseClientRepresentation v2) {
        ComparisonResult result = compare(v1, v2);
        if (!result.allMatch()) {
            throw new AssertionError("V1 and V2 representations do not match:\n" + result);
        }
    }
}
