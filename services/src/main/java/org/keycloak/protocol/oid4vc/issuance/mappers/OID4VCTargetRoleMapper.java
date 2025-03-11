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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.model.Role;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adds the users roles to the credential subject
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCTargetRoleMapper extends OID4VCMapper {

    private static final Logger LOGGER = Logger.getLogger(OID4VCTargetRoleMapper.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String MAPPER_ID = "oid4vc-target-role-mapper";
    public static final String SUBJECT_PROPERTY_CONFIG_KEY = "subjectProperty";
    public static final String CLIENT_CONFIG_KEY = "clientId";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty subjectPropertyNameConfig = new ProviderConfigProperty();
        subjectPropertyNameConfig.setName(SUBJECT_PROPERTY_CONFIG_KEY);
        subjectPropertyNameConfig.setLabel("Roles Property Name");
        subjectPropertyNameConfig.setHelpText("Property to add the roles to in the credential subject.");
        subjectPropertyNameConfig.setDefaultValue("roles");
        subjectPropertyNameConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(subjectPropertyNameConfig);
    }

    @Override
    protected List<ProviderConfigProperty> getIndividualConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getDisplayType() {
        return "Target-Role Mapper";
    }

    @Override
    public String getHelpText() {
        return "Map the assigned role to the credential subject, providing the client id as the target.";
    }

    public static ProtocolMapperModel create(String clientId, String name) {
        var mapperModel = new ProtocolMapperModel();
        mapperModel.setName(name);
        Map<String, String> configMap = new HashMap<>();
        configMap.put(SUBJECT_PROPERTY_CONFIG_KEY, "roles");
        configMap.put(CLIENT_CONFIG_KEY, clientId);
        mapperModel.setConfig(configMap);
        mapperModel.setProtocol(OID4VCLoginProtocolFactory.PROTOCOL_ID);
        mapperModel.setProtocolMapper(MAPPER_ID);
        return mapperModel;
    }

    @Override
    public ProtocolMapper create(KeycloakSession session) {
        return new OID4VCTargetRoleMapper();
    }

    @Override
    public String getId() {
        return MAPPER_ID;
    }

    @Override
    public void setClaimsForCredential(VerifiableCredential verifiableCredential,
                                       UserSessionModel userSessionModel) {
        // nothing to do for the mapper.
    }

    @Override
    public void setClaimsForSubject(Map<String, Object> claims,
                                    UserSessionModel userSessionModel) {
        String client = mapperModel.getConfig().get(CLIENT_CONFIG_KEY);
        String propertyName = mapperModel.getConfig().get(SUBJECT_PROPERTY_CONFIG_KEY);
        ClientModel clientModel = userSessionModel.getRealm().getClientByClientId(client);
        if (clientModel == null) {
            LOGGER.warnf("Client %s not found in realm.", client);
            return;
        }

        // Retrieve only the roles assigned to the user for this specific client
        List<RoleModel> userRoles = userSessionModel.getUser().getClientRoleMappingsStream(clientModel).toList();
        if (userRoles.isEmpty()) {
            LOGGER.debugf("No roles assigned to client '%s'. Skipping claim assignment.", client);
            return;
        }

        // Create ClientRoleModel and convert to roles claim
        ClientRoleModel clientRoleModel = new ClientRoleModel(clientModel.getClientId(), userRoles);
        Role rolesClaim = toRolesClaim(clientRoleModel);
        if (rolesClaim.getNames().isEmpty()) {
            LOGGER.debugf("No valid role names found for client '%s'. Skipping claim assignment.", client);
            return;
        }

        Map<String, Object> modelMap = OBJECT_MAPPER.convertValue(rolesClaim, new TypeReference<>() {});

        Object existingProperty = claims.get(propertyName);
        if (existingProperty == null) {
            Set<Map<String, Object>> roles = new HashSet<>();
            roles.add(modelMap);
            claims.put(propertyName, roles);
        } else if (existingProperty instanceof Set<?> rawSet) {
            if (rawSet.stream().allMatch(item -> item instanceof Map<?, ?>)) {
                @SuppressWarnings("unchecked")
                Set<Map<String, Object>> rolesProperty = (Set<Map<String, Object>>) rawSet;
                rolesProperty.add(modelMap);
            } else {
                LOGGER.warnf("Claim '%s' contains incompatible types. Expected Set<Map<String, Object>>, found '%s'. Skipping role assignment for client '%s'.",
                        propertyName, existingProperty.getClass().getSimpleName(), client);
            }
        } else {
            LOGGER.warnf("Claim '%s' is of type '%s', expected Set. Skipping role assignment for client '%s'.",
                    propertyName, existingProperty.getClass().getSimpleName(), client);
        }
    }

    private Role toRolesClaim(ClientRoleModel crm) {
        Set<String> roleNames = crm
                .getRoleModels()
                .stream()
                .map(RoleModel::getName)
                .collect(Collectors.toSet());
        return new Role(roleNames, crm.getClientId());
    }

    private static class ClientRoleModel {
        private final String clientId;
        private final List<RoleModel> roleModels;

        public ClientRoleModel(String clientId, List<RoleModel> roleModels) {
            this.clientId = clientId;
            this.roleModels = roleModels;
        }

        public String getClientId() {
            return clientId;
        }

        public List<RoleModel> getRoleModels() {
            return roleModels;
        }
    }
}
