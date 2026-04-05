/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.utils.RoleResolveUtil;

import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION;

/**
 * Protocol mapper, which adds all client_ids of "allowed" clients to the audience field of the token. Allowed client means the client
 * for which user has at least one client role
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AudienceResolveProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, TokenIntrospectionTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
    static {
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, AudienceResolveProtocolMapper.class);
    }

    public static final String PROVIDER_ID = "oidc-audience-resolve-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Audience Resolve";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Adds all client_ids of \"allowed\" clients to the audience field of the token. Allowed client means the client\n" +
                " for which user has at least one client role";
    }

    @Override
    public int getPriority() {
        return ProtocolMapperUtils.PRIORITY_AUDIENCE_RESOLVE_MAPPER;
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        boolean shouldUseLightweightToken = getShouldUseLightweightToken(session);
        boolean includeInAccessToken = shouldUseLightweightToken ?  OIDCAttributeMapperHelper.includeInLightweightAccessToken(mappingModel) : includeInAccessToken(mappingModel);
        if (!includeInAccessToken){
            return token;
        }
        setAudience(token, clientSessionCtx, session);
        return token;
    }

    private boolean includeInAccessToken(ProtocolMapperModel mappingModel) {
        String includeInAccessToken = mappingModel.getConfig().get(INCLUDE_IN_ACCESS_TOKEN);

        // Backwards compatibility
        if (includeInAccessToken == null) {
            return true;
        }

        return "true".equals(includeInAccessToken);
    }

    @Override
    public AccessToken transformIntrospectionToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                                   UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        if (!includeInIntrospection(mappingModel)) {
            return token;
        }
        setAudience(token, clientSessionCtx, session);
        return token;
    }

    private boolean includeInIntrospection(ProtocolMapperModel mappingModel) {
        String includeInIntrospection = mappingModel.getConfig().get(INCLUDE_IN_INTROSPECTION);

        // Backwards compatibility
        if (includeInIntrospection == null) {
            return true;
        }

        return "true".equals(includeInIntrospection);
    }

    @Override
    public ProtocolMapperModel getEffectiveModel(KeycloakSession session, RealmModel realm, ProtocolMapperModel protocolMapperModel) {
        // Effectively clone
        ProtocolMapperModel copy = RepresentationToModel.toModel(ModelToRepresentation.toRepresentation(protocolMapperModel));

        copy.getConfig().put(INCLUDE_IN_ACCESS_TOKEN, String.valueOf(includeInAccessToken(copy)));
        copy.getConfig().put(INCLUDE_IN_INTROSPECTION, String.valueOf(includeInIntrospection(copy)));

        return copy;
    }

    private void setAudience(AccessToken token, ClientSessionContext clientSessionCtx, KeycloakSession session) {
        String clientId = clientSessionCtx.getClientSession().getClient().getClientId();

        for (Map.Entry<String, AccessToken.Access> entry : RoleResolveUtil.getAllResolvedClientRoles(session, clientSessionCtx).entrySet()) {
            // Don't add client itself to the audience
            if (entry.getKey().equals(clientId)) {
                continue;
            }

            AccessToken.Access access = entry.getValue();
            if (access != null && access.getRoles() != null && !access.getRoles().isEmpty()) {
                token.addAudience(entry.getKey());
            }
        }
    }

    public static ProtocolMapperModel createClaimMapper(String name, boolean accessToken, boolean introspectionEndpoint) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        if (accessToken) {
            config.put(INCLUDE_IN_ACCESS_TOKEN, "true");
        } else {
            config.put(INCLUDE_IN_ACCESS_TOKEN, "false");
        }
        if (introspectionEndpoint) {
            config.put(INCLUDE_IN_INTROSPECTION, "true");
        } else {
            config.put(INCLUDE_IN_INTROSPECTION, "false");
        }
        mapper.setConfig(config);
        return mapper;
    }
}
