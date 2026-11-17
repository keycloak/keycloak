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

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.keycloak.OID4VCConstants;
import org.keycloak.broker.oid4vp.mappers.DcqlQueryContributor;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6">OID4VP 1.0 §6 — Digital Credentials Query Language</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DcqlQuery(
        List<CredentialQuery> credentials,
        @JsonProperty("credential_sets") List<CredentialSetQuery> credentialSets) {

    private static final String INFERRED_CREDENTIAL_ID = "credential";

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]+");

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
            List<ClaimsQuery> claims,
            @JsonProperty("claim_sets") List<List<String>> claimSets) {
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
     * Each path element is a string for an object member, null for all array elements or a non
     * negative integer for an array index (OID4VP 1.0 §7).
     *
     * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6.3">OID4VP 1.0 §6.3 — Claims Query</a>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClaimsQuery(String id, List<Object> path, List<Object> values) {
    }

    /**
     * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6.2">OID4VP 1.0 §6.2 — Credential Set Query</a>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CredentialSetQuery(List<List<String>> options, Boolean required) {
    }

    /**
     * Parses and validates a query. Unknown members are rejected because they would not be sent to
     * the wallet.
     *
     * @throws IllegalArgumentException if the query is invalid
     */
    public static DcqlQuery parse(String json) {
        DcqlQuery query;
        try {
            query = JsonSerialization.mapper.readerFor(DcqlQuery.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(json);
        } catch (UnrecognizedPropertyException e) {
            throw new IllegalArgumentException(
                    "The DCQL query member '" + e.getPropertyName() + "' is not supported", e);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The DCQL query is not valid: " + e.getOriginalMessage(), e);
        }
        if (query == null) {
            throw new IllegalArgumentException("The DCQL query is empty");
        }
        query.validate();
        return query;
    }

    /**
     * Builds the query from the claims the mappers request, in the order of the mapper names, plus
     * the principal attribute. The wallet may present a credential of any of the given types.
     *
     * @throws IllegalStateException if no credential type is given
     */
    public static DcqlQuery fromMappers(KeycloakSessionFactory sessionFactory,
            Stream<IdentityProviderMapperModel> mappers, List<String> credentialTypes, String principalAttribute) {
        if (credentialTypes.isEmpty()) {
            throw new IllegalStateException("No credential types configured on the identity provider");
        }
        // A claim must not be requested twice (OID4VP 1.0 §6.1), even if several mappers read it.
        Map<List<Object>, ClaimsQuery> claims = new LinkedHashMap<>();
        mappers.sorted(Comparator.comparing(IdentityProviderMapperModel::getName)).forEach(mapper -> {
            IdentityProviderMapper target = (IdentityProviderMapper) sessionFactory
                    .getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
            if (target instanceof DcqlQueryContributor contributor) {
                contributor.requestedClaims(mapper).forEach(claim -> claims.putIfAbsent(claim.path(), claim));
            }
        });
        // The principal attribute names a top level claim, see requirePrincipalClaim.
        if (StringUtil.isNotBlank(principalAttribute)) {
            List<Object> principalPath = List.of(principalAttribute);
            claims.putIfAbsent(principalPath, new ClaimsQuery(null, principalPath, null));
        }

        return new DcqlQuery(List.of(new CredentialQuery(INFERRED_CREDENTIAL_ID, OID4VCConstants.FORMAT_SD_JWT_VC,
                null, new Meta(List.copyOf(credentialTypes)), null, null, List.copyOf(claims.values()), null)), null);
    }

    private void validate() {
        requireNonEmpty(credentials, "credentials");
        // The response handling accepts a single presentation, see extractCredential.
        if (credentials.size() > 1) {
            throw new IllegalArgumentException("Requesting more than one credential is not supported");
        }
        CredentialQuery credential = credentials.get(0);
        requireId(credential.id(), "credential id");
        validate(credential);
        if (credentialSets != null) {
            requireNonEmpty(credentialSets, "credential_sets");
            for (CredentialSetQuery credentialSet : credentialSets) {
                requireOptions(credentialSet.options(), Set.of(credential.id()), "credential_sets", "credential id");
            }
        }
    }

    private static void validate(CredentialQuery credential) {
        String id = credential.id();
        if (Boolean.TRUE.equals(credential.multiple())) {
            throw new IllegalArgumentException("Requesting multiple presentations of the credential '" + id
                    + "' is not supported");
        }
        if (!OID4VCConstants.FORMAT_SD_JWT_VC.equals(credential.format())) {
            throw new IllegalArgumentException("The format '" + credential.format() + "' of the credential '" + id
                    + "' is not supported, only " + OID4VCConstants.FORMAT_SD_JWT_VC + " is");
        }
        if (credential.meta() == null) {
            throw new IllegalArgumentException("The credential '" + id + "' has no meta");
        }
        requireNonEmpty(credential.meta().vctValues(), "vct_values of the credential '" + id + "'");
        if (credential.meta().vctValues().stream().anyMatch(StringUtil::isBlank)) {
            throw new IllegalArgumentException("The vct_values of the credential '" + id + "' contain a blank value");
        }
        if (credential.trustedAuthorities() != null) {
            requireNonEmpty(credential.trustedAuthorities(), "trusted_authorities of the credential '" + id + "'");
            for (TrustedAuthoritiesQuery authority : credential.trustedAuthorities()) {
                if (StringUtil.isBlank(authority.type())) {
                    throw new IllegalArgumentException("A trusted authority of the credential '" + id
                            + "' has no type");
                }
                requireNonEmpty(authority.values(), "values of a trusted authority of the credential '" + id + "'");
            }
        }

        Set<String> claimIds = new HashSet<>();
        if (credential.claims() != null) {
            requireNonEmpty(credential.claims(), "claims of the credential '" + id + "'");
            Set<List<Object>> paths = new HashSet<>();
            for (ClaimsQuery claim : credential.claims()) {
                requirePath(claim.path(), id);
                if (!paths.add(claim.path())) {
                    throw new IllegalArgumentException("The credential '" + id + "' requests the claim "
                            + claim.path() + " twice");
                }
                if (claim.id() != null) {
                    requireId(claim.id(), "claim id");
                    if (!claimIds.add(claim.id())) {
                        throw new IllegalArgumentException("The claim id '" + claim.id() + "' of the credential '"
                                + id + "' is not unique");
                    }
                }
                if (claim.values() != null) {
                    requireValues(claim.values(), id);
                }
            }
        }
        if (credential.claimSets() != null) {
            if (credential.claims() == null) {
                throw new IllegalArgumentException("The credential '" + id + "' has claim_sets but no claims");
            }
            if (credential.claims().stream().anyMatch(claim -> claim.id() == null)) {
                throw new IllegalArgumentException("Every claim of the credential '" + id
                        + "' needs an id, since it has claim_sets");
            }
            requireOptions(credential.claimSets(), claimIds, "claim_sets of the credential '" + id + "'", "claim id");
        }
    }

    private static void requirePath(List<Object> path, String credentialId) {
        // Null selects all array elements, so requireNonEmpty would reject valid paths.
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("The path of a claim of the credential '" + credentialId
                    + "' must not be empty");
        }
        for (Object element : path) {
            boolean valid = element == null || element instanceof String
                    || (isInteger(element) && ((Number) element).longValue() >= 0);
            if (!valid) {
                throw new IllegalArgumentException("The claim path " + path + " of the credential '" + credentialId
                        + "' may only contain strings, nulls and non negative integers");
            }
        }
    }

    private static void requireValues(List<Object> values, String credentialId) {
        requireNonEmpty(values, "values of a claim of the credential '" + credentialId + "'");
        for (Object value : values) {
            if (!(value instanceof String || value instanceof Boolean || isInteger(value))) {
                throw new IllegalArgumentException("The claim values " + values + " of the credential '"
                        + credentialId + "' may only contain strings, integers and booleans");
            }
        }
    }

    private static void requireOptions(List<List<String>> options, Set<String> knownIds, String name,
            String idName) {
        requireNonEmpty(options, name);
        for (List<String> option : options) {
            requireNonEmpty(option, "options of the " + name);
            for (String optionId : option) {
                if (!knownIds.contains(optionId)) {
                    throw new IllegalArgumentException("The " + name + " reference the unknown " + idName + " '"
                            + optionId + "'");
                }
            }
        }
    }

    private static void requireId(String id, String name) {
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("The " + name + " '" + id
                    + "' must consist of letters, digits, '_' and '-'");
        }
    }

    private static void requireNonEmpty(List<?> list, String name) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("The " + name + " must not be empty");
        }
        if (list.contains(null)) {
            throw new IllegalArgumentException("The " + name + " must not contain null");
        }
    }

    private static boolean isInteger(Object value) {
        return value instanceof Integer || value instanceof Long;
    }
}
