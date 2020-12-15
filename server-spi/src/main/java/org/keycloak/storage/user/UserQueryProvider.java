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
package org.keycloak.storage.user;

import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Optional capability interface implemented by UserStorageProviders.
 * Defines complex queries that are used to locate one or more users.  You must implement this interface
 * if you want to view and manager users from the administration console.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserQueryProvider {

    /**
     * Returns the number of users, without consider any service account.
     *
     * @param realm the realm
     * @return the number of users
     */
    int getUsersCount(RealmModel realm);

    /**
     * Returns the number of users that are in at least one of the groups
     * given.
     *
     * @param realm    the realm
     * @param groupIds set of groups id to check for
     * @return the number of users that are in at least one of the groups
     */
    default int getUsersCount(RealmModel realm, Set<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return 0;
        }
        return countUsersInGroups(getUsersStream(realm), groupIds);
    }

    /**
     * Returns the number of users that match the given criteria.
     *
     * @param search search criteria
     * @param realm  the realm
     * @return number of users that match the search
     */
    default int getUsersCount(String search, RealmModel realm) {
        return (int) searchForUserStream(search, realm).count();
    }

    /**
     * Returns the number of users that match the given criteria and are in
     * at least one of the groups given.
     *
     * @param search   search criteria
     * @param realm    the realm
     * @param groupIds set of groups to check for
     * @return number of users that match the search and given groups
     */
    default int getUsersCount(String search, RealmModel realm, Set<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return 0;
        }
        return countUsersInGroups(searchForUserStream(search, realm), groupIds);
    }

    /**
     * Returns the number of users that match the given filter parameters.
     *
     * @param params filter parameters
     * @param realm  the realm
     * @return number of users that match the given filters
     */
    default int getUsersCount(Map<String, String> params, RealmModel realm) {
        return (int) searchForUserStream(params, realm).count();
    }

    /**
     * Returns the number of users that match the given filter parameters and is in
     * at least one of the given groups.
     *
     * @param params   filter parameters
     * @param realm    the realm
     * @param groupIds set if groups to check for
     * @return number of users that match the given filters and groups
     */
    default int getUsersCount(Map<String, String> params, RealmModel realm, Set<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return 0;
        }
        return countUsersInGroups(searchForUserStream(params, realm), groupIds);
    }

    /**
     * Returns the number of users from the given list of users that are in at
     * least one of the groups given in the groups set.
     *
     * @param users    list of users to check
     * @param groupIds id of groups that should be checked for
     * @return number of users that are in at least one of the groups
     */
    static int countUsersInGroups(Stream<UserModel> users, Set<String> groupIds) {
        return (int) users.filter(u -> u.getGroupsStream().map(GroupModel::getId).anyMatch(groupIds::contains)).count();
    }

    /**
     * Returns the number of users.
     *
     * @param realm the realm
     * @param includeServiceAccount if true, the number of users will also include service accounts. Otherwise, only the number of users.
     * @return the number of users
     */
    default int getUsersCount(RealmModel realm, boolean includeServiceAccount) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * @deprecated Use {@link #getUsersStream(RealmModel) getUsersStream} instead.
     */
    @Deprecated
    List<UserModel> getUsers(RealmModel realm);
    /**
     * Searches all users in the realm.
     *
     * @param realm a reference to the realm.
     * @return a non-null {@link Stream} of users.
     */
    default Stream<UserModel> getUsersStream(RealmModel realm) {
        List<UserModel> value = this.getUsers(realm);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * @deprecated Use {@link #getUsersStream(RealmModel, int, int) getUsersStream} instead.
     */
    @Deprecated
    List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults);

    /**
     * Searches all users in the realm, starting from the {@code firstResult} and containing at most {@code maxResults}.
     *
     * @param realm a reference to the realm.
     * @param firstResult first result to return. Ignored if negative.
     * @param maxResults maximum number of results to return. Ignored if negative.
     * @return a non-null {@link Stream} of users.
     */
    default Stream<UserModel> getUsersStream(RealmModel realm, int firstResult, int maxResults) {
        List<UserModel> value = this.getUsers(realm, firstResult, maxResults);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * Search for users with username, email or first + last name that is like search string.
     *
     * If possible, implementations should treat the parameter values as partial match patterns i.e. in RDMBS terms use LIKE.
     *
     * This method is used by the admin console search box
     *
     * @param search
     * @param realm
     * @return
     * @deprecated Use {@link #searchForUserStream(String, RealmModel) searchForUserStream} instead.
     */
    @Deprecated
    List<UserModel> searchForUser(String search, RealmModel realm);

    /**
     * Searches for users with username, email or first + last name that is like search string.  If possible, implementations
     * should treat the parameter values as partial match patterns (i.e. in RDMBS terms use LIKE).
     * <p/>
     * This method is used by the admin console search box
     *
     * @param search case sensitive search string.
     * @param realm a reference to the realm.
     * @return a non-null {@link Stream} of users that match the search string.
     */
    default Stream<UserModel> searchForUserStream(String search, RealmModel realm) {
        List<UserModel> value = this.searchForUser(search, realm);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * Search for users with username, email or first + last name that is like search string.
     *
     * If possible, implementations should treat the parameter values as partial match patterns i.e. in RDMBS terms use LIKE.
     *
     * This method is used by the admin console search box
     *
     * @param search
     * @param realm
     * @param firstResult
     * @param maxResults
     * @return
     * @deprecated Use {@link #searchForUserStream(String, RealmModel, Integer, Integer) searchForUserStream} instead.
     */
    @Deprecated
    List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults);

    /**
     * Searches for users with username, email or first + last name that is like search string. If possible, implementations
     * should treat the parameter values as partial match patterns (i.e. in RDMBS terms use LIKE).
     * <p/>
     * This method is used by the admin console search box
     *
     * @param search case sensitive search string.
     * @param realm a reference to the realm.
     * @param firstResult first result to return. Ignored if negative.
     * @param maxResults maximum number of results to return. Ignored if negative.
     * @return a non-null {@link Stream} of users that match the search criteria.
     */
    default Stream<UserModel> searchForUserStream(String search, RealmModel realm, Integer firstResult, Integer maxResults) {
        List<UserModel> value = this.searchForUser(search, realm, firstResult == null ? -1 : firstResult, maxResults == null ? -1 : maxResults);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * Search for user by parameter.  Valid parameters are:
     * "first" - first name
     * "last" - last name
     * "email" - email
     * "username" - username
     *
     * If possible, implementations should treat the parameter values as partial match patterns i.e. in RDMBS terms use LIKE.
     *
     * This method is used by the REST API when querying users.
     *
     *
     * @param params
     * @param realm
     * @return
     * @deprecated Use {@link #searchForUserStream(Map, RealmModel) searchForUserStream} instead.
     */
    @Deprecated
    List<UserModel> searchForUser(Map<String, String> params, RealmModel realm);

    /**
     * Searches for user by parameter. If possible, implementations should treat the parameter values as partial match patterns
     * (i.e. in RDMBS terms use LIKE). Valid parameters are:
     * <ul>
     *   <li><b>first</b> - first name</li>
     *   <li><b>last</b> - last name</li>
     *   <li><b>email</b> - email</li>
     *   <li><b>username</b> - username</li>
     *   <li><b>enabled</b> - if user is enabled (true/false)</li>
     * </ul>
     * This method is used by the REST API when querying users.
     *
     * @param params a map containing the search parameters.
     * @param realm a reference to the realm.
     * @return a non-null {@link Stream} of users that match the search parameters.
     */
    default Stream<UserModel> searchForUserStream(Map<String, String> params, RealmModel realm) {
        List<UserModel> value = this.searchForUser(params, realm);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * Search for user by parameter.    Valid parameters are:
     * "first" - first name
     * "last" - last name
     * "email" - email
     * "username" - username
     * "enabled" - is user enabled (true/false)
     *
     * If possible, implementations should treat the parameter values as patterns i.e. in RDMBS terms use LIKE.
     * This method is used by the REST API when querying users.
     *
     *
     * @param params
     * @param realm
     * @param firstResult
     * @param maxResults
     * @return
     * @deprecated Use {@link #searchForUserStream(Map, RealmModel, Integer, Integer) searchForUserStream} instead.
     */
    @Deprecated
    List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults);

    /**
     * Searches for user by parameter. If possible, implementations should treat the parameter values as partial match patterns
     * (i.e. in RDMBS terms use LIKE). Valid parameters are:
     * <ul>
     *   <li><b>first</b> - first name</li>
     *   <li><b>last</b> - last name</li>
     *   <li><b>email</b> - email</li>
     *   <li><b>username</b> - username</li>
     *   <li><b>enabled</b> - if user is enabled (true/false)</li>
     * </ul>
     * This method is used by the REST API when querying users.
     *
     * @param params a map containing the search parameters.
     * @param realm a reference to the realm.
     * @param firstResult first result to return. Ignored if negative.
     * @param maxResults maximum number of results to return. Ignored if negative.
     * @return a non-null {@link Stream} of users that match the search criteria.
     */
    default Stream<UserModel> searchForUserStream(Map<String, String> params, RealmModel realm, Integer firstResult, Integer maxResults) {
        List<UserModel> value = this.searchForUser(params, realm, firstResult == null ? -1 : firstResult, maxResults == null ? -1 : maxResults);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * Get users that belong to a specific group. Implementations do not have to search in UserFederatedStorageProvider
     * as this is done automatically.
     *
     * @see org.keycloak.storage.federated.UserFederatedStorageProvider
     *
     * @param realm
     * @param group
     * @return
     * @deprecated Use {@link #getGroupMembersStream(RealmModel, GroupModel) getGroupMembersStream} instead.
     */
    @Deprecated
    List<UserModel> getGroupMembers(RealmModel realm, GroupModel group);

    /**
     * Obtains users that belong to a specific group. Implementations do not have to search in {@code UserFederatedStorageProvider}
     * as this is done automatically.
     *
     * @see org.keycloak.storage.federated.UserFederatedStorageProvider
     *
     * @param realm a reference to the realm.
     * @param group a reference to the group.
     * @return a non-null {@link Stream} of users that belong to the group.
     */
    default Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group) {
        List<UserModel> value = this.getGroupMembers(realm, group);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * Get users that belong to a specific group.  Implementations do not have to search in UserFederatedStorageProvider
     * as this is done automatically.
     *
     * @see org.keycloak.storage.federated.UserFederatedStorageProvider
     *
     * @param realm
     * @param group
     * @param firstResult
     * @param maxResults
     * @return
     * @deprecated Use {@link #getGroupMembersStream(RealmModel, GroupModel, Integer, Integer) getGroupMembersStream} instead.
     */
    @Deprecated
    List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults);

    /**
     * Obtains users that belong to a specific group.  Implementations do not have to search in {@code UserFederatedStorageProvider}
     * as this is done automatically.
     *
     * @see org.keycloak.storage.federated.UserFederatedStorageProvider
     *
     * @param realm a reference to the realm.
     * @param group a reference to the group.
     * @param firstResult first result to return. Ignored if negative.
     * @param maxResults maximum number of results to return. Ignored if negative.
     * @return a non-null {@link Stream} of users that belong to the group.
     */
    default Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        List<UserModel> value = this.getGroupMembers(realm, group, firstResult == null ? -1 : firstResult, maxResults == null ? -1 : maxResults);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * Get users that belong to a specific role.
     *
     * @param realm
     * @param role
     * @return
     * @deprecated Use {@link #getRoleMembersStream(RealmModel, RoleModel) getRoleMembersStream} instead.
     */
    @Deprecated
    default List<UserModel> getRoleMembers(RealmModel realm, RoleModel role) {
        return this.getRoleMembersStream(realm, role).collect(Collectors.toList());
    }

    /**
     * Obtains users that have the specified role.
     *
     * @param realm a reference to the realm.
     * @param role a reference to the role.
     * @return a non-null {@link Stream} of users that have the specified role.
     */
    default Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role) {
        return getRoleMembersStream(realm, role, null, null);
    }

    /**
     * Search for users that have a specific role with a specific roleId.
     *
     * @param role
     * @param firstResult
     * @param maxResults
     * @return
     * @deprecated Use {@link #getRoleMembersStream(RealmModel, RoleModel, Integer, Integer) getRoleMembersStream} instead.
     */
    @Deprecated
    default List<UserModel> getRoleMembers(RealmModel realm, RoleModel role, int firstResult, int maxResults) {
        return this.getRoleMembersStream(realm, role, firstResult, maxResults).collect(Collectors.toList());
    }

    /**
     * Searches for users that have the specified role.
     *
     * @param realm a reference to the realm.
     * @param role a reference to the role.
     * @param firstResult first result to return. Ignored if negative.
     * @param maxResults maximum number of results to return. Ignored if negative.
     * @return a non-null {@link Stream} of users that have the specified role.
     */
    default Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    /**
     * Search for users that have a specific attribute with a specific value.
     * Implementations do not have to search in UserFederatedStorageProvider
     * as this is done automatically.
     *
     * @see org.keycloak.storage.federated.UserFederatedStorageProvider
     *
     * @param attrName
     * @param attrValue
     * @param realm
     * @return
     * @deprecated Use {@link #searchForUserByUserAttributeStream(String, String, RealmModel) searchForUserByUserAttributeStream}
     * instead.
     */
    @Deprecated
    List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm);

    /**
     * Searches for users that have a specific attribute with a specific value. Implementations do not have to search in
     * {@code UserFederatedStorageProvider} as this is done automatically.
     *
     * @see org.keycloak.storage.federated.UserFederatedStorageProvider
     *
     * @param attrName the attribute name.
     * @param attrValue the attribute value.
     * @param realm a reference to the realm.
     * @return a non-null {@link Stream} of users that match the search criteria.
     */
    default Stream<UserModel> searchForUserByUserAttributeStream(String attrName, String attrValue, RealmModel realm) {
        List<UserModel> value = this.searchForUserByUserAttribute(attrName, attrValue, realm);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * The {@link Streams} interface makes all collection-based methods in {@link UserQueryProvider} default by
     * providing implementations that delegate to the {@link Stream}-based variants instead of the other way around.
     * <p/>
     * It allows for implementations to focus on the {@link Stream}-based approach for processing sets of data and benefit
     * from the potential memory and performance optimizations of that approach.
     */
    interface Streams extends UserQueryProvider {
        @Override
        default List<UserModel> getUsers(RealmModel realm) {
            return this.getUsersStream(realm).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> getUsersStream(RealmModel realm);

        @Override
        default List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
            return this.getUsersStream(realm, firstResult, maxResults).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> getUsersStream(RealmModel realm, int firstResult, int maxResults);

        @Override
        default List<UserModel> searchForUser(String search, RealmModel realm) {
            return this.searchForUserStream(search, realm).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> searchForUserStream(String search, RealmModel realm);

        @Override
        default List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
            return this.searchForUserStream(search, realm, firstResult, maxResults).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> searchForUserStream(String search, RealmModel realm, Integer firstResult, Integer maxResults);

        @Override
        default List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
            return this.searchForUserStream(params, realm).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> searchForUserStream(Map<String, String> params, RealmModel realm);

        @Override
        default List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
            return this.searchForUserStream(params, realm, firstResult, maxResults).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> searchForUserStream(Map<String, String> params, RealmModel realm, Integer firstResult, Integer maxResults);

        @Override
        default List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
            return this.getGroupMembersStream(realm, group).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group);

        @Override
        default List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
            return this.getGroupMembersStream(realm, group, firstResult, maxResults).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults);

        @Override
        default List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
            return this.searchForUserByUserAttributeStream(attrName, attrValue, realm).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> searchForUserByUserAttributeStream(String attrName, String attrValue, RealmModel realm);
    }
}
