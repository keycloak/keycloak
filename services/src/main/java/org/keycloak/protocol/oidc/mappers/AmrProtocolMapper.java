/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.protocol.oidc.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.AmrUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import org.jboss.logging.Logger;

/**
 * @author Ben Cresitello-Dittmar
 * This protocol mapper sets the 'amr' claim on the OIDC tokens to the reference values configured on the
 * completed authenticators found in the user session notes.
 */
public class AmrProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper {

    private static final Logger logger = Logger.getLogger(AmrProtocolMapper.class);

    public static final String PROVIDER_ID = "oidc-amr-mapper";

    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = new ArrayList<>();
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, AmrProtocolMapper.class);
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Authentication Method Reference (AMR)";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Add authentication method reference (AMR) to the token.";
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx) {
        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        List<String> amr = getAmr(clientSession, userSession.getRealm());
        token.setOtherClaims(OAuth2Constants.AUTHENTICATOR_METHOD_REFERENCE, amr);
    }

    public static ProtocolMapperModel create(String name, boolean accessToken, boolean idToken) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        if (accessToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        mapper.setConfig(config);
        return mapper;
    }

    /**
     * Extract the AMR values from the existing session.
     *
     * @param clientSession The existing authenticated session
     * @param realmModel The realm the mapper is executed in. Used to get the execution configuration.
     * @return The authenticator reference values associated with the completed executions
     */
    protected List<String> getAmr(AuthenticatedClientSessionModel clientSession, RealmModel realmModel) {
        Map<String, Integer> executions = AuthenticatorUtils.parseCompletedExecutions(clientSession.getUserSession().getNote(Constants.AUTHENTICATORS_COMPLETED));
        logger.debugf("found the following completed authentication executions: %s", executions.toString());
        List<String> refs = AmrUtils.getAuthenticationExecutionReferences(executions, realmModel);
        logger.debugf("amr %s set in token", refs);
        return refs;
    }
}
