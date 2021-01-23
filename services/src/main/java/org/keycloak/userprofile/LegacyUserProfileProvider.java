/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.userprofile;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.userprofile.validation.StaticValidators;
import org.keycloak.userprofile.validation.UserProfileValidationResult;
import org.keycloak.userprofile.validation.ValidationChainBuilder;

/**
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public class LegacyUserProfileProvider implements UserProfileProvider {

    private static final Logger logger = Logger.getLogger(LegacyUserProfileProvider.class);
    private final KeycloakSession session;
    private final Pattern readOnlyAttributes;
    private final Pattern adminReadOnlyAttributes;

    public LegacyUserProfileProvider(KeycloakSession session, Pattern readOnlyAttributes, Pattern adminReadOnlyAttributes) {
        this.session = session;
        this.readOnlyAttributes = readOnlyAttributes;
        this.adminReadOnlyAttributes = adminReadOnlyAttributes;
    }

    @Override
    public void close() {

    }

    @Override
    public UserProfileValidationResult validate(UserProfileContext updateContext, UserProfile updatedProfile) {
        RealmModel realm = this.session.getContext().getRealm();

        ValidationChainBuilder builder = ValidationChainBuilder.builder();
        switch (updateContext.getUpdateEvent()) {
            case UserResource:
                addReadOnlyAttributeValidators(builder, adminReadOnlyAttributes, updateContext, updatedProfile);
                break;
            case IdpReview:
                addBasicValidators(builder, !realm.isRegistrationEmailAsUsername());
                addReadOnlyAttributeValidators(builder, readOnlyAttributes, updateContext, updatedProfile);
                break;
            case Account:
            case RegistrationProfile:
            case UpdateProfile:
                addBasicValidators(builder, !realm.isRegistrationEmailAsUsername() && realm.isEditUsernameAllowed());
                addReadOnlyAttributeValidators(builder, readOnlyAttributes, updateContext, updatedProfile);
                addSessionValidators(builder);
                break;
            case RegistrationUserCreation:
                addUserCreationValidators(builder);
                addReadOnlyAttributeValidators(builder, readOnlyAttributes, updateContext, updatedProfile);
                break;
        }
        return new UserProfileValidationResult(builder.build().validate(updateContext,updatedProfile));
    }

    private void addUserCreationValidators(ValidationChainBuilder builder) {
        RealmModel realm = this.session.getContext().getRealm();

        if (realm.isRegistrationEmailAsUsername()) {
            builder.addAttributeValidator().forAttribute(UserModel.EMAIL)
                    .addSingleAttributeValueValidationFunction(Messages.INVALID_EMAIL, StaticValidators.isEmailValid())
                    .addSingleAttributeValueValidationFunction(Messages.MISSING_EMAIL, StaticValidators.isBlank())
                    .addSingleAttributeValueValidationFunction(Messages.EMAIL_EXISTS, StaticValidators.doesEmailExist(session)).build()
                    .build();


        } else {
            builder.addAttributeValidator().forAttribute(UserModel.USERNAME)
                    .addSingleAttributeValueValidationFunction(Messages.MISSING_USERNAME, StaticValidators.isBlank())
                    .addSingleAttributeValueValidationFunction(Messages.USERNAME_EXISTS,
                            (value, o) -> session.users().getUserByUsername(realm, value) == null)
                    .build();
        }
    }

    private void addBasicValidators(ValidationChainBuilder builder, boolean userNameExistsCondition) {

        builder.addAttributeValidator().forAttribute(UserModel.USERNAME)
                .addSingleAttributeValueValidationFunction(Messages.MISSING_USERNAME, StaticValidators.checkUsernameExists(userNameExistsCondition)).build()

                .addAttributeValidator().forAttribute(UserModel.FIRST_NAME)
                .addSingleAttributeValueValidationFunction(Messages.MISSING_FIRST_NAME, StaticValidators.isBlank()).build()

                .addAttributeValidator().forAttribute(UserModel.LAST_NAME)
                .addSingleAttributeValueValidationFunction(Messages.MISSING_LAST_NAME, StaticValidators.isBlank()).build()

                .addAttributeValidator().forAttribute(UserModel.EMAIL)
                .addSingleAttributeValueValidationFunction(Messages.MISSING_EMAIL, StaticValidators.isBlank())
                .addSingleAttributeValueValidationFunction(Messages.INVALID_EMAIL, StaticValidators.isEmailValid())
                .build();
    }

    private void addSessionValidators(ValidationChainBuilder builder) {
        RealmModel realm = this.session.getContext().getRealm();
        builder.addAttributeValidator().forAttribute(UserModel.USERNAME)
                .addSingleAttributeValueValidationFunction(Messages.USERNAME_EXISTS, StaticValidators.userNameExists(session))
                .addSingleAttributeValueValidationFunction(Messages.READ_ONLY_USERNAME, StaticValidators.isUserMutable(realm)).build()

                .addAttributeValidator().forAttribute(UserModel.EMAIL)
                .addSingleAttributeValueValidationFunction(Messages.EMAIL_EXISTS, StaticValidators.isEmailDuplicated(session))
                .addSingleAttributeValueValidationFunction(Messages.USERNAME_EXISTS, StaticValidators.doesEmailExistAsUsername(session)).build()
                .build();
    }

    private void addReadOnlyAttributeValidators(ValidationChainBuilder builder, Pattern configuredReadOnlyAttrs, UserProfileContext updateContext, UserProfile updatedProfile) {
        addValidatorsForAllAttributeOfUser(builder, configuredReadOnlyAttrs, updatedProfile);
        addValidatorsForAllAttributeOfUser(builder, configuredReadOnlyAttrs, updateContext.getCurrentProfile());
    }


    private void addValidatorsForAllAttributeOfUser(ValidationChainBuilder builder, Pattern configuredReadOnlyAttrsPattern, UserProfile profile) {
        if (profile == null) {
            return;
        }

        profile.getAttributes().keySet().stream()
                .filter(currentAttrName -> configuredReadOnlyAttrsPattern.matcher(currentAttrName).find())
                .forEach((currentAttrName) ->
                        builder.addAttributeValidator().forAttribute(currentAttrName)
                                .addValidationFunction(Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED, StaticValidators.isAttributeUnchanged(currentAttrName)).build()
                );
    }
}