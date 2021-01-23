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
package org.keycloak.testsuite.federation;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.group.GroupStorageProvider;
import org.keycloak.storage.group.GroupStorageProviderModel;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class HardcodedGroupStorageProvider implements GroupStorageProvider {
    private final GroupStorageProviderModel component;
    private final String groupName;


    public HardcodedGroupStorageProvider(GroupStorageProviderModel component) {
        this.component = component;
        this.groupName = component.getConfig().getFirst(HardcodedGroupStorageProviderFactory.GROUP_NAME);
    }

    @Override
    public void close() {
    }

    @Override
    public GroupModel getGroupById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        final String groupName = storageId.getExternalId();
        if (this.groupName.equals(groupName)) return new HardcodedGroupAdapter(realm);
        return null;
    }

    @Override
    public Stream<GroupModel> searchForGroupByNameStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        if (Boolean.parseBoolean(component.getConfig().getFirst(HardcodedGroupStorageProviderFactory.DELAYED_SEARCH))) try {
            Thread.sleep(5000l);
        } catch (InterruptedException ex) {
            Logger.getLogger(HardcodedGroupStorageProvider.class).warn(ex.getCause());
            return Stream.empty();
        }
        if (search != null && this.groupName.toLowerCase().contains(search.toLowerCase())) {
            return Stream.of(new HardcodedGroupAdapter(realm));
        }

        return Stream.empty();
    }


    public class HardcodedGroupAdapter implements GroupModel.Streams {

        private final RealmModel realm;
        private StorageId storageId;

        public HardcodedGroupAdapter(RealmModel realm) {
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
            return groupName;
        }

        @Override
        public Stream<RoleModel> getRealmRoleMappingsStream() {
            return Stream.empty();
        }

        @Override
        public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
            return Stream.empty();
        }

        @Override
        public boolean hasRole(RoleModel role) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Stream<RoleModel> getRoleMappingsStream() {
            return Stream.empty();
        }

        @Override
        public String getFirstAttribute(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Stream<String> getAttributeStream(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Map<String, List<String>> getAttributes() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public GroupModel getParent() {
            return null;
        }

        @Override
        public String getParentId() {
            return null;
        }

        @Override
        public Stream<GroupModel> getSubGroupsStream() {
            return Stream.empty();
        }

        @Override
        public void deleteRoleMapping(RoleModel role) {
            throw new ReadOnlyException("group is read only");
        }

        @Override
        public void grantRole(RoleModel role) {
            throw new ReadOnlyException("group is read only");
        }

        @Override
        public void setParent(GroupModel group) {
            throw new ReadOnlyException("group is read only");
        }

        @Override
        public void addChild(GroupModel subGroup) {
            throw new ReadOnlyException("group is read only");
        }

        @Override
        public void removeChild(GroupModel subGroup) {
            throw new ReadOnlyException("group is read only");
        }

        @Override
        public void setName(String name) {
            throw new ReadOnlyException("group is read only");
        }

        @Override
        public void setSingleAttribute(String name, String value) {
            throw new ReadOnlyException("group is read only");
        }

        @Override
        public void setAttribute(String name, List<String> values) {
            throw new ReadOnlyException("group is read only");
        }

        @Override
        public void removeAttribute(String name) {
            throw new ReadOnlyException("group is read only");
        }
    }


}
