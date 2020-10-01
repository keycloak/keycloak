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

import org.keycloak.userprofile.utils.StoredUserProfile;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.validation.UserUpdateEvent;

/**
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public class DefaultUserProfileContext implements UserProfileContext {
    private boolean isCreated;
    private StoredUserProfile currentUserProfile;
    private UserProfile updatedUserProfile;
    private UserUpdateEvent userUpdateEvent;

    public DefaultUserProfileContext(UserUpdateEvent userUpdateEvent, UserProfile updatedUserProfile) {
        this.userUpdateEvent = userUpdateEvent;
        this.isCreated = false;
        this.currentUserProfile = null;
        this.updatedUserProfile = updatedUserProfile;
    }

    public DefaultUserProfileContext(UserUpdateEvent userUpdateEvent, StoredUserProfile currentUserProfile, UserProfile updatedUserProfile) {
        this.userUpdateEvent = userUpdateEvent;
        this.isCreated = true;
        this.currentUserProfile = currentUserProfile;
        this.updatedUserProfile = updatedUserProfile;
    }


    @Override
    public boolean isCreate() {
        return isCreated;
    }

    @Override
    public UserProfile getCurrent() {
        return currentUserProfile;
    }

    @Override
    public UserProfile getUpdated() {
        return updatedUserProfile;
    }

    @Override
    public  UserUpdateEvent getUpdateEvent(){
        return  userUpdateEvent;
    }
}
