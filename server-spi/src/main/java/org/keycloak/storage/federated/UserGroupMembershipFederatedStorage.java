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
package org.keycloak.storage.federated;

import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserGroupMembershipFederatedStorage {

    /**
     * @deprecated Use {@link #getGroupsStream(RealmModel, String) getGroupsStream} instead.
     */
    @Deprecated
    Set<GroupModel> getGroups(RealmModel realm, String userId);

    /**
     * Obtains the groups associated with the federated user.
     *
     * @param realm a reference to the realm.
     * @param userId the user identifier.
     * @return a non-null {@code Stream} of groups.
     */
    default Stream<GroupModel> getGroupsStream(RealmModel realm, String userId) {
        Set<GroupModel> value = this.getGroups(realm, userId);
        return value != null ? value.stream() : Stream.empty();
    }

    void joinGroup(RealmModel realm, String userId, GroupModel group);
    void leaveGroup(RealmModel realm, String userId, GroupModel group);

    /**
     * @deprecated Use {@link #getMembershipStream(RealmModel, GroupModel, Integer, Integer) getMembershipStream} instead.
     */
    @Deprecated
    List<String> getMembership(RealmModel realm, GroupModel group, int firstResult, int max);

    /**
     * Obtains the federated users that are members of the given {@code group} in the specified {@code realm}.
     *
     * @param realm a reference to the realm.
     * @param group a reference to the group whose federated members are being searched.
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param max maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@code Stream} of federated user ids that are members of the group in the realm.
     */
    default Stream<String> getMembershipStream(RealmModel realm, GroupModel group, Integer firstResult, Integer max) {
        List<String> value = this.getMembership(realm, group, firstResult, max);
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * The {@link Streams} interface makes all collection-based methods in {@link UserGroupMembershipFederatedStorage}
     * default by providing implementations that delegate to the {@link Stream}-based variants instead of the other way
     * around.
     * <p/>
     * It allows for implementations to focus on the {@link Stream}-based approach for processing sets of data and benefit
     * from the potential memory and performance optimizations of that approach.
     */
    interface Streams extends UserGroupMembershipFederatedStorage {
        @Override
        default Set<GroupModel> getGroups(RealmModel realm, String userId) {
            return getGroupsStream(realm, userId).collect(Collectors.toSet());
        }

        @Override
        Stream<GroupModel> getGroupsStream(RealmModel realm, String userId);

        @Override
        default List<String> getMembership(RealmModel realm, GroupModel group, int firstResult, int max) {
            return this.getMembershipStream(realm, group, firstResult, max).collect(Collectors.toList());
        }

        @Override
        Stream<String> getMembershipStream(RealmModel realm, GroupModel group, Integer firstResult, Integer max);
    }
}
