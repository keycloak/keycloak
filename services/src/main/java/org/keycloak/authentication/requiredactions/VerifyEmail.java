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

package org.keycloak.authentication.requiredactions;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.verifyemail.VerifyEmailActionToken;
import org.keycloak.authentication.requiredactions.util.EmailCooldownManager;
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
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.policy.MaxAuthAgePasswordPolicyProviderFactory;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.jboss.logging.Logger;

import static org.keycloak.services.managers.AuthenticationManager.NEW_USER_REGISTERED;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class VerifyEmail implements RequiredActionProvider, RequiredActionFactory {
    public static final String EMAIL_RESEND_COOLDOWN_KEY_PREFIX = "verify-email-cooldown-";
    private static final Logger logger = Logger.getLogger(VerifyEmail.class);

    // Auth note to set that verifyEmail is triggered during registration
    private static final String VERIFY_EMAIL_DURING_REGISTRATION = "VERIFY_EMAIL_DURING_REGISTRATION";

    // Absolute expiry shared by every polling token issued for the current verification email, so that
    // re-rendering the page (a refresh, or the resend cooldown) cannot extend it past that email's own link.
    private static final String VERIFY_EMAIL_POLLING_EXPIRATION = "VERIFY_EMAIL_POLLING_EXPIRATION";

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getRealm().isVerifyEmail() && !context.getUser().isEmailVerified()) {
            if (Validation.isBlank(context.getUser().getEmail())) {
                logger.debug("Skipping VERIFY_EMAIL because the user has no email set");
                return;
            }
            // Don't add VERIFY_EMAIL if UPDATE_EMAIL is already present (UPDATE_EMAIL takes precedence)
            if (context.getUser().getRequiredActionsStream().noneMatch(action -> UserModel.RequiredAction.UPDATE_EMAIL.name().equals(action))) {
                context.getUser().addRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
                logger.debug("User is required to verify email");
            } else {
                logger.debug("Skipping VERIFY_EMAIL because UPDATE_EMAIL is already present");
            }
        }
    }

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        process(context, true);
    }

    private void process(RequiredActionContext context, boolean isChallenge) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        if (context.getUser().isEmailVerified()) {
            if ("true".equals(context.getAuthenticationSession().getAuthNote(NEW_USER_REGISTERED))) {
                // Email was verified in a different browser/tab while this registration session was still open.
                // Show a success page rather than silently redirecting to the login restart page.
                Response response = context.form()
                        .setUser(context.getUser())
                        .createVerifyEmailSuccessPage();
                context.challenge(response);
                return;
            }
            context.success();
            authSession.removeAuthNote(Constants.VERIFY_EMAIL_KEY);
            return;
        }

        // When triggered during registration, make sure to add requiredAction also to the authSession
        if ("true".equals(context.getAuthenticationSession().getAuthNote(NEW_USER_REGISTERED))) {
            context.getAuthenticationSession().addRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
        }

        String email = context.getUser().getEmail();
        if (Validation.isBlank(email)) {
            context.ignore();
            return;
        }

        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());
        Response challenge;
        authSession.setClientNote(AuthorizationEndpointBase.APP_INITIATED_FLOW, null);

        configureRegistrationSessionPolling(context, loginFormsProvider);

        // Do not allow resending e-mail by simple page refresh, i.e. when e-mail sent, it should be resent properly via email-verification endpoint
        if (!Objects.equals(authSession.getAuthNote(Constants.VERIFY_EMAIL_KEY), email) && !(isCurrentActionTriggeredFromAIA(context) && isChallenge)) {
            // Adding the cooldown entry first to prevent concurrent operations
            EmailCooldownManager.addCooldownEntry(context, EMAIL_RESEND_COOLDOWN_KEY_PREFIX);
            authSession.setAuthNote(Constants.VERIFY_EMAIL_KEY, email);
            EventBuilder event = context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, email);
            challenge = sendVerifyEmail(context, event);
        } else {
            challenge = loginFormsProvider.createResponse(UserModel.RequiredAction.VERIFY_EMAIL);
        }

        context.challenge(challenge);
    }

    /**
     * Points the session polling of a registration's verify-email page at the verify-email-success endpoint,
     * instead of the default ssoLoginInOtherTabsUrl. Once the email is verified in another tab there is no
     * required action left to run, so anything that re-enters the authentication flow just completes it and
     * redirects to the client. Polling only knows that some session appeared, so the endpoint re-checks the
     * verified status of the user named by the signed token. The auth-session check is suppressed for the same
     * reason: it would reload this page into that same flow once the other tab changes the auth session cookie.
     *
     * Must be applied to every rendering of the verify-email page. The resend cooldown page in
     * {@link #processAction} is a separate request, so it builds its own form and would otherwise fall back to
     * the default scripts.
     */
    private void configureRegistrationSessionPolling(RequiredActionContext context, LoginFormsProvider form) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        if (!"true".equals(authSession.getAuthNote(NEW_USER_REGISTERED))) {
            return;
        }

        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();

        String tokenString = session.tokens()
                .encode(new VerifyEmailSuccessToken(context.getUser().getId(), pollingExpiration(context)));

        String pollingRedirectUrl = Urls.loginActionsVerifyEmailSuccess(context.getUriInfo().getBaseUri(),
                realm.getName(), tokenString, authSession.getClient().getClientId(), authSession.getTabId(),
                AuthenticationProcessor.getClientData(session, authSession)).toString();

        form.setAttribute(LoginFormsProvider.SESSION_POLLING_REDIRECT_URL, pollingRedirectUrl);
        form.setAttribute(LoginFormsProvider.SKIP_CHECK_AUTH_SESSION, Boolean.TRUE);
    }

    /**
     * Absolute expiry for the polling tokens of the current verification email, given the same lifespan the
     * email's own link gets. It is created once and then reused, so that re-rendering the verify-email page
     * cannot keep pushing it out.
     *
     * Reuse is tied to the address the pending email was sent to. Whenever that no longer matches the user's
     * current email a fresh link is about to be issued - because the address changed, or because a resend
     * cleared the note - and the polling tokens have to follow the new link rather than the replaced one.
     */
    private int pollingExpiration(RequiredActionContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        String existing = authSession.getAuthNote(VERIFY_EMAIL_POLLING_EXPIRATION);
        boolean sameEmailStillPending = Objects.equals(authSession.getAuthNote(Constants.VERIFY_EMAIL_KEY),
                context.getUser().getEmail());
        if (existing != null && sameEmailStillPending) {
            return Integer.parseInt(existing);
        }

        int expiration = Time.currentTime()
                + context.getRealm().getActionTokenGeneratedByUserLifespan(VerifyEmailActionToken.TOKEN_TYPE);
        authSession.setAuthNote(VERIFY_EMAIL_POLLING_EXPIRATION, String.valueOf(expiration));

        return expiration;
    }

    private boolean isCurrentActionTriggeredFromAIA(RequiredActionContext context) {
        return Objects.equals(context.getAuthenticationSession().getClientNote(Constants.KC_ACTION), getId());
    }

    @Override
    public void processAction(RequiredActionContext context) {
        logger.debugf("Re-sending email requested for user: %s", context.getUser().getUsername());

        Long remaining = EmailCooldownManager.retrieveCooldownEntry(context, EMAIL_RESEND_COOLDOWN_KEY_PREFIX);
        if (remaining != null) {
            LoginFormsProvider retryForm = context.form()
                    .setError(Messages.COOLDOWN_VERIFICATION_EMAIL, remaining);
            configureRegistrationSessionPolling(context, retryForm);

            // re-render same verify email page
            context.challenge(retryForm.createResponse(UserModel.RequiredAction.VERIFY_EMAIL));
            return;
        }

        // This will allow user to re-send email again. Clearing the note also re-bases the polling token expiry
        // on the new email's link, see pollingExpiration.
        context.getAuthenticationSession().removeAuthNote(Constants.VERIFY_EMAIL_KEY);

        process(context, false);

    }


    @Override
    public void close() {

    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getDisplayText() {
        return "Verify Email";
    }


    @Override
    public String getId() {
        return UserModel.RequiredAction.VERIFY_EMAIL.name();
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {

        ProviderConfigProperty maxAge = new ProviderConfigProperty();
        maxAge.setName(Constants.MAX_AUTH_AGE_KEY);
        maxAge.setLabel("Maximum Age of Authentication");
        maxAge.setHelpText("Configures the duration in seconds this action can be used after the last authentication before the user is required to re-authenticate. " +
                "This parameter is used just in the context of AIA when the kc_action parameter is available in the request, which is for instance when user " +
                "himself updates his password in the account console.");
        maxAge.setType(ProviderConfigProperty.STRING_TYPE);
        maxAge.setDefaultValue(MaxAuthAgePasswordPolicyProviderFactory.DEFAULT_MAX_AUTH_AGE);

        return List.of(maxAge, EmailCooldownManager.createCooldownConfigProperty());
    }


    private Response sendVerifyEmail(RequiredActionContext context, EventBuilder event) throws UriBuilderException, IllegalArgumentException {
        RealmModel realm = context.getRealm();
        UriInfo uriInfo = context.getUriInfo();
        UserModel user = context.getUser();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        KeycloakSession session = context.getSession();

        int validityInSecs = realm.getActionTokenGeneratedByUserLifespan(VerifyEmailActionToken.TOKEN_TYPE);
        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;

        String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authSession).getEncodedId();
        VerifyEmailActionToken token = new VerifyEmailActionToken(user.getId(), absoluteExpirationInSecs, authSessionEncodedId, user.getEmail(), authSession.getClient().getClientId());
        UriBuilder builder = Urls.actionTokenBuilder(uriInfo.getBaseUri(), token.serialize(session, realm, uriInfo),
                authSession.getClient().getClientId(), authSession.getTabId(), AuthenticationProcessor.getClientData(session, authSession));
        String link = builder.build(realm.getName()).toString();
        long expirationInMinutes = TimeUnit.SECONDS.toMinutes(validityInSecs);

        try {
            session
              .getProvider(EmailTemplateProvider.class)
              .setAuthenticationSession(authSession)
              .setRealm(realm)
              .setUser(user)
              .sendVerifyEmail(link, expirationInMinutes);
            event.success();

            return context.form().createResponse(UserModel.RequiredAction.VERIFY_EMAIL);
        } catch (EmailException e) {
            event.clone().event(EventType.SEND_VERIFY_EMAIL)
                    .detail(Details.REASON, e.getMessage())
                    .user(user)
                    .error(Errors.EMAIL_SEND_FAILED);
            logger.error("Failed to send verification email", e);
            context.failure(Messages.EMAIL_SENT_ERROR);
            return context.form()
                    .setError(Messages.EMAIL_SENT_ERROR)
                    .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}
