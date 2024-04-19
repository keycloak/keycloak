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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sets an ID for the credential, either randomly generated or statically configured
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCSubjectIdMapper extends OID4VCMapper {

    public static final String MAPPER_ID = "oid4vc-subject-id-mapper";
    public static final String ID_KEY = "subjectIdProperty";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty idPropertyNameConfig = new ProviderConfigProperty();
        idPropertyNameConfig.setName(ID_KEY);
        idPropertyNameConfig.setLabel("ID Property Name");
        idPropertyNameConfig.setHelpText("Name of the property to contain the id.");
        idPropertyNameConfig.setDefaultValue("id");
        idPropertyNameConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(idPropertyNameConfig);
    }

    @Override
    protected List<ProviderConfigProperty> getIndividualConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    public static ProtocolMapperModel create(String name, String subjectId) {
        var mapperModel = new ProtocolMapperModel();
        mapperModel.setName(name);
        Map<String, String> configMap = new HashMap<>();
        configMap.put(ID_KEY, subjectId);
        configMap.put(SUPPORTED_CREDENTIALS_KEY, "VerifiableCredential");
        mapperModel.setConfig(configMap);
        mapperModel.setProtocol(OID4VCLoginProtocolFactory.PROTOCOL_ID);
        mapperModel.setProtocolMapper(MAPPER_ID);
        return mapperModel;
    }

    public void setClaimsForCredential(VerifiableCredential verifiableCredential,
                                       UserSessionModel userSessionModel) {
        // nothing to do for the mapper.
    }

    @Override
    public void setClaimsForSubject(Map<String, Object> claims, UserSessionModel userSessionModel) {
        claims.put("id", mapperModel.getConfig().getOrDefault(ID_KEY, String.format("urn:uuid:%s", UUID.randomUUID())));
    }

    @Override
    public String getDisplayType() {
        return "CredentialSubject ID Mapper";
    }

    @Override
    public String getHelpText() {
        return "Assigns a subject ID to the credentials subject. If no specific id is configured, a randomly generated one is used.";
    }

    @Override
    public ProtocolMapper create(KeycloakSession session) {
        return new OID4VCSubjectIdMapper();
    }

    @Override
    public String getId() {
        return MAPPER_ID;
    }
}