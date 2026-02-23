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
 *
 */

package org.keycloak.models.oid4vci;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.VCFormat;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import static org.keycloak.constants.OID4VCIConstants.OID4VC_PROTOCOL;

/**
 * This class acts as delegate for a {@link ClientScopeModel} implementation and adds additional functionality for
 * OpenId4VC credentials
 *
 * @author Pascal Knüppel
 */
public class CredentialScopeModel implements ClientScopeModel {


    public static final String SD_JWT_VISIBLE_CLAIMS_DEFAULT = "id,iat,nbf,exp,jti";
    public static final int SD_JWT_DECOYS_DEFAULT = 10;
    public static final String FORMAT_DEFAULT = VCFormat.SD_JWT_VC;
    public static final String HASH_ALGORITHM_DEFAULT = "sha-256";
    public static final String TOKEN_TYPE_DEFAULT = "JWS";
    public static final int EXPIRY_IN_SECONDS_DEFAULT = 31536000; // 1 year
    public static final String CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT = "jwk";

    /**
     * the credential configuration id as provided in the metadata endpoint
     */
    public static final String CONFIGURATION_ID = "vc.credential_configuration_id";
    public static final String CREDENTIAL_IDENTIFIER = "vc.credential_identifier";
    public static final String FORMAT = "vc.format";
    public static final String EXPIRY_IN_SECONDS = "vc.expiry_in_seconds";
    public static final String ISSUER_DID = "vc.issuer_did";
    public static final String VCT = "vc.verifiable_credential_type";

    /**
     * the value that is added into the "types"-attribute of a verifiable credential
     */
    public static final String TYPES = "vc.supported_credential_types";

    /**
     * the value that is entered into the "@contexts"-attribute of a verifiable credential
     */
    public static final String CONTEXTS = "vc.credential_contexts";

    /**
     * The credential signature algorithm. If it is not configured, then the realm active key is used to sign the verifiable credential
     */
    public static final String SIGNING_ALG = "vc.credential_signing_alg";

    /**
     * if the credential is only meant for specific cryptographic binding algorithms the global default list can be
     * overridden here. The global default list is retrieved from the available keys in the realm.
     */
    public static final String CRYPTOGRAPHIC_BINDING_METHODS = "vc.cryptographic_binding_methods_supported";

    /**
     * an optional configuration that can be used to select a specific key for signing the credential
     */
    public static final String SIGNING_KEY_ID = "vc.signing_key_id";

    /**
     * an optional attribute for the metadata endpoint
     */
    public static final String VC_DISPLAY = "vc.display";

    /**
     * this attribute holds a customizable value for the number of decoys to use in a SD-JWT credential
     */
    public static final String SD_JWT_NUMBER_OF_DECOYS = "vc.sd_jwt.number_of_decoys";

    /**
     * an optional attribute that tells us which attributes should be added into the SD-JWT body.
     */
    public static final String SD_JWT_VISIBLE_CLAIMS = "vc.credential_build_config.sd_jwt.visible_claims";

    /**
     * an optional configuration that can be used to select a specific hash algorithm
     */
    public static final String HASH_ALGORITHM = "vc.credential_build_config.hash_algorithm";

    /**
     * this attribute holds the 'typ' value that will be added into the JWS header of the credential.
     */
    public static final String TOKEN_JWS_TYPE = "vc.credential_build_config.token_jws_type";

    /**
     * this configuration property can be used to enforce specific claims to be included in the metadata, if they would
     * normally not and vice versa
     */
    public static final String INCLUDE_IN_METADATA = "vc.include_in_metadata";

    /**
     * OPTIONAL. Object that describes the requirement for key attestations as described in Appendix D, which the
     * Credential Issuer expects the Wallet to send within the proof(s) of the Credential Request. If the Credential
     * Issuer does not require a key attestation, this parameter MUST NOT be present in the metadata. If both
     * key_storage and user_authentication parameters are absent, the key_attestations_required parameter may be empty,
     * indicating a key attestation is needed without additional constraints.
     */
    public static final String KEY_ATTESTATION_REQUIRED = "vc.key_attestations_required";

    /**
     * OPTIONAL. A non-empty array defining values specified in Appendix D.2 accepted by the Credential Issuer.
     */
    public static final String KEY_ATTESTATION_REQUIRED_KEY_STORAGE = "vc.key_attestations_required.key_storage";

    /**
     * OPTIONAL. A non-empty array defining values specified in Appendix D.2 accepted by the Credential Issuer.
     */
    public static final String KEY_ATTESTATION_REQUIRED_USER_AUTH = "vc.key_attestations_required.user_authentication";


