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

package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.models.RealmModel;

import java.util.Set;

public class ClientScopeListQuery extends AbstractRevisioned implements ClientScopeQuery {
    private final Set<String> clientScopes;
    private final String realm;
    private final String realmName;
    private String clientUuid;

    public ClientScopeListQuery(Long revisioned, String id, RealmModel realm, Set<String> clientScopes) {
        super(revisioned, id);
        this.realm = realm.getId();
        this.realmName = realm.getName();
        this.clientScopes = clientScopes;
    }

    public ClientScopeListQuery(Long revisioned, String id, RealmModel realm, String clientUuid, Set<String> clientScopes) {
        this(revisioned, id, realm, clientScopes);
        this.clientUuid = clientUuid;
    }

    @Override
    public Set<String> getClientScopes() {
        return clientScopes;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getClientId() {
        return clientUuid;
    }

    @Override
    public String toString() {
        return "ClientScopeListQuery{" +
                "id='" + getId() + "'" +
                ", realmName='" + realmName + '\'' +
                ", clientUuid='" + clientUuid + '\'' +
                '}';
    }
}
