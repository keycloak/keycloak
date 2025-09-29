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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.verifyemail.VerifyEmailActionToken;
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
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserModel;
import org.keycloak.policy.MaxAuthAgePasswordPolicyProviderFactory;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class VerifyEmail implements RequiredActionProvider, RequiredActionFactory {
    private static final String EMAIL_RESEND_COOLDOWN_SECONDS = "emailResendCooldownSeconds";
    private static final int EMAIL_RESEND_COOLDOWN_DEFAULT_SECONDS = 30;
    public static final String EMAIL_RESEND_COOLDOWN_KEY_PREFIX = "verify-email-cooldown-";
    private static final Logger logger = Logger.getLogger(VerifyEmail.class);
    private static final String KEY_EXPIRE = "expire";

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getRealm().isVerifyEmail() && !context.getUser().isEmailVerified()) {
            context.getUser().addRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
            logger.debug("User is required to verify email");
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
            context.success();
            authSession.removeAuthNote(Constants.VERIFY_EMAIL_KEY);
            return;
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

        // Do not allow resending e-mail by simple page refresh, i.e. when e-mail sent, it should be resent properly via email-verification endpoint
        if (!Objects.equals(authSession.getAuthNote(Constants.VERIFY_EMAIL_KEY), email) && !(isCurrentActionTriggeredFromAIA(context) && isChallenge)) {
            // Adding the cooldown entry first to prevent concurrent operations
            addCooldownEntry(context);
            authSession.setAuthNote(Constants.VERIFY_EMAIL_KEY, email);
            EventBuilder event = context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, email);
            challenge = sendVerifyEmail(context, event);
        } else {
            challenge = loginFormsProvider.createResponse(UserModel.RequiredAction.VERIFY_EMAIL);
        }

        context.challenge(challenge);
    }

    private boolean isCurrentActionTriggeredFromAIA(RequiredActionContext context) {
        return Objects.equals(context.getAuthenticationSession().getClientNote(Constants.KC_ACTION), getId());
    }

    @Override
    public void processAction(RequiredActionContext context) {
        logger.debugf("Re-sending email requested for user: %s", context.getUser().getUsername());

        Long remaining = retrieveCooldownEntry(context);
        if (remaining != null) {
            Response retryPage = context.form()
                    .setError(Messages.COOLDOWN_VERIFICATION_EMAIL, remaining)
                    .createResponse(UserModel.RequiredAction.VERIFY_EMAIL); // re-render same verify email page

            context.challenge(retryPage);
            return;
        }

        // This will allow user to re-send email again
        context.getAuthenticationSession().removeAuthNote(Constants.VERIFY_EMAIL_KEY);

        process(context, false);

    }

    private Long retrieveCooldownEntry(RequiredActionContext context) {
        SingleUseObjectProvider singleUseCache = context.getSession().singleUseObjects();
        Map<String, String> cooldownDetails = singleUseCache.get(getCacheKey(context));
        if (cooldownDetails == null) {
            return null;
        }
        long remaining = (Long.parseLong(cooldownDetails.get(KEY_EXPIRE)) - Time.currentTime());
        // Avoid the awkward situation where due to rounding the value is zero
        return remaining > 0 ? remaining : null;
    }

    private void addCooldownEntry(RequiredActionContext context) {
        SingleUseObjectProvider cache = context.getSession().singleUseObjects();
        long cooldownSeconds = getCooldownInSeconds(context);
        cache.put(getCacheKey(context), cooldownSeconds, Map.of("expire", Long.toString(Time.currentTime() + cooldownSeconds)));
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

        ProviderConfigProperty cooldown = new ProviderConfigProperty();
        cooldown.setName(EMAIL_RESEND_COOLDOWN_SECONDS);
        cooldown.setLabel("Cooldown Between Email Resend (seconds)");
        cooldown.setHelpText("Minimum delay in seconds before another email verification email can be sent.");
        cooldown.setType(ProviderConfigProperty.STRING_TYPE);
        cooldown.setDefaultValue(String.valueOf(EMAIL_RESEND_COOLDOWN_DEFAULT_SECONDS));
        return List.of(maxAge,cooldown);
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

    private static String getCacheKey(RequiredActionContext context) {
        return EMAIL_RESEND_COOLDOWN_KEY_PREFIX + context.getUser().getId();
    }

    private long getCooldownInSeconds(RequiredActionContext context) {
        try {
            RequiredActionProviderModel model = context.getRealm().getRequiredActionProviderByAlias(getId());
            if (model == null || model.getConfig() == null) {
                logger.warn("No RequiredActionProviderModel found for alias: " + getId());
                return EMAIL_RESEND_COOLDOWN_DEFAULT_SECONDS;
            }

            String value = model.getConfig().getOrDefault(EMAIL_RESEND_COOLDOWN_SECONDS, String.valueOf(EMAIL_RESEND_COOLDOWN_DEFAULT_SECONDS));
            return Long.parseLong(value);
        } catch (RuntimeException e) {
            logger.error("Failed to fetch cooldown from config: ", e);
            return EMAIL_RESEND_COOLDOWN_DEFAULT_SECONDS;
        }
    }
}
