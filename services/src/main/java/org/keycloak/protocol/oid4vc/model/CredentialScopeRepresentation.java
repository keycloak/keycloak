package org.keycloak.protocol.oid4vc.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.representations.idm.ClientScopeRepresentation;

import static org.keycloak.models.ClientScopeModel.INCLUDE_IN_TOKEN_SCOPE;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VCT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BUILD_CONFIG_HASH_ALGORITHM;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BUILD_CONFIG_HASH_ALGORITHM_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BUILD_CONFIG_SD_JWT_VISIBLE_CLAIMS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BUILD_CONFIG_SD_JWT_VISIBLE_CLAIMS_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_CONFIGURATION_ID;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_CONTEXTS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_CRYPTOGRAPHIC_BINDING_METHODS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_DISPLAY;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_EXPIRY_IN_SECONDS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_EXPIRY_IN_SECONDS_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_FORMAT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_FORMAT_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_IDENTIFIER;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_ISSUER_DID;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_KEY_ATTESTATION_REQUIRED;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_KEY_ATTESTATION_REQUIRED_KEY_STORAGE;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_KEY_ATTESTATION_REQUIRED_USER_AUTH;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SD_JWT_NUMBER_OF_DECOYS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SD_JWT_NUMBER_OF_DECOYS_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SIGNING_ALG;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SIGNING_KEY_ID;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SUPPORTED_TYPES;

/**
 * An extension of {@link ClientScopeRepresentation} which and adds additional functionality for OID4VCI
 *
 * @author Thomas Diesler
 */
public class CredentialScopeRepresentation extends ClientScopeRepresentation {

    public CredentialScopeRepresentation(String name) {
        this.name = name;
        this.protocol = OID4VCIConstants.OID4VC_PROTOCOL;
        setBuildConfigHashAlgorithm(VC_BUILD_CONFIG_HASH_ALGORITHM_DEFAULT);
        setBuildConfigSdJwtVisibleClaims(VC_BUILD_CONFIG_SD_JWT_VISIBLE_CLAIMS_DEFAULT);
        setBuildConfigTokenJwsType(VC_BUILD_CONFIG_TOKEN_JWS_TYPE_DEFAULT);
        setSdJwtNumberOfDecoys(VC_SD_JWT_NUMBER_OF_DECOYS_DEFAULT);
        setExpiryInSeconds(VC_EXPIRY_IN_SECONDS_DEFAULT);
        setFormat(VC_FORMAT_DEFAULT);
    }

    public CredentialScopeRepresentation(ClientScopeRepresentation clientScope) {
        this.id = clientScope.getId();
        this.name = clientScope.getName();
        this.description = clientScope.getDescription();
        this.protocol = clientScope.getProtocol();
        this.attributes = clientScope.getAttributes();
        this.protocolMappers = clientScope.getProtocolMappers();
    }

    public boolean getIncludeInTokenScope() {
        return Boolean.parseBoolean(getAttribute(INCLUDE_IN_TOKEN_SCOPE));
    }

    public CredentialScopeRepresentation setIncludeInTokenScope(boolean includeInScope) {
        return setAttribute(INCLUDE_IN_TOKEN_SCOPE, String.valueOf(includeInScope));
    }

    public String getIssuerDid() {
        return getAttribute(VC_ISSUER_DID);
    }

    public CredentialScopeRepresentation setIssuerDid(String issuerDid) {
        return setAttribute(VC_ISSUER_DID, issuerDid);
    }

    public String getCredentialConfigurationId() {
        return getAttribute(VC_CONFIGURATION_ID);
    }

    public CredentialScopeRepresentation setCredentialConfigurationId(String credentialConfigurationId) {
        return setAttribute(VC_CONFIGURATION_ID, credentialConfigurationId);
    }

    public String getCredentialIdentifier() {
        return getAttribute(VC_IDENTIFIER);
    }

    public CredentialScopeRepresentation setCredentialIdentifier(String credentialIdentifier) {
        return setAttribute(VC_IDENTIFIER, credentialIdentifier);
    }

