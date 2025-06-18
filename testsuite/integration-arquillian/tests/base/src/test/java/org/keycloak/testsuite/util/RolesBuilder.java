/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.testsuite.util;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class RolesBuilder {

    private RolesRepresentation rep;

    public static RolesBuilder create() {
        return new RolesBuilder();
    }

    private RolesBuilder() {
        rep = new RolesRepresentation();
    }

    public RolesBuilder realmRole(RoleRepresentation role) {
        if (rep.getRealm() == null) {
            rep.setRealm(new LinkedList<RoleRepresentation>());
        }

        rep.getRealm().add(role);
        return this;
    }

    public RolesBuilder clientRole(String client, RoleRepresentation role) {
        if (rep.getClient() == null) {
            rep.setClient(new HashMap<String, List<RoleRepresentation>>());
        }

        List<RoleRepresentation> clientList = rep.getClient().get(client);
        if (clientList == null) {
            rep.getClient().put(client, new LinkedList<RoleRepresentation>());
        }

        rep.getClient().get(client).add(role);
        return this;
    }

    public RolesRepresentation build() {
        return rep;
    }
}
