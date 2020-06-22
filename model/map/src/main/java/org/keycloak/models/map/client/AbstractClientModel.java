/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.client;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.common.AbstractEntity;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractClientModel<E extends AbstractEntity> implements ClientModel {

    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final E entity;

    public AbstractClientModel(KeycloakSession session, RealmModel realm, E entity) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(realm, "realm");

        this.session = session;
        this.realm = realm;
        this.entity = entity;
    }

    @Override
    public void addClientScopes(Set<ClientScopeModel> clientScopes, boolean defaultScope) {
        for (ClientScopeModel cs : clientScopes) {
            addClientScope(cs, defaultScope);
        }
    }

    @Override
    public Set<RoleModel> getRealmScopeMappings() {
        String realmId = realm.getId();
        return getScopeMappingsStream()
          .filter(rm -> Objects.equals(rm.getContainerId(), realmId))
          .collect(Collectors.toSet());
    }

    @Override
    public RoleModel getRole(String name) {
        return session.realms().getClientRole(realm, this, name);
    }

    @Override
    public RoleModel addRole(String name) {
        return session.realms().addClientRole(realm, this, name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return session.realms().addClientRole(realm, this, id, name);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return session.realms().removeRole(realm, role);
    }

    @Override
    public Set<RoleModel> getRoles() {
        return session.realms().getClientRoles(realm, this);
    }

    @Override
    public Set<RoleModel> getRoles(Integer firstResult, Integer maxResults) {
        return session.realms().getClientRoles(realm, this, firstResult, maxResults);
    }

    @Override
    public Set<RoleModel> searchForRoles(String search, Integer first, Integer max) {
        return session.realms().searchForClientRoles(realm, this, search, first, max);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientModel)) return false;

        ClientModel that = (ClientModel) o;
        return Objects.equals(that.getId(), getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
