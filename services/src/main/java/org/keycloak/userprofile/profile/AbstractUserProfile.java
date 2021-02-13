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
import org.keycloak.userprofile.UserProfileAttributes;
import org.keycloak.userprofile.UserProfileProvider;

import java.util.List;
import java.util.Map;

public abstract class AbstractUserProfile implements UserProfile {

    private final UserProfileAttributes attributes;


    public AbstractUserProfile(Map<String, List<String>> attributes, UserProfileProvider profileProvider) {
        this.attributes = new UserProfileAttributes(attributes, profileProvider);
    }

    @Override
    public UserProfileAttributes getAttributes() {
        return this.attributes;
    }
}
