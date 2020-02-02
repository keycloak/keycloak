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
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * Same like classic username+password form, but for use in IdP linking.
 *
 * User identity is optionally established by the preceding idp-create-user-if-unique execution.
 * If no identity had been established, the user will be prompted to enter login name.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdpUsernamePasswordForm extends UsernamePasswordForm {

    @Override
    protected Response challenge(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        UserModel existingUser = null;

        try {
            existingUser = AbstractIdpAuthenticator.getExistingUser(context.getSession(), context.getRealm(), context.getAuthenticationSession());
        } catch(AuthenticationFlowException ex) {
            log.debug("No existing user in authSession", ex);
        }

        return setupForm(context, formData, existingUser)
                .setStatus(Response.Status.OK)
                .createLoginUsernamePassword();
    }

    @Override
    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        UserModel existingUser = null;
        try {
            existingUser = AbstractIdpAuthenticator.getExistingUser(context.getSession(), context.getRealm(), context.getAuthenticationSession());
        } catch (AuthenticationFlowException ex) {
            log.debug("No existing user in authSession", ex);
        }

        // Restore formData for the case of error
        setupForm(context, formData, existingUser);

        if (existingUser != null) {
            context.setUser(existingUser);
            return validatePassword(context, existingUser, formData);
        } else {
            return validateUserAndPassword(context, formData);
        }
    }

    protected LoginFormsProvider setupForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData, UserModel existingUser) {
        SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(context.getAuthenticationSession(), AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
        if (serializedCtx == null) {
            throw new AuthenticationFlowException("Not found serialized context in clientSession", AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }

        String message;
        Object[] args;
        LoginFormsProvider form = context.form();

        if (existingUser != null) {
            formData.putSingle(AuthenticationManager.FORM_USERNAME, existingUser.getUsername());
            message = Messages.FEDERATED_IDENTITY_CONFIRM_REAUTHENTICATE_MESSAGE;
            args = new Object[]{existingUser.getUsername(), serializedCtx.getIdentityProviderId()};
            form.setAttribute(LoginFormsProvider.USERNAME_EDIT_DISABLED, true)
                .setAttribute(LoginFormsProvider.IDENTITY_PROVIDERS_FILTERED, true);
        } else {
            message = Messages.FEDERATED_IDENTITY_CONFIRM_REAUTHENTICATE_NO_USER_MESSAGE;
            args = new Object[]{serializedCtx.getIdentityProviderId()};
            form.setAttribute(LoginFormsProvider.IDENTITY_PROVIDERS_DISABLED, true);
        }

        form.setFormData(formData)
            .setInfo(message, args)
            .setAttribute(LoginFormsProvider.REGISTRATION_DISABLED, true);

        return form;
    }
}
