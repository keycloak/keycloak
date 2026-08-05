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
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.protocol.mappers.OrganizationRoleMapperUtils;
import org.keycloak.organization.protocol.mappers.OrganizationRoleMapperUtils.OrganizationRoleClaims;
import org.keycloak.organization.utils.Organizations;
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

import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME;

public class OrganizationRoleMembershipMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper, TokenIntrospectionTokenMapper, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "oidc-organization-role-membership-mapper";

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> properties = new ArrayList<>();
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(properties, OrganizationRoleMembershipMapper.class);
        return properties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Organization Role Membership";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map user Organization role membership";
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel model, UserSessionModel userSession, KeycloakSession session, ClientSessionContext clientSessionCtx) {
        if (!Organizations.isEnabled(session)) {
            return;
        }

        ProtocolMapperModel organizationMapperModel = getOrganizationMapperModel(clientSessionCtx);
        if (organizationMapperModel == null) {
            return;
        }

        String orgClaimName = organizationMapperModel.getConfig().get(TOKEN_CLAIM_NAME);
        model = getEffectiveModel(session, userSession.getRealm(), model);
        model.getConfig().put(TOKEN_CLAIM_NAME, orgClaimName);

        Map<String, Object> orgClaims = OIDCAttributeMapperHelper.getOrInitializeOrganizationClaimAsMap(token, model);
        UserModel user = userSession.getUser();

        resolveOrganizations(session, userSession, clientSessionCtx).forEach(organization -> {
            OrganizationRoleClaims claims = OrganizationRoleMapperUtils.resolveRoleClaims(organization, user);
            if (claims.isEmpty()) {
                return;
            }

            String orgAlias = organization.getAlias();
            Map<String, Object> orgData = getOrCreateOrganizationData(orgClaims, orgAlias);
            OrganizationRoleMapperUtils.addToOrganizationClaim(orgData, claims);
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrCreateOrganizationData(Map<String, Object> orgClaims, String orgAlias) {
        Object current = orgClaims.get(orgAlias);
        if (current instanceof Map<?, ?>) {
            return (Map<String, Object>) current;
        }

        Map<String, Object> orgData = new HashMap<>();
        orgClaims.put(orgAlias, orgData);
        return orgData;
    }

    private Stream<OrganizationModel> resolveOrganizations(KeycloakSession session, UserSessionModel userSession, ClientSessionContext context) {
        String orgId = context.getClientSession().getNote(OrganizationModel.ORGANIZATION_ATTRIBUTE);
        if (orgId != null) {
            OrganizationProvider orgProvider = Organizations.getProvider(session);
            OrganizationModel organization = orgProvider.getById(orgId);
            return organization == null ? Stream.empty() : Stream.of(organization);
        }

        OrganizationModel organization = session.getContext().getOrganization();
        if (organization != null) {
            return Stream.of(organization);
        }

        String rawScopes = context.getScopeString(true);
        OrganizationScope scope = OrganizationScope.valueOfScope(session, rawScopes);
        return scope == null ? Stream.empty() : scope.resolveOrganizations(userSession.getUser(), rawScopes, session);
    }

    private ProtocolMapperModel getOrganizationMapperModel(ClientSessionContext clientSessionContext) {
        return clientSessionContext.getProtocolMappersStream()
                .filter(m -> OrganizationMembershipMapper.PROVIDER_ID.equals(m.getProtocolMapper()))
                .findAny()
                .orElse(null);
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
        return 20;
    }
}
