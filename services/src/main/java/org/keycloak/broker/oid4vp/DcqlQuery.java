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

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.OID4VCConstants;
import org.keycloak.broker.oid4vp.mappers.ClaimPath;
import org.keycloak.broker.oid4vp.mappers.DcqlQueryContributor;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.common.VerificationException;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.sdjwt.consumer.PresentationRequirements;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

/**
 * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6">OID4VP 1.0 §6 — Digital Credentials Query Language</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DcqlQuery(
        List<CredentialQuery> credentials,
        @JsonProperty("credential_sets") List<CredentialSetQuery> credentialSets) {

    private static final Logger logger = Logger.getLogger(DcqlQuery.class);

    /**
     * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6.1">OID4VP 1.0 §6.1 — Credential Query</a>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CredentialQuery(
            String id,
            String format,
            Boolean multiple,
            Meta meta,
            @JsonProperty("trusted_authorities") List<TrustedAuthoritiesQuery> trustedAuthorities,
            @JsonProperty("require_cryptographic_holder_binding") Boolean requireCryptographicHolderBinding,
            // Claims are optional, but a present claims array must not be empty (OID4VP 1.0 §6.1).
            // Without claims only the always disclosed claims are presented.
            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ClaimsQuery> claims,
            @JsonProperty("claim_sets") List<List<String>> claimSets) implements PresentationRequirements {

        /**
         * Requires the presented credential to have one of the requested types and to disclose the
         * requested claims (OID4VP 1.0 §6.4.1).
         *
         * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-8.6">OID4VP 1.0 §8.6 — VP Token Validation</a>
         */
        @Override
        public void checkIfSatisfiedBy(JsonNode disclosedPayload) throws VerificationException {
            JsonNode type = disclosedPayload.get(OID4VCConstants.CLAIM_NAME_VCT);
            String vct = type != null && type.isTextual() ? type.textValue() : null;
            if (!meta.vctValues().contains(vct)) {
                throw new VerificationException("The credential '" + id + "' has the type '" + vct
                        + "', which is not requested");
            }
            // TODO once claim sets are requested, require the claims of one claim set option instead of all claims.
            for (ClaimsQuery claim : claims) {
                if (!claim.isDisclosedIn(disclosedPayload)) {
                    throw new VerificationException("The credential '" + id + "' does not disclose the requested claim '"
                            + claim.path() + "'");
                }
            }
        }
    }

    /**
     * The accepted credential types (vct) of an SD JWT VC credential.
     *
     * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#appendix-B.3.5">OID4VP 1.0 §B.3.5 — SD JWT VC Parameters in the meta parameter</a>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Meta(@JsonProperty("vct_values") List<String> vctValues) {
    }

    /**
     * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6.1.1">OID4VP 1.0 §6.1.1 — Trusted Authorities Query</a>
     */
    public record TrustedAuthoritiesQuery(String type, List<String> values) {
    }

    /**
     * The path is serialized as a DCQL claims path pointer (OID4VP 1.0 §7).
     *
     * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6.3">OID4VP 1.0 §6.3 — Claims Query</a>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClaimsQuery(String id, ClaimPath path, List<Object> values) {

        /**
         * Whether the path selects at least one claim of the disclosed payload (OID4VP 1.0 §7.1.1).
         * A null value or an element of the wrong kind selects nothing, as in {@link ClaimPath#select}.
         */
        public boolean isDisclosedIn(JsonNode disclosedPayload) {
            return !path.select(disclosedPayload).isEmpty();
        }
    }

    /**
     * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6.2">OID4VP 1.0 §6.2 — Credential Set Query</a>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CredentialSetQuery(List<List<String>> options, Boolean required) {
    }

    /**
     * Builds one credential query per requested credential, holding the claims the mappers of that
     * credential read, in the order of the mapper names, and the principal claim on its credential.
     * The credential sets are taken as configured. A mapper that refers to a credential that is not
     * requested is skipped.
     */
    public static DcqlQuery fromMappers(KeycloakSessionFactory sessionFactory,
            Stream<IdentityProviderMapperModel> mappers, RequestedCredentials requested) {
        // A claim must not be requested twice (OID4VP 1.0 §6.1), even if several mappers read it.
        Map<String, Map<List<ClaimPath.Step>, ClaimsQuery>> claimsByCredential = new LinkedHashMap<>();
        requested.credentials().forEach(credential -> claimsByCredential.put(credential.id(), new LinkedHashMap<>()));
        mappers.sorted(Comparator.comparing(IdentityProviderMapperModel::getName)).forEach(mapper -> {
            IdentityProviderMapper target = (IdentityProviderMapper) sessionFactory
                    .getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
            if (target instanceof DcqlQueryContributor contributor) {
                List<ClaimsQuery> requestedClaims = contributor.requestedClaims(mapper);
                if (requestedClaims.isEmpty()) {
                    return;
                }
                Map<List<ClaimPath.Step>, ClaimsQuery> claims = claimsByCredential.get(contributor.credentialId(mapper));
                if (claims == null) {
                    logger.warnf("Skipping the mapper '%s', which refers to the credential '%s' that is not requested",
                            mapper.getName(), contributor.credentialId(mapper));
                    return;
                }
                requestedClaims.forEach(claim -> claims.putIfAbsent(claim.path().steps(), claim));
            }
        });
        ClaimPath principalPath = requested.principal().claimPath();
        claimsByCredential.get(requested.principal().credential())
                .putIfAbsent(principalPath.steps(), new ClaimsQuery(null, principalPath, null));

        List<CredentialQuery> credentials = requested.credentials().stream()
                .map(credential -> {
                    Collection<ClaimsQuery> claims = claimsByCredential.get(credential.id()).values();
                    return new CredentialQuery(credential.id(), credential.format(), null,
                            new Meta(List.copyOf(credential.types())), null, null, List.copyOf(claims), null);
                })
                .toList();
        List<CredentialSetQuery> credentialSets = requested.credentialSets() == null ? null
                : requested.credentialSets().stream()
                        .map(set -> new CredentialSetQuery(set.options(), set.required()))
                        .toList();
        return new DcqlQuery(credentials, credentialSets);
    }
}
