/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.broker.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.io.IOException;

/**
 * @author <a href="https://www.n-k.de">Niko Köbler</a>
 */
public class EntraOIDCIdentityProvider extends OIDCIdentityProvider {

    public EntraOIDCIdentityProvider(KeycloakSession session, EntraOIDCIdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        if (getConfig().getLogoutUrl() == null || getConfig().getLogoutUrl().trim().isEmpty()) return null;
        String idToken = userSession.getNote(FEDERATED_ID_TOKEN);
        String loginHint = extractLoginHintFromIdToken(idToken);
        if (getConfig().isBackchannelSupported()) {
            backchannelLogout(userSession, idToken);
            return null;
        } else {
            String sessionId = userSession.getId();
            UriBuilder logoutUri = UriBuilder.fromUri(getConfig().getLogoutUrl())
                    .queryParam("state", sessionId);
            if (getConfig().isSendIdTokenOnLogout() && idToken != null) {
                logoutUri.queryParam("id_token_hint", idToken);
            }
            if (((EntraOIDCIdentityProviderConfig) getConfig()).isSendLogoutHintOnLogout() && StringUtil.isNotBlank(loginHint)) {
                logoutUri.queryParam("logout_hint", loginHint);
            }
            if (getConfig().isSendClientIdOnLogout()) {
                logoutUri.queryParam("client_id", getConfig().getClientId());
            }
            String redirect = RealmsResource.brokerUrl(uriInfo)
                    .path(IdentityBrokerService.class, "getEndpoint")
                    .path(OIDCEndpoint.class, "logoutResponse")
                    .build(realm.getName(), getConfig().getAlias()).toString();
            logoutUri.queryParam("post_logout_redirect_uri", redirect);
            return Response.status(302).location(logoutUri.build()).build();
        }
    }

    protected String extractLoginHintFromIdToken(String idToken) {
        if (StringUtil.isBlank(idToken)) return null;
        try {
            JsonNode jsonNode = JsonSerialization.readValue(parseTokenInput(idToken, false), JsonNode.class);
            return jsonNode.path("login_hint").asText();
        } catch (IOException | IdentityBrokerException e) {
            logger.warn("Failed to extract loginHint from id_token.", e);
            return null;
        }
    }

}
