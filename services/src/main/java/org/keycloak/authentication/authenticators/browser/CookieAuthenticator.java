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

import java.net.URI;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.authenticators.util.AcrStore;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationScope;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.AuthenticationFlowURLHelper;
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
            if (restartIfAuthenticationSessionRootRemoved(context)) {
                return;
            }
            context.attempted();
        } else {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            LoginProtocol protocol = context.getSession().getProvider(LoginProtocol.class, authSession.getProtocol());
            authSession.setAuthNote(Constants.LOA_MAP, authResult.session().getNote(Constants.LOA_MAP));
            context.setUser(authResult.user());
            AcrStore acrStore = new AcrStore(context.getSession(), authSession);

            // Cookie re-authentication is skipped if re-authentication is required
            if (protocol.requireReauthentication(authResult.session(), authSession)) {
                // Full re-authentication, so we start with no loa
                acrStore.setLevelAuthenticatedToCurrentRequest(Constants.NO_LOA);
                authSession.setAuthNote(AuthenticationManager.FORCED_REAUTHENTICATION, "true");
                context.setForwardedInfoMessage(Messages.REAUTHENTICATE);
                context.attempted();
            } else if(AuthenticatorUtil.isForkedFlow(authSession)){
                context.attempted();
            } else {
                String topLevelFlowId = context.getTopLevelFlow().getId();
                int previouslyAuthenticatedLevel = acrStore.getHighestAuthenticatedLevelFromPreviousAuthentication(topLevelFlowId);
                AuthenticatorUtils.updateCompletedExecutions(context.getAuthenticationSession(), authResult.session(), context.getExecution().getId());

                // an SSO session satisfies level 0 also when no level was recorded for it
                if (acrStore.getRequestedLevelOfAuthentication(context.getTopLevelFlow()) > Math.max(previouslyAuthenticatedLevel, Constants.MINIMUM_LOA)) {
                    // Step-up authentication, we keep the loa from the existing user session.
                    // The cookie alone is not enough and other authentications must follow.
                    acrStore.setLevelAuthenticatedToCurrentRequest(previouslyAuthenticatedLevel);

                    if (authSession.getClientNote(Constants.KC_ACTION) != null) {
                        context.setForwardedInfoMessage(Messages.AUTHENTICATE_STRONG);
                    }

                    context.attempted();
                } else {
                    // Cookie only authentication
                    acrStore.setLevelAuthenticatedToCurrentRequest(previouslyAuthenticatedLevel);
                    authSession.setAuthNote(AuthenticationManager.SSO_AUTH, authResult.user().getId());
                    context.attachUserSession(authResult.session());

                    if (isOrganizationContext(context)) {
                        // if re-authenticating in the scope of an organization, an organization must be resolved prior to authenticating the user
                        context.attempted();
                    } else {
                        context.success();
                    }
                }
            }
        }

    }

    /**
     * Validating the identity cookie removes the root authentication session that has the id of an expired user
     * session. The login can be running in that very root: the authorization endpoint joins the root that the
     * AUTH_SESSION_ID cookie points to. The flow would then continue with an authentication session that no longer
     * exists, and the login form it shows could not be submitted. The root cannot be kept, as its id would become the
     * id of the next user session. Restart the flow in a fresh root with a random id instead.
     */
    private boolean restartIfAuthenticationSessionRootRemoved(AuthenticationFlowContext context) {
        KeycloakSession session = context.getSession();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        if (session.authenticationSessions().getRootAuthenticationSession(context.getRealm(), authSession.getParentSession().getId()) != null) {
            return false;
        }

        AuthenticationSessionModel restarted = AuthenticationProcessor.recreate(session, authSession);
        AuthenticationProcessor.resetFlow(restarted, context.getFlowPath());
        session.getContext().setAuthenticationSession(restarted);

        URI flowUrl = new AuthenticationFlowURLHelper(session, context.getRealm(), context.getUriInfo()).getLastExecutionUrl(restarted);
        context.forceChallenge(Response.status(Response.Status.FOUND).location(flowUrl).build());
        return true;
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

    private boolean isOrganizationContext(AuthenticationFlowContext context) {
        KeycloakSession session = context.getSession();

        if (Organizations.isEnabledAndOrganizationsPresent(session)) {
            return OrganizationScope.valueOfScope(session) != null;
        }

        return false;
    }
}
