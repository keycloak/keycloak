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
import java.util.function.Function;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation.Flow;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;

/**
 * Utility class for comparing v1 ClientRepresentation against v2 client representations.
 */
public class ClientRepresentationComparator {

    private final ComparisonResult result = new ComparisonResult();
    private final ClientRepresentation v1;
    private final BaseClientRepresentation v2;

    private ClientRepresentationComparator(ClientRepresentation v1, BaseClientRepresentation v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    public static ComparisonResult compare(ClientRepresentation v1, BaseClientRepresentation v2) {
        return new ClientRepresentationComparator(v1, v2).execute();
    }

    public static void assertMatches(ClientRepresentation v1, BaseClientRepresentation v2) {
        ComparisonResult result = compare(v1, v2);
        if (!result.allMatch()) {
            throw new AssertionError("V1 and V2 representations do not match:\n" + result);
        }
    }

    private ComparisonResult execute() {
        compareBaseFields();

        if (v2 instanceof OIDCClientRepresentation oidc) {
            compareOIDCFields(oidc);
        } else if (v2 instanceof SAMLClientRepresentation saml) {
            compareSAMLFields(saml);
        }

        recordV1OnlyFields();
        return result;
    }

    private void compareBaseFields() {
        compare("clientId", v1.getClientId(), v2.getClientId());
        compare("name→displayName", v1.getName(), v2.getDisplayName());
        compare("description", v1.getDescription(), v2.getDescription());
        compare("enabled", v1.isEnabled(), v2.getEnabled());
        compare("baseUrl→appUrl", v1.getBaseUrl(), v2.getAppUrl());
        compare("protocol", v1.getProtocol(), v2.getProtocol());
        compareAsSet("redirectUris", v1.getRedirectUris(), v2.getRedirectUris());
    }

    private void compareOIDCFields(OIDCClientRepresentation oidc) {
        compareAsSet("webOrigins", v1.getWebOrigins(), oidc.getWebOrigins());
        compare("flows→loginFlows", buildExpectedFlows(), oidc.getLoginFlows());
        compareAuth(oidc);
        result.addV2OnlyField("auth.certificate", getAuthField(oidc, OIDCClientRepresentation.Auth::getCertificate));
        result.addV2OnlyField("serviceAccountRoles", nullIfEmpty(oidc.getServiceAccountRoles()));
    }

    private Set<Flow> buildExpectedFlows() {
        Set<Flow> flows = new HashSet<>();
        if (Boolean.TRUE.equals(v1.isStandardFlowEnabled())) flows.add(Flow.STANDARD);
        if (Boolean.TRUE.equals(v1.isImplicitFlowEnabled())) flows.add(Flow.IMPLICIT);
        if (Boolean.TRUE.equals(v1.isDirectAccessGrantsEnabled())) flows.add(Flow.DIRECT_GRANT);
        if (Boolean.TRUE.equals(v1.isServiceAccountsEnabled())) flows.add(Flow.SERVICE_ACCOUNT);
        return flows;
    }

    private void compareAuth(OIDCClientRepresentation oidc) {
        String expectedMethod = Boolean.TRUE.equals(v1.isPublicClient()) ? "none"
                : v1.getClientAuthenticatorType() != null ? v1.getClientAuthenticatorType()
                : v1.getSecret() != null ? "client-secret" : null;

        compare("clientAuthenticatorType→auth.method", expectedMethod, getAuthField(oidc, OIDCClientRepresentation.Auth::getMethod));
        compare("secret→auth.secret", v1.getSecret(), getAuthField(oidc, OIDCClientRepresentation.Auth::getSecret));
    }

    private <T> T getAuthField(OIDCClientRepresentation oidc, Function<OIDCClientRepresentation.Auth, T> getter) {
        return oidc.getAuth() != null ? getter.apply(oidc.getAuth()) : null;
    }

    private void compareSAMLFields(SAMLClientRepresentation saml) {
        compare("frontchannelLogout→frontChannelLogout", v1.isFrontchannelLogout(), saml.getFrontChannelLogout());

        Map<String, String> attrs = v1.getAttributes();
        if (attrs == null) return;

        compare("attr[saml_name_id_format]→nameIdFormat", attrs.get("saml_name_id_format"), saml.getNameIdFormat());
        compareSamlBoolean("saml_force_name_id_format", "forceNameIdFormat", saml.getForceNameIdFormat());
        compareSamlBoolean("saml.authnstatement", "includeAuthnStatement", saml.getIncludeAuthnStatement());
        compareSamlBoolean("saml.server.signature", "signDocuments", saml.getSignDocuments());
        compareSamlBoolean("saml.assertion.signature", "signAssertions", saml.getSignAssertions());
        compareSamlBoolean("saml.client.signature", "clientSignatureRequired", saml.getClientSignatureRequired());
        compareSamlBoolean("saml.force.post.binding", "forcePostBinding", saml.getForcePostBinding());
        compareSamlBoolean("saml.allow.ecp.flow", "allowEcpFlow", saml.getAllowEcpFlow());
        compare("attr[saml.signature.algorithm]→signatureAlgorithm", attrs.get("saml.signature.algorithm"), saml.getSignatureAlgorithm());
        compare("attr[saml_signature_canonicalization_method]→signatureCanonicalizationMethod", attrs.get("saml_signature_canonicalization_method"), saml.getSignatureCanonicalizationMethod());
        compare("attr[saml.signing.certificate]→signingCertificate", attrs.get("saml.signing.certificate"), saml.getSigningCertificate());
    }

    private void compareSamlBoolean(String attrName, String v2FieldName, Boolean v2Value) {
        String attrValue = v1.getAttributes() != null ? v1.getAttributes().get(attrName) : null;
        Boolean v1Value = attrValue != null ? Boolean.parseBoolean(attrValue) : null;
        compare("attr[" + attrName + "]→" + v2FieldName, v1Value, v2Value);
    }

    private void recordV1OnlyFields() {
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

    private void compare(String fieldName, Object v1Value, Object v2Value) {
        if (Objects.equals(v1Value, v2Value) || bothNullOrEmpty(v1Value, v2Value)) {
            result.addMatch(fieldName, v1Value != null ? v1Value : "null/empty");
        } else {
            result.addMismatch(fieldName, v1Value, v2Value);
        }
    }

    private <T> void compareAsSet(String fieldName, Collection<T> v1Coll, Collection<T> v2Coll) {
        Set<T> v1Set = v1Coll != null ? new HashSet<>(v1Coll) : Set.of();
        Set<T> v2Set = v2Coll != null ? new HashSet<>(v2Coll) : Set.of();

        if (v1Set.equals(v2Set)) {
            result.addMatch(fieldName, v1Set.isEmpty() ? "empty" : v1Set);
        } else {
            result.addMismatch(fieldName, v1Set, v2Set);
        }
    }

    private static boolean bothNullOrEmpty(Object a, Object b) {
        return (a == null && isEmpty(b)) || (b == null && isEmpty(a));
    }

    private static boolean isEmpty(Object value) {
        if (value instanceof Collection<?> c) return c.isEmpty();
        if (value instanceof Map<?, ?> m) return m.isEmpty();
        return false;
    }

    private static <T> T nullIfEmpty(Collection<T> collection) {
        return collection != null && !collection.isEmpty() ? (T) collection : null;
    }

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

        public List<String> getMismatches() { return mismatches; }
        public List<String> getMatches() { return matches; }
        public List<String> getV1OnlyFields() { return v1OnlyFields; }
        public List<String> getV2OnlyFields() { return v2OnlyFields; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            appendSection(sb, "MISMATCHES", mismatches);
            appendSection(sb, "MATCHES", matches);
            appendSection(sb, "V1-ONLY FIELDS (not mapped to v2)", v1OnlyFields);
            appendSection(sb, "V2-ONLY FIELDS (not mapped from v1)", v2OnlyFields);
            return sb.toString();
        }

        private void appendSection(StringBuilder sb, String title, List<String> items) {
            if (!items.isEmpty()) {
                sb.append(title).append(":\n");
                items.forEach(item -> sb.append("  - ").append(item).append("\n"));
            }
        }
    }
}
