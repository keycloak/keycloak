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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RoleRepresentation.Composites;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RoleBuilder {

    private final RoleRepresentation rep;

    public static RoleBuilder create() {
        return new RoleBuilder(new RoleRepresentation());
    }

    public static RoleBuilder edit(RoleRepresentation rep) {
        return new RoleBuilder(rep);
    }

    private RoleBuilder(RoleRepresentation rep) {
        this.rep = rep;
    }

    public RoleBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public RoleBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public RoleBuilder description(String description) {
        rep.setDescription(description);
        return this;
    }

    public RoleBuilder composite() {
        rep.setComposite(true);
        return this;
    }
    
    public RoleBuilder attributes(Map<String, List<String>> attributes) {
        rep.setAttributes(attributes);
        return this;
    }

    public RoleBuilder singleAttribute(String name, String value) {
        rep.singleAttribute(name, value);
        return this;
    }

    private void checkCompositesNull() {
        if (rep.getComposites() == null) {
            rep.setComposites(new Composites());
        }
    }

    public RoleBuilder realmComposite(RoleRepresentation role) {
        return realmComposite(role.getName());
    }

    public RoleBuilder realmComposite(String compositeRole) {
        checkCompositesNull();

        if (rep.getComposites().getRealm() == null) {
            rep.getComposites().setRealm(new HashSet<>());
        }

        rep.getComposites().getRealm().add(compositeRole);
        return this;
    }

    public RoleBuilder clientComposite(String client, RoleRepresentation compositeRole) {
        return clientComposite(client, compositeRole.getName());
    }

    public RoleBuilder clientComposite(String client, String compositeRole) {
        checkCompositesNull();

        if (rep.getComposites().getClient() == null) {
            rep.getComposites().setClient(new HashMap<>());
        }

        if (rep.getComposites().getClient().get(client) == null) {
            rep.getComposites().getClient().put(client, new LinkedList<>());
        }

        rep.getComposites().getClient().get(client).add(compositeRole);
        return this;
    }

    public RoleRepresentation build() {
        return rep;
    }

}
