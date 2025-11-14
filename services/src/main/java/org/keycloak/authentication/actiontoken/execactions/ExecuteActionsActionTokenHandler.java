/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication.actiontoken.execactions;

import java.util.Objects;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.TokenVerifier;
import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.TokenUtils;
import org.keycloak.authentication.requiredactions.util.RequiredActionsValidator;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;

import static org.keycloak.models.utils.DefaultRequiredActions.getDefaultRequiredActionCaseInsensitively;

/**
 *
 * @author hmlnarik
 */
public class ExecuteActionsActionTokenHandler extends AbstractActionTokenHandler<ExecuteActionsActionToken> {

    public ExecuteActionsActionTokenHandler() {
        super(
          ExecuteActionsActionToken.TOKEN_TYPE,
          ExecuteActionsActionToken.class,
          Messages.INVALID_CODE,
          EventType.EXECUTE_ACTIONS,
          Errors.NOT_ALLOWED
        );
    }

    @Override
    public Predicate<? super ExecuteActionsActionToken>[] getVerifiers(ActionTokenContext<ExecuteActionsActionToken> tokenContext) {
        return TokenUtils.predicates(
          TokenUtils.checkThat(
            // either redirect URI is not specified or must be valid for the client
            t -> t.getRedirectUri() == null
                 || RedirectUtils.verifyRedirectUri(tokenContext.getSession(), t.getRedirectUri(),
                      tokenContext.getAuthenticationSession().getClient()) != null,
            Errors.INVALID_REDIRECT_URI,
            Messages.INVALID_REDIRECT_URI
          ),

          verifyEmail(tokenContext),
          verifyRequiredActions(tokenContext)
        );
    }

    @Override
    public Response handleToken(ExecuteActionsActionToken token, ActionTokenContext<ExecuteActionsActionToken> tokenContext) {
        AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();
        final UriInfo uriInfo = tokenContext.getUriInfo();
        final RealmModel realm = tokenContext.getRealm();
        final KeycloakSession session = tokenContext.getSession();
        if (tokenContext.isAuthenticationSessionFresh()) {
            // Update the authentication session in the token
            String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authSession).getEncodedId();
            token.setCompoundAuthenticationSessionId(authSessionEncodedId);
            UriBuilder builder = Urls.actionTokenBuilder(uriInfo.getBaseUri(), token.serialize(session, realm, uriInfo),
                    authSession.getClient().getClientId(), authSession.getTabId(), AuthenticationProcessor.getClientData(session, authSession));
            String confirmUri = builder.build(realm.getName()).toString();

            return session.getProvider(LoginFormsProvider.class)
                    .setAuthenticationSession(authSession)
                    .setUser(authSession.getAuthenticatedUser())
                    .setSuccess(Messages.CONFIRM_EXECUTION_OF_ACTIONS)
                    .setAttribute(Constants.TEMPLATE_ATTR_ACTION_URI, confirmUri)
                    .setAttribute(Constants.TEMPLATE_ATTR_REQUIRED_ACTIONS, token.getRequiredActions())
                    .createInfoPage();
        }

        String redirectUri = RedirectUtils.verifyRedirectUri(tokenContext.getSession(), token.getRedirectUri(), authSession.getClient());

        if (redirectUri != null) {
            authSession.setAuthNote(AuthenticationManager.SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS, "true");

            authSession.setRedirectUri(redirectUri);
            authSession.setClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);
        }

        token.getRequiredActions().stream().forEach(authSession::addRequiredAction);

        UserModel user = tokenContext.getAuthenticationSession().getAuthenticatedUser();
        // verify user email as we know it is valid as this entry point would never have gotten here.
        user.setEmailVerified(true);

        String nextAction = AuthenticationManager.nextRequiredAction(tokenContext.getSession(), authSession, tokenContext.getRequest(), tokenContext.getEvent());
        return AuthenticationManager.redirectToRequiredActions(tokenContext.getSession(), tokenContext.getRealm(), authSession, tokenContext.getUriInfo(), nextAction);
    }

    @Override
    public boolean canUseTokenRepeatedly(ExecuteActionsActionToken token, ActionTokenContext<ExecuteActionsActionToken> tokenContext) {
        RealmModel realm = tokenContext.getRealm();
        KeycloakSessionFactory sessionFactory = tokenContext.getSession().getKeycloakSessionFactory();

        return token.getRequiredActions().stream()
          .map(realm::getRequiredActionProviderByAlias)    // get realm-specific model from action name and filter out irrelevant
          .filter(Objects::nonNull)
          .filter(RequiredActionProviderModel::isEnabled)

          .map(RequiredActionProviderModel::getProviderId)      // get provider ID from model

          .map(providerId -> (RequiredActionFactory) sessionFactory.getProviderFactory(RequiredActionProvider.class, getDefaultRequiredActionCaseInsensitively(providerId)))
          .filter(Objects::nonNull)

          .noneMatch(RequiredActionFactory::isOneTimeAction);
    }

    // Verify required actions included in the token are valid
    protected TokenVerifier.Predicate<ExecuteActionsActionToken> verifyRequiredActions(ActionTokenContext<ExecuteActionsActionToken> tokenContext) {
        return TokenUtils.checkThat(t -> RequiredActionsValidator.validRequiredActions(tokenContext.getSession(), t.getRequiredActions()),
                Errors.RESOLVE_REQUIRED_ACTIONS, Messages.INVALID_TOKEN_REQUIRED_ACTIONS
        );
    }
}
