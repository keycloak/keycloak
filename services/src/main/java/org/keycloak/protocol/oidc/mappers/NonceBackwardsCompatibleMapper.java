/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.AccessToken;

/**
 * <p>Simple mapper that adds the nonce claim into the access token as before.
 * Just adding the mapper reverts back to full old behavior in all the
 * tokens (access, refresh and ID).</p>
 *
 * @author rmartinc
 */
public class NonceBackwardsCompatibleMapper implements OIDCAccessTokenMapper, ProtocolMapper {

    public static final String PROVIDER_ID = "oidc-nonce-backwards-compatible-mapper";

    @Override
    public String getProtocol() {
        return OIDCLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public final ProtocolMapper create(KeycloakSession session) {
        return new NonceBackwardsCompatibleMapper();
    }

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Nonce backwards compatible";
    }

    @Override
    public String getDisplayCategory() {
        return AbstractOIDCProtocolMapper.TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Adds the nonce claim to Access, Refresh and ID token";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create().build();
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        token.setNonce(clientSessionCtx.getAttribute(OIDCLoginProtocol.NONCE_PARAM, String.class));
        return token;
    }

    public static ProtocolMapperModel create(String name) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        mapper.setConfig(config);
        return mapper;
    }
}
