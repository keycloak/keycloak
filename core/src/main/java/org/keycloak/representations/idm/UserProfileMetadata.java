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
package org.keycloak.representations.idm;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

/**
 * @author Vlastimil Elias <velias@redhat.com>
 */
public class UserProfileMetadata {

    private List<UserProfileAttributeMetadata> attributes;
    private List<UserProfileAttributeGroupMetadata> groups;

    public UserProfileMetadata() {

    }

    public UserProfileMetadata(List<UserProfileAttributeMetadata> attributes, List<UserProfileAttributeGroupMetadata> groups) {
        super();
        this.attributes = attributes;
        this.groups = groups;
    }

    public List<UserProfileAttributeMetadata> getAttributes() {
        return attributes;
    }

    public List<UserProfileAttributeGroupMetadata> getGroups() {
        return groups;
    }

    public void setAttributes(List<UserProfileAttributeMetadata> attributes) {
        this.attributes = attributes;
    }

    public UserProfileAttributeMetadata getAttributeMetadata(String name) {
        for (UserProfileAttributeMetadata m : Optional.ofNullable(getAttributes()).orElse(emptyList())) {
            if (m.getName().equals(name)) {
                return m;
            }
        }

        return null;
    }

    public UserProfileAttributeGroupMetadata getAttributeGroupMetadata(String name) {
        for (UserProfileAttributeGroupMetadata m : Optional.ofNullable(getGroups()).orElse(emptyList())) {
            if (m.getName().equals(name)) {
                return m;
            }
        }

        return null;
    }
}