    /**
     * the actual object that is represented by this scope
     */
    private final ClientScopeModel clientScope;

    public CredentialScopeModel(ClientScopeModel clientScope) {
        this.clientScope = clientScope;
        assert OID4VC_PROTOCOL.equals(clientScope.getProtocol());
    }

    public String getIssuerDid() {
        return clientScope.getAttribute(ISSUER_DID);
    }

    public void setIssuerDid(String issuerDid) {
        clientScope.setAttribute(ISSUER_DID, issuerDid);
    }

    public String getScope() {
        return clientScope.getName();
    }

    public String getCredentialConfigurationId() {
        return Optional.ofNullable(clientScope.getAttribute(CONFIGURATION_ID)).orElse(getName());
    }

    public void setCredentialConfigurationId(String credentialConfigurationId) {
        clientScope.setAttribute(CONFIGURATION_ID, Optional.ofNullable(credentialConfigurationId).orElse(getName()));
    }

    public String getCredentialIdentifier() {
        return Optional.ofNullable(clientScope.getAttribute(CREDENTIAL_IDENTIFIER)).orElse(getName());
    }

    public void setCredentialIdentifier(String credentialIdentifier) {
        clientScope.setAttribute(CREDENTIAL_IDENTIFIER, Optional.ofNullable(credentialIdentifier).orElse(getName()));
    }

    public String getFormat() {
        return Optional.ofNullable(clientScope.getAttribute(FORMAT)).orElse(FORMAT_DEFAULT);
    }

    public void setFormat(String credentialFormat) {
        clientScope.setAttribute(FORMAT, Optional.ofNullable(credentialFormat).orElse(FORMAT_DEFAULT));
    }

    public Integer getExpiryInSeconds() {
        return Optional.ofNullable(clientScope.getAttribute(EXPIRY_IN_SECONDS)).map(Integer::parseInt)
                       .orElse(EXPIRY_IN_SECONDS_DEFAULT);
    }

    public void setExpiryInSeconds(Integer expiryInSeconds) {
        clientScope.setAttribute(EXPIRY_IN_SECONDS,
                                 Optional.ofNullable(expiryInSeconds).map(String::valueOf)
                                         .orElse(String.valueOf(EXPIRY_IN_SECONDS_DEFAULT)));
    }

    public int getSdJwtNumberOfDecoys() {
        return Optional.ofNullable(clientScope.getAttribute(SD_JWT_NUMBER_OF_DECOYS)).map(Integer::parseInt)
                       .orElse(SD_JWT_DECOYS_DEFAULT);
    }

    public void setSdJwtNumberOfDecoys(Integer sdJwtNumberOfDecoys) {
        clientScope.setAttribute(SD_JWT_NUMBER_OF_DECOYS,
                                 Optional.ofNullable(sdJwtNumberOfDecoys).map(String::valueOf)
                                         .orElse(String.valueOf(SD_JWT_DECOYS_DEFAULT)));
    }

    public String getVct() {
        return Optional.ofNullable(clientScope.getAttribute(VCT)).orElse(getName());
    }

    public void setVct(String vct) {
        clientScope.setAttribute(VCT, Optional.ofNullable(vct).orElse(getName()));
    }

    public String getTokenJwsType() {
        return Optional.ofNullable(clientScope.getAttribute(TOKEN_JWS_TYPE)).orElse(TOKEN_TYPE_DEFAULT);
    }

    public void setTokenJwsType(String tokenJwsType) {
        clientScope.setAttribute(TOKEN_JWS_TYPE, Optional.ofNullable(tokenJwsType).orElse(TOKEN_TYPE_DEFAULT));
    }

    public String getSigningKeyId() {
        return clientScope.getAttribute(SIGNING_KEY_ID);
    }

    public void setSigningKeyId(String signingKeyId) {
        clientScope.setAttribute(SIGNING_KEY_ID, signingKeyId);
    }

    public String getHashAlgorithm() {
        return Optional.ofNullable(clientScope.getAttribute(HASH_ALGORITHM)).orElse(HASH_ALGORITHM_DEFAULT);
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        clientScope.setAttribute(HASH_ALGORITHM, hashAlgorithm);
    }

    public List<String> getSupportedCredentialTypes() {
        return Optional.ofNullable(clientScope.getAttribute(TYPES))
                       .map(s -> s.split(","))
                       .map(Arrays::asList)
                       .orElse(Collections.singletonList(getName()));
    }

