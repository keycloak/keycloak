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
import org.keycloak.models.search.SearchQueryJson;

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
        return countUsersInGroups(searchForUserStream(realm, Collections.emptyMap()), groupIds);
    }

    /**
     * Returns the number of users that would be returned by a call to {@link #searchForUserStream(RealmModel, String) searchForUserStream}
     *
     * @param realm  the realm
     * @param search case insensitive list of strings separated by whitespaces.
     * @return number of users that match the search
     */
    default int getUsersCount(RealmModel realm, String search) {
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
        return (int) searchForUserStream(realm, params).count();
    }

    /**
     * Returns the number of users that match the given filter parameters.
     *
     * @param realm  the realm
     * @param query search filter in json
     * @return number of users that match the given filters
     */
    default int getUsersCount(RealmModel realm, SearchQueryJson query) {
        return getUsersCount(query, realm);
    }
    /**
     * @deprecated Use {@link #getUsersCount(RealmModel, Set) getUsersCount} instead.
     */
    @Deprecated
    default int getUsersCount(SearchQueryJson query, RealmModel realm) {
        return (int) searchForUserStream(realm, query, null, null).count();
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
     * Searches all users in the realm.
     *
     * @param realm a reference to the realm.
     * @return a non-null {@link Stream} of users.
     * @deprecated Use {@link #searchForUserStream(RealmModel, Map)} with an empty params map instead.
     */
    @Deprecated
    default Stream<UserModel> getUsersStream(RealmModel realm) {
        return searchForUserStream(realm, Collections.emptyMap());
    }

    /**
     * Searches all users in the realm, starting from the {@code firstResult} and containing at most {@code maxResults}.
     *
     * @param realm a reference to the realm.
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users.
     * @deprecated Use {@link #searchForUserStream(RealmModel, Map, Integer, Integer)} with an empty params map instead.
     */
    @Deprecated
    default Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        return searchForUserStream(realm, Collections.emptyMap(), firstResult, maxResults);
    }

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
        return searchForUserStream(realm, search, null, null);
    }

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
    Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults);

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
        return searchForUserStream(realm, params, null, null);
    }

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
    Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults);

    /**
     * Obtains users that belong to a specific group.
     *
     * @param realm a reference to the realm.
     * @param group a reference to the group.
     * @return a non-null {@link Stream} of users that belong to the group.
     */
    default Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group) {
        return getGroupMembersStream(realm, group, null, null);
    }

    /**
     * Obtains users that belong to a specific group.
     *
     * @param realm a reference to the realm.
     * @param group a reference to the group.
     * @param firstResult first result to return. Ignored if negative, zero, or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users that belong to the group.
     */
    Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults);

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
     * Searches for users that have the specified role.
     *
     * @param realm a reference to the realm.
     * @param role a reference to the role.
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users that have the specified role.
     */
    default Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    /**
     * Searches for users that have a specific attribute with a specific value.
     *
     * @param realm a reference to the realm.
     * @param attrName the attribute name.
     * @param attrValue the attribute value.
     * @return a non-null {@link Stream} of users that match the search criteria.
     */
    Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue);

    /**
     * Searches for user by json query. 
     *
     * This method is used by the REST API when querying users.
     *
     * @param realm a reference to the realm.
     * @param query a query containing the search parameters.
     * @return a non-null {@link Stream} of users that match the search criteria.
     */
    default Stream<UserModel> searchForUserStream(RealmModel realm, SearchQueryJson query) {
        return searchForUserStream(realm, query, null, null);
    }

    /**
     * Searches for user by json query. 
     *
     * This method is used by the REST API when querying users.
     *
     * @param realm a reference to the realm.
     * @param query a query containing the search parameters.
     * @param firstResult first result to return. Ignored if negative, zero, or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users that match the search criteria.
     */
    Stream<UserModel> searchForUserStream(RealmModel realm, SearchQueryJson query, Integer firstResult, Integer maxResults);

    /**
     * The {@link Streams} interface makes all collection-based methods in {@link UserQueryProvider} default by
     * providing implementations that delegate to the {@link Stream}-based variants instead of the other way around.
     * <p/>
     * It allows for implementations to focus on the {@link Stream}-based approach for processing sets of data and benefit
     * from the potential memory and performance optimizations of that approach.
     * @deprecated This interface is no longer necessary, collection-based methods were removed from the parent interface
     * and therefore the parent interface can be used directly
     */
    @Deprecated
    interface Streams extends UserQueryProvider {
    }
}
