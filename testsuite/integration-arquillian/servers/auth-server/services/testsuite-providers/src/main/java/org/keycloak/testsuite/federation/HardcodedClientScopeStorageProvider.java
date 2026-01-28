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
package org.keycloak.testsuite.federation;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.clientscope.ClientScopeLookupProvider;
import org.keycloak.storage.clientscope.ClientScopeStorageProvider;
import org.keycloak.storage.clientscope.ClientScopeStorageProviderModel;


public class HardcodedClientScopeStorageProvider implements ClientScopeStorageProvider, ClientScopeLookupProvider {

    private final ClientScopeStorageProviderModel component;
    private final String clientScopeName;

    public HardcodedClientScopeStorageProvider(KeycloakSession session, ClientScopeStorageProviderModel component) {
        this.component = component;
        this.clientScopeName = component.getConfig().getFirst(HardcodedClientScopeStorageProviderFactory.SCOPE_NAME);
    }

    @Override
    public ClientScopeModel getClientScopeById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        final String scopeName = storageId.getExternalId();
        if (this.clientScopeName.equals(scopeName)) return new HardcodedClientScopeAdapter(realm);
        return null;
    }

    @Override
    public void close() {
    }

    public class HardcodedClientScopeAdapter implements ClientScopeModel {

        private final RealmModel realm;
        private StorageId storageId;

        public HardcodedClientScopeAdapter(RealmModel realm) {
            this.realm = realm;
        }

        @Override
        public String getId() {
            if (storageId == null) {
                storageId = new StorageId(component.getId(), getName());
            }
            return storageId.getId();
        }

        @Override
        public String getName() {
            return clientScopeName;
        }

        @Override
        public RealmModel getRealm() {
            return realm;
        }

        @Override
        public void setName(String name) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public String getDescription() {
            return "Federated client scope";
        }

        @Override
        public void setDescription(String description) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public String getProtocol() {
            return "openid-connect";
        }

        @Override
        public void setProtocol(String protocol) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void setAttribute(String name, String value) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void removeAttribute(String name) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public String getAttribute(String name) {
            return null;
        }

        @Override
        public Map<String, String> getAttributes() {
            return Collections.EMPTY_MAP;
        }

        @Override
        public Stream<ProtocolMapperModel> getProtocolMappersStream() {
            return Stream.empty();
        }

        @Override
        public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void removeProtocolMapper(ProtocolMapperModel mapping) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void updateProtocolMapper(ProtocolMapperModel mapping) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public ProtocolMapperModel getProtocolMapperById(String id) {
            return null;
        }

        @Override
        public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
            return null;
        }

        @Override
        public Stream<RoleModel> getScopeMappingsStream() {
            return Stream.empty();
        }

        @Override
        public Stream<RoleModel> getRealmScopeMappingsStream() {
            return Stream.empty();
        }

        @Override
        public void addScopeMapping(RoleModel role) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void deleteScopeMapping(RoleModel role) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public boolean hasScope(RoleModel role) {
            return false;
        }
    }
}
