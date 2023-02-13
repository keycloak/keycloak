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
package org.keycloak.authentication.actiontoken.resetcred;

import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.actiontoken.*;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.LoginActionsServiceChecks.IsActionRequired;
import org.keycloak.sessions.CommonClientSessionModel.Action;
import javax.ws.rs.core.Response;

import static org.keycloak.services.resources.LoginActionsService.RESET_CREDENTIALS_PATH;

/**
 *
 * @author hmlnarik
 */
public class ResetCredentialsActionTokenHandler extends AbstractActionTokenHandler<ResetCredentialsActionToken> {

    public ResetCredentialsActionTokenHandler() {
        super(
          ResetCredentialsActionToken.TOKEN_TYPE,
          ResetCredentialsActionToken.class,
          Messages.RESET_CREDENTIAL_NOT_ALLOWED,
          EventType.RESET_PASSWORD,
          Errors.NOT_ALLOWED
        );

    }

    @Override
    public Predicate<? super ResetCredentialsActionToken>[] getVerifiers(ActionTokenContext<ResetCredentialsActionToken> tokenContext) {
        return TokenUtils.predicates(
            TokenUtils.checkThat(tokenContext.getRealm()::isResetPasswordAllowed, Errors.NOT_ALLOWED, Messages.RESET_CREDENTIAL_NOT_ALLOWED),

            verifyEmail(tokenContext),

            new IsActionRequired(tokenContext, Action.AUTHENTICATE)
        );
    }

    @Override
    public Response handleToken(ResetCredentialsActionToken token, ActionTokenContext tokenContext) {
        AuthenticationProcessor authProcessor = new ResetCredsAuthenticationProcessor();

        return tokenContext.processFlow(
          false,
          RESET_CREDENTIALS_PATH,
          tokenContext.getRealm().getResetCredentialsFlow(),
          null,
          authProcessor
        );
    }

    @Override
    public boolean canUseTokenRepeatedly(ResetCredentialsActionToken token, ActionTokenContext tokenContext) {
        return false;
    }

    public static class ResetCredsAuthenticationProcessor extends AuthenticationProcessor {

        @Override
        protected Response authenticationComplete() {
            boolean firstBrokerLoginInProgress = (authenticationSession.getAuthNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE) != null);
            if (firstBrokerLoginInProgress) {

                UserModel linkingUser = AbstractIdpAuthenticator.getExistingUser(session, realm, authenticationSession);
                SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(authenticationSession, AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
                authenticationSession.setAuthNote(AbstractIdpAuthenticator.FIRST_BROKER_LOGIN_SUCCESS, serializedCtx.getIdentityProviderId());

                logger.debugf("Forget-password flow finished when authenticated user '%s' after first broker login with identity provider '%s'.",
                        linkingUser.getUsername(), serializedCtx.getIdentityProviderId());

                return LoginActionsService.redirectToAfterBrokerLoginEndpoint(session, realm, uriInfo, authenticationSession, true);
            } else {
                return super.authenticationComplete();
            }
        }

    }
}
