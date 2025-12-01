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

package org.keycloak.authentication.authenticators.broker;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.actiontoken.idpverifyemail.IdpVerifyAccountLinkActionToken;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdpEmailVerificationAuthenticator extends AbstractIdpAuthenticator {

    private static Logger logger = Logger.getLogger(IdpEmailVerificationAuthenticator.class);

    public static final String VERIFY_ACCOUNT_IDP_USERNAME = "VERIFY_ACCOUNT_IDP_USERNAME";

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        if (realm.getSmtpConfig().isEmpty()) {
            ServicesLogger.LOGGER.smtpNotConfigured();
            context.attempted();
            return;
        }

        if (Boolean.parseBoolean(context.getAuthenticationSession().getAuthNote(AbstractIdentityProvider.UPDATE_PROFILE_USERNAME_CHANGED))
                || Boolean.parseBoolean(context.getAuthenticationSession().getAuthNote(AbstractIdentityProvider.UPDATE_PROFILE_EMAIL_CHANGED))) {
            logger.debug("Email or username changed on review. Ignoring email verification authenticator.");
            context.attempted();
            return;
        }

        if (Objects.equals(authSession.getAuthNote(VERIFY_ACCOUNT_IDP_USERNAME), brokerContext.getUsername())) {
            UserModel existingUser = getExistingUser(session, realm, authSession);

            logger.debugf("User '%s' confirmed that wants to link with identity provider '%s' . Identity provider username is '%s' ", existingUser.getUsername(),
                    brokerContext.getIdpConfig().getAlias(), brokerContext.getUsername());

            context.setUser(existingUser);
            context.success();
            return;
        }

        UserModel existingUser = getExistingUser(session, realm, authSession);

        // Do not allow resending e-mail by simple page refresh
        if (! Objects.equals(authSession.getAuthNote(Constants.VERIFY_EMAIL_KEY), existingUser.getEmail())) {
            authSession.setAuthNote(Constants.VERIFY_EMAIL_KEY, existingUser.getEmail());
            sendVerifyEmail(session, context, existingUser, brokerContext);
        } else {
            showEmailSentPage(context, brokerContext);
        }
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        logger.debugf("Re-sending email requested for user, details follow");

        // This will allow user to re-send email again
        context.getAuthenticationSession().removeAuthNote(Constants.VERIFY_EMAIL_KEY);

        authenticateImpl(context, serializedCtx, brokerContext);
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    private void sendVerifyEmail(KeycloakSession session, AuthenticationFlowContext context, UserModel existingUser, BrokeredIdentityContext brokerContext) throws UriBuilderException, IllegalArgumentException {
        RealmModel realm = session.getContext().getRealm();
        UriInfo uriInfo = session.getContext().getUri();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        int validityInSecs = realm.getActionTokenGeneratedByUserLifespan(IdpVerifyAccountLinkActionToken.TOKEN_TYPE);
        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;

        EventBuilder event = context.getEvent().clone().event(EventType.SEND_IDENTITY_PROVIDER_LINK)
                .user(existingUser)
                .detail(Details.USERNAME, existingUser.getUsername())
                .detail(Details.EMAIL, existingUser.getEmail())
                .detail(Details.CODE_ID, authSession.getParentSession().getId())
                .removeDetail(Details.AUTH_METHOD)
                .removeDetail(Details.AUTH_TYPE);

        String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authSession).getEncodedId();
        IdpVerifyAccountLinkActionToken token = new IdpVerifyAccountLinkActionToken(
          existingUser.getId(), existingUser.getEmail(), absoluteExpirationInSecs, authSessionEncodedId,
          brokerContext.getUsername(), brokerContext.getIdpConfig().getAlias(), authSession.getClient().getClientId()
        );
        UriBuilder builder = Urls.actionTokenBuilder(uriInfo.getBaseUri(), token.serialize(session, realm, uriInfo),
                authSession.getClient().getClientId(), authSession.getTabId(), AuthenticationProcessor.getClientData(session, authSession));
        String link = builder
                .queryParam(Constants.EXECUTION, context.getExecution().getId())
                .build(realm.getName()).toString();
        long expirationInMinutes = TimeUnit.SECONDS.toMinutes(validityInSecs);

        try {
            context.getSession().getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .setAuthenticationSession(authSession)
                    .setUser(existingUser)
                    .setAttribute(EmailTemplateProvider.IDENTITY_PROVIDER_BROKER_CONTEXT, brokerContext)
                    .sendConfirmIdentityBrokerLink(link, expirationInMinutes);

            authSession.addRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);

            event.success();
        } catch (EmailException e) {
            event.error(Errors.EMAIL_SEND_FAILED);

            ServicesLogger.LOGGER.confirmBrokerEmailFailed(e);
            Response challenge = context.form()
                    .setError(Messages.EMAIL_SENT_ERROR)
                    .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, challenge);
            return;
        }

        showEmailSentPage(context, brokerContext);
    }


    protected void showEmailSentPage(AuthenticationFlowContext context, BrokeredIdentityContext brokerContext) {
        String accessCode = context.generateAccessCode();
        URI action = context.getActionUrl(accessCode);

        Response challenge = context.form()
                .setStatus(Response.Status.OK)
                .setAttribute(LoginFormsProvider.IDENTITY_PROVIDER_BROKER_CONTEXT, brokerContext)
                .setActionUri(action)
                .setExecution(context.getExecution().getId())
                .createIdpLinkEmailPage();
        context.forceChallenge(challenge);
    }

}
