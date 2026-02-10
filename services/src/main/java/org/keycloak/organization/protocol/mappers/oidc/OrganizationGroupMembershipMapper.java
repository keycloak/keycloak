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

package org.keycloak.organization.protocol.mappers.oidc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.TokenIntrospectionTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

public class OrganizationGroupMembershipMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper, TokenIntrospectionTokenMapper, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "oidc-organization-group-membership-mapper";

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> properties = new ArrayList<>();
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(properties, OrganizationGroupMembershipMapper.class);
        return properties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Organization Group Membership";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map user Organization group membership";
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel model, UserSessionModel userSession, KeycloakSession session, ClientSessionContext clientSessionCtx) {
        // Get organization ID from client session or resolve from scopes
        String orgId = clientSessionCtx.getClientSession().getNote(OrganizationModel.ORGANIZATION_ATTRIBUTE);
        Stream<OrganizationModel> organizations;

        if (orgId == null) {
            organizations = resolveFromRequestedScopes(session, userSession, clientSessionCtx);
        } else {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getById(orgId);
            organizations = org != null ? Stream.of(org) : Stream.empty();
        }

        UserModel user = userSession.getUser();
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);

        // Get or create organization claims map for composition
        final Map<String, Object> orgClaims = OIDCAttributeMapperHelper.getOrInitializeOrganizationClaimAsMap(token, OAuth2Constants.ORGANIZATION);

        // Add groups to each organization
        organizations.forEach(org -> {
            if (org == null || !org.isEnabled() || !org.isMember(user)) {
                return;
            }

            // Get user's groups in this organization
            List<String> groupPaths = orgProvider.getOrganizationGroupsByMember(org, user)
                .map(ModelToRepresentation::buildGroupPath)
                .collect(Collectors.toList());

            String orgAlias = org.getAlias();

            // Get or create organization data map
            Map<String, Object> orgData = (Map<String, Object>) orgClaims.get(orgAlias);
            if (orgData == null) {
                orgData = new HashMap<>();
                orgClaims.put(orgAlias, orgData);
            }

            // Add groups
            orgData.put("groups", groupPaths);
        });
    }

    private Stream<OrganizationModel> resolveFromRequestedScopes(KeycloakSession session, UserSessionModel userSession, ClientSessionContext context) {
        String rawScopes = context.getScopeString(true);
        OrganizationScope scope = OrganizationScope.valueOfScope(session, rawScopes);

        return scope.resolveOrganizations(userSession.getUser(), rawScopes, session);
    }

    public static ProtocolMapperModel create(String name, boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        if (accessToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        if (introspectionEndpoint) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, "true");
        mapper.setConfig(config);

        return mapper;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION);
    }

    @Override
    public int getPriority() {
        // Run after OrganizationMembershipMapper (higher number = later execution)
        return 10;
    }
}
