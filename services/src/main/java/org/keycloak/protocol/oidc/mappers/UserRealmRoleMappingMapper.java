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

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.utils.RoleResolveUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows mapping of user realm role mappings to an ID and Access Token claim.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class UserRealmRoleMappingMapper extends AbstractUserRoleMappingMapper {

    public static final String PROVIDER_ID = "oidc-usermodel-realm-role-mapper";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {

        ProviderConfigProperty realmRolePrefix = new ProviderConfigProperty();
        realmRolePrefix.setName(ProtocolMapperUtils.USER_MODEL_REALM_ROLE_MAPPING_ROLE_PREFIX);
        realmRolePrefix.setLabel(ProtocolMapperUtils.USER_MODEL_REALM_ROLE_MAPPING_ROLE_PREFIX_LABEL);
        realmRolePrefix.setHelpText(ProtocolMapperUtils.USER_MODEL_REALM_ROLE_MAPPING_ROLE_PREFIX_HELP_TEXT);
        realmRolePrefix.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(realmRolePrefix);

        ProviderConfigProperty multiValued = new ProviderConfigProperty();
        multiValued.setName(ProtocolMapperUtils.MULTIVALUED);
        multiValued.setLabel(ProtocolMapperUtils.MULTIVALUED_LABEL);
        multiValued.setHelpText(ProtocolMapperUtils.MULTIVALUED_HELP_TEXT);
        multiValued.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        multiValued.setDefaultValue("true");
        CONFIG_PROPERTIES.add(multiValued);

        OIDCAttributeMapperHelper.addAttributeConfig(CONFIG_PROPERTIES, UserRealmRoleMappingMapper.class);
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
        return "User Realm Role";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a user realm role to a token claim.";
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession session, ClientSessionContext clientSessionCtx) {
        String rolePrefix = mappingModel.getConfig().get(ProtocolMapperUtils.USER_MODEL_REALM_ROLE_MAPPING_ROLE_PREFIX);

        AccessToken.Access access = RoleResolveUtil.getResolvedRealmRoles(session, clientSessionCtx, false);
        if (access == null) {
            return;
        }

        AbstractUserRoleMappingMapper.setClaim(token, mappingModel, access.getRoles(),null, rolePrefix);
    }

    public static ProtocolMapperModel create(String realmRolePrefix,
                                             String name,
                                             String tokenClaimName, boolean accessToken, boolean idToken) {

        return create(realmRolePrefix, name, tokenClaimName, accessToken, idToken, false);
    }

    public static ProtocolMapperModel create(String realmRolePrefix,
                                             String name,
                                             String tokenClaimName, boolean accessToken, boolean idToken, boolean multiValued) {
        ProtocolMapperModel mapper = OIDCAttributeMapperHelper.createClaimMapper(name, "foo",
          tokenClaimName, "String",
          accessToken, idToken, false,
          PROVIDER_ID);

        mapper.getConfig().put(ProtocolMapperUtils.MULTIVALUED, String.valueOf(multiValued));
        mapper.getConfig().put(ProtocolMapperUtils.USER_MODEL_REALM_ROLE_MAPPING_ROLE_PREFIX, realmRolePrefix);
        return mapper;
    }
}
