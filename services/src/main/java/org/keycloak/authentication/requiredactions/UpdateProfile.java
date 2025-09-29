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

import static java.util.Optional.ofNullable;

import jakarta.ws.rs.core.MultivaluedHashMap;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.validation.Validation;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.EventAuditingAttributeChangeListener;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UpdateProfile implements RequiredActionProvider, RequiredActionFactory {
    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        context.challenge(createResponse(context, null, null));
    }

    @Override
    public void processAction(RequiredActionContext context) {
        EventBuilder event = context.getEvent();
        event.event(EventType.UPDATE_PROFILE).detail(Details.CONTEXT, UserProfileContext.UPDATE_PROFILE.name());
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>(context.getHttpRequest().getDecodedFormParameters());
        UserModel user = context.getUser();
        String newEmail = formData.getFirst(UserModel.EMAIL);
        boolean isEmailUpdated = newEmail != null && !ofNullable(user.getEmail()).orElse("").equals(newEmail);
        RealmModel realm = context.getRealm();
        boolean isForceEmailVerification = isEmailUpdated && UpdateEmail.isVerifyEmailEnabled(realm);

        try {
            UserProfileProvider provider = context.getSession().getProvider(UserProfileProvider.class);
            UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, formData, user);

            profile.update(false, new EventAuditingAttributeChangeListener(profile, event));

            context.success();

            if (isForceEmailVerification && !realm.isVerifyEmail()) {
                user.addRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL);
                UpdateEmail.forceEmailVerification(context.getSession());
            }
        } catch (ValidationException pve) {
            List<FormMessage> errors = Validation.getFormErrorsFromValidation(pve.getErrors());

            context.challenge(createResponse(context, formData, errors));
        }
    }

    protected UserModel.RequiredAction getResponseAction(){
        return UserModel.RequiredAction.UPDATE_PROFILE;
    }

    protected Response createResponse(RequiredActionContext context, MultivaluedMap<String, String> formData, List<FormMessage> errors) {
        LoginFormsProvider form = context.form();

        if (errors != null && !errors.isEmpty()) {
            form.setErrors(errors);
        }

        if(formData != null) {
            form = form.setFormData(formData);
        }

        form.setUser(context.getUser());

        return form.createResponse(getResponseAction());
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
        return "Update Profile";
    }


    @Override
    public String getId() {
        return UserModel.RequiredAction.UPDATE_PROFILE.name();
    }
}
