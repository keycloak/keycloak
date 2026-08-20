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

package org.keycloak.protocol.oidc.utils;

import java.net.URI;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.Encode;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Utilities for OIDC logout
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LogoutUtil {

    public static Response sendResponseAfterLogoutFinished(KeycloakSession session, AuthenticationSessionModel logoutSession) {
        String redirectUri = logoutSession.getAuthNote(OIDCLoginProtocol.LOGOUT_REDIRECT_URI);
        URI finalRedirectUri = getRedirectUriWithAttachedState(redirectUri, logoutSession);
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientModel(logoutSession.getClient());
        LoginFormsProvider loginFormsProvider = session.getProvider(LoginFormsProvider.class);

        if (finalRedirectUri != null) {
            if (!config.isLogoutConfirmationEnabled()) {
                return Response.status(302).location(finalRedirectUri).build();
            }
            loginFormsProvider.setAttribute("pageRedirectUri", finalRedirectUri.toString());
        }

        SystemClientUtil.checkSkipLink(session, logoutSession);

        return loginFormsProvider
                .setSuccess(Messages.SUCCESS_LOGOUT)
                .setDetachedAuthSession()
                .createInfoPage();
    }


    public static URI getRedirectUriWithAttachedState(String redirectUri, AuthenticationSessionModel logoutSession) {
        if (redirectUri == null) return null;
        String state = logoutSession.getAuthNote(OIDCLoginProtocol.LOGOUT_STATE_PARAM);

        KeycloakUriBuilder uriBuilder = KeycloakUriBuilder.fromUri(redirectUri, false);
        if (state != null) {
            removeQueryParamByDecodedName(uriBuilder, OIDCLoginProtocol.STATE_PARAM);
            uriBuilder.queryParam(OIDCLoginProtocol.STATE_PARAM, state);
        }
        return uriBuilder.build();
    }

    private static void removeQueryParamByDecodedName(KeycloakUriBuilder uriBuilder, String name) {
        String query = uriBuilder.getQuery();
        if (query == null || query.isEmpty()) return;

        StringBuilder cleaned = new StringBuilder();
        for (String param : query.split("&")) {
            int eqPos = param.indexOf('=');
            String rawName = eqPos >= 0 ? param.substring(0, eqPos) : param;
            if (name.equals(Encode.decode(rawName))) continue;
            if (!cleaned.isEmpty()) cleaned.append("&");
            cleaned.append(param);
        }
        uriBuilder.replaceQuery(!cleaned.isEmpty() ? cleaned.toString() : null, false);
    }
}
