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

import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.TokenUtils;
import org.keycloak.authentication.authenticators.broker.IdpEmailVerificationAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserModel;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Action token handler for verification of e-mail address.
 * @author hmlnarik
 */
public class IdpVerifyAccountLinkActionTokenHandler extends AbstractActionTokenHandler<IdpVerifyAccountLinkActionToken> {

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
            verifyEmail(tokenContext)
        );
    }

    @Override
    public Response handleToken(IdpVerifyAccountLinkActionToken token, ActionTokenContext<IdpVerifyAccountLinkActionToken> tokenContext) {
        UserModel user = tokenContext.getAuthenticationSession().getAuthenticatedUser();
        EventBuilder event = tokenContext.getEvent();
        final UriInfo uriInfo = tokenContext.getUriInfo();
        final RealmModel realm = tokenContext.getRealm();
        final KeycloakSession session = tokenContext.getSession();

        event.event(EventType.IDENTITY_PROVIDER_LINK_ACCOUNT)
          .detail(Details.EMAIL, user.getEmail())
          .detail(Details.IDENTITY_PROVIDER, token.getIdentityProviderAlias())
          .detail(Details.IDENTITY_PROVIDER_USERNAME, token.getIdentityProviderUsername());

        AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();

        if (authSession.getAuthNote(IdpEmailVerificationAuthenticator.VERIFY_ACCOUNT_IDP_USERNAME) != null) {
            return sendEmailAlreadyVerified(session, event, user);
        }

        AuthenticationSessionManager asm = new AuthenticationSessionManager(session);

        if (tokenContext.isAuthenticationSessionFresh()) {
            AuthenticationSessionCompoundId compoundId = AuthenticationSessionCompoundId.encoded(token.getCompoundAuthenticationSessionId());
            ClientModel originalClient = realm.getClientById(compoundId.getClientUUID());
            AuthenticationSessionModel origAuthSession = asm.getAuthenticationSessionByIdAndClient(realm,
                    compoundId.getRootSessionId(), originalClient, compoundId.getTabId());
            if (origAuthSession == null || origAuthSession.getAuthNote(IdpEmailVerificationAuthenticator.VERIFY_ACCOUNT_IDP_USERNAME) != null) {
                return sendEmailAlreadyVerified(session, event, user);
            }

            token.setOriginalCompoundAuthenticationSessionId(token.getCompoundAuthenticationSessionId());

            String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authSession).getEncodedId();
            token.setCompoundAuthenticationSessionId(authSessionEncodedId);
            UriBuilder builder = Urls.actionTokenBuilder(uriInfo.getBaseUri(), token.serialize(session, realm, uriInfo),
                    authSession.getClient().getClientId(), authSession.getTabId(), AuthenticationProcessor.getClientData(session, authSession));
            String confirmUri = builder.build(realm.getName()).toString();

            LoginFormsProvider forms = session.getProvider(LoginFormsProvider.class);
            return forms.setAuthenticationSession(authSession)
                    .setAttribute("messageHeader", forms.getMessage(Messages.CONFIRM_ACCOUNT_LINKING, token.getIdentityProviderUsername(), token.getIdentityProviderAlias()))
                    .setSuccess(Messages.CONFIRM_ACCOUNT_LINKING_BODY, token.getIdentityProviderUsername(), token.getIdentityProviderAlias())
                    .setAttribute(Constants.TEMPLATE_ATTR_ACTION_URI, confirmUri)
                    .createInfoPage();
        }

        // verify user email as we know it is valid as this entry point would never have gotten here.
        user.setEmailVerified(true);
        event.success();

        if (token.getOriginalCompoundAuthenticationSessionId() != null) {
            asm.removeAuthenticationSession(realm, authSession, true);

            AuthenticationSessionCompoundId compoundId = AuthenticationSessionCompoundId.encoded(token.getOriginalCompoundAuthenticationSessionId());
            ClientModel originalClient = realm.getClientById(compoundId.getClientUUID());
            authSession = asm.getAuthenticationSessionByIdAndClient(realm, compoundId.getRootSessionId(), originalClient, compoundId.getTabId());

            if (authSession != null) {
                authSession.setAuthNote(IdpEmailVerificationAuthenticator.VERIFY_ACCOUNT_IDP_USERNAME, token.getIdentityProviderUsername());
            }

            setUserVerifiedSingleObject(token, realm, session, user);

            return session.getProvider(LoginFormsProvider.class)
                    .setAuthenticationSession(authSession)
                    .setSuccess(Messages.IDENTITY_PROVIDER_LINK_SUCCESS, token.getIdentityProviderAlias(), token.getIdentityProviderUsername())
                    .setAttribute(Constants.SKIP_LINK, true)
                    .createInfoPage();
        }

        authSession.setAuthNote(IdpEmailVerificationAuthenticator.VERIFY_ACCOUNT_IDP_USERNAME, token.getIdentityProviderUsername());

        return tokenContext.brokerFlow(null, null, authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH));
    }

    private void setUserVerifiedSingleObject(IdpVerifyAccountLinkActionToken token, RealmModel realm, KeycloakSession session, UserModel user) {
        int singleObjectLifespan = realm.getActionTokenGeneratedByUserLifespan();
        String userId = user.getId();
        String idpAlias = token.getIdentityProviderAlias();
        session.singleUseObjects().put(getUserVerifiedSingleObjectKey(userId, idpAlias), singleObjectLifespan, Map.of());
    }

    public static boolean runIfUserVerified(KeycloakSession session, UserModel user, IdentityProviderModel broker, Runnable runnable) {
        if (user == null) {
            return false;
        }

        SingleUseObjectProvider singleObjects = session.singleUseObjects();
        String singleObjectKey = getUserVerifiedSingleObjectKey(user.getId(), broker.getAlias());
        boolean isUserVerified = singleObjects.remove(singleObjectKey) != null;

        if (isUserVerified) {
            runnable.run();
        }

        return isUserVerified;
    }

    private static String getUserVerifiedSingleObjectKey(String userId, String idpAlias) {
        return "kc.brokering.user.verified." + userId  + "." + idpAlias;
    }

    private Response sendEmailAlreadyVerified(KeycloakSession session, EventBuilder event, UserModel user) {
        event.user(user).error(Errors.EMAIL_ALREADY_VERIFIED);
        return session.getProvider(LoginFormsProvider.class)
                .setAuthenticationSession(session.getContext().getAuthenticationSession())
                .setInfo(Messages.EMAIL_VERIFIED_ALREADY, user.getEmail())
                .createInfoPage();
    }
}