    public String getFormat() {
        return getAttribute(VC_FORMAT);
    }

    public CredentialScopeRepresentation setFormat(String credentialFormat) {
        return setAttribute(VC_FORMAT, credentialFormat);
    }

    public Integer getExpiryInSeconds() {
        return Optional.ofNullable(getAttribute(VC_EXPIRY_IN_SECONDS))
                .map(Integer::parseInt)
                .orElse(null);
    }

    public CredentialScopeRepresentation setExpiryInSeconds(Integer expiryInSeconds) {
        return setAttribute(VC_EXPIRY_IN_SECONDS, Optional.ofNullable(expiryInSeconds)
                        .map(String::valueOf)
                        .orElse(null));
    }

    public Integer getSdJwtNumberOfDecoys() {
        return Optional.ofNullable(getAttribute(VC_SD_JWT_NUMBER_OF_DECOYS))
                .map(Integer::parseInt)
                .orElse(null);
    }

    public CredentialScopeRepresentation setSdJwtNumberOfDecoys(Integer sdJwtNumberOfDecoys) {
        return setAttribute(VC_SD_JWT_NUMBER_OF_DECOYS, Optional.ofNullable(sdJwtNumberOfDecoys)
                .map(String::valueOf)
                .orElse(null));
    }

    public String getVct() {
        return getAttribute(VCT);
    }

    public CredentialScopeRepresentation setVct(String vct) {
        return setAttribute(VCT, vct);
    }

    public String getBuildConfigTokenJwsType() {
        return getAttribute(VC_BUILD_CONFIG_TOKEN_JWS_TYPE);
    }

    public CredentialScopeRepresentation setBuildConfigTokenJwsType(String tokenJwsType) {
        return setAttribute(VC_BUILD_CONFIG_TOKEN_JWS_TYPE, tokenJwsType);
    }

    public String getSigningKeyId() {
        return getAttribute(VC_SIGNING_KEY_ID);
    }

    public CredentialScopeRepresentation setSigningKeyId(String signingKeyId) {
        return setAttribute(VC_SIGNING_KEY_ID, signingKeyId);
    }

    public String getBuildConfigHashAlgorithm() {
        return getAttribute(VC_BUILD_CONFIG_HASH_ALGORITHM);
    }

    public CredentialScopeRepresentation setBuildConfigHashAlgorithm(String hashAlgorithm) {
        return setAttribute(VC_BUILD_CONFIG_HASH_ALGORITHM, hashAlgorithm);
    }

    public List<String> getSupportedCredentialTypes() {
        return Optional.ofNullable(getAttribute(VC_SUPPORTED_TYPES))
                .map(s -> s.split(","))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
    }

    public CredentialScopeRepresentation setSupportedCredentialTypes(String supportedCredentialTypes) {
        return setAttribute(VC_SUPPORTED_TYPES, supportedCredentialTypes);
    }

    public CredentialScopeRepresentation setSupportedCredentialTypes(List<String> supportedCredentialTypes) {
        return setAttribute(VC_SUPPORTED_TYPES, String.join(",", supportedCredentialTypes));
    }

    public List<String> getVcContexts() {
        return Optional.ofNullable(getAttribute(VC_CONTEXTS))
                .map(s -> s.split(","))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
    }

    public CredentialScopeRepresentation setVcContexts(String vcContexts) {
        return setAttribute(VC_CONTEXTS, vcContexts);
    }

    public CredentialScopeRepresentation setVcContexts(List<String> vcContexts) {
        return setAttribute(VC_CONTEXTS, String.join(",", vcContexts));
    }

    public String getSigningAlg() {
        return getAttribute(VC_SIGNING_ALG);
    }

    public CredentialScopeRepresentation setSigningAlg(String signingAlg) {
        return setAttribute(VC_SIGNING_ALG, signingAlg);
    }

    public List<String> getCryptographicBindingMethods() {
        return Optional.ofNullable(getAttribute(VC_CRYPTOGRAPHIC_BINDING_METHODS))
                .map(s -> s.split(","))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
    }

