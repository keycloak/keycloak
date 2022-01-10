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
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.utils.RoleResolveUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Map an assigned role to a different position and name in the token
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleNameMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String ROLE_CONFIG = "role";
    public static String NEW_ROLE_NAME = "new.role.name";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ROLE_CONFIG);
        property.setLabel("Role");
        property.setHelpText("Role name you want changed.  Click 'Select Role' button to browse roles, or just type it in the textbox.  To reference a client role the syntax is clientname.clientrole, i.e. myclient.myrole");
        property.setType(ProviderConfigProperty.ROLE_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(NEW_ROLE_NAME);
        property.setLabel("New Role Name");
        property.setHelpText("The new role name.  The new name format corresponds to where in the access token the role will be mapped to.  So, a new name of 'myapp.newname' will map the role to that position in the access token.  A new name of 'newname' will map the role to the realm roles in the token.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "oidc-role-name-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Role Name Mapper";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map an assigned role to a new name or position in the token.";
    }

    @Override
    public int getPriority() {
        return ProtocolMapperUtils.PRIORITY_ROLE_NAMES_MAPPER;
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        String role = mappingModel.getConfig().get(ROLE_CONFIG);
        String newName = mappingModel.getConfig().get(NEW_ROLE_NAME);

        String[] scopedRole = KeycloakModelUtils.parseRole(role);
        String[] newScopedRole = KeycloakModelUtils.parseRole(newName);
        String appName = scopedRole[0];
        String roleName = scopedRole[1];
        if (appName != null) {
            AccessToken.Access access = RoleResolveUtil.getResolvedClientRoles(session, clientSessionCtx, appName, false);
            if (access == null) return token;
            if (!access.getRoles().contains(roleName)) return token;
            access.getRoles().remove(roleName);
        } else {
            AccessToken.Access access = RoleResolveUtil.getResolvedRealmRoles(session, clientSessionCtx, false);
            if (access == null || !access.getRoles().contains(roleName)) return token;
            access.getRoles().remove(roleName);
        }

        String newAppName = newScopedRole[0];
        String newRoleName = newScopedRole[1];
        AccessToken.Access access = null;
        if (newAppName == null) {
            access = RoleResolveUtil.getResolvedRealmRoles(session, clientSessionCtx, true);
        } else {
            access = RoleResolveUtil.getResolvedClientRoles(session, clientSessionCtx, newAppName, true);
        }

        access.addRole(newRoleName);
        return token;
    }

    public static ProtocolMapperModel create(String name,
                                             String role,
                                             String newName) {
        String mapperId = PROVIDER_ID;
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(mapperId);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(ROLE_CONFIG, role);
        config.put(NEW_ROLE_NAME, newName);
        mapper.setConfig(config);
        return mapper;

    }

}
