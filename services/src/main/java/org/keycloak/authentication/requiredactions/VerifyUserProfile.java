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

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
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
public class VerifyUserProfile extends UpdateProfile {

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.NOT_SUPPORTED;
    }
    
    @Override
    protected UserModel.RequiredAction getResponseAction(){
        return UserModel.RequiredAction.VERIFY_PROFILE;
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

            context.challenge(createResponse(context, parameters, errors));
            
            EventBuilder event = context.getEvent().clone();
            event.event(EventType.VERIFY_PROFILE);
            event.detail(Details.FIELDS_TO_UPDATE, collectFields(errors));
            event.success();
        }
    }

    private String collectFields(List<FormMessage> errors) {
        return errors.stream().map(FormMessage::getField).distinct().collect(Collectors.joining(","));
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public String getDisplayText() {
        return "Verify Profile";
    }


    @Override
    public String getId() {
        return UserModel.RequiredAction.VERIFY_PROFILE.name();
    }

}