    public void setSupportedCredentialTypes(String supportedCredentialTypes) {
        clientScope.setAttribute(TYPES, Optional.ofNullable(supportedCredentialTypes).orElse(getName()));
    }

    public void setSupportedCredentialTypes(List<String> supportedCredentialTypes) {
        clientScope.setAttribute(TYPES, String.join(",", supportedCredentialTypes));
    }

    public List<String> getVcContexts() {
        return Optional.ofNullable(clientScope.getAttribute(CONTEXTS))
                       .map(s -> s.split(","))
                       .map(Arrays::asList)
                       .orElse(Collections.singletonList(getName()));
    }

    public void setVcContexts(String vcContexts) {
        clientScope.setAttribute(CONTEXTS, Optional.ofNullable(vcContexts).orElse(getName()));
    }

    public void setVcContexts(List<String> vcContexts) {
        clientScope.setAttribute(CONTEXTS, String.join(",", vcContexts));
    }

    public String getSigningAlg() {
        return clientScope.getAttribute(SIGNING_ALG);
    }

    public void setSigningAlg(String signingAlg) {
        clientScope.setAttribute(SIGNING_ALG, signingAlg);
    }

    public List<String> getCryptographicBindingMethods() {
        return Optional.ofNullable(clientScope.getAttribute(CRYPTOGRAPHIC_BINDING_METHODS))
                       .map(s -> s.split(","))
                       .map(Arrays::asList)
                       .orElse(Collections.emptyList());
    }

    public void setCryptographicBindingMethods(String cryptographicBindingMethods) {
        clientScope.setAttribute(CRYPTOGRAPHIC_BINDING_METHODS, cryptographicBindingMethods);
    }

    public void setCryptographicBindingMethods(List<String> cryptographicBindingMethods) {
        clientScope.setAttribute(CRYPTOGRAPHIC_BINDING_METHODS,
                                 String.join(",", cryptographicBindingMethods));
    }

    public List<String> getSdJwtVisibleClaims() {
        return Optional.ofNullable(clientScope.getAttribute(SD_JWT_VISIBLE_CLAIMS))
                       .map(s -> s.split(","))
                       .map(Arrays::asList)
                       .orElse(List.of(SD_JWT_VISIBLE_CLAIMS_DEFAULT.split(",")));
    }

    public void setSdJwtVisibleClaims(String sdJwtVisibleClaims) {
        clientScope.setAttribute(SD_JWT_VISIBLE_CLAIMS, Optional.ofNullable(sdJwtVisibleClaims)
                                                                .orElse(SD_JWT_VISIBLE_CLAIMS_DEFAULT));
    }

    public void setSdJwtVisibleClaims(List<String> sdJwtVisibleClaims) {
        clientScope.setAttribute(SD_JWT_VISIBLE_CLAIMS,
                                 String.join(",", sdJwtVisibleClaims));
    }

    public String getVcDisplay() {
        return clientScope.getAttribute(VC_DISPLAY);
    }

    public void setVcDisplay(String vcDisplay) {
        clientScope.setAttribute(VC_DISPLAY, vcDisplay);
    }

    public boolean isKeyAttestationRequired() {
        return Optional.ofNullable(clientScope.getAttribute(KEY_ATTESTATION_REQUIRED))
                       .map(Boolean::parseBoolean)
                       .orElse(false);
    }

    public void setKeyAttestationRequired(boolean keyAttestationRequired) {
        clientScope.setAttribute(KEY_ATTESTATION_REQUIRED, String.valueOf(keyAttestationRequired));
    }

    public List<String> getRequiredKeyAttestationKeyStorage() {
        return Optional.ofNullable(clientScope.getAttribute(KEY_ATTESTATION_REQUIRED_KEY_STORAGE))
                       .map(s -> Arrays.asList(s.split(",")))
                       // it is important to return null here instead of an empty list:
                       // If both key_storage and user_authentication parameters are absent, the
                       // key_attestations_required parameter may be empty, indicating a key attestation is needed
                       // without additional constraints. Meaning we must not add empty objects to the metadata endpoint
                       .orElse(null);
    }

    public void setRequiredKeyAttestationKeyStorage(List<String> keyStorage) {
        clientScope.setAttribute(KEY_ATTESTATION_REQUIRED_KEY_STORAGE,
                                 Optional.ofNullable(keyStorage).map(list -> String.join(",")).orElse(null));
    }

