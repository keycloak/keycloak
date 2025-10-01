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

import org.keycloak.models.jpa.entities.RoleAttributeEntity;
import org.keycloak.models.jpa.entities.RoleEntity;

public class RoleEntityBuilder {

    private RoleEntity roleEntity;

    public static RoleEntityBuilder create() {
        return new RoleEntityBuilder();
    }

    private RoleEntityBuilder() {
        this.roleEntity = new RoleEntity();
        this.roleEntity.setAttributes(new ArrayList<>());
    }

    public RoleEntityBuilder id(String roleId) {
        roleEntity.setId(roleId);
        return this;
    }

    public RoleEntityBuilder name(String name) {
        roleEntity.setName(name);
        return this;
    }

    public RoleEntityBuilder realmId(String realmId) {
        roleEntity.setRealmId(realmId);
        return this;
    }

    public RoleEntityBuilder addAttribute(String attrName, String value) {
        RoleAttributeEntity attrEntity = new RoleAttributeEntity();
        attrEntity.setId(UUID.randomUUID().toString());
        attrEntity.setName(attrName);
        attrEntity.setValue(value);
        attrEntity.setRole(roleEntity);
        roleEntity.getAttributes().add(attrEntity);
        return this;
    }

    public RoleEntity build() {
        return this.roleEntity;
    }
}
