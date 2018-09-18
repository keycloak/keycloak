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

import org.keycloak.Config;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractOIDCProtocolMapper implements ProtocolMapper {

    public static final String TOKEN_MAPPER_CATEGORY = "Token mapper";

    @Override
    public String getProtocol() {
        return OIDCLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public void close() {

    }

    @Override
    public final ProtocolMapper create(KeycloakSession session) {
        throw new RuntimeException("UNSUPPORTED METHOD");
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    public AccessToken transformUserInfoToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                              UserSessionModel userSession, ClientSessionContext clientSessionCtx) {

        if (!OIDCAttributeMapperHelper.includeInUserInfo(mappingModel)) {
            return token;
        }

        setClaim(token, mappingModel, userSession, session, clientSessionCtx);
        return token;
    }

    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {

        if (!OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)){
            return token;
        }

        setClaim(token, mappingModel, userSession, session, clientSessionCtx);
        return token;
    }

    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                    UserSessionModel userSession, ClientSessionContext clientSessionCtx) {

        if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)){
            return token;
        }

        setClaim(token, mappingModel, userSession, session, clientSessionCtx);
        return token;
    }

    /**
     * Intended to be overridden in {@link ProtocolMapper} implementations to add claims to an token.
     * @param token
     * @param mappingModel
     * @param userSession
     *
     * @deprecated override {@link #setClaim(IDToken, ProtocolMapperModel, UserSessionModel, KeycloakSession, ClientSessionContext)} instead.
     */
    @Deprecated
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
    }

    /**
     * Intended to be overridden in {@link ProtocolMapper} implementations to add claims to an token.
     * @param token
     * @param mappingModel
     * @param userSession
     * @param keycloakSession
     * @param clientSessionCtx
     */
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx) {
        // we delegate to the old #setClaim(...) method for backwards compatibility
        setClaim(token, mappingModel, userSession);
    }
}
