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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.keycloak.VCFormat;
import org.keycloak.mdoc.MdocAlgorithm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.oid4vci.CredentialScopeModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

import static org.keycloak.OID4VCConstants.CRYPTOGRAPHIC_BINDING_METHOD_COSE_KEY;
import static org.keycloak.OID4VCConstants.CRYPTOGRAPHIC_BINDING_METHOD_JWK;

/**
 * A supported credential, as used in the Credentials Issuer Metadata in OID4VCI
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-issuer-metadata}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportedCredentialConfiguration {

    private static final Logger LOGGER = Logger.getLogger(SupportedCredentialConfiguration.class);

    @JsonIgnore
    private static final String FORMAT_KEY = "format";
    @JsonIgnore
    private static final String SCOPE_KEY = "scope";
    @JsonIgnore
    private static final String CRYPTOGRAPHIC_BINDING_METHODS_SUPPORTED_KEY = "cryptographic_binding_methods_supported";
    @JsonIgnore
    private static final String CREDENTIAL_SIGNING_ALG_VALUES_SUPPORTED_KEY = "credential_signing_alg_values_supported";
    @JsonIgnore
    private static final String PROOF_TYPES_SUPPORTED_KEY = "proof_types_supported";
    @JsonIgnore
    public static final String VERIFIABLE_CREDENTIAL_TYPE_KEY = "vct";
    @JsonIgnore
    public static final String DOC_TYPE_KEY = "doctype";
    @JsonIgnore
    private static final String CREDENTIAL_DEFINITION_KEY = "credential_definition";
    @JsonIgnore
    public static final String CREDENTIAL_BUILD_CONFIG_KEY = "credential_build_config";
    @JsonIgnore
    private static final String CREDENTIAL_METADATA_KEY = "credential_metadata";

    @JsonIgnore
    private String id;

    @JsonProperty(FORMAT_KEY)
    private String format;

    @JsonProperty(SCOPE_KEY)
    private String scope;

    @JsonProperty(CRYPTOGRAPHIC_BINDING_METHODS_SUPPORTED_KEY)
    private List<String> cryptographicBindingMethodsSupported;

    @JsonProperty(CREDENTIAL_SIGNING_ALG_VALUES_SUPPORTED_KEY)
    private List<Object> credentialSigningAlgValuesSupported;

    @JsonProperty(VERIFIABLE_CREDENTIAL_TYPE_KEY)
    private String vct;

    @JsonProperty(DOC_TYPE_KEY)
    private String docType;

    @JsonProperty(CREDENTIAL_DEFINITION_KEY)
    private CredentialDefinition credentialDefinition;

    @JsonProperty(PROOF_TYPES_SUPPORTED_KEY)
    private ProofTypesSupported proofTypesSupported;

    @JsonProperty(CREDENTIAL_METADATA_KEY)
    private CredentialMetadata credentialMetadata;

    // This is not a normative field for supported credential metadata,
    // but will allow configuring the issuance of the credential internally.
    @JsonIgnore
    private CredentialBuildConfig credentialBuildConfig;

    /**
     * @param credentialScope                  The scope that holds the credentials configuration
     * @param globalSupportedSigningAlgorithms added as a parameter to avoid reading the global config from the session
     *                                         for each credential
     * @return the credentials configuration that was entered into the ClientScope
     */
    public static SupportedCredentialConfiguration parse(KeycloakSession keycloakSession,
                                                         CredentialScopeModel credentialScope,
                                                         List<String> globalSupportedSigningAlgorithms) {
        SupportedCredentialConfiguration credentialConfiguration = new SupportedCredentialConfiguration();

        String credentialConfigurationId = Optional.ofNullable(credentialScope.getCredentialConfigurationId())
                                                   .orElse(credentialScope.getName());
        credentialConfiguration.setId(credentialConfigurationId);

        credentialConfiguration.setScope(credentialScope.getName());

        String format = Optional.ofNullable(credentialScope.getFormat()).orElse(VCFormat.SD_JWT_VC);
        credentialConfiguration.setFormat(format);

        KeyAttestationsRequired keyAttestationsRequired = KeyAttestationsRequired.parse(credentialScope);
        boolean bindingRequired = credentialScope.isBindingRequired();
        List<String> requiredProofTypes = credentialScope.getRequiredProofTypes();
        List<String> configuredBindingMethods = credentialScope.getCryptographicBindingMethods();

        // OID4VCI 1.0 Section 12.2.4 defines binding methods as the key-material representation in the issued
        // credential: "jwk" for JWK and "cose_key" for COSE_Key, which is the representation used by ISO mdoc.
        // Normalize configured values so unsupported admin/API configuration does not leak into issuer metadata.
        List<String> allowedBindingMethods = VCFormat.MSO_MDOC.equals(format)
                ? List.of(CRYPTOGRAPHIC_BINDING_METHOD_COSE_KEY)
                : List.of(CRYPTOGRAPHIC_BINDING_METHOD_JWK);
        List<String> effectiveBindingMethods = Optional.ofNullable(configuredBindingMethods)
                .orElse(Collections.emptyList())
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(allowedBindingMethods::contains)
                .collect(Collectors.toList());

        if (configuredBindingMethods != null && !configuredBindingMethods.isEmpty()
                && effectiveBindingMethods.isEmpty()) {
            LOGGER.warnf("All configured cryptographic binding methods %s are unsupported. " +
                            "This credential configuration will not advertise cryptographic binding in metadata.",
                    configuredBindingMethods);
        }

        List<String> allowedProofTypes = List.of(ProofType.JWT, ProofType.ATTESTATION);
        List<String> effectiveProofTypes = Optional.ofNullable(requiredProofTypes)
                .orElse(Collections.emptyList())
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(allowedProofTypes::contains)
                .collect(Collectors.toList());

        if (requiredProofTypes != null && !requiredProofTypes.isEmpty()
                && effectiveProofTypes.isEmpty()) {
            LOGGER.warnf("All configured proof types %s are unsupported. " +
                            "This credential configuration will not advertise proof_types_supported in metadata.",
                    requiredProofTypes);
        }

        // According to OID4VCI 1.0 Section 12.2.4:
        // - If cryptographic_binding_methods_supported is present, cryptographic holder binding is REQUIRED.
        // - If it is absent, binding is NOT required.
        // - proof_types_supported MUST be present if cryptographic_binding_methods_supported is present.
        //
        // We therefore only emit these two metadata fields when:
        //   - binding has been explicitly marked as required
        //   - at least one proof type is configured for this credential configuration
        //   - and at least one cryptographic binding method has been configured
        if (bindingRequired
                && !effectiveProofTypes.isEmpty()
                && !effectiveBindingMethods.isEmpty()) {

            ProofTypesSupported allProofTypes = ProofTypesSupported.parse(keycloakSession, keyAttestationsRequired,
                    globalSupportedSigningAlgorithms);
            ProofTypesSupported proofTypesSupported = allProofTypes.filterByTypes(effectiveProofTypes);
            credentialConfiguration.setProofTypesSupported(proofTypesSupported);

            credentialConfiguration.setCryptographicBindingMethodsSupported(effectiveBindingMethods);
        }

        // Return single configured value for the signature algorithm if any.
        List<String> signingAlgsSupported = CredentialSigningAlgorithmResolver.resolveMetadataSigningAlgorithms(
                keycloakSession,
                credentialScope,
                format,
                globalSupportedSigningAlgorithms
        );
        if (signingAlgsSupported.isEmpty()) {
            LOGGER.warnf("No compatible signing algorithm is available for credential configuration '%s' with format '%s'.",
                    credentialConfigurationId, format);
        }
        credentialConfiguration.setCredentialSigningAlgValuesSupported(
                toCredentialSigningAlgValuesSupported(format, signingAlgsSupported)
        );

        // Parse credential metadata (includes display and claims)
        CredentialMetadata credentialMetadata = CredentialMetadata.parse(keycloakSession, credentialScope);
        credentialConfiguration.setCredentialMetadata(credentialMetadata);

        CredentialBuildConfig credentialBuildConfig = CredentialBuildConfig.parse(keycloakSession,
                                                                                  credentialConfiguration,
                                                                                  credentialScope);
        credentialConfiguration.setCredentialBuildConfig(credentialBuildConfig);

        return credentialConfiguration;
    }

    /**
     * Return the format-specific credential type identifier. For SD-JWT VC it is emitted as "vct"; for mDoc it is
     * emitted as "doctype".
     * <p>
     * For jwt_vc and ldp_vc, we will be inferring from the "credential_definition" See:
     * https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-request-3
     *
     * @return
     */
    public VerifiableCredentialType deriveType() {
        if (Objects.equals(format, VCFormat.SD_JWT_VC)) {
            return VerifiableCredentialType.from(vct);
        }
        return null;
    }

    public CredentialConfigId deriveConfigId() {
        return CredentialConfigId.from(id);
    }

    public String getFormat() {
        return format;
    }

    public SupportedCredentialConfiguration setFormat(String format) {
        this.format = format;
        return this;
    }

    public String getDocType() {
        return docType;
    }

    public SupportedCredentialConfiguration setDocType(String docType) {
        this.docType = docType;
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

    public String getId() {
        return id;
    }

    public SupportedCredentialConfiguration setId(String id) {
        this.id = id;
        return this;
    }

    public List<Object> getCredentialSigningAlgValuesSupported() {
        return credentialSigningAlgValuesSupported;
    }

    public SupportedCredentialConfiguration setCredentialSigningAlgValuesSupported(List<Object> credentialSigningAlgValuesSupported) {
        this.credentialSigningAlgValuesSupported = Collections.unmodifiableList(credentialSigningAlgValuesSupported);
        return this;
    }

    private static List<Object> toCredentialSigningAlgValuesSupported(String format, List<String> signingAlgorithmsSupported) {
        if (!VCFormat.MSO_MDOC.equals(format)) {
            return List.copyOf(signingAlgorithmsSupported);
        }

        // OID4VCI 1.0 Appendix A.2.2 uses numeric COSE algorithm identifiers for mDoc issuer metadata, while
        // Keycloak key providers are configured with JOSE names. Convert only the mDoc metadata representation here.
        return signingAlgorithmsSupported.stream()
                .map(SupportedCredentialConfiguration::toMdocCredentialSigningAlgorithm)
                .collect(Collectors.toList());
    }

    private static Integer toMdocCredentialSigningAlgorithm(String algorithm) {
        return MdocAlgorithm.fromJoseAlgorithm(algorithm).getCoseAlgorithmIdentifier();
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

    public CredentialMetadata getCredentialMetadata() {
        return credentialMetadata;
    }

    public SupportedCredentialConfiguration setCredentialMetadata(CredentialMetadata credentialMetadata) {
        this.credentialMetadata = credentialMetadata;
        return this;
    }

    public CredentialBuildConfig getCredentialBuildConfig() {
        return credentialBuildConfig;
    }

    public SupportedCredentialConfiguration setCredentialBuildConfig(CredentialBuildConfig credentialBuildConfig) {
        this.credentialBuildConfig = credentialBuildConfig;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupportedCredentialConfiguration that = (SupportedCredentialConfiguration) o;
        return Objects.equals(id, that.id) && Objects.equals(format, that.format) && Objects.equals(scope, that.scope) && Objects.equals(cryptographicBindingMethodsSupported, that.cryptographicBindingMethodsSupported) && Objects.equals(credentialSigningAlgValuesSupported, that.credentialSigningAlgValuesSupported) && Objects.equals(vct, that.vct) && Objects.equals(docType, that.docType) && Objects.equals(credentialDefinition, that.credentialDefinition) && Objects.equals(proofTypesSupported, that.proofTypesSupported) && Objects.equals(credentialMetadata, that.credentialMetadata) && Objects.equals(credentialBuildConfig, that.credentialBuildConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, format, scope, cryptographicBindingMethodsSupported, credentialSigningAlgValuesSupported, vct, docType, credentialDefinition, proofTypesSupported, credentialMetadata, credentialBuildConfig);
    }
}
