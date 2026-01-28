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

package org.keycloak.testsuite.util;

import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.GroupRepresentation;

/**
 *
 * @author <a href="mailto:hmlnarik@redhat.com">Hynek Mlnarik</a>
 */
public class GroupBuilder {

    private final GroupRepresentation rep;

    public static GroupBuilder create() {
        final GroupRepresentation rep = new GroupRepresentation();
        return new GroupBuilder(rep);
    }

    public static GroupBuilder edit(GroupRepresentation rep) {
        return new GroupBuilder(rep);
    }

    private GroupBuilder(GroupRepresentation rep) {
        this.rep = rep;
    }

    public GroupRepresentation build() {
        return rep;
    }

    public GroupBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public GroupBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public GroupBuilder path(String path) {
        rep.setPath(path);
        return this;
    }

    public GroupBuilder realmRoles(List<String> realmRoles) {
        rep.setRealmRoles(realmRoles);
        return this;
    }

    public GroupBuilder clientRoles(Map<String, List<String>> clientRoles) {
        rep.setClientRoles(clientRoles);
        return this;
    }

    public GroupBuilder attributes(Map<String, List<String>> attributes) {
        rep.setAttributes(attributes);
        return this;
    }

    public GroupBuilder singleAttribute(String name, String value) {
        rep.singleAttribute(name, value);
        return this;
    }

    public GroupBuilder subGroups(List<GroupRepresentation> subGroups) {
        rep.setSubGroups(subGroups);
        return this;
    }

}
