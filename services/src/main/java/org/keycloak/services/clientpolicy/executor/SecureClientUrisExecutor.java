/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.executor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisterContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdateContext;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureClientUrisExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    private static final Logger logger = Logger.getLogger(SecureClientUrisExecutor.class);

    private final KeycloakSession session;

    public SecureClientUrisExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return SecureClientUrisExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTER:
                if (context instanceof AdminClientRegisterContext || context instanceof DynamicClientRegisterContext) {
                    confirmSecureUris(((ClientCRUDContext)context).getProposedClientRepresentation());
                } else {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
                }
                return;
            case UPDATE:
                if (context instanceof AdminClientUpdateContext || context instanceof DynamicClientUpdateContext) {
                    confirmSecureUris(((ClientCRUDContext)context).getProposedClientRepresentation());
                } else {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
                }
                return;
            case AUTHORIZATION_REQUEST:
                confirmSecureRedirectUri(((AuthorizationRequestContext)context).getRedirectUri());
                return;
            default:
                return;
        }
    }

    private void confirmSecureUris(ClientRepresentation clientRep) throws ClientPolicyException {
        // rootUrl
        String rootUrl = clientRep.getRootUrl();
        if (rootUrl != null) confirmSecureUris(Arrays.asList(rootUrl), "rootUrl");

        // adminUrl
        String adminUrl = clientRep.getAdminUrl();
        if (adminUrl != null) confirmSecureUris(Arrays.asList(adminUrl), "adminUrl");

        // baseUrl
        String baseUrl = clientRep.getBaseUrl();
        if (baseUrl != null) confirmSecureUris(Arrays.asList(baseUrl), "baseUrl");

        // web origins
        List<String> webOrigins = clientRep.getWebOrigins();
        if (webOrigins != null) confirmSecureUris(webOrigins, "webOrigins");

        // backchannel logout URL
        String logoutUrl = Optional.ofNullable(clientRep.getAttributes()).orElse(Collections.emptyMap()).get(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL);
        if (logoutUrl != null) confirmSecureUris(Arrays.asList(logoutUrl), "logoutUrl");

        // OAuth2 : redirectUris
        List<String> redirectUris = clientRep.getRedirectUris();
        if (redirectUris != null) confirmSecureUris(redirectUris, "redirectUris");

        // OAuth2 : jwks_uri
        String jwksUri = Optional.ofNullable(clientRep.getAttributes()).orElse(Collections.emptyMap()).get(OIDCConfigAttributes.JWKS_URL);
        if (jwksUri != null) confirmSecureUris(Arrays.asList(jwksUri), "jwksUri");

        // OIDD : requestUris
        List<String> requestUris = getAttributeMultivalued(clientRep, OIDCConfigAttributes.REQUEST_URIS);
        if (requestUris != null) confirmSecureUris(requestUris, "requestUris");
    }

    private List<String> getAttributeMultivalued(ClientRepresentation clientRep, String attrKey) {
        String attrValue = Optional.ofNullable(clientRep.getAttributes()).orElse(Collections.emptyMap()).get(attrKey);
        if (attrValue == null) return Collections.emptyList();
        return Arrays.asList(Constants.CFG_DELIMITER_PATTERN.split(attrValue));
    }

    private void confirmSecureUris(List<String> uris, String uriType) throws ClientPolicyException {
        if (uris == null || uris.isEmpty()) {
            return;
        }

        for (String uri : uris) {
            logger.tracev("{0} = {1}", uriType, uri);
            if (!uri.startsWith("https://")  || uri.contains("*")) {
                throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Invalid " + uriType);
            }
        }
    }

    private void confirmSecureRedirectUri(String redirectUri) throws ClientPolicyException {
        if (redirectUri == null || redirectUri.isEmpty()) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "no redirect_uri specified.");
        }

        logger.tracev("Redirect URI = {0}", redirectUri);
        if (!redirectUri.startsWith("https://") || redirectUri.contains("*")) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid redirect_uri");
        }

    }
}