    public List<String> getRequiredKeyAttestationUserAuthentication() {
        return Optional.ofNullable(clientScope.getAttribute(KEY_ATTESTATION_REQUIRED_USER_AUTH))
                       .map(s -> Arrays.asList(s.split(",")))
                       // it is important to return null here instead of an empty list:
                       // If both key_storage and user_authentication parameters are absent, the
                       // key_attestations_required parameter may be empty, indicating a key attestation is needed
                       // without additional constraints. Meaning we must not add empty objects to the metadata endpoint
                       .orElse(null);
    }

    public void getRequiredKeyAttestationUserAuthentication(List<String> userAuthentication) {
        clientScope.setAttribute(KEY_ATTESTATION_REQUIRED_USER_AUTH,
                                 Optional.ofNullable(userAuthentication).map(list -> String.join(",")).orElse(null));
    }

    @Override
    public String getId() {
        return clientScope.getId();
    }

    @Override
    public String getName() {
        return clientScope.getName();
    }

    @Override
    public void setName(String name) {
        clientScope.setName(name);
    }

    @Override
    public RealmModel getRealm() {
        return clientScope.getRealm();
    }

    @Override
    public String getDescription() {
        return clientScope.getDescription();
    }

    @Override
    public void setDescription(String description) {
        clientScope.setDescription(description);
    }

    @Override
    public String getProtocol() {
        return clientScope.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        clientScope.setProtocol(protocol);
    }

    @Override
    public void setAttribute(String name, String value) {
        clientScope.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        clientScope.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        return clientScope.getAttribute(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        return clientScope.getAttributes();
    }

    @Override
    public boolean isDisplayOnConsentScreen() {
        return clientScope.isDisplayOnConsentScreen();
    }

    @Override
    public void setDisplayOnConsentScreen(boolean displayOnConsentScreen) {
        clientScope.setDisplayOnConsentScreen(displayOnConsentScreen);
    }

    @Override
    public String getConsentScreenText() {
        return clientScope.getConsentScreenText();
    }

    @Override
    public void setConsentScreenText(String consentScreenText) {
        clientScope.setConsentScreenText(consentScreenText);
    }

    @Override
    public String getGuiOrder() {
        return clientScope.getGuiOrder();
    }

    @Override
    public void setGuiOrder(String guiOrder) {
        clientScope.setGuiOrder(guiOrder);
    }

    @Override
    public boolean isIncludeInTokenScope() {
        return clientScope.isIncludeInTokenScope();
    }

    @Override
    public void setIncludeInTokenScope(boolean includeInTokenScope) {
        clientScope.setIncludeInTokenScope(includeInTokenScope);
    }

    @Override
    public boolean isDynamicScope() {
        return clientScope.isDynamicScope();
    }

    @Override
    public void setIsDynamicScope(boolean isDynamicScope) {
        clientScope.setIsDynamicScope(isDynamicScope);
    }

    @Override
    public String getDynamicScopeRegexp() {
        return clientScope.getDynamicScopeRegexp();
    }

    public Stream<Oid4vcProtocolMapperModel> getOid4vcProtocolMappersStream() {
        return clientScope.getProtocolMappersStream().filter(pm -> {
            return OID4VCIConstants.OID4VC_PROTOCOL.equals(pm.getProtocol());
        }).map(Oid4vcProtocolMapperModel::new);
    }

    @Override
    public Stream<ProtocolMapperModel> getProtocolMappersStream() {
        return clientScope.getProtocolMappersStream();
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        return clientScope.addProtocolMapper(model);
    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        clientScope.removeProtocolMapper(mapping);
    }

    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        clientScope.updateProtocolMapper(mapping);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        return clientScope.getProtocolMapperById(id);
    }

    @Override
    public List<ProtocolMapperModel> getProtocolMapperByType(String type) {
        return clientScope.getProtocolMapperByType(type);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        return clientScope.getProtocolMapperByName(protocol, name);
    }

    @Override
    public Stream<RoleModel> getScopeMappingsStream() {
        return clientScope.getScopeMappingsStream();
    }

    @Override
    public Stream<RoleModel> getRealmScopeMappingsStream() {
        return clientScope.getRealmScopeMappingsStream();
    }

    @Override
    public void addScopeMapping(RoleModel role) {
        clientScope.addScopeMapping(role);
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        clientScope.deleteScopeMapping(role);
    }

    @Override
    public boolean hasDirectScope(RoleModel role) {
        return clientScope.hasDirectScope(role);
    }

    @Override
    public boolean hasScope(RoleModel role) {
        return clientScope.hasScope(role);
    }
}
