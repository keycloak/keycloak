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

        return countUsersInGroups(getUsers(realm), groupIds);
    }

    /**
     * Returns the number of users that match the given criteria.
     *
     * @param search search criteria
     * @param realm  the realm
     * @return number of users that match the search
     */
    default int getUsersCount(String search, RealmModel realm) {
        return searchForUser(search, realm).size();
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

        List<UserModel> users = searchForUser(search, realm);
        return countUsersInGroups(users, groupIds);
    }

    /**
     * Returns the number of users that match the given filter parameters.
     *
     * @param params filter parameters
     * @param realm  the realm
     * @return number of users that match the given filters
     */
    default int getUsersCount(Map<String, String> params, RealmModel realm) {
        return searchForUser(params, realm).size();
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

        List<UserModel> users = searchForUser(params, realm);
        return countUsersInGroups(users, groupIds);
    }

    /**
     * Returns the number of users from the given list of users that are in at
     * least one of the groups given in the groups set.
     *
     * @param users    list of users to check
     * @param groupIds id of groups that should be checked for
     * @return number of users that are in at least one of the groups
     */
    static int countUsersInGroups(List<UserModel> users, Set<String> groupIds) {
        return (int) users.stream().filter(u -> {
            for (GroupModel group : u.getGroups()) {
                if (groupIds.contains(group.getId())) {
                    return true;
                }
            }
            return false;
        }).count();
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

    List<UserModel> getUsers(RealmModel realm);
    List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults);

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
     */
    List<UserModel> searchForUser(String search, RealmModel realm);

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
     */
    List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults);

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
     */
    List<UserModel> searchForUser(Map<String, String> params, RealmModel realm);

    /**
     * Search for user by parameter.    Valid parameters are:
     * "first" - first name
     * "last" - last name
     * "email" - email
     * "username" - username
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
     */
    List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults);

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
     */
    List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults);

    /**
     * Get users that belong to a specific role.
     *
     *
     *
     * @param realm
     * @param role
     * @return
     */
    default List<UserModel> getRoleMembers(RealmModel realm, RoleModel role)
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * Search for users that have a specific role with a specific roleId.
     *
     *
     *
     * @param firstResult
     * @param maxResults
     * @param role
     * @return
     */
    default List<UserModel> getRoleMembers(RealmModel realm, RoleModel role, int firstResult, int maxResults)
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * Get users that belong to a specific group.  Implementations do not have to search in UserFederatedStorageProvider
     * as this is done automatically.
     *
     * @see org.keycloak.storage.federated.UserFederatedStorageProvider
     *
     *
     *
     * @param realm
     * @param group
     * @return
     */
    List<UserModel> getGroupMembers(RealmModel realm, GroupModel group);

    /**
     * Search for users that have a specific attribute with a specific value.
     * Implementations do not have to search in UserFederatedStorageProvider
     * as this is done automatically.
     *
     * @see org.keycloak.storage.federated.UserFederatedStorageProvider
     *

     *
     * @param attrName
     * @param attrValue
     * @param realm
     * @return
     */
    List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm);
}
