/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.actiontoken.updateemail;

import java.util.List;
import java.util.Objects;
import javax.ws.rs.core.Response;
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.TokenUtils;
import org.keycloak.authentication.requiredactions.UpdateEmail;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.ValidationException;

public class UpdateEmailActionTokenHandler extends AbstractActionTokenHandler<UpdateEmailActionToken> {

    public UpdateEmailActionTokenHandler() {
        super(UpdateEmailActionToken.TOKEN_TYPE, UpdateEmailActionToken.class, Messages.STALE_VERIFY_EMAIL_LINK,
                EventType.EXECUTE_ACTIONS, Errors.INVALID_TOKEN);
    }

    @Override
    public TokenVerifier.Predicate<? super UpdateEmailActionToken>[] getVerifiers(
            ActionTokenContext<UpdateEmailActionToken> tokenContext) {
        return TokenUtils.predicates(TokenUtils.checkThat(
                t -> Objects.equals(t.getOldEmail(), tokenContext.getAuthenticationSession().getAuthenticatedUser().getEmail()),
                Errors.INVALID_EMAIL, getDefaultErrorMessage()));
    }

    @Override
    public Response handleToken(UpdateEmailActionToken token, ActionTokenContext<UpdateEmailActionToken> tokenContext) {
        AuthenticationSessionModel authenticationSession = tokenContext.getAuthenticationSession();
        UserModel user = authenticationSession.getAuthenticatedUser();

        KeycloakSession session = tokenContext.getSession();

        LoginFormsProvider forms = session.getProvider(LoginFormsProvider.class).setAuthenticationSession(authenticationSession)
                .setUser(user);

        String newEmail = token.getNewEmail();

        UserProfile emailUpdateValidationResult;
        try {
            emailUpdateValidationResult = UpdateEmail.validateEmailUpdate(session, user, newEmail);
        } catch (ValidationException pve) {
            List<FormMessage> errors = Validation.getFormErrorsFromValidation(pve.getErrors());
            return forms.setErrors(errors).createErrorPage(Response.Status.BAD_REQUEST);
        }

        UpdateEmail.updateEmailNow(tokenContext.getEvent(), user, emailUpdateValidationResult);

        tokenContext.getEvent().success();

        // verify user email as we know it is valid as this entry point would never have gotten here.
        user.setEmailVerified(true);
        user.removeRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL);
        tokenContext.getAuthenticationSession().removeRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL);
        user.removeRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
        tokenContext.getAuthenticationSession().removeRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);

        return forms.setAttribute("messageHeader", forms.getMessage("emailUpdatedTitle")).setSuccess("emailUpdated", newEmail)
                .createInfoPage();
    }

    @Override
    public boolean canUseTokenRepeatedly(UpdateEmailActionToken token,
            ActionTokenContext<UpdateEmailActionToken> tokenContext) {
        return false;
    }
}
