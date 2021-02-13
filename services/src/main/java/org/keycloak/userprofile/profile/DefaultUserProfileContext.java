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

import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.validation.UserProfileValidationResult;
import org.keycloak.userprofile.validation.UserUpdateEvent;

/**
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public class DefaultUserProfileContext implements UserProfileContext {
    private UserProfile currentUserProfile;
    private final UserProfile updatedProfile;
    private final UserProfileProvider profileProvider;
    private UserUpdateEvent userUpdateEvent;

    DefaultUserProfileContext(UserUpdateEvent userUpdateEvent, UserProfile currentUserProfile,
            UserProfile updatedProfile,
            UserProfileProvider profileProvider) {
        this.userUpdateEvent = userUpdateEvent;
        this.currentUserProfile = currentUserProfile;
        this.updatedProfile = updatedProfile;
        this.profileProvider = profileProvider;
    }

    @Override
    public UserProfile getCurrentProfile() {
        return currentUserProfile;
    }

    @Override
    public  UserUpdateEvent getUpdateEvent(){
        return  userUpdateEvent;
    }

    @Override
    public UserProfileValidationResult validate() {
        return profileProvider.validate(this, updatedProfile);
    }
}
