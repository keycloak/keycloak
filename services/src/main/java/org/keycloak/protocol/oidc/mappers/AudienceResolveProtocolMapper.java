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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.utils.RoleResolveUtil;

/**
 * Protocol mapper, which adds all client_ids of "allowed" clients to the audience field of the token. Allowed client means the client
 * for which user has at least one client role
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AudienceResolveProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();


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

        return token;
    }

    public static ProtocolMapperModel createClaimMapper(String name) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setConfig(Collections.emptyMap());
        return mapper;
    }
}
