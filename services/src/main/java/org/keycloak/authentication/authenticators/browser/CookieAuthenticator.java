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
import org.keycloak.authentication.authenticators.util.AcrStore;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CookieAuthenticator implements Authenticator {

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
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            LoginProtocol protocol = context.getSession().getProvider(LoginProtocol.class, authSession.getProtocol());
            authSession.setAuthNote(Constants.LOA_MAP, authResult.getSession().getNote(Constants.LOA_MAP));
            context.setUser(authResult.getUser());
            AcrStore acrStore = new AcrStore(authSession);

            // Cookie re-authentication is skipped if re-authentication is required
            if (protocol.requireReauthentication(authResult.getSession(), authSession)) {
                // Full re-authentication, so we start with no loa
                acrStore.setLevelAuthenticatedToCurrentRequest(Constants.NO_LOA);
                authSession.setAuthNote(AuthenticationManager.FORCED_REAUTHENTICATION, "true");
                context.setForwardedInfoMessage(Messages.REAUTHENTICATE);
                context.attempted();
            } else {
                int previouslyAuthenticatedLevel = acrStore.getHighestAuthenticatedLevelFromPreviousAuthentication();
                if (acrStore.getRequestedLevelOfAuthentication() > previouslyAuthenticatedLevel) {
                    // Step-up authentication, we keep the loa from the existing user session.
                    // The cookie alone is not enough and other authentications must follow.
                    acrStore.setLevelAuthenticatedToCurrentRequest(previouslyAuthenticatedLevel);
                    context.attempted();
                } else {
                    // Cookie only authentication
                    acrStore.setLevelAuthenticatedToCurrentRequest(previouslyAuthenticatedLevel);
                    authSession.setAuthNote(AuthenticationManager.SSO_AUTH, "true");
                    context.attachUserSession(authResult.getSession());
                    context.success();
                }
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
}
