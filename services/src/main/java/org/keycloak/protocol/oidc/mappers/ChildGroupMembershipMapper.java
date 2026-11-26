/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

/**
 * Maps the names of the groups that are children (direct or indirect) of a configured parent group.
 * Direct membership in the parent group itself does not count as a "child" and is excluded.
 */
public class ChildGroupMembershipMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper, TokenIntrospectionTokenMapper {

    public static final String PROVIDER_ID = "oidc-child-group-membership-mapper";
    public static final String PARENT_GROUP_PATH = "parent.group.path";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty parentGroup = new ProviderConfigProperty();
        parentGroup.setName(PARENT_GROUP_PATH);
        parentGroup.setLabel("Parent group path");
        parentGroup.setType(ProviderConfigProperty.STRING_TYPE);
        parentGroup.setHelpText("Full path of the parent group whose children should be mapped, e.g. /parent-group");
        configProperties.add(parentGroup);

        ProviderConfigProperty multivalued = new ProviderConfigProperty();
        multivalued.setName("multivalued");
        multivalued.setLabel("Multivalued");
        multivalued.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        multivalued.setDefaultValue("true");
        multivalued.setHelpText("Always return the groups claim as a JSON array, even with a single match.");
        configProperties.add(multivalued);

        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, ChildGroupMembershipMapper.class);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Child Group Membership";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Maps the names of groups that are children (direct or indirect) of a configured parent group.";
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                             KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        String parentPath = mappingModel.getConfig().get(PARENT_GROUP_PATH);
        if (parentPath == null || parentPath.isBlank()) {
            return;
        }

        UserModel user = userSession.getUser();
        List<String> childGroupNames = user.getGroupsStream()
                .filter(group -> isDescendantOf(group, parentPath))
                .map(GroupModel::getName)
                .collect(Collectors.toList());

        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, childGroupNames);
    }

    private boolean isDescendantOf(GroupModel group, String parentPath) {
        GroupModel current = group.getParent();
        while (current != null) {
            if (KeycloakModelUtils.buildGroupPath(current).equals(parentPath)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
