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
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Allows mapping of user client role mappings to an ID and Access Token claim.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class UserClientRoleMappingMapper extends AbstractUserRoleMappingMapper {

    public static final String PROVIDER_ID = "oidc-usermodel-client-role-mapper";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<ProviderConfigProperty>();

    static {

        ProviderConfigProperty clientId = new ProviderConfigProperty();
        clientId.setName(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID);
        clientId.setLabel(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID_LABEL);
        clientId.setHelpText(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID_HELP_TEXT);
        clientId.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(clientId);

        ProviderConfigProperty clientRolePrefix = new ProviderConfigProperty();
        clientRolePrefix.setName(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX);
        clientRolePrefix.setLabel(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX_LABEL);
        clientRolePrefix.setHelpText(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX_HELP_TEXT);
        clientRolePrefix.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(clientRolePrefix);

        OIDCAttributeMapperHelper.addAttributeConfig(CONFIG_PROPERTIES);
    }

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

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {

        UserModel user = userSession.getUser();

        String clientId = mappingModel.getConfig().get(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID);
        if (clientId != null) {

            ClientModel clientModel = userSession.getRealm().getClientByClientId(clientId.trim());
            Set<RoleModel> clientRoleMappings = user.getClientRoleMappings(clientModel);

            String rolePrefix = mappingModel.getConfig().get(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX);
            Set<String> clientRoleNames = flattenRoleModelToRoleNames(clientRoleMappings, rolePrefix);

            OIDCAttributeMapperHelper.mapClaim(token, mappingModel, clientRoleNames);
        }
    }
}
