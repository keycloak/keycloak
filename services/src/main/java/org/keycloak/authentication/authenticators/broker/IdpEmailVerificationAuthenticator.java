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

import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.requiredactions.VerifyEmail;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdpEmailVerificationAuthenticator extends AbstractIdpAuthenticator {

    protected static ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        ClientSessionModel clientSession = context.getClientSession();

        if (realm.getSmtpConfig().size() == 0) {
            logger.smtpNotConfigured();
            context.attempted();
            return;
        }

        // Create action cookie to detect if email verification happened in same browser
        LoginActionsService.createActionCookie(context.getRealm(), context.getUriInfo(), context.getConnection(), context.getClientSession().getId());

        VerifyEmail.setupKey(clientSession);

        UserModel existingUser = getExistingUser(session, realm, clientSession);

        String link = UriBuilder.fromUri(context.getActionUrl())
                .queryParam(Constants.KEY, clientSession.getNote(Constants.VERIFY_EMAIL_KEY))
                .build().toString();

        EventBuilder event = context.getEvent().clone().event(EventType.SEND_IDENTITY_PROVIDER_LINK)
                .user(existingUser)
                .detail(Details.USERNAME, existingUser.getUsername())
                .detail(Details.EMAIL, existingUser.getEmail())
                .detail(Details.CODE_ID, clientSession.getId())
                .removeDetail(Details.AUTH_METHOD)
                .removeDetail(Details.AUTH_TYPE);

        long expiration = TimeUnit.SECONDS.toMinutes(context.getRealm().getAccessCodeLifespanUserAction());
        try {

            context.getSession().getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .setUser(existingUser)
                    .setAttribute(EmailTemplateProvider.IDENTITY_PROVIDER_BROKER_CONTEXT, brokerContext)
                    .sendConfirmIdentityBrokerLink(link, expiration);

            event.success();
        } catch (EmailException e) {
            event.error(Errors.EMAIL_SEND_FAILED);

            logger.confirmBrokerEmailFailed(e);
            Response challenge = context.form()
                    .setError(Messages.EMAIL_SENT_ERROR)
                    .createErrorPage();
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, challenge);
            return;
        }

        Response challenge = context.form()
                .setStatus(Response.Status.OK)
                .setAttribute(LoginFormsProvider.IDENTITY_PROVIDER_BROKER_CONTEXT, brokerContext)
                .createIdpLinkEmailPage();
        context.forceChallenge(challenge);
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        MultivaluedMap<String, String> queryParams = context.getSession().getContext().getUri().getQueryParameters();
        String key = queryParams.getFirst(Constants.KEY);
        ClientSessionModel clientSession = context.getClientSession();
        RealmModel realm = context.getRealm();
        KeycloakSession session = context.getSession();

        if (key != null) {
            String keyFromSession = clientSession.getNote(Constants.VERIFY_EMAIL_KEY);
            clientSession.removeNote(Constants.VERIFY_EMAIL_KEY);
            if (key.equals(keyFromSession)) {
                UserModel existingUser = getExistingUser(session, realm, clientSession);

                logger.debugf("User '%s' confirmed that wants to link with identity provider '%s' . Identity provider username is '%s' ", existingUser.getUsername(),
                        brokerContext.getIdpConfig().getAlias(), brokerContext.getUsername());

                String actionCookieValue = LoginActionsService.getActionCookie(session.getContext().getRequestHeaders(), realm, session.getContext().getUri(), context.getConnection());
                if (actionCookieValue == null || !actionCookieValue.equals(clientSession.getId())) {
                    clientSession.setNote(IS_DIFFERENT_BROWSER, "true");
                }

                context.setUser(existingUser);
                context.success();
            } else {
                logger.keyParamDoesNotMatch();
                Response challengeResponse = context.form()
                        .setError(Messages.INVALID_ACCESS_CODE)
                        .createErrorPage();
                context.failureChallenge(AuthenticationFlowError.IDENTITY_PROVIDER_ERROR, challengeResponse);
            }
        } else {
            Response challengeResponse = context.form()
                    .setError(Messages.MISSING_PARAMETER, Constants.KEY)
                    .createErrorPage();
            context.failureChallenge(AuthenticationFlowError.IDENTITY_PROVIDER_ERROR, challengeResponse);
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }
}
