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

package org.keycloak.models.map.authorization;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import org.keycloak.models.map.authorization.entity.MapPolicyEntity;
import org.keycloak.models.map.authorization.entity.MapResourceEntity;
import org.keycloak.models.map.authorization.entity.MapResourceServerEntity;
import org.keycloak.models.map.authorization.entity.MapScopeEntity;
import org.keycloak.models.map.storage.MapStorage;


/**
 * @author mhajas 
 */
public class MapAuthorizationStore implements StoreFactory {

    private final PolicyStore policyStore;
    private final ResourceServerStore resourceServerStore;
    private final ResourceStore resourceStore;
    private final ScopeStore scopeStore;
    private final PermissionTicketStore permissionTicketStore;
    private boolean readOnly;

    public MapAuthorizationStore(KeycloakSession session, MapStorage permissionTicketStore, MapStorage policyStore, MapStorage resourceServerStore, MapStorage resourceStore, MapStorage scopeStore, AuthorizationProvider provider) {
        this.permissionTicketStore = new MapPermissionTicketStore(session, permissionTicketStore, provider);
        this.policyStore = new MapPolicyStore(session, policyStore, provider);
        this.resourceServerStore = new MapResourceServerStore(session, resourceServerStore, provider);
        this.resourceStore = new MapResourceStore(session, resourceStore, provider);
        this.scopeStore = new MapScopeStore(session, scopeStore, provider);
    }

    @Override
    public ResourceStore getResourceStore() {
        return resourceStore;
    }

    @Override
    public ResourceServerStore getResourceServerStore() {
        return resourceServerStore;
    }

    @Override
    public ScopeStore getScopeStore() {
        return scopeStore;
    }

    @Override
    public PolicyStore getPolicyStore() {
        return policyStore;
    }

    @Override
    public PermissionTicketStore getPermissionTicketStore() {
        return permissionTicketStore;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void close() {

    }
}
