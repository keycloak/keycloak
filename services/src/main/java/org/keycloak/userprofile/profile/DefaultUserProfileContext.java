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

package org.keycloak.userprofile.profile;

import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileAttributes;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.profile.representations.IdpUserProfile;
import org.keycloak.userprofile.profile.representations.UserModelUserProfile;
import org.keycloak.userprofile.profile.representations.UserRepresentationUserProfile;
import org.keycloak.userprofile.validation.UserUpdateEvent;

/**
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public class DefaultUserProfileContext implements UserProfileContext {
    private UserProfile currentUserProfile;
    private UserUpdateEvent userUpdateEvent;

    private DefaultUserProfileContext(UserUpdateEvent userUpdateEvent, UserProfile currentUserProfile) {
        this.userUpdateEvent = userUpdateEvent;
        this.currentUserProfile = currentUserProfile;
    }

    public static DefaultUserProfileContext forIdpReview(SerializedBrokeredIdentityContext currentUser) {
        return new DefaultUserProfileContext(UserUpdateEvent.IdpReview, new IdpUserProfile(currentUser));
    }

    public static DefaultUserProfileContext forUpdateProfile(UserModel currentUser) {
        return new DefaultUserProfileContext(UserUpdateEvent.UpdateProfile, new UserModelUserProfile(currentUser));
    }

    public static DefaultUserProfileContext forAccountService(UserModel currentUser) {
        return new DefaultUserProfileContext(UserUpdateEvent.Account, new UserModelUserProfile(currentUser));
    }

    public static DefaultUserProfileContext forRegistrationUserCreation() {
        return new DefaultUserProfileContext(UserUpdateEvent.RegistrationUserCreation, null);
    }

    public static DefaultUserProfileContext forRegistrationProfile() {
        return new DefaultUserProfileContext(UserUpdateEvent.RegistrationProfile, null);
    }

    public static DefaultUserProfileContext forUserResource(UserRepresentation rep) {
        return new DefaultUserProfileContext(UserUpdateEvent.UserResource, new UserRepresentationUserProfile(rep));
    }

    @Override
    public UserProfile getCurrentProfile() {
        return currentUserProfile;
    }

    @Override
    public  UserUpdateEvent getUpdateEvent(){
        return  userUpdateEvent;
    }
}
