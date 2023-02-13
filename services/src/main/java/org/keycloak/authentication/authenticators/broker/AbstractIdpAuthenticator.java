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

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.broker.util.ExistingUserInfo;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractIdpAuthenticator implements Authenticator {

    // The clientSession note encapsulating all the BrokeredIdentityContext info. When this note is in clientSession, we know that firstBrokerLogin flow is in progress
    public static final String BROKERED_CONTEXT_NOTE = "BROKERED_CONTEXT";

    // The clientSession note with all the info about existing user
    public static final String EXISTING_USER_INFO = "EXISTING_USER_INFO";

    // The clientSession note flag to indicate that email provided by identityProvider was changed on updateProfile page
    public static final String UPDATE_PROFILE_EMAIL_CHANGED = "UPDATE_PROFILE_EMAIL_CHANGED";

    // The clientSession note flag to indicate that updateProfile page will be always displayed even if "updateProfileOnFirstLogin" is off
    public static final String ENFORCE_UPDATE_PROFILE = "ENFORCE_UPDATE_PROFILE";

    // clientSession.note flag specifies if we imported new user to keycloak (true) or we just linked to an existing keycloak user (false)
    public static final String BROKER_REGISTERED_NEW_USER = "BROKER_REGISTERED_NEW_USER";

    // Set after firstBrokerLogin is successfully finished and contains the providerId of the provider, whose 'first-broker-login' flow was just finished
    public static final String FIRST_BROKER_LOGIN_SUCCESS = "FIRST_BROKER_LOGIN_SUCCESS";

    // Set if nested firstBrokerLogin is detected, allowing to report a detailed error
    public static final String NESTED_FIRST_BROKER_CONTEXT = "NESTED_FIRST_BROKER_CONTEXT";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(authSession, BROKERED_CONTEXT_NOTE);
        if (serializedCtx == null) {
            throw new AuthenticationFlowException("Not found serialized context in clientSession", AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }
        BrokeredIdentityContext brokerContext = serializedCtx.deserialize(context.getSession(), authSession);

        if (!brokerContext.getIdpConfig().isEnabled()) {
            sendFailureChallenge(context, Response.Status.BAD_REQUEST, Errors.IDENTITY_PROVIDER_ERROR, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR, AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }

        authenticateImpl(context, serializedCtx, brokerContext);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        AuthenticationSessionModel clientSession = context.getAuthenticationSession();

        SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(clientSession, BROKERED_CONTEXT_NOTE);
        if (serializedCtx == null) {
            throw new AuthenticationFlowException("Not found serialized context in clientSession", AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }
        BrokeredIdentityContext brokerContext = serializedCtx.deserialize(context.getSession(), clientSession);

        if (!brokerContext.getIdpConfig().isEnabled()) {
            sendFailureChallenge(context, Response.Status.BAD_REQUEST, Errors.IDENTITY_PROVIDER_ERROR, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR, AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }

        actionImpl(context, serializedCtx, brokerContext);
    }

    protected abstract void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext);
    protected abstract void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext);

    protected void sendFailureChallenge(AuthenticationFlowContext context, Response.Status status, String eventError, String errorMessage, AuthenticationFlowError flowError) {
        context.getEvent().user(context.getUser())
                .error(eventError);
        Response challengeResponse = context.form()
                .setError(errorMessage)
                .createErrorPage(status);
        context.failureChallenge(flowError, challengeResponse);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {

    }

    public static UserModel getExistingUser(KeycloakSession session, RealmModel realm, AuthenticationSessionModel authSession) {
        String existingUserId = authSession.getAuthNote(EXISTING_USER_INFO);
        if (existingUserId == null) {
            throw new AuthenticationFlowException("Unexpected state. There is no existing duplicated user identified in ClientSession",
                    AuthenticationFlowError.INTERNAL_ERROR);
        }

        ExistingUserInfo duplication = ExistingUserInfo.deserialize(existingUserId);

        UserModel existingUser = session.users().getUserById(realm, duplication.getExistingUserId());
        if (existingUser == null) {
            throw new AuthenticationFlowException("User with ID '" + existingUserId + "' not found.", AuthenticationFlowError.INVALID_USER);
        }

        if (!existingUser.isEnabled()) {
            throw new AuthenticationFlowException("User with ID '" + existingUserId + "', username '" + existingUser.getUsername() + "' disabled.", AuthenticationFlowError.USER_DISABLED);
        }

        return existingUser;
    }
}