    public CredentialScopeRepresentation setCryptographicBindingMethods(String cryptographicBindingMethods) {
        return setAttribute(VC_CRYPTOGRAPHIC_BINDING_METHODS, cryptographicBindingMethods);
    }

    public CredentialScopeRepresentation setCryptographicBindingMethods(List<String> cryptographicBindingMethods) {
        return setAttribute(VC_CRYPTOGRAPHIC_BINDING_METHODS, String.join(",", cryptographicBindingMethods));
    }

    public List<String> getBuildConfigSdJwtVisibleClaims() {
        return Optional.ofNullable(getAttribute(VC_BUILD_CONFIG_SD_JWT_VISIBLE_CLAIMS))
                .map(s -> s.split(","))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
    }

    public CredentialScopeRepresentation setBuildConfigSdJwtVisibleClaims(String sdJwtVisibleClaims) {
        return setAttribute(VC_BUILD_CONFIG_SD_JWT_VISIBLE_CLAIMS, sdJwtVisibleClaims);
    }

    public CredentialScopeRepresentation setBuildConfigSdJwtVisibleClaims(List<String> sdJwtVisibleClaims) {
        return setAttribute(VC_BUILD_CONFIG_SD_JWT_VISIBLE_CLAIMS, String.join(",", sdJwtVisibleClaims));
    }

    public String getDisplay() {
        return getAttribute(VC_DISPLAY);
    }

    public CredentialScopeRepresentation setDisplay(String vcDisplay) {
        return setAttribute(VC_DISPLAY, vcDisplay);
    }

    public boolean isKeyAttestationRequired() {
        return Boolean.parseBoolean(getAttribute(VC_KEY_ATTESTATION_REQUIRED));
    }

    public CredentialScopeRepresentation setKeyAttestationRequired(boolean keyAttestationRequired) {
        return setAttribute(VC_KEY_ATTESTATION_REQUIRED, String.valueOf(keyAttestationRequired));
    }

    public List<String> getRequiredKeyAttestationKeyStorage() {
        return Optional.ofNullable(getAttribute(VC_KEY_ATTESTATION_REQUIRED_KEY_STORAGE))
                .map(s -> Arrays.asList(s.split(",")))
                // it is important to return null here instead of an empty list:
                // If both key_storage and user_authentication parameters are absent, the
                // key_attestations_required parameter may be empty, indicating a key attestation is needed
                // without additional constraints. Meaning we must not add empty objects to the metadata endpoint
                .orElse(null);
    }

    public CredentialScopeRepresentation setRequiredKeyAttestationKeyStorage(List<String> keyStorage) {
        return setAttribute(VC_KEY_ATTESTATION_REQUIRED_KEY_STORAGE, Optional.ofNullable(keyStorage)
                .map(list -> String.join(",")).orElse(null));
    }

    public List<String> getRequiredKeyAttestationUserAuthentication() {
        return Optional.ofNullable(getAttribute(VC_KEY_ATTESTATION_REQUIRED_USER_AUTH))
                .map(s -> Arrays.asList(s.split(",")))
                // it is important to return null here instead of an empty list:
                // If both key_storage and user_authentication parameters are absent, the
                // key_attestations_required parameter may be empty, indicating a key attestation is needed
                // without additional constraints. Meaning we must not add empty objects to the metadata endpoint
                .orElse(null);
    }

    public CredentialScopeRepresentation setRequiredKeyAttestationUserAuthentication(List<String> userAuthentication) {
        return setAttribute(VC_KEY_ATTESTATION_REQUIRED_USER_AUTH, Optional.ofNullable(userAuthentication)
                .map(list -> String.join(",")).orElse(null));
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private String getAttribute(String key) {
        return attributes != null ? attributes.get(key) : null;
    }

    private CredentialScopeRepresentation setAttribute(String key, String value) {
        if (attributes == null) {
            attributes = new LinkedHashMap<>();
        }
        if (value != null) {
            attributes.put(key, value);
        } else {
            attributes.remove(key);
        }
        return this;
    }
}
