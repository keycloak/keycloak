/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.admin.user;

import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.Assert.assertNames;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:volker.suschke@bosch-si.com">Volker Suschke</a>
 * @author <a href="mailto:leon.graser@bosch-si.com">Leon Graser</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class UserGroupMembershipTest extends AbstractAdminTest {

    public String createUser() {
        return createUser("user1", "user1@localhost");
    }

    public String createUser(String username, String email) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setRequiredActions(Collections.emptyList());
        user.setEnabled(true);

        return createUser(user);
    }

    private String createUser(UserRepresentation userRep) {
        Response response = realm.users().create(userRep);
        String createdId = ApiUtil.getCreatedId(response);
        response.close();

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userResourcePath(createdId), userRep, ResourceType.USER);

        getCleanup().addUserId(createdId);

        return createdId;
    }

    @Test
    public void verifyCreateUser() {
        createUser();
    }

    private GroupRepresentation createGroup(RealmResource realm, GroupRepresentation group) {
        Response response = realm.groups().add(group);
        String groupId = ApiUtil.getCreatedId(response);
        getCleanup().addGroupId(groupId);
        response.close();

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.groupPath(groupId), group, ResourceType.GROUP);

        // Set ID to the original rep
        group.setId(groupId);
        return group;
    }

    @Test
    public void groupMembershipPaginated() {
        Response response = realm.users().create(UserBuilder.create().username("user-a").build());
        String userId = ApiUtil.getCreatedId(response);
        response.close();
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userResourcePath(userId), ResourceType.USER);

        for (int i = 1; i <= 10; i++) {
            GroupRepresentation group = new GroupRepresentation();
            group.setName("group-" + i);
            String groupId = createGroup(realm, group).getId();
            realm.users().get(userId).joinGroup(groupId);
            assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userGroupPath(userId, groupId), group, ResourceType.GROUP_MEMBERSHIP);
        }

        List<GroupRepresentation> groups = realm.users().get(userId).groups(5, 6);
        assertEquals(groups.size(), 5);
        assertNames(groups, "group-5","group-6","group-7","group-8","group-9");
    }

    @Test
    public void groupMembershipSearch() {
        Response response = realm.users().create(UserBuilder.create().username("user-b").build());
        String userId = ApiUtil.getCreatedId(response);
        response.close();
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userResourcePath(userId), ResourceType.USER);

        for (int i = 1; i <= 10; i++) {
            GroupRepresentation group = new GroupRepresentation();
            group.setName("group-" + i);
            String groupId = createGroup(realm, group).getId();
            realm.users().get(userId).joinGroup(groupId);
            assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userGroupPath(userId, groupId), group, ResourceType.GROUP_MEMBERSHIP);
        }

        List<GroupRepresentation> groups = realm.users().get(userId).groups("-3", 0, 10);
        assertEquals(1, groups.size());
        assertNames(groups, "group-3");

        List<GroupRepresentation> groups2 = realm.users().get(userId).groups("1", 0, 10);
        assertEquals(2, groups2.size());
        assertNames(groups2, "group-1", "group-10");

        List<GroupRepresentation> groups3 = realm.users().get(userId).groups("1", 2, 10);
        assertEquals(0, groups3.size());

        List<GroupRepresentation> groups4 = realm.users().get(userId).groups("gr", 2, 10);
        assertEquals(8, groups4.size());

        List<GroupRepresentation> groups5 = realm.users().get(userId).groups("Gr", 2, 10);
        assertEquals(8, groups5.size());
    }

}
