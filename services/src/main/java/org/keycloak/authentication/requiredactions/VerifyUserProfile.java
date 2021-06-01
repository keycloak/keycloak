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

package org.keycloak.authentication.requiredactions;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.DisplayTypeRequiredActionFactory;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.validation.Validation;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class VerifyUserProfile implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        UserModel user = context.getUser();
        UserProfileProvider provider = context.getSession().getProvider(UserProfileProvider.class);
        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, user);

        try {
            profile.validate();
            context.getAuthenticationSession().removeRequiredAction(getId());
            user.removeRequiredAction(getId());
        } catch (ValidationException e) {
            context.getAuthenticationSession().addRequiredAction(getId());
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        UserProfileProvider provider = context.getSession().getProvider(UserProfileProvider.class);
        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, context.getUser());

        try {
            profile.validate();
            context.success();
        } catch (ValidationException ve) {
            List<FormMessage> errors = Validation.getFormErrorsFromValidation(ve.getErrors());
            MultivaluedMap<String, String> parameters;

            if (context.getHttpRequest().getHttpMethod().equalsIgnoreCase(HttpMethod.GET)) {
                parameters = new MultivaluedHashMap<>();
            } else {
                parameters = context.getHttpRequest().getDecodedFormParameters();
            }

            context.challenge(createResponse(context, profile, parameters, errors));
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        EventBuilder event = context.getEvent();
        event.event(EventType.VERIFY_PROFILE);
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (!context.getRealm().isEditUsernameAllowed()) {
            formData.putSingle(UserModel.USERNAME, context.getUser().getUsername());
        }

        UserProfileProvider provider = context.getSession().getProvider(UserProfileProvider.class);
        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, formData, context.getUser());

        try {
            profile.update();
            context.success();
        } catch (ValidationException ve) {
            List<FormMessage> errors = Validation.getFormErrorsFromValidation(ve.getErrors());
            context.challenge(createResponse(context, profile, formData, errors));
        }
    }


    @Override
    public void close() {

    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleUpdateProfile.SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getDisplayText() {
        return "Verify Profile";
    }


    @Override
    public String getId() {
        return UserModel.RequiredAction.VERIFY_PROFILE.name();
    }

    private Response createResponse(RequiredActionContext context, UserProfile profile,
            MultivaluedMap<String, String> formData, List<FormMessage> errors) {
        LoginFormsProvider form = context.form();

        if (!errors.isEmpty()) {
            form.setErrors(errors);
        }

        return form.setFormData(formData)
                .createResponse(UserModel.RequiredAction.VERIFY_PROFILE);
    }
}
