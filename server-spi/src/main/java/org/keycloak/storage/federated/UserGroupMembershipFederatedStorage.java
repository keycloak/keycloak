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
    @Deprecated
    default Set<GroupModel> getGroups(RealmModel realm, String userId) {
        return getGroupsStream(realm, userId).collect(Collectors.toSet());
    }

    Stream<GroupModel> getGroupsStream(RealmModel realm, String userId);

    void joinGroup(RealmModel realm, String userId, GroupModel group);
    void leaveGroup(RealmModel realm, String userId, GroupModel group);

    /**
     * @deprecated Use {@link #getMembershipStream(RealmModel, GroupModel, int, int) getMembershipStream} instead.
     */
    @Deprecated
    default List<String> getMembership(RealmModel realm, GroupModel group, int firstResult, int max) {
        return this.getMembershipStream(realm, group, firstResult, max).collect(Collectors.toList());
    }

    /**
     * Obtains the federated users that are members of the given {@code group} in the specified {@code realm}.
     *
     * @param realm a reference to the realm.
     * @param group a reference to the group whose federated members are being searched.
     * @param firstResult first result to return. Ignored if negative.
     * @param max maximum number of results to return. Ignored if negative.
     * @return a non-null {@code Stream} of federated user ids that are members of the group in the realm.
     */
    Stream<String> getMembershipStream(RealmModel realm, GroupModel group, int firstResult, int max);

}
