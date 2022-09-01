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

package org.keycloak.representations.account;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.keycloak.json.StringListMapDeserializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserRepresentation {

    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private boolean emailVerified;
    private UserProfileMetadata userProfileMetadata;

    @JsonDeserialize(using = StringListMapDeserializer.class)
    private Map<String, List<String>> attributes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    public void singleAttribute(String name, String value) {
        if (this.attributes == null) this.attributes=new HashMap<>();
        attributes.put(name, (value == null ? new ArrayList<String>() : Arrays.asList(value)));
    }

    public String firstAttribute(String key) {
        return this.attributes == null ? null : this.attributes.containsKey(key) ? this.attributes.get(key).get(0) : null;
    }

    public Map<String, List<String>> toAttributes() {
        Map<String, List<String>> attrs = new HashMap<>();

        if (getAttributes() != null) attrs.putAll(getAttributes());

        if (getUsername() != null)
            attrs.put("username", Collections.singletonList(getUsername()));
        else
            attrs.remove("username");

        if (getEmail() != null)
            attrs.put("email", Collections.singletonList(getEmail()));
        else
            attrs.remove("email");

        if (getLastName() != null)
            attrs.put("lastName", Collections.singletonList(getLastName()));

        if (getFirstName() != null)
            attrs.put("firstName", Collections.singletonList(getFirstName()));


        return attrs;
    }

    public UserProfileMetadata getUserProfileMetadata() {
        return userProfileMetadata;
    }

    public void setUserProfileMetadata(UserProfileMetadata userProfileMetadata) {
        this.userProfileMetadata = userProfileMetadata;
    }
}
