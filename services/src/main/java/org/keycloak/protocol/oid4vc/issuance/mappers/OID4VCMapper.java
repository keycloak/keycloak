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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.OID4VCEnvironmentProviderFactory;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.provider.ProviderConfigProperty;

import org.apache.commons.collections4.ListUtils;

import static org.keycloak.OID4VCConstants.CREDENTIAL_SUBJECT;

/**
 * Base class for OID4VC Mappers, to provide common configuration and functionality for all of them
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public abstract class OID4VCMapper implements ProtocolMapper, OID4VCEnvironmentProviderFactory {

    public static final String CLAIM_NAME = "claim.name";
    public static final String USER_ATTRIBUTE_KEY = "userAttribute";
    private static final List<ProviderConfigProperty> OID4VC_CONFIG_PROPERTIES = new ArrayList<>();
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
        return switch (Optional.ofNullable(format).orElse("")) {
            case Format.JWT_VC, Format.LDP_VC -> List.of(CREDENTIAL_SUBJECT);
            default -> Collections.emptyList();
        };
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
    public abstract void setClaimsForCredential(VerifiableCredential verifiableCredential,
                                                UserSessionModel userSessionModel);

    /**
     * Set the claims to the credential subject.
     */
    public abstract void setClaimsForSubject(Map<String, Object> claims,
                                             UserSessionModel userSessionModel);

}
