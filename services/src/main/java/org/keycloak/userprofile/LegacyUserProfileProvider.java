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
    private KeycloakSession session;

    public LegacyUserProfileProvider(KeycloakSession session) {
        this.session = session;
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
                break;
            case IdpReview:
                addBasicValidators(builder, !realm.isRegistrationEmailAsUsername());
                break;
            case Account:
            case RegistrationProfile:
            case UpdateProfile:
                addBasicValidators(builder, !realm.isRegistrationEmailAsUsername() && realm.isEditUsernameAllowed());
                addSessionValidators(builder);
                break;
            case RegistrationUserCreation:
                addUserCreationValidators(builder);
                break;
        }
        return new UserProfileValidationResult(builder.build().validate(updateContext,updatedProfile));
    }

    private void addUserCreationValidators(ValidationChainBuilder builder) {
        RealmModel realm = this.session.getContext().getRealm();

        if (realm.isRegistrationEmailAsUsername()) {
            builder.addAttributeValidator().forAttribute(UserModel.EMAIL)
                    .addValidationFunction(Messages.INVALID_EMAIL, StaticValidators.isEmailValid())
                    .addValidationFunction(Messages.MISSING_EMAIL, StaticValidators.isBlank())
                    .addValidationFunction(Messages.EMAIL_EXISTS, StaticValidators.doesEmailExist(session)).build()
                    .build();


        } else {
            builder.addAttributeValidator().forAttribute(UserModel.USERNAME)
                    .addValidationFunction(Messages.MISSING_USERNAME, StaticValidators.isBlank())
                    .addValidationFunction(Messages.USERNAME_EXISTS,
                            (value, o) -> session.users().getUserByUsername(value, realm) == null)
                    .build();
        }
    }

    private void addBasicValidators(ValidationChainBuilder builder, boolean userNameExistsCondition) {

        builder.addAttributeValidator().forAttribute(UserModel.USERNAME)
                .addValidationFunction(Messages.MISSING_USERNAME, StaticValidators.checkUsernameExists(userNameExistsCondition)).build()

                .addAttributeValidator().forAttribute(UserModel.FIRST_NAME)
                .addValidationFunction(Messages.MISSING_FIRST_NAME, StaticValidators.isBlank()).build()

                .addAttributeValidator().forAttribute(UserModel.LAST_NAME)
                .addValidationFunction(Messages.MISSING_LAST_NAME, StaticValidators.isBlank()).build()

                .addAttributeValidator().forAttribute(UserModel.EMAIL)
                .addValidationFunction(Messages.MISSING_EMAIL, StaticValidators.isBlank())
                .addValidationFunction(Messages.INVALID_EMAIL, StaticValidators.isEmailValid())
                .build();
    }

    private void addSessionValidators(ValidationChainBuilder builder) {
        RealmModel realm = this.session.getContext().getRealm();
        builder.addAttributeValidator().forAttribute(UserModel.USERNAME)
                .addValidationFunction(Messages.USERNAME_EXISTS, StaticValidators.userNameExists(session))
                .addValidationFunction(Messages.READ_ONLY_USERNAME, StaticValidators.isUserMutable(realm)).build()

                .addAttributeValidator().forAttribute(UserModel.EMAIL)
                .addValidationFunction(Messages.EMAIL_EXISTS, StaticValidators.isEmailDuplicated(session))
                .addValidationFunction(Messages.USERNAME_EXISTS, StaticValidators.doesEmailExistAsUsername(session)).build()
                .build();
    }

}