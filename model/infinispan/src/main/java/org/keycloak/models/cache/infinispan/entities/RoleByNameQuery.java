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
package org.keycloak.models.cache.infinispan.entities;

import java.util.Set;

import org.keycloak.models.RealmModel;

/**
 * @author Alexander Schwartz
 * @version $Revision: 1 $
 */
public class RoleByNameQuery extends AbstractRevisioned implements RoleQuery, InClient {
    private final String role;
    private final String realm;
    private String client;

    public RoleByNameQuery(Long revisioned, String id, RealmModel realm, String role) {
        super(revisioned, id);
        this.realm = realm.getId();
        this.role = role;
    }

    public RoleByNameQuery(Long revision, String id, RealmModel realm, String role, String client) {
        this(revision, id, realm, role);
        this.client = client;
    }

    @Override
    public Set<String> getRoles() {
        return role == null ? Set.of() : Set.of(role);
    }

    public String getRole() {
        return role;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getClientId() {
        return client;
    }

    @Override
    public String toString() {
        return "RoleNameQuery{" +
                "id='" + getId() + "'" +
                ", realm='" + realm + '\'' +
                ", clientUuid='" + client + '\'' +
                '}';
    }
}
