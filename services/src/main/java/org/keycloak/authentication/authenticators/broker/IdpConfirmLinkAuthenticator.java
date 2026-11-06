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

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.authenticators.broker.util.ExistingUserInfo;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdpConfirmLinkAuthenticator extends AbstractIdpAuthenticator {

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        String existingUserInfo = authSession.getAuthNote(EXISTING_USER_INFO);
        if (existingUserInfo == null) {
            ServicesLogger.LOGGER.noDuplicationDetected();
            context.attempted();
            return;
        }

        ExistingUserInfo duplicationInfo = ExistingUserInfo.deserialize(existingUserInfo);

        if (sendErrorIfExistingUserDisabled(context, duplicationInfo)) {
            return;
        }

        // hide the review button if the idp review execution was not successfully executed before
        boolean hideReviewButton = authSession.getExecutionStatus().entrySet().stream()
                .filter(entry -> CommonClientSessionModel.ExecutionStatus.SUCCESS.equals(entry.getValue()))
                .map(entry -> context.getRealm().getAuthenticationExecutionById(entry.getKey()))
                .filter(exec -> IdpReviewProfileAuthenticatorFactory.PROVIDER_ID.equals(exec.getAuthenticator()))
                .findAny()
                .isEmpty();

        Response challenge = context.form()
                .setStatus(Response.Status.OK)
                .setAttribute(LoginFormsProvider.IDENTITY_PROVIDER_BROKER_CONTEXT, brokerContext)
                .setAttribute("hideReviewButton", hideReviewButton ? Boolean.TRUE : null)
                .setError(Messages.FEDERATED_IDENTITY_CONFIRM_LINK_MESSAGE, duplicationInfo.getDuplicateAttributeName(), duplicationInfo.getDuplicateAttributeValue())
                .createIdpLinkConfirmLinkPage();
        context.challenge(challenge);
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String action = formData.getFirst("submitAction");
        if (action != null && action.equals("updateProfile")) {
            context.resetFlow(() -> {
                AuthenticationSessionModel authSession = context.getAuthenticationSession();

                serializedCtx.saveToAuthenticationSession(authSession, BROKERED_CONTEXT_NOTE);
                authSession.setAuthNote(ENFORCE_UPDATE_PROFILE, "true");
            });
        } else if (action != null && action.equals("linkAccount")) {
            // the existing account might have been disabled after the confirm link page was shown
            String existingUserInfo = context.getAuthenticationSession().getAuthNote(EXISTING_USER_INFO);
            if (existingUserInfo != null && sendErrorIfExistingUserDisabled(context, ExistingUserInfo.deserialize(existingUserInfo))) {
                return;
            }
            context.success();
        } else {
            throw new AuthenticationFlowException("Unknown action: " + action,
                    AuthenticationFlowError.INTERNAL_ERROR);
        }
    }

    /**
     * Linking to a disabled account can never complete, so report it with an error page instead of continuing with the link.
     *
     * @return {@code true} if the existing account is disabled and the error page was sent
     */
    private boolean sendErrorIfExistingUserDisabled(AuthenticationFlowContext context, ExistingUserInfo duplicationInfo) {
        UserModel existingUser = context.getSession().users().getUserById(context.getRealm(), duplicationInfo.getExistingUserId());
        if (existingUser == null || existingUser.isEnabled()) {
            return false;
        }

        context.getEvent().clone().user(existingUser).error(Errors.USER_DISABLED);
        Response challenge = context.form()
                .setError(Messages.ACCOUNT_DISABLED)
                .createErrorPage(Response.Status.BAD_REQUEST);
        context.failureChallenge(AuthenticationFlowError.USER_DISABLED, challenge);
        return true;
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
