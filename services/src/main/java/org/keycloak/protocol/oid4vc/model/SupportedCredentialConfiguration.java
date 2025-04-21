/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A supported credential, as used in the Credentials Issuer Metadata in OID4VCI
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-issuer-metadata}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportedCredentialConfiguration {

    public static final String DOT_SEPARATOR = ".";

    @JsonIgnore
    private static final String FORMAT_KEY = "format";
    @JsonIgnore
    private static final String SCOPE_KEY = "scope";
    @JsonIgnore
    private static final String CRYPTOGRAPHIC_BINDING_METHODS_SUPPORTED_KEY = "cryptographic_binding_methods_supported";
    @JsonIgnore
    private static final String CRYPTOGRAPHIC_SUITES_SUPPORTED_KEY = "cryptographic_suites_supported";
    @JsonIgnore
    private static final String CREDENTIAL_SIGNING_ALG_VALUES_SUPPORTED_KEY = "credential_signing_alg_values_supported";
    @JsonIgnore
    private static final String DISPLAY_KEY = "display";
    @JsonIgnore
    private static final String PROOF_TYPES_SUPPORTED_KEY = "proof_types_supported";
    @JsonIgnore
    private static final String CLAIMS_KEY = "claims";
    @JsonIgnore
    public static final String VERIFIABLE_CREDENTIAL_TYPE_KEY = "vct";
    @JsonIgnore
    private static final String CREDENTIAL_DEFINITION_KEY = "credential_definition";
    @JsonIgnore
    public static final String CREDENTIAL_BUILD_CONFIG_KEY = "credential_build_config";

    private String id;

    @JsonProperty(FORMAT_KEY)
    private String format;

    @JsonProperty(SCOPE_KEY)
    private String scope;

    @JsonProperty(CRYPTOGRAPHIC_BINDING_METHODS_SUPPORTED_KEY)
    private List<String> cryptographicBindingMethodsSupported;

    @JsonProperty(CRYPTOGRAPHIC_SUITES_SUPPORTED_KEY)
    private List<String> cryptographicSuitesSupported;

    @JsonProperty(CREDENTIAL_SIGNING_ALG_VALUES_SUPPORTED_KEY)
    private List<String> credentialSigningAlgValuesSupported;

    @JsonProperty(DISPLAY_KEY)
    private List<DisplayObject> display;

    @JsonProperty(VERIFIABLE_CREDENTIAL_TYPE_KEY)
    private String vct;

    @JsonProperty(CREDENTIAL_DEFINITION_KEY)
    private CredentialDefinition credentialDefinition;

    @JsonProperty(PROOF_TYPES_SUPPORTED_KEY)
    private ProofTypesSupported proofTypesSupported;

    @JsonProperty(CLAIMS_KEY)
    private Claims claims;

    // This is not a normative field for supported credential metadata,
    // but will allow configuring the issuance of the credential internally.
    @JsonIgnore
    private CredentialBuildConfig credentialBuildConfig;

    public String getFormat() {
        return format;
    }

    /**
     * Return the verifiable credential type. Sort of confusing in the specification.
     * For sdjwt, we have a "vct" claim.
     *   See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-request-6
     *
     * For iso mdl (not yet supported) we have a "doctype"
     *   See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-request-5
     *
     * For jwt_vc and ldp_vc, we will be inferring from the "credential_definition"
     *   See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-request-3
     *
     * @return
     */
    public VerifiableCredentialType deriveType() {
        if (Objects.equals(format, Format.SD_JWT_VC)) {
            return VerifiableCredentialType.from(vct);
        }
        return null;
    }

    public CredentialConfigId deriveConfiId() {
        return CredentialConfigId.from(id);
    }

    public SupportedCredentialConfiguration setFormat(String format) {
        this.format = format;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public SupportedCredentialConfiguration setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public List<String> getCryptographicBindingMethodsSupported() {
        return cryptographicBindingMethodsSupported;
    }

    public SupportedCredentialConfiguration setCryptographicBindingMethodsSupported(List<String> cryptographicBindingMethodsSupported) {
        this.cryptographicBindingMethodsSupported = Collections.unmodifiableList(cryptographicBindingMethodsSupported);
        return this;
    }

    public List<String> getCryptographicSuitesSupported() {
        return cryptographicSuitesSupported;
    }

    public SupportedCredentialConfiguration setCryptographicSuitesSupported(List<String> cryptographicSuitesSupported) {
        this.cryptographicSuitesSupported = Collections.unmodifiableList(cryptographicSuitesSupported);
        return this;
    }

    public List<DisplayObject> getDisplay() {
        return display;
    }

    public SupportedCredentialConfiguration setDisplay(List<DisplayObject> display) {
        this.display = display;
        return this;
    }

    public String getId() {
        return id;
    }

    public SupportedCredentialConfiguration setId(String id) {
        if (id.contains(".")) {
            throw new IllegalArgumentException("dots are not supported as part of the supported credentials id.");
        }
        this.id = id;
        return this;
    }

    public List<String> getCredentialSigningAlgValuesSupported() {
        return credentialSigningAlgValuesSupported;
    }

    public SupportedCredentialConfiguration setCredentialSigningAlgValuesSupported(List<String> credentialSigningAlgValuesSupported) {
        this.credentialSigningAlgValuesSupported = Collections.unmodifiableList(credentialSigningAlgValuesSupported);
        return this;
    }

    public Claims getClaims() {
        return claims;
    }

    public SupportedCredentialConfiguration setClaims(Claims claims) {
        this.claims = claims;
        return this;
    }

    public String getVct() {
        return vct;
    }

    public SupportedCredentialConfiguration setVct(String vct) {
        this.vct = vct;
        return this;
    }

    public CredentialDefinition getCredentialDefinition() {
        return credentialDefinition;
    }

    public SupportedCredentialConfiguration setCredentialDefinition(CredentialDefinition credentialDefinition) {
        this.credentialDefinition = credentialDefinition;
        return this;
    }

    public ProofTypesSupported getProofTypesSupported() {
        return proofTypesSupported;
    }

    public SupportedCredentialConfiguration setProofTypesSupported(ProofTypesSupported proofTypesSupported) {
        this.proofTypesSupported = proofTypesSupported;
        return this;
    }

    public CredentialBuildConfig getCredentialBuildConfig() {
        return credentialBuildConfig;
    }

    public SupportedCredentialConfiguration setCredentialBuildConfig(CredentialBuildConfig credentialBuildConfig) {
        this.credentialBuildConfig = credentialBuildConfig;
        return this;
    }

    public Map<String, String> toDotNotation() {
        Map<String, String> dotNotation = new HashMap<>();
        Optional.ofNullable(format).ifPresent(format -> dotNotation.put(id + DOT_SEPARATOR + FORMAT_KEY, format));
        Optional.ofNullable(vct).ifPresent(vct -> dotNotation.put(id + DOT_SEPARATOR + VERIFIABLE_CREDENTIAL_TYPE_KEY, vct));
        Optional.ofNullable(scope).ifPresent(scope -> dotNotation.put(id + DOT_SEPARATOR + SCOPE_KEY, scope));
        Optional.ofNullable(cryptographicBindingMethodsSupported).ifPresent(types ->
                dotNotation.put(id + DOT_SEPARATOR + CRYPTOGRAPHIC_BINDING_METHODS_SUPPORTED_KEY, String.join(",", cryptographicBindingMethodsSupported)));
        Optional.ofNullable(cryptographicSuitesSupported).ifPresent(types ->
                dotNotation.put(id + DOT_SEPARATOR + CRYPTOGRAPHIC_SUITES_SUPPORTED_KEY, String.join(",", cryptographicSuitesSupported)));
        Optional.ofNullable(cryptographicSuitesSupported).ifPresent(types ->
                dotNotation.put(id + DOT_SEPARATOR + CREDENTIAL_SIGNING_ALG_VALUES_SUPPORTED_KEY, String.join(",", credentialSigningAlgValuesSupported)));
        Optional.ofNullable(claims).ifPresent(c -> dotNotation.put(id + DOT_SEPARATOR + CLAIMS_KEY, c.toJsonString()));
        Optional.ofNullable(credentialDefinition).ifPresent(cdef -> dotNotation.put(id + DOT_SEPARATOR + CREDENTIAL_DEFINITION_KEY, cdef.toJsonString()));

        Optional.ofNullable(display)
                .ifPresent(d -> d.stream()
                        .filter(Objects::nonNull)
                        .forEach(o -> dotNotation.put(id + DOT_SEPARATOR + DISPLAY_KEY + DOT_SEPARATOR + d.indexOf(o), o.toJsonString())));

        Optional.ofNullable(proofTypesSupported)
                .ifPresent(p -> dotNotation.put(id + DOT_SEPARATOR + PROOF_TYPES_SUPPORTED_KEY, p.toJsonString()));

        Optional.ofNullable(credentialBuildConfig)
                .ifPresent(p -> dotNotation.putAll(credentialBuildConfig.toDotNotation()));

        return dotNotation;
    }

    public static SupportedCredentialConfiguration fromDotNotation(String credentialId, Map<String, String> dotNotated) {

        SupportedCredentialConfiguration supportedCredentialConfiguration = new SupportedCredentialConfiguration().setId(credentialId);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPARATOR + FORMAT_KEY)).ifPresent(supportedCredentialConfiguration::setFormat);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPARATOR + VERIFIABLE_CREDENTIAL_TYPE_KEY)).ifPresent(supportedCredentialConfiguration::setVct);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPARATOR + SCOPE_KEY)).ifPresent(supportedCredentialConfiguration::setScope);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPARATOR + CRYPTOGRAPHIC_BINDING_METHODS_SUPPORTED_KEY))
                .map(cbms -> cbms.split(","))
                .map(Arrays::asList)
                .ifPresent(supportedCredentialConfiguration::setCryptographicBindingMethodsSupported);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPARATOR + CRYPTOGRAPHIC_SUITES_SUPPORTED_KEY))
                .map(css -> css.split(","))
                .map(Arrays::asList)
                .ifPresent(supportedCredentialConfiguration::setCryptographicSuitesSupported);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPARATOR + CREDENTIAL_SIGNING_ALG_VALUES_SUPPORTED_KEY))
                .map(css -> css.split(","))
                .map(Arrays::asList)
                .ifPresent(supportedCredentialConfiguration::setCredentialSigningAlgValuesSupported);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPARATOR + CLAIMS_KEY))
                .map(Claims::fromJsonString)
                .ifPresent(supportedCredentialConfiguration::setClaims);
        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPARATOR + CREDENTIAL_DEFINITION_KEY))
                .map(CredentialDefinition::fromJsonString)
                .ifPresent(supportedCredentialConfiguration::setCredentialDefinition);

        String displayKeyPrefix = credentialId + DOT_SEPARATOR + DISPLAY_KEY + DOT_SEPARATOR;
        List<DisplayObject> displayList = dotNotated.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(displayKeyPrefix))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> DisplayObject.fromJsonString(entry.getValue()))
                .collect(Collectors.toList());

        if (!displayList.isEmpty()){
            supportedCredentialConfiguration.setDisplay(displayList);
        }

        Optional.ofNullable(dotNotated.get(credentialId + DOT_SEPARATOR + PROOF_TYPES_SUPPORTED_KEY))
                .map(ProofTypesSupported::fromJsonString)
                .ifPresent(supportedCredentialConfiguration::setProofTypesSupported);

        Optional.ofNullable(CredentialBuildConfig.fromDotNotation(credentialId, dotNotated))
                .ifPresent(supportedCredentialConfiguration::setCredentialBuildConfig);

        return supportedCredentialConfiguration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupportedCredentialConfiguration that = (SupportedCredentialConfiguration) o;
        return Objects.equals(id, that.id) && Objects.equals(format, that.format) && Objects.equals(scope, that.scope) && Objects.equals(cryptographicBindingMethodsSupported, that.cryptographicBindingMethodsSupported) && Objects.equals(cryptographicSuitesSupported, that.cryptographicSuitesSupported) && Objects.equals(credentialSigningAlgValuesSupported, that.credentialSigningAlgValuesSupported) && Objects.equals(display, that.display) && Objects.equals(vct, that.vct) && Objects.equals(credentialDefinition, that.credentialDefinition) && Objects.equals(proofTypesSupported, that.proofTypesSupported) && Objects.equals(claims, that.claims) && Objects.equals(credentialBuildConfig, that.credentialBuildConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, format, scope, cryptographicBindingMethodsSupported, cryptographicSuitesSupported, credentialSigningAlgValuesSupported, display, vct, credentialDefinition, proofTypesSupported, claims, credentialBuildConfig);
    }
}
