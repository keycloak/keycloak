/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.jpa.testhelper;

import java.util.ArrayList;
import java.util.UUID;

import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserEntity;

public class UserEntityBuilder {
    
    private UserEntity userEntity;

    public static UserEntityBuilder create() {
        return new UserEntityBuilder();
    }

    private UserEntityBuilder() {
        this.userEntity = new UserEntity();
        this.userEntity.setAttributes(new ArrayList<>());
    }

    public UserEntityBuilder id(String userId) {
        userEntity.setId(userId);
        return this;
    }

    public UserEntityBuilder username(String name) {
        userEntity.setUsername(name);
        return this;
    }

    public UserEntityBuilder email(String email) {
        userEntity.setEmail(email, false);
        return this;
    }

    public UserEntityBuilder firstName(String firstName) {
        userEntity.setFirstName(firstName);
        return this;
    }

    public UserEntityBuilder lastName(String lastName) {
        userEntity.setLastName(lastName);
        return this;
    }
    
    public UserEntityBuilder addAttribute(String attrName, String value) {
        UserAttributeEntity attrEntity = new UserAttributeEntity();
        attrEntity.setId(UUID.randomUUID().toString());
        attrEntity.setName(attrName);
        attrEntity.setValue(value);
        attrEntity.setUser(userEntity);
        userEntity.getAttributes().add(attrEntity);
        return this;
    }

    public UserEntity build() {
        return userEntity;
    }
}
