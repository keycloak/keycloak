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

package org.keycloak.models;

import org.keycloak.provider.Provider;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * Provider of group records
 * @author mhajas
 *
 */
public interface GroupProvider extends Provider {

    /**
     * Returns a group from the given realm with the corresponding id
     * 
     * @param id Id.
     * @param realm Realm.
     * @return GroupModel with the corresponding id.
     * @deprecated Use method {@code getGroupById(realm, id)}
     */
    default GroupModel getGroupById(String id, RealmModel realm) {
        return getGroupById(realm, id);
    }

    /**
     * Returns a group from the given realm with the corresponding id
     *
     * @param realm Realm.
     * @param id Id.
     * @return GroupModel with the corresponding id.
     */
    GroupModel getGroupById(RealmModel realm, String id);

    /**
     * Returns groups for the given realm.
     *
     * @param realm Realm.
     * @return List of groups in the Realm.
     * @deprecated Use {@link #getGroupsStream(RealmModel)}  getGroupsStream} instead.
     */
    @Deprecated
    default List<GroupModel> getGroups(RealmModel realm) {
        return getGroupsStream(realm).collect(Collectors.toList());
    }

    /**
     * Returns groups for the given realm.
     *
     * @param realm Realm.
     * @return Stream of groups in the Realm.
     */
    Stream<GroupModel> getGroupsStream(RealmModel realm);

    /**
     * Returns a number of groups/top level groups (i.e. groups without parent group) for the given realm.
     *
     * @param realm Realm.
     * @param onlyTopGroups When true the function returns a count of top level groups only.
     * @return Number of groups/top level groups.
     */
    Long getGroupsCount(RealmModel realm, Boolean onlyTopGroups);

    /**
     * Returns number of groups with the given string in name for the given realm.
     *
     * @param realm Realm.
     * @param search Searched string.
     * @return Number of groups with the given string in its name.
     */
    Long getGroupsCountByNameContaining(RealmModel realm, String search);

    /**
     * Returns groups with the given role in the given realm.
     *
     * @param realm Realm.
     * @param role Role.
     * @param firstResult First result to return. Ignored if negative.
     * @param maxResults Maximum number of results to return. Ignored if negative.
     * @return List of groups with the given role.
     * @deprecated Use {@link #getGroupsByRoleStream(RealmModel, RoleModel, int, int)}  getGroupsByRoleStream} instead.
     */
    @Deprecated
    default List<GroupModel> getGroupsByRole(RealmModel realm, RoleModel role, int firstResult, int maxResults) {
        return  getGroupsByRoleStream(realm, role, firstResult, maxResults).collect(Collectors.toList());
    }

    /**
     * Returns groups with the given role in the given realm.
     *
     * @param realm Realm.
     * @param role Role.
     * @param firstResult First result to return. Ignored if negative.
     * @param maxResults Maximum number of results to return. Ignored if negative.
     * @return Stream of groups with the given role.
     */
     Stream<GroupModel> getGroupsByRoleStream(RealmModel realm, RoleModel role, int firstResult, int maxResults);

    /**
     * Returns all top level groups (i.e. groups without parent group) for the given realm.
     *
     * @param realm Realm.
     * @return List of all top level groups in the realm.
     * @deprecated Use {@link #getTopLevelGroupsStream(RealmModel)}  getTopLevelGroupsStream} instead.
     */
    @Deprecated
    default List<GroupModel> getTopLevelGroups(RealmModel realm) {
        return getTopLevelGroupsStream(realm).collect(Collectors.toList());
    }

