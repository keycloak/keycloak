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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.utils.RoleResolveUtil;

/**
 * Add a role to a token
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HardcodedRole extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, UserInfoTokenMapper, TokenIntrospectionTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String ROLE_CONFIG = "role";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ROLE_CONFIG);
        property.setLabel("Role");
        property.setHelpText("Role you want added to the token.  Click 'Select Role' button to browse roles, or just type it in the textbox.  To reference a client role the syntax is clientname.clientrole, i.e. myclient.myrole");
        property.setType(ProviderConfigProperty.ROLE_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "oidc-hardcoded-role-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Hardcoded Role";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Hardcode a role into the access token.";
    }

    @Override
    public int getPriority() {
        return ProtocolMapperUtils.PRIORITY_HARDCODED_ROLE_MAPPER;
    }

    @Override
    public AccessToken transformUserInfoToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                              UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        // the mapper is always executed and then other role mappers decide if the claims are really set to the token
        setClaim(token, mappingModel, userSession, session, clientSessionCtx);
        return token;
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        // the mapper is always executed and then other role mappers decide if the claims are really set to the token
        setClaim(token, mappingModel, userSession, session, clientSessionCtx);
        return token;
    }

    @Override
    public AccessToken transformIntrospectionToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        // the mapper is always executed and then other role mappers decide if the claims are really set to the token
        setClaim(token, mappingModel, userSession, session, clientSessionCtx);
        return token;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession session,
                            ClientSessionContext clientSessionCtx) {

        String role = mappingModel.getConfig().get(ROLE_CONFIG);
        String[] scopedRole = KeycloakModelUtils.parseRole(role);
        String appName = scopedRole[0];
        String roleName = scopedRole[1];
        if (appName != null) {
            AccessToken.Access access = RoleResolveUtil.getResolvedClientRoles(session, clientSessionCtx, appName, true);
            access.addRole(roleName);
        } else {
            AccessToken.Access access = RoleResolveUtil.getResolvedRealmRoles(session, clientSessionCtx, true);
            access.addRole(role);
        }
    }

    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel client,
            ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        Map<String, String> config = mapperModel.getConfig();
        if (config == null || config.isEmpty()) return;

        String role = config.get(ROLE_CONFIG);
        if (role == null || role.isEmpty()) return;

        UserModel configuringUser = getConfiguringUser(session, realm);
        if (configuringUser == null) return;

        String[] parts = KeycloakModelUtils.parseRole(role);
        String clientId = parts[0];
        String roleName = parts[1];

        RoleModel roleModel;
        if (clientId != null) {
            ClientModel roleClient = realm.getClientByClientId(clientId);
            if (roleClient == null) return;
            roleModel = roleClient.getRole(roleName);
        } else {
            roleModel = realm.getRole(roleName);
        }

        if (roleModel == null) return;

        if (!configuringUser.hasRole(roleModel)) {
            throw hardcodedRoleRequiresRole();
        }
    }

    private UserModel getConfiguringUser(KeycloakSession session, RealmModel realm) throws ProtocolMapperConfigException {
        if (!(session.getContext().getBearerToken() instanceof org.keycloak.representations.JsonWebToken token)) {
            return null;
        }

        String subject = token.getSubject();
        if (subject == null) {
            throw hardcodedRoleRequiresRole();
        }

        UserModel configuringUser = session.users().getUserById(realm, subject);
        if (configuringUser == null) {
            throw hardcodedRoleRequiresRole();
        }

        return configuringUser;
    }

    private ProtocolMapperConfigException hardcodedRoleRequiresRole() {
        return new ProtocolMapperConfigException(
                "Authenticated user must hold the configured role to use a hardcoded role mapper",
                "error-hardcoded-role-requires-role");
    }

    public static ProtocolMapperModel create(String name,
                                             String role) {
        String mapperId = PROVIDER_ID;
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(mapperId);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(ROLE_CONFIG, role);
        mapper.setConfig(config);
        return mapper;

    }

}
