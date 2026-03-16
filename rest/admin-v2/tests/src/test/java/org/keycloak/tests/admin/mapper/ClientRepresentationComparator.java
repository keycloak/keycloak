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

import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_ALLOW_ECP_FLOW;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_ASSERTION_SIGNATURE;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_AUTHNSTATEMENT;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_CANONICALIZATION_METHOD_ATTRIBUTE;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_FORCE_POST_BINDING;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_SERVER_SIGNATURE;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE;

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
        result.addV2OnlyField("roles", nullIfEmpty(v2.getRoles()));
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
        String expectedMethod = determineExpectedAuthMethod();
        compare("clientAuthenticatorType→auth.method", expectedMethod, getAuthField(oidc, OIDCClientRepresentation.Auth::getMethod));
        compare("secret→auth.secret", v1.getSecret(), getAuthField(oidc, OIDCClientRepresentation.Auth::getSecret));
    }

    private String determineExpectedAuthMethod() {
        if (Boolean.TRUE.equals(v1.isPublicClient())) {
            return "none";
        }
        if (v1.getClientAuthenticatorType() != null) {
            return v1.getClientAuthenticatorType();
        }
        if (v1.getSecret() != null) {
            return "client-secret";
        }
        return null;
    }

    private <T> T getAuthField(OIDCClientRepresentation oidc, Function<OIDCClientRepresentation.Auth, T> getter) {
        return oidc.getAuth() != null ? getter.apply(oidc.getAuth()) : null;
    }

    private void compareSAMLFields(SAMLClientRepresentation saml) {
        compare("frontchannelLogout→frontChannelLogout", v1.isFrontchannelLogout(), saml.getFrontChannelLogout());

        Map<String, String> attrs = v1.getAttributes();
        if (attrs == null) return;

        compare("attr[saml_name_id_format]→nameIdFormat", attrs.get(SAML_NAME_ID_FORMAT_ATTRIBUTE), saml.getNameIdFormat());
        compareSamlBoolean(SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, "forceNameIdFormat", saml.getForceNameIdFormat());
        compareSamlBoolean(SAML_AUTHNSTATEMENT, "includeAuthnStatement", saml.getIncludeAuthnStatement());
        compareSamlBoolean(SAML_SERVER_SIGNATURE, "signDocuments", saml.getSignDocuments());
        compareSamlBoolean(SAML_ASSERTION_SIGNATURE, "signAssertions", saml.getSignAssertions());
        compareSamlBoolean(SAML_CLIENT_SIGNATURE_ATTRIBUTE, "clientSignatureRequired", saml.getClientSignatureRequired());
        compareSamlBoolean(SAML_FORCE_POST_BINDING, "forcePostBinding", saml.getForcePostBinding());
        compareSamlBoolean(SAML_ALLOW_ECP_FLOW, "allowEcpFlow", saml.getAllowEcpFlow());
        compare("attr[saml.signature.algorithm]→signatureAlgorithm", attrs.get(SAML_SIGNATURE_ALGORITHM), saml.getSignatureAlgorithm());
        compare("attr[saml_signature_canonicalization_method]→signatureCanonicalizationMethod", attrs.get(SAML_CANONICALIZATION_METHOD_ATTRIBUTE), saml.getSignatureCanonicalizationMethod());
        compare("attr[saml.signing.certificate]→signingCertificate", attrs.get(SAML_SIGNING_CERTIFICATE_ATTRIBUTE), saml.getSigningCertificate());
    }

    private void compareSamlBoolean(String attrName, String v2FieldName, Boolean v2Value) {
        String attrValue = v1.getAttributes() != null ? v1.getAttributes().get(attrName) : null;
        Boolean v1Value = attrValue != null ? Boolean.parseBoolean(attrValue) : null;
        compare("attr[" + attrName + "]→" + v2FieldName, v1Value, v2Value);
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
        private final List<String> v2OnlyFields = new ArrayList<>();

        public void addMismatch(String fieldName, Object v1Value, Object v2Value) {
            mismatches.add(String.format("%s: v1='%s' vs v2='%s'", fieldName, v1Value, v2Value));
        }

        public void addMatch(String fieldName, Object value) {
            matches.add(String.format("%s: '%s'", fieldName, value));
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
        public List<String> getV2OnlyFields() { return v2OnlyFields; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            appendSection(sb, "MISMATCHES", mismatches);
            appendSection(sb, "MATCHES", matches);
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
