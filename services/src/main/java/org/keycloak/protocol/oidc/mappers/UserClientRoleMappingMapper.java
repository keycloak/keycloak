/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Allows mapping of user client role mappings to an ID and Access Token claim.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class UserClientRoleMappingMapper extends AbstractUserRoleMappingMapper {

    public static final String PROVIDER_ID = "oidc-usermodel-client-role-mapper";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {

        ProviderConfigProperty clientId = new ProviderConfigProperty();
        clientId.setName(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID);
        clientId.setLabel(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID_LABEL);
        clientId.setHelpText(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID_HELP_TEXT);
        clientId.setType(ProviderConfigProperty.CLIENT_LIST_TYPE);
        CONFIG_PROPERTIES.add(clientId);

        ProviderConfigProperty clientRolePrefix = new ProviderConfigProperty();
        clientRolePrefix.setName(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX);
        clientRolePrefix.setLabel(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX_LABEL);
        clientRolePrefix.setHelpText(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX_HELP_TEXT);
        clientRolePrefix.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(clientRolePrefix);

        ProviderConfigProperty multiValued = new ProviderConfigProperty();
        multiValued.setName(ProtocolMapperUtils.MULTIVALUED);
        multiValued.setLabel(ProtocolMapperUtils.MULTIVALUED_LABEL);
        multiValued.setHelpText(ProtocolMapperUtils.MULTIVALUED_HELP_TEXT);
        multiValued.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        multiValued.setDefaultValue(false);
        CONFIG_PROPERTIES.add(multiValued);

        OIDCAttributeMapperHelper.addAttributeConfig(CONFIG_PROPERTIES, UserClientRoleMappingMapper.class);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User Client Role";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a user client role to a token claim.";
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        String clientId = mappingModel.getConfig().get(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID);
        String rolePrefix = mappingModel.getConfig().get(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX);

        setClaim(token, mappingModel, userSession, getClientRoleFilter(clientId, userSession), rolePrefix);
    }

    private static Predicate<RoleModel> getClientRoleFilter(String clientId, UserSessionModel userSession) {
        if (clientId == null) {
            return RoleModel::isClientRole;
        }

        RealmModel clientRealm = userSession.getRealm();
        ClientModel client = clientRealm.getClientByClientId(clientId.trim());

        if (client == null) {
            return RoleModel::isClientRole;
        }

        ClientTemplateModel template = client.getClientTemplate();
        boolean useTemplateScope = template != null && client.useTemplateScope();
        boolean fullScopeAllowed = (useTemplateScope && template.isFullScopeAllowed()) || client.isFullScopeAllowed();

        Set<RoleModel> clientRoleMappings = client.getRoles();
        if (fullScopeAllowed) {
            return clientRoleMappings::contains;
        }

        Set<RoleModel> scopeMappings = new HashSet<>();

        if (useTemplateScope) {
            Set<RoleModel> templateScopeMappings = template.getScopeMappings();
            if (templateScopeMappings != null) {
                scopeMappings.addAll(templateScopeMappings);
            }
        }

        Set<RoleModel> clientScopeMappings = client.getScopeMappings();
        if (clientScopeMappings != null) {
            scopeMappings.addAll(clientScopeMappings);
        }

        return role -> clientRoleMappings.contains(role) && scopeMappings.contains(role);
    }

    public static ProtocolMapperModel create(String clientId, String clientRolePrefix,
                                             String name,
                                             String tokenClaimName,
                                             boolean accessToken, boolean idToken) {
        return create(clientId, clientRolePrefix, name, tokenClaimName, accessToken, idToken, false);

    }

    public static ProtocolMapperModel create(String clientId, String clientRolePrefix,
                                             String name,
                                             String tokenClaimName,
                                             boolean accessToken, boolean idToken, boolean multiValued) {
        ProtocolMapperModel mapper = OIDCAttributeMapperHelper.createClaimMapper(name, "foo",
          tokenClaimName, "String",
          true, name,
          accessToken, idToken,
          PROVIDER_ID);

        mapper.getConfig().put(ProtocolMapperUtils.MULTIVALUED, String.valueOf(multiValued));
        mapper.getConfig().put(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID, clientId);
        mapper.getConfig().put(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX, clientRolePrefix);
        return mapper;
    }
}
