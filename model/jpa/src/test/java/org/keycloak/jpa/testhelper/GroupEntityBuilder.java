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

import org.keycloak.models.GroupModel;
import org.keycloak.models.jpa.entities.GroupAttributeEntity;
import org.keycloak.models.jpa.entities.GroupEntity;

public class GroupEntityBuilder {

    private GroupEntity groupEntity;

    public static GroupEntityBuilder create() {
        return new GroupEntityBuilder();
    }

    private GroupEntityBuilder() {
        this.groupEntity = new GroupEntity();
        this.groupEntity.setParentId("");
        this.groupEntity.setAttributes(new ArrayList<>());
    }

    public GroupEntityBuilder id(String groupId) {
        groupEntity.setId(groupId);
        return this;
    }

    public GroupEntityBuilder name(String name) {
        groupEntity.setName(name);
        return this;
    }

    public GroupEntityBuilder realmId(String realmId) {
        groupEntity.setRealm(realmId);
        return this;
    }
    
    public GroupEntityBuilder type(GroupModel.Type type) {
        groupEntity.setType(type.intValue());
        return this;
    }

    public GroupEntityBuilder addAttribute(String attrName, String value) {
        GroupAttributeEntity attrEntity = new GroupAttributeEntity();
        attrEntity.setId(UUID.randomUUID().toString());
        attrEntity.setName(attrName);
        attrEntity.setValue(value);
        attrEntity.setGroup(groupEntity);
        groupEntity.getAttributes().add(attrEntity);
        return this;
    }

    public GroupEntity build() {
        return this.groupEntity;
    }
}
