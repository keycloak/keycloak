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
package org.keycloak.authentication.actiontoken.idpverifyemail;

import org.keycloak.authentication.actiontoken.AbstractActionTokenHander;
import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.actiontoken.*;
import org.keycloak.authentication.authenticators.broker.IdpEmailVerificationAuthenticator;
import org.keycloak.events.*;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.AuthenticationSessionProvider;
import java.util.Collections;
import javax.ws.rs.core.Response;

/**
 * Action token handler for verification of e-mail address.
 * @author hmlnarik
 */
public class IdpVerifyAccountLinkActionTokenHandler extends AbstractActionTokenHander<IdpVerifyAccountLinkActionToken> {

    public IdpVerifyAccountLinkActionTokenHandler() {
        super(
          IdpVerifyAccountLinkActionToken.TOKEN_TYPE,
          IdpVerifyAccountLinkActionToken.class,
          Messages.STALE_CODE,
          EventType.IDENTITY_PROVIDER_LINK_ACCOUNT,
          Errors.INVALID_TOKEN
        );
    }

    @Override
    public Predicate<? super IdpVerifyAccountLinkActionToken>[] getVerifiers(ActionTokenContext<IdpVerifyAccountLinkActionToken> tokenContext) {
        return TokenUtils.predicates(
        );
    }

    @Override
    public Response handleToken(IdpVerifyAccountLinkActionToken token, ActionTokenContext<IdpVerifyAccountLinkActionToken> tokenContext) {
        UserModel user = tokenContext.getAuthenticationSession().getAuthenticatedUser();
        EventBuilder event = tokenContext.getEvent();

        event.event(EventType.IDENTITY_PROVIDER_LINK_ACCOUNT)
          .detail(Details.EMAIL, user.getEmail())
          .detail(Details.IDENTITY_PROVIDER, token.getIdentityProviderAlias())
          .detail(Details.IDENTITY_PROVIDER_USERNAME, token.getIdentityProviderUsername())
          .success();

        // verify user email as we know it is valid as this entry point would never have gotten here.
        user.setEmailVerified(true);

        AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();
        if (tokenContext.isAuthenticationSessionFresh()) {
            AuthenticationSessionManager asm = new AuthenticationSessionManager(tokenContext.getSession());
            asm.removeAuthenticationSession(tokenContext.getRealm(), authSession, true);

            AuthenticationSessionProvider authSessProvider = tokenContext.getSession().authenticationSessions();
            authSession = authSessProvider.getAuthenticationSession(tokenContext.getRealm(), token.getAuthenticationSessionId());

            if (authSession != null) {
                authSession.setAuthNote(IdpEmailVerificationAuthenticator.VERIFY_ACCOUNT_IDP_USERNAME, token.getIdentityProviderUsername());
            } else {
                authSessProvider.updateNonlocalSessionAuthNotes(
                  token.getAuthenticationSessionId(),
                  Collections.singletonMap(IdpEmailVerificationAuthenticator.VERIFY_ACCOUNT_IDP_USERNAME, token.getIdentityProviderUsername())
                );
            }

            return tokenContext.getSession().getProvider(LoginFormsProvider.class)
                    .setSuccess(Messages.IDENTITY_PROVIDER_LINK_SUCCESS, token.getIdentityProviderAlias(), token.getIdentityProviderUsername())
                    .setAttribute(Constants.SKIP_LINK, true)
                    .createInfoPage();
        }

        authSession.setAuthNote(IdpEmailVerificationAuthenticator.VERIFY_ACCOUNT_IDP_USERNAME, token.getIdentityProviderUsername());

        return tokenContext.brokerFlow(null, authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH));
    }

}
