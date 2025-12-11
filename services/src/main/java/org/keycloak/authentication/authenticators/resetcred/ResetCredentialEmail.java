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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.credential.PasswordCredentialProviderFactory;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.DefaultActionTokenKey;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.storage.StorageId;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResetCredentialEmail implements Authenticator, AuthenticatorFactory {

    private static final Logger logger = Logger.getLogger(ResetCredentialEmail.class);

    public static final String PROVIDER_ID = "reset-credential-email";
    public static final String FORCE_LOGIN = "force-login";
    public static final String FEDERATED_OPTION = "only-federated";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
        String username = authenticationSession.getAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);

        // we don't want people guessing usernames, so if there was a problem obtaining the user, the user will be null.
        // just reset login for with a success message
        if (user == null) {
            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
            return;
        }

        String actionTokenUserId = authenticationSession.getAuthNote(DefaultActionTokenKey.ACTION_TOKEN_USER_ID);
        if (actionTokenUserId != null && Objects.equals(user.getId(), actionTokenUserId)) {
            logger.debugf("Forget-password triggered when reauthenticating user after authentication via action token. Skipping " + PROVIDER_ID + " screen and using user '%s' ", user.getUsername());
            if (forceLogin(context.getAuthenticatorConfig(), user)) {
                // force end of auth session after the required actions
                context.getAuthenticationSession().setAuthNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS, "true");
            }
            context.success();
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

        int validityInSecs = context.getRealm().getActionTokenGeneratedByUserLifespan(ResetCredentialsActionToken.TOKEN_TYPE);
        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;

        // We send the secret in the email in a link as a query param.
        String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authenticationSession).getEncodedId();
        ResetCredentialsActionToken token = new ResetCredentialsActionToken(user.getId(), user.getEmail(), absoluteExpirationInSecs, authSessionEncodedId, authenticationSession.getClient().getClientId());
        String link = UriBuilder
          .fromUri(context.getActionTokenUrl(token.serialize(context.getSession(), context.getRealm(), context.getUriInfo())))
          .build()
          .toString();
        long expirationInMinutes = TimeUnit.SECONDS.toMinutes(validityInSecs);
        try {
            context.getSession().getProvider(EmailTemplateProvider.class).setRealm(context.getRealm()).setUser(user).setAuthenticationSession(authenticationSession).sendPasswordReset(link, expirationInMinutes);

            event.clone().event(EventType.SEND_RESET_PASSWORD)
                         .user(user)
                         .detail(Details.USERNAME, username)
                         .detail(Details.EMAIL, user.getEmail()).detail(Details.CODE_ID, authenticationSession.getParentSession().getId()).success();
            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
        } catch (EmailException e) {
            event.clone().event(EventType.SEND_RESET_PASSWORD)
                    .detail(Details.REASON, e.getMessage())
                    .detail(Details.USERNAME, username)
                    .user(user)
                    .error(Errors.EMAIL_SEND_FAILED);
            ServicesLogger.LOGGER.failedToSendPwdResetEmail(e);
            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
        }
    }

    public static Long getLastChangedTimestamp(KeycloakSession session, RealmModel realm, UserModel user) {
        // TODO(hmlnarik): Make this more generic to support non-password credential types
        PasswordCredentialProvider passwordProvider = (PasswordCredentialProvider) session.getProvider(CredentialProvider.class, PasswordCredentialProviderFactory.PROVIDER_ID);
        CredentialModel password = passwordProvider.getPassword(realm, user);

        return password == null ? null : password.getCreatedDate();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
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
        return true;
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
        return ProviderConfigurationBuilder.create()
                .property()
                .name(FORCE_LOGIN)
                .label("Force login after reset")
                .helpText(
                        """
                        If this property is true, the user needs to login again after the reset credentials.
                        If this property is false, the user will be automatically logged in after the succesful
                        reset credentials when the same authentication session is used.
                        If this property is only-federated (default), only federated users will be forced to login again,
                        users stored in the internal database will be logged in if using the same authentication session.
                        """
                )
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString(), FEDERATED_OPTION))
                .defaultValue(FEDERATED_OPTION)
                .add()
                .build();
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

    private boolean forceLogin(AuthenticatorConfigModel config, UserModel user) {
        final String forceLogin = config != null? config.getConfig().get(FORCE_LOGIN) : null;
        if (forceLogin == null || FEDERATED_OPTION.equalsIgnoreCase(forceLogin)) {
            // default is only-federated, return true only for federated users
            return !StorageId.isLocalStorage(user.getId()) || user.isFederated();
        } else if (Boolean.TRUE.toString().equalsIgnoreCase(forceLogin)) {
            return Boolean.TRUE;
        } else if (Boolean.FALSE.toString().equalsIgnoreCase(forceLogin)) {
            return Boolean.FALSE;
        } else {
            logger.warnf("Invalid value for force-login option: %s", forceLogin);
            throw new AuthenticationFlowException("Invalid value for force-login option: " + forceLogin, AuthenticationFlowError.INTERNAL_ERROR);
        }
    }
}
