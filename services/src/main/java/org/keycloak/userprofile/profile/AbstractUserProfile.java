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
import org.keycloak.userprofile.utils.StoredUserProfile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public abstract class AbstractUserProfile implements UserProfile , StoredUserProfile {


    /*
    The user attributes handling is different in each user representation so we have to use the setAttributes and getAttributes from the original object
     */

    @Override
    public abstract Map<String, List<String>> getAttributes();

    @Override
    public abstract void setAttribute(String key, List<String> value);

    /*
    The user id is different in each user representation
     */
    @Override
    public abstract String getId();

    @Override
    public void setSingleAttribute(String key, String value) {
        this.setAttribute(key, Collections.singletonList(value));
    }

    @Override
    public String getFirstAttribute(String key) {
        return this.getAttributes() == null ? null : this.getAttributes().get(key) == null ? null : this.getAttributes().get(key).size() == 0 ? null : this.getAttributes().get(key).get(0);
    }

    @Override
    public List<String> getAttribute(String key) {
        return getAttributes().get(key);
    }

    @Override
    public void removeAttribute(String attr) {
        getAttributes().remove(attr);
    }
}