    /**
     * Returns all top level groups (i.e. groups without parent group) for the given realm.
     *
     * @param realm Realm.
     * @return Stream of all top level groups in the realm.
     */
    Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm);

    /**
     * Returns top level groups (i.e. groups without parent group) for the given realm.
     *
     * @param realm Realm.
     * @param firstResult First result to return.
     * @param maxResults Maximum number of results to return.
     * @return List of top level groups in the realm.
     * @deprecated Use {@link #getTopLevelGroupsStream(RealmModel, Integer, Integer)}  getTopLevelGroupsStream} instead.
     */
    @Deprecated
    default List<GroupModel> getTopLevelGroups(RealmModel realm, Integer firstResult, Integer maxResults) {
        return getTopLevelGroupsStream(realm, firstResult, maxResults).collect(Collectors.toList());
    }

    /**
     * Returns top level groups (i.e. groups without parent group) for the given realm.
     *
     * @param realm Realm.
     * @param firstResult First result to return.
     * @param maxResults Maximum number of results to return.
     * @return Stream of top level groups in the realm.
     */
    Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm, Integer firstResult, Integer maxResults);

    /**
     * Returns groups with the given string in name for the given realm.
     *
     * @param realm Realm.
     * @param search Searched string.
     * @param firstResult First result to return. Ignored if {@code null}.
     * @param maxResults Maximum number of results to return. Ignored if {@code null}.
     * @return List of groups with the given string in name.
     * @deprecated Use {@link #searchForGroupByNameStream(RealmModel, String, Integer, Integer)}  searchForGroupByNameStream} instead.
     */
    @Deprecated
    default List<GroupModel> searchForGroupByName(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        return searchForGroupByNameStream(realm, search, firstResult, maxResults).collect(Collectors.toList());
    }

    /**
     * Returns groups with the given string in name for the given realm.
     *
     * @param realm Realm.
     * @param search Searched string.
     * @param firstResult First result to return. Ignored if {@code null}.
     * @param maxResults Maximum number of results to return. Ignored if {@code null}.
     * @return Stream of groups with the given string in name.
     */
    Stream<GroupModel> searchForGroupByNameStream(RealmModel realm, String search, Integer firstResult, Integer maxResults);

    /**
     * Creates a new group with the given name in the given realm.
     * Effectively the same as {@code createGroup(realm, null, name, null)}.
     *
     * @param realm Realm.
     * @param name Name.
     * @return Model of the created group.
     */
    default GroupModel createGroup(RealmModel realm, String name) {
        return createGroup(realm, null, name, null);
    }

    /**
     * Creates a new group with the given id and name in the given realm.
     * Effectively the same as {@code createGroup(realm, id, name, null)}
     *
     * @param realm Realm.
     * @param id Id.
     * @param name Name.
     * @return Model of the created group
     */
    default GroupModel createGroup(RealmModel realm, String id, String name) {
        return createGroup(realm, id, name, null);
    }

    /**
     * Creates a new group with the given name and parent to the given realm.
     * Effectively the same as {@code createGroup(realm, null, name, toParent)}.
     *
     * @param realm Realm.
     * @param name Name.
     * @param toParent Parent group.
     * @return Model of the created group.
     */
    default GroupModel createGroup(RealmModel realm, String name, GroupModel toParent) {
        return createGroup(realm, null, name, toParent);
    }

    /**
     * Creates a new group with the given name, id, name and parent to the given realm.
     *
     * @param realm Realm.
     * @param id Id, will be generated if {@code null}.
     * @param name Name.
     * @param toParent Parent group, or {@code null} if the group is top level group
     * @return Model of the created group
     */
    GroupModel createGroup(RealmModel realm, String id, String name, GroupModel toParent);

    /**
     * Removes the given group for the given realm.
     *
     * @param realm Realm.
     * @param group Group.
     * @return true if the group was removed, false if group doesn't exist or doesn't belong to the given realm
     */
    boolean removeGroup(RealmModel realm, GroupModel group);

    /**
     * This method is used for moving groups in group structure, for example:
     * <ul>
     * <li>making an existing child group child group of some other group,</li>
     * <li>setting a top level group (i.e. group without parent group) child of some group,</li>
     * <li>making a child group top level group (i.e. removing its parent group).</li>
     * <ul/>
     *
     * @param realm Realm owning this group.
     * @param group Group to update.
     * @param toParent New parent group, or {@code null} if we are moving the group to top level group.
     */
    void moveGroup(RealmModel realm, GroupModel group, GroupModel toParent);

    /**
     * Removes parent group for the given group in the given realm.
     *
     * @param realm Realm.
     * @param subGroup Group.
     */
    void addTopLevelGroup(RealmModel realm, GroupModel subGroup);
}
