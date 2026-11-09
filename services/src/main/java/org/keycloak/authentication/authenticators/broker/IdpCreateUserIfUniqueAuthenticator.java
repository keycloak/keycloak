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

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.util.ExistingUserInfo;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.UserProfileUtil;

import org.jboss.logging.Logger;

import static org.keycloak.authentication.actiontoken.idpverifyemail.IdpVerifyAccountLinkActionTokenHandler.runIfUserVerified;
import static org.keycloak.broker.provider.AbstractIdentityProvider.BROKER_REGISTERED_NEW_USER;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdpCreateUserIfUniqueAuthenticator extends AbstractIdpAuthenticator {

    private static Logger logger = Logger.getLogger(IdpCreateUserIfUniqueAuthenticator.class);


    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    }

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {

        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();

        if (context.getAuthenticationSession().getAuthNote(EXISTING_USER_INFO) != null) {
            context.attempted();
            return;
        }

        String username = getUsername(context, serializedCtx, brokerContext);
        if (username == null || username.trim().isEmpty()) {
            ServicesLogger.LOGGER.resetFlow(realm.isRegistrationEmailAsUsername() ? "Email" : "Username");
            context.getAuthenticationSession().setAuthNote(ENFORCE_UPDATE_PROFILE, "true");
            context.resetFlow();
            return;
        }

        if (AuthenticatorUtils.isUsernameTooLong(username)) {
            ServicesLogger.LOGGER.resetFlow("Username exceeds maximum length");
            context.getAuthenticationSession().setAuthNote(ENFORCE_UPDATE_PROFILE, "true");
            context.resetFlow();
            return;
        }

        IdentityProviderModel broker = brokerContext.getIdpConfig();
        ExistingUserInfo duplication = broker.isTransientUsers() ? null : checkExistingUser(context, username, serializedCtx, brokerContext);

        UserModel federatedUser = null;
        if (broker.isTransientUsers()) {
            logger.debugf("Transient brokering requested. Recording user details for account '%s' and from identity provider '%s' .",
                    username, broker.getAlias());

            federatedUser = new LightweightUserAdapter(session, context.getAuthenticationSession().getParentSession().getId());
            federatedUser.setUsername(username);
        } else if (duplication == null) {
            // A user that does not exist yet can never be the user of a session that is still valid (eg. on prompt=login).
            // AuthenticationProcessor.attachSession() rejects that mismatch too, but only after the new user was committed
            // without its identity link, which locks them out. Reject before creating it instead.
            rejectIfAnotherUserHoldsTheSession(context);

            logger.debugf("No duplication detected. Creating account for user '%s' and linking with identity provider '%s' .",
                    username, broker.getAlias());

            federatedUser = session.users().addUser(realm, username);
        }

        if (federatedUser != null) {
            federatedUser.setEnabled(true);

            for (Map.Entry<String, List<String>> attr : serializedCtx.getAttributes().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                if (!UserModel.USERNAME.equalsIgnoreCase(attr.getKey())) {
                    federatedUser.setAttribute(attr.getKey(), attr.getValue());
                }
            }

            UserProfile profile = session.getProvider(UserProfileProvider.class).create(UserProfileContext.IDP_REVIEW, federatedUser);
            UserProfileUtil.setDefaultValuesForReadOnlyAttributes(federatedUser, profile.getAttributes());

            AuthenticatorConfigModel config = context.getAuthenticatorConfig();
            if (config != null && Boolean.parseBoolean(config.getConfig().get(IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION))) {
                logger.debugf("User '%s' required to update password", federatedUser.getUsername());
                federatedUser.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
            }

            userRegisteredSuccess(context, federatedUser, serializedCtx, brokerContext);

            context.setUser(federatedUser);
            context.getAuthenticationSession().setAuthNote(BROKER_REGISTERED_NEW_USER, "true");
            context.success();
        } else if (duplication != null) {
            UserModel user = session.users().getUserById(realm, duplication.getExistingUserId());

            if (runIfUserVerified(session, user, broker, brokerContext.getId(),
                    () -> {
                        context.setUser(user);
                        context.success();
                    })) {
                return;
            }

            logger.debugf("Duplication detected. There is already existing user with %s '%s' .",
                    duplication.getDuplicateAttributeName(), duplication.getDuplicateAttributeValue());

            // Set duplicated user, so next authenticators can deal with it
            context.getAuthenticationSession().setAuthNote(EXISTING_USER_INFO, duplication.serialize());
            //Only show error message if the authenticator was required
            if (context.getExecution().isRequired()) {
                Response challengeResponse = context.form()
                        .setError(Messages.FEDERATED_IDENTITY_EXISTS, duplication.getDuplicateAttributeName(), duplication.getDuplicateAttributeValue())
                        .createErrorPage(Response.Status.CONFLICT);
                context.challenge(challengeResponse);
                context.getEvent()
                        .user(duplication.getExistingUserId())
                        .detail("existing_" + duplication.getDuplicateAttributeName(), duplication.getDuplicateAttributeValue())
                        .removeDetail(Details.AUTH_METHOD)
                        .removeDetail(Details.AUTH_TYPE)
                        .error(Errors.FEDERATED_IDENTITY_EXISTS);
                return;
            }

            context.attempted();
        }
    }

    private void rejectIfAnotherUserHoldsTheSession(AuthenticationFlowContext context) {
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        // The authentication session shares its ID with the user session it was started on top of when the browser holds an SSO session.
        UserSessionModel userSession = session.sessions().getUserSession(realm, context.getAuthenticationSession().getParentSession().getId());
        if (userSession == null || userSession.getUser() == null || !AuthenticationManager.isSessionValid(realm, userSession)) {
            return;
        }

        context.getEvent().detail(Details.EXISTING_USER, userSession.getUser().getId()).error(Errors.DIFFERENT_USER_AUTHENTICATED);
        throw new ErrorPageException(session, context.getAuthenticationSession(), Response.Status.BAD_REQUEST,
                Messages.DIFFERENT_USER_AUTHENTICATED, userSession.getUser().getUsername());
    }

    // Could be overriden to detect duplication based on other criterias (firstName, lastName, ...)
    protected ExistingUserInfo checkExistingUser(AuthenticationFlowContext context, String username, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {

        RealmModel realm = context.getRealm();
        UserProvider users = context.getSession().users();
        String email = brokerContext.getEmail();

        if (email != null && !realm.isDuplicateEmailsAllowed()) {
            UserModel existingUser = users.getUserByEmail(realm, email);
            if (existingUser != null) {
                return new ExistingUserInfo(existingUser.getId(), UserModel.EMAIL, existingUser.getEmail());
            }

            // When login with email is allowed an e-mail address also identifies a user at login time, so a brokered
            // e-mail that matches the username of an existing account has to be treated as a duplication as well.
            // Otherwise the new account would shadow the existing one and lock it out of every username based login.
            if (realm.isLoginWithEmailAllowed()) {
                existingUser = users.getUserByUsername(realm, email);
                if (existingUser != null) {
                    return new ExistingUserInfo(existingUser.getId(), UserModel.USERNAME, existingUser.getUsername());
                }
            }
        }

        UserModel existingUser = users.getUserByUsername(realm, username);
        if (existingUser != null) {
            return new ExistingUserInfo(existingUser.getId(), UserModel.USERNAME, existingUser.getUsername());
        }

        // Same reasoning as above, for a brokered username that matches the e-mail of an existing account
        if (realm.isLoginWithEmailAllowed() && username.indexOf('@') > 0) {
            existingUser = users.getUserByEmail(realm, username);
            if (existingUser != null) {
                return new ExistingUserInfo(existingUser.getId(), UserModel.EMAIL, existingUser.getEmail());
            }
        }

        return null;
    }

    protected String getUsername(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        RealmModel realm = context.getRealm();
        return realm.isRegistrationEmailAsUsername() ? brokerContext.getEmail() : brokerContext.getModelUsername();
    }


    // Empty method by default. This exists, so subclass can override and add callback after new user is registered through social
    protected void userRegisteredSuccess(AuthenticationFlowContext context, UserModel registeredUser, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {

    }


    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

}
