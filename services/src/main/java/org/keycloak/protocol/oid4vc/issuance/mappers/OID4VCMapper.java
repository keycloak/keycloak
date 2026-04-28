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

package org.keycloak.protocol.oid4vc.issuance.mappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.models.oid4vci.Oid4vcProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.OID4VCEnvironmentProviderFactory;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.utils.JsonUtils;

import org.apache.commons.collections4.ListUtils;

import static org.keycloak.OID4VCConstants.CREDENTIAL_SUBJECT;
import static org.keycloak.VCFormat.MSO_MDOC;
import static org.keycloak.VCFormat.SD_JWT_VC;

/**
 * Base class for OID4VC Mappers, to provide common configuration and functionality for all of them
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public abstract class OID4VCMapper implements ProtocolMapper, OID4VCEnvironmentProviderFactory {

    public static final String CLAIM_NAME = "claim.name";
    public static final String MDOC_NAMESPACE = "mdoc.namespace";
    public static final String USER_ATTRIBUTE_KEY = "userAttribute";
    private static final List<ProviderConfigProperty> OID4VC_CONFIG_PROPERTIES = new ArrayList<>();
    private static final List<ProviderConfigProperty> MDOC_CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty property;

        // Add vc.mandatory property - indicates whether this claim is mandatory in the credential
        property = new ProviderConfigProperty();
        property.setName(Oid4vcProtocolMapperModel.MANDATORY);
        property.setLabel("Mandatory Claim");
        property.setHelpText("Indicates whether this claim must be present in the issued credential. " +
                "This information is included in the credential metadata for wallet applications.");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
        OID4VC_CONFIG_PROPERTIES.add(property);

        // Add vc.display property - display information for wallet UIs
        property = new ProviderConfigProperty();
        property.setName(Oid4vcProtocolMapperModel.DISPLAY);
        property.setLabel("Claim Display Information");
        property.setHelpText("Display metadata for wallet applications to show user-friendly claim names. " +
                "Provide display entries with name and locale for internationalization support.");
        property.setType(ProviderConfigProperty.CLAIM_DISPLAY_TYPE);
        property.setDefaultValue(null);
        OID4VC_CONFIG_PROPERTIES.add(property);

        property = new ProviderConfigProperty();
        property.setName(MDOC_NAMESPACE);
        property.setLabel("mDoc Namespace");
        property.setHelpText("Namespace for mso_mdoc claims. Used only when issuing mso_mdoc credentials.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue(null);
        MDOC_CONFIG_PROPERTIES.add(property);
    }

    protected ProtocolMapperModel mapperModel;
    protected String format;

    protected abstract List<ProviderConfigProperty> getIndividualConfigProperties();

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        Stream<ProviderConfigProperty> configProperties = OID4VC_CONFIG_PROPERTIES.stream();
        if (Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI_MDOC)) {
            configProperties = Stream.concat(configProperties, MDOC_CONFIG_PROPERTIES.stream());
        }
        return Stream.concat(configProperties, getIndividualConfigProperties().stream()).toList();
    }

    public OID4VCMapper setMapperModel(ProtocolMapperModel mapperModel, String format) {
        this.mapperModel = mapperModel;
        this.format = format;
        return this;
    }

    /**
     * some specific claims should not be added into the metadata. Examples are jti, sub, iss etc. Since we have the
     * possibility to add these credentials with specific claims we should also be able to exclude these specific
     * attributes from the metadata
     */
    public boolean includeInMetadata() {
        return Optional.ofNullable(mapperModel.getConfig().get(CredentialScopeModel.VC_INCLUDE_IN_METADATA))
                       .map(Boolean::parseBoolean)
                       .orElse(true);
    }

    /**
     * Some mappers target format-specific container fields instead of subject/data-element claims. Callers use this
     * hook for both metadata and issuance so unsupported mappers are not advertised or applied for a credential format.
     */
    public boolean supportsCredentialFormat(String credentialFormat) {
        return true;
    }

    /**
     * Returns the externally visible claim path used in credential metadata and authorization_details validation.
     * JSON credentials use their normal credentialSubject/top-level paths; mDoc prepends the configured namespace
     * because OID4VCI mDoc paths address namespace -> data element -> optional nested value.
     */
    public List<String> getMetadataAttributePath() {
        final String attributeName = getClaimName();
        return getMetadataAttributePath(attributeName);
    }

    protected List<String> getMetadataAttributePath(String attributeName) {
        if (attributeName == null) {
            return Collections.emptyList();
        }

        List<String> attributePath = MSO_MDOC.equals(format) ? JsonUtils.splitClaimPath(attributeName) : List.of(attributeName);
        return prefixMetadataAttributePath(attributePath);
    }

    protected List<String> prefixMetadataAttributePath(List<String> attributePath) {
        if (attributePath.isEmpty()) {
            return Collections.emptyList();
        }

        if (MSO_MDOC.equals(format)) {
            String namespace = mapperModel.getConfig().get(MDOC_NAMESPACE);
            if (namespace == null || namespace.isBlank()) {
                return Collections.emptyList();
            }
            // OID4VCI 1.0 Appendix C.2: mDoc claim paths start with namespace and data element identifier,
            // followed by optional path components inside the selected data element value.
            return ListUtils.union(List.of(namespace), attributePath);
        }

        return ListUtils.union(getAttributePrefix(), attributePath);
    }

    /**
     * Returns the raw claim lookup path in the intermediate map populated by {@link #setClaim(Map, UserSessionModel)}.
     * This is intentionally separate from {@link #getMetadataAttributePath()}: mDoc metadata paths add a namespace
     * that is not present in the raw mapper output, and simple mappers may write a dotted claim name as one literal key.
     */
    protected List<String> getClaimLookupPath() {
        return getClaimLookupPath(getClaimName());
    }

    protected List<String> getClaimLookupPath(String claimName) {
        if (claimName == null) {
            return Collections.emptyList();
        }
        return List.of(claimName);
    }

    protected String getClaimName() {
        return mapperModel.getConfig().get(CLAIM_NAME);
    }

    protected String getClaimName(String defaultClaimName) {
        return Optional.ofNullable(getClaimName()).orElse(defaultClaimName);
    }

    protected List<String> getAttributePrefix() {
        if (SD_JWT_VC.equals(format)) {
            return Collections.emptyList();
        } else {
            return List.of(CREDENTIAL_SUBJECT);
        }
    }

    @Override
    public String getProtocol() {
        return OID4VCLoginProtocolFactory.PROTOCOL_ID;
    }

    @Override
    public String getDisplayCategory() {
        return "OID4VC Mapper";
    }

    @Override
    public void init(Config.Scope scope) {
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        // try to get the credentials
    }

    @Override
    public void close() {
    }

    /**
     * Set the claims to credential, like f.e. the context
     */
    public abstract void setClaim(VerifiableCredential verifiableCredential,
                                  UserSessionModel userSessionModel);

    /**
     * Set the claims to the credential subject.
     */
    public abstract void setClaim(Map<String, Object> claims,
                                  UserSessionModel userSessionModel);

    /**
     * Reads a mapper-produced claim from the intermediate, un-prefixed claim map.
     *
     * Some mappers write {@code address.street} as one literal key, while the user-attribute mapper writes it as
     * {@code {"address": {"street": "Main Street"}}}. This method follows the mapper-specific raw lookup path and
     * returns only the claim value; the caller then writes that value to the externally visible path, for example
     * {@code ["credentialSubject", "address", "street"]} for JWT VC or {@code ["namespace", "address", "street"]}
     * for mDoc.
     */
    private Object getNestedClaimValue(Map<String, Object> claims, List<String> claimPath) {
        Object current = claims;
        for (String pathElement : claimPath) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> typedMap = (Map<String, Object>) currentMap;
            current = typedMap.get(pathElement);
            if (current == null) {
                return null;
            }
        }

        return current;
    }

    /**
     * Copies the mapper claim value into {@code claimsWithPrefix} using the externally visible credential path. This
     * is used for authorization_details validation and for mDoc issuance, where the credential subject is
     * namespace-shaped even though individual mappers write un-namespaced raw claims.
     *
     * @param claimsOrig Map with the original claims, which were returned by {@link #setClaim(Map, UserSessionModel)} . This method usually just reads from this map
     * @param claimsWithPrefix Map with the claims including path prefix. This method might write to this map
     */
    public void setClaimWithMetadataPrefix(Map<String, Object> claimsOrig, Map<String, Object> claimsWithPrefix) {
        List<String> attributePath = getMetadataAttributePath();
        if (attributePath.isEmpty()) {
            return;
        }

        Object claimValue = getNestedClaimValue(claimsOrig, getClaimLookupPath());
        if (claimValue != null) {
            Map<String, Object> current = claimsWithPrefix;

            for (int i = 0; i < attributePath.size(); i++) {
                String currentSnippetName = attributePath.get(i);
                if (i < attributePath.size() - 1) {
                    Map<String, Object> obj = (Map<String, Object>) current.get(currentSnippetName);
                    if (obj == null) {
                        obj = new HashMap<>();
                        current.put(currentSnippetName, obj);
                    }
                    current = obj;
                } else {
                    // Last element
                    current.put(currentSnippetName, claimValue);
                }
            }
        }
    }

}
