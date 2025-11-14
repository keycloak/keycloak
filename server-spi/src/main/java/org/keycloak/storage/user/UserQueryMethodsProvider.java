/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

/**
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
public interface UserQueryMethodsProvider {

    /**
     * Searches for users whose username, email, first name or last name contain any of the strings in {@code search} separated by whitespace.
     * <p/>
     * If possible, implementations should treat the parameter values as partial match patterns (i.e. in RDMBS terms use LIKE).
     * <p/>
     * This method is used by the admin console search box
     *
     * @param realm  a reference to the realm.
     * @param search case insensitive list of string separated by whitespaces.
     * @return a non-null {@link Stream} of users that match the search string.
     * @deprecated Use {@link #searchForUserStream(RealmModel, Map)} with an {@code params} map containing {@link UserModel#SEARCH} instead.
     */
    @Deprecated
    default Stream<UserModel> searchForUserStream(RealmModel realm, String search) {
        return searchForUserStream(realm, Map.of(UserModel.SEARCH, search), null, null);
    }

    /**
     * Searches for users whose username, email, first name or last name contain any of the strings in {@code search} separated by whitespace.
     * <p/>
     * If possible, implementations should treat the parameter values as partial match patterns (i.e. in RDMBS terms use LIKE).
     * <p/>
     * This method is used by the admin console search box
     *
     * @param realm       a reference to the realm.
     * @param search      case insensitive list of string separated by whitespaces.
     * @param firstResult first result to return. Ignored if negative, zero, or {@code null}.
     * @param maxResults  maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users that match the search criteria.
     * @deprecated Use {@link #searchForUserStream(RealmModel, Map, Integer, Integer)} with an {@code params} map containing {@link UserModel#SEARCH} instead.
     */
    @Deprecated
    default Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        return searchForUserStream(realm, Map.of(UserModel.SEARCH, search), firstResult, maxResults);
    }

    /**
     * Searches for user by parameter.
     * If possible, implementations should treat the parameter values as partial match patterns (i.e. in RDMBS terms use LIKE).
     * <p/>
     * Valid parameters are:
     * <ul>
     *     <li>{@link UserModel#SEARCH} - search for users whose username, email, first name or last name contain any of the strings in {@code search} separated by whitespace, when {@code SEARCH} is set all other params are ignored</li>
     *     <li>{@link UserModel#FIRST_NAME} - first name (case insensitive string)</li>
     *     <li>{@link UserModel#LAST_NAME} - last name (case insensitive string)</li>
     *     <li>{@link UserModel#EMAIL} - email (case insensitive string)</li>
     *     <li>{@link UserModel#USERNAME} - username (case insensitive string)</li>
     *     <li>{@link UserModel#EXACT} - whether search with FIRST_NAME, LAST_NAME, USERNAME or EMAIL should be exact match</li>
     *     <li>{@link UserModel#EMAIL_VERIFIED} - search only for users with verified/non-verified email (true/false)</li>
     *     <li>{@link UserModel#ENABLED} - search only for enabled/disabled users (true/false)</li>
     *     <li>{@link UserModel#IDP_ALIAS} - search only for users that have a federated identity
     *     from idp with the given alias configured (case sensitive string)</li>
     *     <li>{@link UserModel#IDP_USER_ID} - search for users with federated identity with
     *     the given userId (case sensitive string)</li>
     * </ul>
     * <p>
     * Any other parameters will be treated as custom user attributes.
     * <p>
     * This method is used by the REST API when querying users.
     *
     * @param realm  a reference to the realm.
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
     *     <li>{@link UserModel#SEARCH} - search for users whose username, email, first name or last name contain any of the strings in {@code search} separated by whitespace, when {@code SEARCH} is set all other params are ignored</li>
     *     <li>{@link UserModel#FIRST_NAME} - first name (case insensitive string)</li>
     *     <li>{@link UserModel#LAST_NAME} - last name (case insensitive string)</li>
     *     <li>{@link UserModel#EMAIL} - email (case insensitive string)</li>
     *     <li>{@link UserModel#USERNAME} - username (case insensitive string)</li>
     *     <li>{@link UserModel#EXACT} - whether search with FIRST_NAME, LAST_NAME, USERNAME or EMAIL should be exact match</li>
     *     <li>{@link UserModel#EMAIL_VERIFIED} - search only for users with verified/non-verified email (true/false)</li>
     *     <li>{@link UserModel#ENABLED} - search only for enabled/disabled users (true/false)</li>
     *     <li>{@link UserModel#IDP_ALIAS} - search only for users that have a federated identity
     *     from idp with the given alias configured (case sensitive string)</li>
     *     <li>{@link UserModel#IDP_USER_ID} - search for users with federated identity with
     *     the given userId (case sensitive string)</li>
     * </ul>
     * <p>
     * Any other parameters will be treated as custom user attributes.
     * <p>
     * This method is used by the REST API when querying users.
     *
     * @param realm       a reference to the realm.
     * @param params      a map containing the search parameters.
     * @param firstResult first result to return. Ignored if negative, zero, or {@code null}.
     * @param maxResults  maximum number of results to return. Ignored if negative or {@code null}.
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
     * @param realm       a reference to the realm.
     * @param group       a reference to the group.
     * @param firstResult first result to return. Ignored if negative, zero, or {@code null}.
     * @param maxResults  maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users that belong to the group.
     */
    Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults);

    /**
     * Obtains users that belong to a specific group, filtered according to the search parameters.
     *
     * @param realm     a reference to the realm.
     * @param group     a reference to the group.
     * @param search    the search string. It can represent either the user's username, e-mail, first name, or last name.
     * @param exact     a boolean indicating if the search should be exact or not. If {@code true}, it selects only users
     *                  whose main attributes (username, e-mail, first name, or last name) exactly match the search string.
     *                  If {@code false}, it selects the users whose main attributes partially match the search string.
     * @param first     the position of the first result to be processed (pagination offset). Ignored if negative or {@code null}.
     * @param max       the maximum number of results to be returned. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of filtered users that belong to the group.
     */
    default Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, String search, Boolean exact,
                                                    Integer first, Integer max) {

        Stream<UserModel> groupMembers = getGroupMembersStream(realm, group).filter(user -> {
            if (StringUtil.isBlank(search)) return true;
            if (Boolean.TRUE.equals(exact)) {
                return search.equals(user.getUsername()) || search.equals(user.getEmail())
                        || search.equals(user.getFirstName()) || search.equals(user.getLastName());
            } else {
                return Optional.ofNullable(user.getUsername()).orElse("").toLowerCase().contains(search.toLowerCase()) ||
                        Optional.ofNullable(user.getEmail()).orElse("").toLowerCase().contains(search.toLowerCase()) ||
                        Optional.ofNullable(user.getFirstName()).orElse("").toLowerCase().contains(search.toLowerCase()) ||
                        Optional.ofNullable(user.getLastName()).orElse("").toLowerCase().contains(search.toLowerCase());
            }
        });

        // Copied over from StreamsUtil from server-spi-private which is not available here
        if (first != null && first > 0) {
            groupMembers = groupMembers.skip(first);
        }
        if (max != null && max >= 0) {
            groupMembers = groupMembers.limit(max);
        }
        return groupMembers;
    }

    /**
     * Obtains users that have the specified role.
     *
     * @param realm a reference to the realm.
     * @param role  a reference to the role.
     * @return a non-null {@link Stream} of users that have the specified role.
     */
    default Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role) {
        return getRoleMembersStream(realm, role, null, null);
    }

    /**
     * Searches for users that have the specified role.
     *
     * @param realm       a reference to the realm.
     * @param role        a reference to the role.
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults  maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users that have the specified role.
     */
    default Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    /**
     * Searches for users that have a specific attribute with a specific value.
     *
     * @param realm     a reference to the realm.
     * @param attrName  the attribute name.
     * @param attrValue the attribute value.
     * @return a non-null {@link Stream} of users that match the search criteria.
     */
    Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue);
}
