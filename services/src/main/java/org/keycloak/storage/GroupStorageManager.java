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
package org.keycloak.storage;

import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.group.GroupLookupProvider;
import org.keycloak.storage.group.GroupStorageProvider;
import org.keycloak.storage.group.GroupStorageProviderFactory;
import org.keycloak.storage.group.GroupStorageProviderModel;

import java.util.stream.Stream;

public class GroupStorageManager extends AbstractStorageManager<GroupStorageProvider, GroupStorageProviderModel> implements GroupProvider {

    public GroupStorageManager(KeycloakSession session) {
        super(session, GroupStorageProviderFactory.class, GroupStorageProvider.class,
                GroupStorageProviderModel::new, "group");
    }

    /* GROUP PROVIDER LOOKUP METHODS - implemented by group storage providers */

    @Override
    public GroupModel getGroupById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        if (storageId.getProviderId() == null) {
            return session.groupLocalStorage().getGroupById(realm, id);
        }

        GroupLookupProvider provider = getStorageProviderInstance(realm, storageId.getProviderId(), GroupLookupProvider.class);
        if (provider == null) return null;

        return provider.getGroupById(realm, id);
    }

    /**
     * Obtaining groups from an external client storage is time-bounded. In case the external group storage
     * isn't available at least groups from a local storage are returned. For this purpose
     * the {@link org.keycloak.services.DefaultKeycloakSessionFactory#getClientStorageProviderTimeout()} property is used.
     * Default value is 3000 milliseconds and it's configurable.
     * See {@link org.keycloak.services.DefaultKeycloakSessionFactory} for details.
     *
     */
    @Override
    public Stream<GroupModel> searchForGroupByNameStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        Stream<GroupModel> local = session.groupLocalStorage().searchForGroupByNameStream(realm, search,  firstResult, maxResults);
        Stream<GroupModel> ext = flatMapEnabledStorageProvidersWithTimeout(realm, GroupLookupProvider.class,
                        p -> p.searchForGroupByNameStream(realm, search, firstResult, maxResults));
        
        return Stream.concat(local, ext);
    }

    /* GROUP PROVIDER METHODS - provided only by local storage (e.g. not supported by storage providers) */

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm) {
        return session.groupLocalStorage().getGroupsStream(realm);
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        return session.groupLocalStorage().getGroupsStream(realm, ids, search, first, max);
    }

    @Override
    public Long getGroupsCount(RealmModel realm, Boolean onlyTopGroups) {
        return session.groupLocalStorage().getGroupsCount(realm, onlyTopGroups);
    }

    @Override
    public Long getGroupsCountByNameContaining(RealmModel realm, String search) {
        return session.groupLocalStorage().getGroupsCountByNameContaining(realm, search);
    }

    @Override
    public Stream<GroupModel> getGroupsByRoleStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        return session.groupLocalStorage().getGroupsByRoleStream(realm, role, firstResult, maxResults);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm) {
        return session.groupLocalStorage().getTopLevelGroupsStream(realm);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        return session.groupLocalStorage().getTopLevelGroupsStream(realm, firstResult, maxResults);
    }

    @Override
    public GroupModel createGroup(RealmModel realm, String id, String name, GroupModel toParent) {
        return session.groupLocalStorage().createGroup(realm, id, name, toParent);
    }

    @Override
    public boolean removeGroup(RealmModel realm, GroupModel group) {
        return session.groupLocalStorage().removeGroup(realm, group);
    }

    @Override
    public void moveGroup(RealmModel realm, GroupModel group, GroupModel toParent) {
        session.groupLocalStorage().moveGroup(realm, group, toParent);
    }

    @Override
    public void addTopLevelGroup(RealmModel realm, GroupModel subGroup) {
        session.groupLocalStorage().addTopLevelGroup(realm, subGroup);
    }

    @Override
    public void close() {

    }
}
