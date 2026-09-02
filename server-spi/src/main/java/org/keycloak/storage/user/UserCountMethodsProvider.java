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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * This is an optional capability interface that is intended to be implemented by 
 * <code>UserStorageProvider</code> that supports count queries. 
 *
 * By implementing this interface, count queries could be leveraged when querying users with some offset ({@code firstResult}).
 * 
 * <p/>
 * Note that all methods in this interface should limit search only to data available within the storage that is
 * represented by this provider. They should not lookup other storage providers for additional information.
 */
public interface UserCountMethodsProvider {
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
        if (groupIds == null || groupIds.isEmpty() || !(this instanceof UserQueryMethodsProvider)) {
            return 0;
        }
        return countUsersInGroups(((UserQueryMethodsProvider)this).searchForUserStream(realm, Collections.emptyMap()), groupIds);
    }

    /**
     * Returns the number of users that would be returned by a call to {@link #searchForUserStream(RealmModel, String) searchForUserStream}
     *
     * @param realm  the realm
     * @param search case insensitive list of strings separated by whitespaces.
     * @return number of users that match the search
     * @deprecated Use {@link #getUsersCount(RealmModel, Map)} with an {@code params} map containing {@link UserModel#SEARCH} instead.
     */
    @Deprecated
    default int getUsersCount(RealmModel realm, String search) {
        if (!(this instanceof UserQueryMethodsProvider)) {
            return 0;
        }

        return (int) ((UserQueryMethodsProvider)this).searchForUserStream(realm, search).count();
    }

    /**
     * Returns the number of users that would be returned by a call to {@link #searchForUserStream(RealmModel, String) searchForUserStream}
     * and are members of at least one of the groups given by the {@code groupIds} set.
     *
     * @param realm    the realm
     * @param search   case insensitive list of strings separated by whitespaces.
     * @param groupIds set of groups IDs, the returned user needs to belong to at least one of them
     * @return number of users that match the search and given groups
     * @deprecated Use {@link #getUsersCount(RealmModel, Map, Set)} with an {@code params} map containing {@link UserModel#SEARCH} instead.
     */
    @Deprecated
    default int getUsersCount(RealmModel realm, String search, Set<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty() || !(this instanceof UserQueryMethodsProvider)) {
            return 0;
        }
        return countUsersInGroups(((UserQueryMethodsProvider)this).searchForUserStream(realm, search), groupIds);
    }

    /**
     * Returns the number of users that match the given filter parameters.
     *
     * @param realm  the realm
     * @param params filter parameters
     * @return number of users that match the given filters
     */
    default int getUsersCount(RealmModel realm, Map<String, String> params) {
        if (!(this instanceof UserQueryMethodsProvider)) {
            return 0;
        }
        return (int) ((UserQueryMethodsProvider)this).searchForUserStream(realm, params).count();
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
        if (groupIds == null || groupIds.isEmpty() || !(this instanceof UserQueryMethodsProvider)) {
            return 0;
        }
        return countUsersInGroups(((UserQueryMethodsProvider)this).searchForUserStream(realm, params), groupIds);
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
     * @param realm                 the realm
     * @param includeServiceAccount if true, the number of users will also include service accounts. Otherwise, only the number of users.
     * @return the number of users
     */
    default int getUsersCount(RealmModel realm, boolean includeServiceAccount) {
        throw new RuntimeException("Not implemented");
    }
}
