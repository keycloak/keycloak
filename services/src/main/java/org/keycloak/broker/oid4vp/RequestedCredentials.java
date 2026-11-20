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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.keycloak.OID4VCConstants;
import org.keycloak.broker.oid4vp.mappers.ClaimPath;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * The credentials the verifier requests from the wallet and the claim that links the user. The
 * DCQL query is generated from it together with the identity provider mappers, and the wallet
 * response is verified against it.
 *
 * @param credentials the requested credentials, each becoming one credential query
 * @param credentialSets the combinations of credentials the wallet may present, all credentials are
 *        required if there are none
 * @param principal the credential and claim that become the brokered user id
 */
public record RequestedCredentials(List<Credential> credentials, List<CredentialSet> credentialSets,
        Principal principal) {

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]+");

    /**
     * @param id the DCQL credential id, also the key of its presentation in the vp_token
     * @param format the credential format, only {@value OID4VCConstants#FORMAT_SD_JWT_VC} for now
     * @param types the accepted credential types, the vct values for SD JWT VC
     * @param trustMaterialIdps aliases of the trust material identity providers trusted to issue it
     */
    public record Credential(String id, String format, List<String> types, List<String> trustMaterialIdps) {
    }

    /**
     * @param options the alternatives, each a list of credential ids the wallet presents together
     * @param required whether the login fails if no option is presented, true if not set
     * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6.2">OID4VP 1.0 §6.2 Credential Set Query</a>
     */
    public record CredentialSet(List<List<String>> options, Boolean required) {

        public boolean isSatisfiedBy(Set<String> presentedIds) {
            return options.stream().anyMatch(presentedIds::containsAll);
        }
    }

    /**
     * @param credential the id of the credential holding the claim. At least one required credential
     *        set must contain it in every option
     * @param claim the claim path in the dot notation of the mapper claims
     */
    public record Principal(String credential, String claim) {

        public ClaimPath claimPath() {
            return ClaimPath.parse(claim);
        }
    }

    /**
     * Parses and validates the credentials. Unknown members are rejected, so a typo does not
     * silently change what is requested.
     *
     * @throws IllegalArgumentException if the credentials are invalid
     */
    public static RequestedCredentials parse(String json) {
        if (StringUtil.isBlank(json)) {
            throw new IllegalArgumentException("The requested credentials are required");
        }
        RequestedCredentials requested;
        try {
            requested = JsonSerialization.mapper.readerFor(RequestedCredentials.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(json);
        } catch (UnrecognizedPropertyException e) {
            throw new IllegalArgumentException(
                    "The requested credentials member '" + e.getPropertyName() + "' is not supported", e);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "The requested credentials are not valid: " + e.getOriginalMessage(), e);
        }
        if (requested == null) {
            throw new IllegalArgumentException("The requested credentials are required");
        }
        requested.validate();
        return requested;
    }

    public Optional<Credential> credential(String id) {
        return credentials.stream().filter(credential -> credential.id().equals(id)).findFirst();
    }

    /**
     * Whether the presented credentials satisfy every required credential set, or contain all
     * credentials if there are no credential sets.
     */
    public boolean isSatisfiedBy(Set<String> presentedIds) {
        if (credentialSets == null) {
            return credentials.stream().map(Credential::id).allMatch(presentedIds::contains);
        }
        return credentialSets.stream().filter(RequestedCredentials::isRequired).allMatch(set -> set.isSatisfiedBy(presentedIds));
    }

    /**
     * Whether every presentation that satisfies the query carries the principal credential, which
     * holds when it is part of every option of at least one required credential set.
     */
    @JsonIgnore
    public boolean isPrincipalAlwaysPresented() {
        return credentialSets == null || credentialSets.stream().filter(RequestedCredentials::isRequired)
                .anyMatch(set -> set.options().stream().allMatch(option -> option.contains(principal.credential())));
    }

    private void validate() {
        if (credentials == null || credentials.isEmpty() || credentials.contains(null)) {
            throw new IllegalArgumentException("At least one credential must be requested");
        }
        Set<String> ids = new HashSet<>();
        for (Credential credential : credentials) {
            validate(credential);
            if (!ids.add(credential.id())) {
                throw new IllegalArgumentException("The credential id '" + credential.id() + "' is not unique");
            }
        }

        if (principal == null || StringUtil.isBlank(principal.credential())) {
            throw new IllegalArgumentException("The principal credential is required");
        }
        Optional<Credential> principalCredential = credential(principal.credential());
        if (principalCredential.isEmpty()) {
            throw new IllegalArgumentException(
                    "The principal refers to the unknown credential '" + principal.credential() + "'");
        }
        validatePrincipalTypes(principalCredential.get().types());
        if (credentialSets != null) {
            validateCredentialSets(ids);
        }
        if (StringUtil.isBlank(principal.claim()) || principal.claimPath() == null) {
            throw new IllegalArgumentException("The principal claim '" + principal.claim() + "' is not a valid claim path");
        }
    }

    // The user is linked by vct#subject, which is ambiguous when one type starts with another type followed by '#'
    private static void validatePrincipalTypes(List<String> types) {
        for (String type : types) {
            for (String other : types) {
                if (type.startsWith(other + "#")) {
                    throw new IllegalArgumentException("The principal credential type '" + type
                            + "' must not start with the type '" + other + "' followed by '#'");
                }
            }
        }
    }

    private void validateCredentialSets(Set<String> ids) {
        if (credentialSets.isEmpty() || credentialSets.contains(null)) {
            throw new IllegalArgumentException("The credential sets must not be empty if present");
        }
        Set<String> referenced = new HashSet<>();
        for (CredentialSet set : credentialSets) {
            if (set.options() == null || set.options().isEmpty()) {
                throw new IllegalArgumentException("Every credential set needs at least one option");
            }
            for (List<String> option : set.options()) {
                if (option == null || option.isEmpty() || option.size() != new HashSet<>(option).size()) {
                    throw new IllegalArgumentException(
                            "Every option of a credential set needs at least one credential and no duplicates");
                }
                for (String id : option) {
                    if (!ids.contains(id)) {
                        throw new IllegalArgumentException("A credential set refers to the unknown credential '" + id + "'");
                    }
                }
                referenced.addAll(option);
            }
        }
        for (String id : ids) {
            if (!referenced.contains(id)) {
                throw new IllegalArgumentException("The credential '" + id + "' is not part of any credential set");
            }
        }
        if (credentialSets.stream().noneMatch(RequestedCredentials::isRequired)) {
            throw new IllegalArgumentException("At least one credential set must be required");
        }
    }

    private static boolean isRequired(CredentialSet set) {
        return !Boolean.FALSE.equals(set.required());
    }

    private static void validate(Credential credential) {
        String id = credential.id();
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "The credential id '" + id + "' must consist of letters, digits, '_' and '-'");
        }
        if (!OID4VCConstants.FORMAT_SD_JWT_VC.equals(credential.format())) {
            throw new IllegalArgumentException("The format '" + credential.format() + "' of the credential '" + id
                    + "' is not supported, only " + OID4VCConstants.FORMAT_SD_JWT_VC + " is");
        }
        if (credential.types() == null || credential.types().isEmpty()
                || credential.types().stream().anyMatch(StringUtil::isBlank)) {
            throw new IllegalArgumentException("The credential '" + id + "' needs at least one type and no blank ones");
        }
        // Aliases are not resolved here, since a realm import may create the trust material identity
        // providers after this one. Unknown or disabled ones are skipped at login.
        if (credential.trustMaterialIdps() == null || credential.trustMaterialIdps().isEmpty()
                || credential.trustMaterialIdps().stream().anyMatch(StringUtil::isBlank)) {
            throw new IllegalArgumentException("The credential '" + id
                    + "' needs at least one trust material identity provider and no blank ones");
        }
    }
}
