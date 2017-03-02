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

package org.keycloak.authentication.authenticators.resetcred;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.*;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;

import java.util.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResetCredentialEmail implements Authenticator, AuthenticatorFactory {

    public static final String PROVIDER_ID = "reset-credential-email";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        LoginActionsService.createActionCookie(context.getRealm(), context.getUriInfo(), context.getConnection(), context.getClientSession().getId());

        UserModel user = context.getUser();
        String username = context.getClientSession().getNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);

        // we don't want people guessing usernames, so if there was a problem obtaining the user, the user will be null.
        // just reset login for with a success message
        if (user == null) {
            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
            return;
        }


        EventBuilder event = context.getEvent();
        // we don't want people guessing usernames, so if there is a problem, just continuously challenge
        if (user.getEmail() == null || user.getEmail().trim().length() == 0) {
            event.user(user)
                    .detail(Details.USERNAME, username)
                    .error(Errors.INVALID_EMAIL);

            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
            return;
        }

        int validityInSecs = context.getRealm().getAccessCodeLifespanUserAction();
        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;
        // We send the secret in the email in a link as a query param.
        // The nonce must match what is stored in the action token store.
        // Client session ID is not part of the link in the e-mail.
        ResetCredentialsActionToken token = new ResetCredentialsActionToken(user.getId(), absoluteExpirationInSecs, null, context.getClientSession());
        KeycloakSession keycloakSession = context.getSession();
        keycloakSession.getProvider(ActionTokenStoreProvider.class)
          .addActionToken(token, token);
        String link = UriBuilder
          .fromUri(context.getActionUrl())
          .queryParam(Constants.KEY, token.serialize(token, keycloakSession, context.getRealm(), context.getUriInfo()))
          .build()
          .toString();
        long expirationInMinutes = TimeUnit.SECONDS.toMinutes(validityInSecs);
        try {

            context.getSession().getProvider(EmailTemplateProvider.class).setRealm(context.getRealm()).setUser(user).sendPasswordReset(link, expirationInMinutes);
            event.clone().event(EventType.SEND_RESET_PASSWORD)
                         .user(user)
                         .detail(Details.USERNAME, username)
                         .detail(Details.EMAIL, user.getEmail()).detail(Details.CODE_ID, context.getClientSession().getId()).success();
            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
        } catch (EmailException e) {
            event.clone().event(EventType.SEND_RESET_PASSWORD)
                    .detail(Details.USERNAME, username)
                    .user(user)
                    .error(Errors.EMAIL_SEND_FAILED);
            ServicesLogger.LOGGER.failedToSendPwdResetEmail(e);
            Response challenge = context.form()
                    .setError(Messages.EMAIL_SENT_ERROR)
                    .createErrorPage();
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, challenge);
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        KeycloakSession keycloakSession = context.getSession();
        String actionTokenString = context.getUriInfo().getQueryParameters().getFirst(Constants.KEY);
        ResetCredentialsActionToken tokenFromMail = null;
        try {
            tokenFromMail = ResetCredentialsActionToken.deserialize(keycloakSession, context.getRealm(), context.getUriInfo(), actionTokenString);
        } catch (VerificationException ex) {
            context.getEvent().detail(Details.REASON, ex.getMessage()).error(Errors.EXPIRED_CODE);
            Response challenge = context.form()
                    .setError(Messages.INVALID_ACCESS_CODE)
                    .createErrorPage();
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, challenge);
        }

        String userId = tokenFromMail == null ? null : tokenFromMail.getUserId();
        UUID nonce = tokenFromMail == null ? null : tokenFromMail.getActionVerificationNonce();

        // Can only guess once!  We remove the note so another guess can't happen
        ActionTokenValueModel tokenFromStore = keycloakSession.getProvider(ActionTokenStoreProvider.class)
          .removeActionToken(ResetCredentialsActionToken.key(userId), nonce);

        if (tokenFromMail == null || tokenFromMail.getActionVerificationNonce() == null || tokenFromStore == null) {
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            Response challenge = context.form()
                    .setError(Messages.INVALID_ACCESS_CODE)
                    .createErrorPage();
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, challenge);
            return;
        }

        UUID secretFromMail = tokenFromMail.getActionVerificationNonce();
        UUID secretFromStore = tokenFromStore.getActionVerificationNonce();
        String clientSessionId = tokenFromStore.getNote(ResetCredentialsActionToken.NOTE_CLIENT_SESSION_ID);
        ClientSessionModel clientSession = clientSessionId == null ? null : keycloakSession.sessions().getClientSession(clientSessionId);

        if (secretFromMail == null || clientSession == null || ! Objects.equals(secretFromMail, secretFromStore)) {
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            Response challenge = context.form()
                    .setError(Messages.INVALID_ACCESS_CODE)
                    .createErrorPage();
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, challenge);
            return;
        }

        // We now know email is valid, so set it to valid.
        context.getUser().setEmailVerified(true);
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public String getDisplayType() {
        return "Send Reset Email";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Send email to user and wait for response.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
