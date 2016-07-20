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

package org.keycloak.authentication.authenticators.browser;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.util.TokenUtil;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CookieAuthenticator implements Authenticator {

    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(context.getSession(),
                context.getRealm(), true);
        if (authResult == null) {
            context.attempted();
        } else {
            // Cookie re-authentication is skipped if re-authentication is required
            if (requireReauthentication(authResult.getSession(), context.getClientSession())) {
                context.attempted();
            } else {
                ClientSessionModel clientSession = context.getClientSession();
                clientSession.setNote(AuthenticationManager.SSO_AUTH, "true");

                context.setUser(authResult.getUser());
                context.attachUserSession(authResult.getSession());
                context.success();
            }
        }

    }

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {

    }

    protected boolean requireReauthentication(UserSessionModel userSession, ClientSessionModel clientSession) {
        return isPromptLogin(clientSession) || isAuthTimeExpired(userSession, clientSession);
    }

    protected boolean isPromptLogin(ClientSessionModel clientSession) {
        String prompt = clientSession.getNote(OIDCLoginProtocol.PROMPT_PARAM);
        return TokenUtil.hasPrompt(prompt, OIDCLoginProtocol.PROMPT_VALUE_LOGIN);
    }

    protected boolean isAuthTimeExpired(UserSessionModel userSession, ClientSessionModel clientSession) {
        String authTime = userSession.getNote(AuthenticationManager.AUTH_TIME);
        String maxAge = clientSession.getNote(OIDCLoginProtocol.MAX_AGE_PARAM);
        if (maxAge == null) {
            return false;
        }

        int authTimeInt = authTime==null ? 0 : Integer.parseInt(authTime);
        int maxAgeInt = Integer.parseInt(maxAge);

        if (authTimeInt + maxAgeInt < Time.currentTime()) {
            logger.debugf("Authentication time is expired in CookieAuthenticator. userSession=%s, clientId=%s, maxAge=%d, authTime=%d", userSession.getId(),
                    clientSession.getClient().getId(), maxAgeInt, authTimeInt);
            return true;
        }

        return false;
    }
}
