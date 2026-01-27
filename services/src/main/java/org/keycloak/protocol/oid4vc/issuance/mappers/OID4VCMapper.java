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

import org.apache.commons.collections4.ListUtils;

import static org.keycloak.OID4VCConstants.CREDENTIAL_SUBJECT;
import static org.keycloak.VCFormat.SD_JWT_VC;

/**
 * Base class for OID4VC Mappers, to provide common configuration and functionality for all of them
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public abstract class OID4VCMapper implements ProtocolMapper, OID4VCEnvironmentProviderFactory {

    public static final String CLAIM_NAME = "claim.name";
    public static final String USER_ATTRIBUTE_KEY = "userAttribute";
    private static final List<ProviderConfigProperty> OID4VC_CONFIG_PROPERTIES = new ArrayList<>();

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
    }

    protected ProtocolMapperModel mapperModel;
    protected String format;

    protected abstract List<ProviderConfigProperty> getIndividualConfigProperties();

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Stream.concat(OID4VC_CONFIG_PROPERTIES.stream(), getIndividualConfigProperties().stream()).toList();
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
        return Optional.ofNullable(mapperModel.getConfig().get(CredentialScopeModel.INCLUDE_IN_METADATA))
                       .map(Boolean::parseBoolean)
                       .orElse(true);
    }

    /**
     * must return ordered list of attribute-names as they are added into the credential. This is required for the
     * metadata endpoint to add the appropriate path-attributes into the claim's description.
     *
     * @return the attribute path that is being mapped into the credential
     */
    public List<String> getMetadataAttributePath() {
        final String claimName = mapperModel.getConfig().get(CLAIM_NAME);
        final String userAttributeName = mapperModel.getConfig().get(USER_ATTRIBUTE_KEY);
        return ListUtils.union(getAttributePrefix(),
                               List.of(Optional.ofNullable(claimName).orElse(userAttributeName)));
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
     * Creates new map "claimsWithPrefix" with the resolved claims including path prefix
     *
     * @param claimsOrig Map with the original claims, which were returned by {@link #setClaim(Map, UserSessionModel)} . This method usually just reads from this map
     * @param claimsWithPrefix Map with the claims including path prefix. This method might write to this map
     */
    public void setClaimWithMetadataPrefix(Map<String, Object> claimsOrig, Map<String, Object> claimsWithPrefix) {
        List<String> attributePath = getMetadataAttributePath();
        String propertyName = attributePath.get(attributePath.size() - 1);
        if (claimsOrig.get(propertyName) != null) {
            Object claimValue = claimsOrig.get(propertyName);
            Map<String, Object> current = claimsWithPrefix;

            for (int i = 0; i < attributePath.size() ; i++) {
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
