/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.jboss.logging.Logger;

/**
 * Conditional authenticator to check if specified client-scope is present in the authentication request
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ConditionalClientScopeAuthenticator implements ConditionalAuthenticator {

    protected static final ConditionalClientScopeAuthenticator SINGLETON = new ConditionalClientScopeAuthenticator();

    private static final Logger logger = Logger.getLogger(ConditionalClientScopeAuthenticator.class);

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        final AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        if (configModel == null || configModel.getConfig() == null) {
            logger.warnf("No configuration defined for the conditional client scope authenticator.");
            return false;
        }

        final String clientScopeName = configModel.getConfig().get(ConditionalClientScopeAuthenticatorFactory.CLIENT_SCOPE);
        boolean negateOutput = Boolean.parseBoolean(configModel.getConfig().get(ConditionalClientScopeAuthenticatorFactory.CONF_NEGATE));
        if (clientScopeName == null) {
            logger.warnf("No client scope configured in the option '%s' of the configuration '%s'.", ConditionalClientScopeAuthenticatorFactory.CLIENT_SCOPE, configModel.getAlias());
            return false;
        }

        final RealmModel realm = context.getRealm();
        ClientScopeModel targetClientScope = KeycloakModelUtils.getClientScopeByName(context.getRealm(), clientScopeName);
        if (targetClientScope == null) {
            logger.warnf("No client scope '%s' defined in the realm '%s'.", clientScopeName, realm.getName());
            return false;
        }

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        boolean clientScopePresent = TokenManager.getRequestedClientScopes(context.getSession(), authSession.getClientNote(OAuth2Constants.SCOPE), authSession.getClient(), authSession.getAuthenticatedUser())
                .anyMatch(clientScope -> targetClientScope.getId().equals(clientScope.getId()));

        return negateOutput != clientScopePresent;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // no-op
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }
}
