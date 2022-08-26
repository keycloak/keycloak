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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * This is an optional capability interface that is intended to be implemented by any
 * <code>UserStorageProvider</code> that supports complex user querying. You must
 * implement this interface if you want to view and manage users from the administration console.
 * <p/>
 * Note that all methods in this interface should limit search only to data available within the storage that is
 * represented by this provider. They should not lookup other storage providers for additional information.
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
    default int getUsersCount(RealmModel realm) {
        return getUsersCount(realm, false);
    }

    /**
     * Returns the number of users that are in at least one of the groups
     * given.
     *
     * @param realm    the realm
     * @param groupIds set of groups IDs, the returned user needs to belong to at least one of them
     * @return the number of users that are in at least one of the groups
     */
    default int getUsersCount(RealmModel realm, Set<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return 0;
        }
        return countUsersInGroups(getUsersStream(realm), groupIds);
    }

    /**
     * Returns the number of users that would be returned by a call to {@link #searchForUserStream(RealmModel, String) searchForUserStream}
     *
     * @param realm  the realm
     * @param search case insensitive list of strings separated by whitespaces.
     * @return number of users that match the search
     */
    default int getUsersCount(RealmModel realm, String search) {
        return getUsersCount(search, realm);
    }

    /**
     * @deprecated Use {@link #getUsersCount(RealmModel, String) getUsersCount}
     */
    @Deprecated
    default int getUsersCount(String search, RealmModel realm) {
        return (int) searchForUserStream(realm, search).count();
    }

    /**
     * Returns the number of users that would be returned by a call to {@link #searchForUserStream(RealmModel, String) searchForUserStream}
     * and are members of at least one of the groups given by the {@code groupIds} set.
     *
     * @param realm    the realm
     * @param search case insensitive list of strings separated by whitespaces.
     * @param groupIds set of groups IDs, the returned user needs to belong to at least one of them
     * @return number of users that match the search and given groups
     */
    default int getUsersCount(RealmModel realm, String search, Set<String> groupIds) {
        return getUsersCount(search, realm, groupIds);
    }

    /**
     * @deprecated Use {@link #getUsersCount(RealmModel, String, Set) getUsersCount} instead.
     */
    @Deprecated
    default int getUsersCount(String search, RealmModel realm, Set<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return 0;
        }
        return countUsersInGroups(searchForUserStream(realm, search), groupIds);
    }

    /**
     * Returns the number of users that match the given filter parameters.
     *
     * @param realm  the realm
     * @param params filter parameters
     * @return number of users that match the given filters
     */
    default int getUsersCount(RealmModel realm, Map<String, String> params) {
        return getUsersCount(params, realm);
    }
    /**
     * @deprecated Use {@link #getUsersCount(RealmModel, Set) getUsersCount} instead.
     */
    @Deprecated
    default int getUsersCount(Map<String, String> params, RealmModel realm) {
        return (int) searchForUserStream(realm, params).count();
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
    default int getUsersCount(RealmModel realm, Map<String, String> params, Set<String> groupIds) {
        return getUsersCount(params, realm, groupIds);
    }
    /**
     * @deprecated Use {@link #getUsersCount(RealmModel, Map, Set) getUsersCount} instead.
     */
    @Deprecated
    default int getUsersCount(Map<String, String> params, RealmModel realm, Set<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return 0;
        }
        return countUsersInGroups(searchForUserStream(realm, params), groupIds);
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
     * @deprecated Use {@link #getUsersStream(RealmModel, Integer, Integer) getUsersStream} instead.
     */
    @Deprecated
    List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults);

    /**
     * Searches all users in the realm, starting from the {@code firstResult} and containing at most {@code maxResults}.
     *
     * @param realm a reference to the realm.
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users.
     */
    default Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        List<UserModel> value = this.getUsers(realm, firstResult == null ? -1 : firstResult,
                maxResults == null ? -1 : maxResults);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * Searches for users whose username, email, first name or last name contain any of the strings in {@code search} separated by whitespace.
     * <p/>
     * If possible, implementations should treat the parameter values as partial match patterns i.e. in RDMBS terms use LIKE.
     * <p/>
     * This method is used by the admin console search box
     *
     * @param search case insensitive list of string separated by whitespaces.
     * @param realm realm to search within
     * @return list of users that satisfies the given search condition
     *
     * @deprecated Use {@link #searchForUserStream(RealmModel, String) searchForUserStream} instead.
     */
    @Deprecated
    List<UserModel> searchForUser(String search, RealmModel realm);

    /**
     * Searches for users whose username, email, first name or last name contain any of the strings in {@code search} separated by whitespace.
     * <p/>
     * If possible, implementations should treat the parameter values as partial match patterns (i.e. in RDMBS terms use LIKE).
     * <p/>
     * This method is used by the admin console search box
     *
     * @param realm a reference to the realm.
     * @param search case insensitive list of string separated by whitespaces.
     * @return a non-null {@link Stream} of users that match the search string.
     */
    default Stream<UserModel> searchForUserStream(RealmModel realm, String search) {
        List<UserModel> value = this.searchForUser(search, realm);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * Searches for users whose username, email, first name or last name contain any of the strings in {@code search} separated by whitespace.
     * The resulting user list should be paginated with respect to parameters {@code firstResult} and {@code maxResults}
     * <p/>
     * If possible, implementations should treat the parameter values as partial match patterns i.e. in RDMBS terms use LIKE.
     * <p/>
     * This method is used by the admin console search box
     *
     * @param search case insensitive list of string separated by whitespaces.
     * @param realm a reference to the realm
     * @param firstResult first result to return. Ignored if negative or zero.
     * @param maxResults maximum number of results to return. Ignored if negative.
     * @return paginated list of users from the realm that satisfies given search
     *
     * @deprecated Use {@link #searchForUserStream(RealmModel, String, Integer, Integer) searchForUserStream} instead.
     */
    @Deprecated
    List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults);

    /**
     * Searches for users whose username, email, first name or last name contain any of the strings in {@code search} separated by whitespace.
     * <p/>
     * If possible, implementations should treat the parameter values as partial match patterns (i.e. in RDMBS terms use LIKE).
     * <p/>
     * This method is used by the admin console search box
     *
     * @param realm a reference to the realm.
     * @param search case insensitive list of string separated by whitespaces.
     * @param firstResult first result to return. Ignored if negative, zero, or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users that match the search criteria.
     */
    default Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        List<UserModel> value = this.searchForUser(search, realm, firstResult == null ? -1 : firstResult, maxResults == null ? -1 : maxResults);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * Search for user by a map of parameters.
     * <p/>
     * Valid parameters are:
     * <ul>
     *     <li>{@link UserModel#FIRST_NAME} - first name (case insensitive string)</li>
     *     <li>{@link UserModel#LAST_NAME} - last name (case insensitive string)</li>
     *     <li>{@link UserModel#EMAIL} - email (case insensitive string)</li>
     *     <li>{@link UserModel#USERNAME} - username (case insensitive string)</li>
     *     <li>{@link UserModel#EMAIL_VERIFIED} - search only for users with verified/non-verified email (true/false)</li>
     *     <li>{@link UserModel#ENABLED} - search only for enabled/disabled users (true/false)</li>
     *     <li>{@link UserModel#IDP_ALIAS} - search only for users that have a federated identity
     *     from idp with the given alias configured (case sensitive string)</li>
     *     <li>{@link UserModel#IDP_USER_ID} - search for users with federated identity with
     *     the given userId (case sensitive string)</li>
     * </ul>
     *
     * If possible, implementations should treat the parameter values as partial match patterns i.e. in RDMBS terms use LIKE.
     * <p/>
     * This method is used by the REST API when querying users.
     *
     * @param params a map containing the search parameters
     * @param realm a reference to the realm
     * @return list of users that satisfies given search conditions
     *
     * @deprecated Use {@link #searchForUserStream(RealmModel, Map) searchForUserStream} instead.
     */
    @Deprecated
    List<UserModel> searchForUser(Map<String, String> params, RealmModel realm);

    /**
     * Searches for user by parameter.
     * If possible, implementations should treat the parameter values as partial match patterns (i.e. in RDMBS terms use LIKE).
     * <p/>
     * Valid parameters are:
     * <ul>
     *     <li>{@link UserModel#FIRST_NAME} - first name (case insensitive string)</li>
     *     <li>{@link UserModel#LAST_NAME} - last name (case insensitive string)</li>
     *     <li>{@link UserModel#EMAIL} - email (case insensitive string)</li>
     *     <li>{@link UserModel#USERNAME} - username (case insensitive string)</li>
     *     <li>{@link UserModel#EMAIL_VERIFIED} - search only for users with verified/non-verified email (true/false)</li>
     *     <li>{@link UserModel#ENABLED} - search only for enabled/disabled users (true/false)</li>
     *     <li>{@link UserModel#IDP_ALIAS} - search only for users that have a federated identity
     *     from idp with the given alias configured (case sensitive string)</li>
     *     <li>{@link UserModel#IDP_USER_ID} - search for users with federated identity with
     *     the given userId (case sensitive string)</li>
     * </ul>
     *
     * This method is used by the REST API when querying users.
     *
     * @param realm a reference to the realm.
     * @param params a map containing the search parameters.
     * @return a non-null {@link Stream} of users that match the search parameters.
     */
    default Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params) {
        List<UserModel> value = this.searchForUser(params, realm);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * Search for user by parameter.
     * <p/>
     * Valid parameters are:
     * <ul>
     *     <li>{@link UserModel#FIRST_NAME} - first name (case insensitive string)</li>
     *     <li>{@link UserModel#LAST_NAME} - last name (case insensitive string)</li>
     *     <li>{@link UserModel#EMAIL} - email (case insensitive string)</li>
     *     <li>{@link UserModel#USERNAME} - username (case insensitive string)</li>
     *     <li>{@link UserModel#EMAIL_VERIFIED} - search only for users with verified/non-verified email (true/false)</li>
     *     <li>{@link UserModel#ENABLED} - search only for enabled/disabled users (true/false)</li>
     *     <li>{@link UserModel#IDP_ALIAS} - search only for users that have a federated identity
     *     from idp with the given alias configured (case sensitive string)</li>
     *     <li>{@link UserModel#IDP_USER_ID} - search for users with federated identity with
     *     the given userId (case sensitive string)</li>
     * </ul>
     *
     * If possible, implementations should treat the parameter values as patterns i.e. in RDMBS terms use LIKE.
     * <p/>
     * This method is used by the REST API when querying users.
     *
     * @param params a map containing the search parameters.
     * @param realm a reference to the realm.
     * @param firstResult first result to return. Ignored if negative.
     * @param maxResults maximum number of results to return. Ignored if negative.
     * @return a non-null {@link Stream} of users that match the search criteria.
     *
     * @deprecated Use {@link #searchForUserStream(RealmModel, Map, Integer, Integer) searchForUserStream} instead.
     */
    @Deprecated
    List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults);

    /**
     * Searches for user by parameter. If possible, implementations should treat the parameter values as partial match patterns
     * (i.e. in RDMBS terms use LIKE).
     * <p/>
     * Valid parameters are:
     * <ul>
     *     <li>{@link UserModel#FIRST_NAME} - first name (case insensitive string)</li>
     *     <li>{@link UserModel#LAST_NAME} - last name (case insensitive string)</li>
     *     <li>{@link UserModel#EMAIL} - email (case insensitive string)</li>
     *     <li>{@link UserModel#USERNAME} - username (case insensitive string)</li>
     *     <li>{@link UserModel#EMAIL_VERIFIED} - search only for users with verified/non-verified email (true/false)</li>
     *     <li>{@link UserModel#ENABLED} - search only for enabled/disabled users (true/false)</li>
     *     <li>{@link UserModel#IDP_ALIAS} - search only for users that have a federated identity
     *     from idp with the given alias configured (case sensitive string)</li>
     *     <li>{@link UserModel#IDP_USER_ID} - search for users with federated identity with
     *     the given userId (case sensitive string)</li>
     * </ul>
     *
     * Any other parameters will be treated as custom user attributes.
     *
     * This method is used by the REST API when querying users.
     *
     * @param realm a reference to the realm.
     * @param params a map containing the search parameters.
     * @param firstResult first result to return. Ignored if negative, zero, or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users that match the search criteria.
     */
    default Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        List<UserModel> value = this.searchForUser(params, realm, firstResult == null ? -1 : firstResult, maxResults == null ? -1 : maxResults);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * Get users that belong to a specific group.
     *
     * @param realm a reference to the realm
     * @param group a reference to the group
     * @return a list of all users that are members of the given group
     *
     * @deprecated Use {@link #getGroupMembersStream(RealmModel, GroupModel) getGroupMembersStream} instead.
     */
    @Deprecated
    List<UserModel> getGroupMembers(RealmModel realm, GroupModel group);

    /**
     * Obtains users that belong to a specific group.
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
     * Gets paginated list of users that belong to a specific group.
     *
     * @param realm a reference to the realm
     * @param group a reference to the group
     * @param firstResult first result to return. Ignored if negative or zero.
     * @param maxResults maximum number of results to return. Ignored if negative.
     * @return paginated list of members of the given group
     *
     * @deprecated Use {@link #getGroupMembersStream(RealmModel, GroupModel, Integer, Integer) getGroupMembersStream} instead.
     */
    @Deprecated
    List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults);

    /**
     * Obtains users that belong to a specific group.
     *
     * @param realm a reference to the realm.
     * @param group a reference to the group.
     * @param firstResult first result to return. Ignored if negative, zero, or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users that belong to the group.
     */
    default Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        List<UserModel> value = this.getGroupMembers(realm, group, firstResult == null ? -1 : firstResult, maxResults == null ? -1 : maxResults);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * Get users that belong to a specific role.
     *
     * @param realm a reference to the realm
     * @param role a reference to the role
     * @return a list of users that has the given role assigned
     *
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
     * @param realm a reference to the realm
     * @param role a reference to the role
     * @param firstResult first result to return. Ignored if negative or zero.
     * @param maxResults maximum number of results to return. Ignored if negative.
     * @return a paginated list of users that has the given role assigned
     *
     * @deprecated Use {@link #getRoleMembersStream(RealmModel, RoleModel, Integer, Integer) getRoleMembersStream} instead.
     */
    @Deprecated
    default List<UserModel> getRoleMembers(RealmModel realm, RoleModel role, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    /**
     * Searches for users that have the specified role.
     *
     * @param realm a reference to the realm.
     * @param role a reference to the role.
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users that have the specified role.
     */
    default Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        return getRoleMembers(realm, role, firstResult == null ? -1 : firstResult, maxResults== null ? -1 : maxResults)
                .stream();
    }

    /**
     * Search for users that have a specific attribute with a specific value.
     *
     * @param attrName a name of the attribute that will be searched
     * @param attrValue a value of the attribute that will be searched
     * @param realm a reference to the realm
     * @return list of users with the given attribute name and value
     *
     * @deprecated Use {@link #searchForUserByUserAttributeStream(RealmModel, String, String) searchForUserByUserAttributeStream}
     * instead.
     */
    @Deprecated
    List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm);

    /**
     * Searches for users that have a specific attribute with a specific value.
     *
     * @param realm a reference to the realm.
     * @param attrName the attribute name.
     * @param attrValue the attribute value.
     * @return a non-null {@link Stream} of users that match the search criteria.
     */
    default Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
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
        default int getUsersCount(RealmModel realm, String search) {
            return (int) searchForUserStream(realm, search).count();
        }

        @Override
        default int getUsersCount(String search, RealmModel realm) {
            return getUsersCount(realm, search);
        }

        @Override
        default int getUsersCount(RealmModel realm, String search, Set<String> groupIds) {
            if (groupIds == null || groupIds.isEmpty()) {
                return 0;
            }
            return countUsersInGroups(searchForUserStream(realm, search), groupIds);
        }

        @Override
        default int getUsersCount(String search, RealmModel realm, Set<String> groupIds) {
            return getUsersCount(realm, search, groupIds);
        }

        @Override
        default int getUsersCount(RealmModel realm, Map<String, String> params) {
            return (int) searchForUserStream(realm, params).count();
        }

        @Override
        default int getUsersCount( Map<String, String> params, RealmModel realm) {
            return getUsersCount(realm, params);
        }

        @Override
        default int getUsersCount(RealmModel realm, Map<String, String> params, Set<String> groupIds) {
            if (groupIds == null || groupIds.isEmpty()) {
                return 0;
            }
            return countUsersInGroups(searchForUserStream(realm, params), groupIds);
        }

        @Override
        default int getUsersCount(Map<String, String> params, RealmModel realm, Set<String> groupIds) {
            return getUsersCount(realm, params, groupIds);
        }

        @Override
        default List<UserModel> getUsers(RealmModel realm) {
            return this.getUsersStream(realm).collect(Collectors.toList());
        }

        @Override
        default Stream<UserModel> getUsersStream(RealmModel realm) {
            return getUsersStream(realm, null, null);
        }

        @Override
        default List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
            return this.getUsersStream(realm, firstResult, maxResults).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults);

        @Override
        default List<UserModel> searchForUser(String search, RealmModel realm) {
            return this.searchForUserStream(realm, search).collect(Collectors.toList());
        }

        @Override
        default Stream<UserModel> searchForUserStream(RealmModel realm, String search) {
            return searchForUserStream(realm, search, null, null);
        }

        @Override
        default List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
            return this.searchForUserStream(realm, search, firstResult, maxResults).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults);

        @Override
        default List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
            return this.searchForUserStream(realm, params).collect(Collectors.toList());
        }

        @Override
        default Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params) {
            return searchForUserStream(realm, params, null, null);
        }

        @Override
        default List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
            return this.searchForUserStream(realm, params, firstResult, maxResults).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults);

        @Override
        default List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
            return this.getGroupMembersStream(realm, group).collect(Collectors.toList());
        }

        @Override
        default Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group) {
            return this.getGroupMembersStream(realm, group, null, null);
        }

        @Override
        default List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
            return this.getGroupMembersStream(realm, group, firstResult, maxResults).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults);

        @Override
        default List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
            return this.searchForUserByUserAttributeStream(realm, attrName, attrValue).collect(Collectors.toList());
        }

        @Override
        Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue);
    }
}
