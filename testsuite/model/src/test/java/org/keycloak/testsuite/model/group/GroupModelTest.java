/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model.group;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.testsuite.model.KeycloakModelTest;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class GroupModelTest extends KeycloakModelTest {

    private String realmId;
    private static final String OLD_VALUE = "oldValue";
    private static final String NEW_VALUE = "newValue";

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "realm");
        s.getContext().setRealm(realm);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testGroupAttributesSetter() {
        String groupId = withRealm(realmId, (session, realm) -> {
            GroupModel groupModel = session.groups().createGroup(realm, "my-group");
            groupModel.setSingleAttribute("key", OLD_VALUE);

            return groupModel.getId();
        });
        withRealm(realmId, (session, realm) -> {
            GroupModel groupModel = session.groups().getGroupById(realm, groupId);
            assertThat(groupModel.getAttributes().get("key"), contains(OLD_VALUE));

            // Change value to NEW_VALUE
            groupModel.setSingleAttribute("key", NEW_VALUE);

            // Check all getters return the new value
            assertThat(groupModel.getAttributes().get("key"), contains(NEW_VALUE));
            assertThat(groupModel.getFirstAttribute("key"), equalTo(NEW_VALUE));
            assertThat(groupModel.getAttributeStream("key").findFirst().get(), equalTo(NEW_VALUE));

            return null;
        });
    }

    @Test
    public void testSubGroupsSorted() {
        List<String> subGroups = Arrays.asList("sub-group-1", "sub-group-2", "sub-group-3");

        String groupId = withRealm(realmId, (session, realm) -> {
            GroupModel group = session.groups().createGroup(realm, "my-group");

            subGroups.stream().sorted(Collections.reverseOrder()).forEach(s -> {
                GroupModel subGroup = session.groups().createGroup(realm, s);
                group.addChild(subGroup);
            });

            return group.getId();
        });
        withRealm(realmId, (session, realm) -> {
            GroupModel group = session.groups().getGroupById(realm, groupId);

            assertThat(group.getSubGroupsStream().map(GroupModel::getName).collect(Collectors.toList()),
                    contains(subGroups.toArray()));

            return null;
        });
    }

    @Test
    public void testGroupByName() {
        String subGroupId1 = withRealm(realmId, (session, realm) -> {
            GroupModel group = session.groups().createGroup(realm, "parent-1");
            GroupModel subGroup = session.groups().createGroup(realm, "sub-group-1", group);
           return subGroup.getId();
        });

        String subGroupId2 = withRealm(realmId, (session, realm) -> {
            GroupModel group = session.groups().createGroup(realm, "parent-2");
            GroupModel subGroup = session.groups().createGroup(realm, "sub-group-1", group);
            return subGroup.getId();
        });
        withRealm(realmId, (session, realm) -> {
            GroupModel group1 = session.groups().getGroupByName(realm, null,"parent-1");
            GroupModel group2 = session.groups().getGroupByName(realm, null,"parent-2");

            GroupModel subGroup1 = session.groups().getGroupByName(realm, group1,"sub-group-1");
            GroupModel subGroup2 = session.groups().getGroupByName(realm, group2,"sub-group-1");

            assertThat(subGroup1.getId(), equalTo(subGroupId1));
            assertThat(subGroup1.getName(), equalTo("sub-group-1"));
            assertThat(subGroup2.getId(), equalTo(subGroupId2));
            assertThat(subGroup2.getName(), equalTo("sub-group-1"));
            return null;
        });
    }

    @Test
    public void testConflictingNames() {
        final String conflictingGroupName = "conflicting-group-name";

        String parentGroupWithChildId = withRealm(realmId, (session, realm) -> {
            GroupModel parentGroupWithChild = session.groups().createGroup(realm, "parent-1");
            GroupModel subGroup1 = session.groups().createGroup(realm, conflictingGroupName, parentGroupWithChild);
            return parentGroupWithChild.getId();
        });

        String parentGroupWithConflictingNameId = withRealm(realmId, (session, realm) -> session.groups().createGroup(realm, conflictingGroupName).getId());
        String parentGroupWithoutChildrenId = withRealm(realmId, (session, realm) -> session.groups().createGroup(realm, "parent-2").getId());

        withRealm(realmId, (session, realm) -> {
            GroupModel searchedGroup = session.groups().getGroupByName(realm, null, conflictingGroupName);
            assertThat(searchedGroup, notNullValue());
            assertThat(searchedGroup.getId(), equalTo(parentGroupWithConflictingNameId));
            return null;
        });

        withRealm(realmId, (session, realm) -> {
            GroupModel parentGroupWithChild = session.groups().getGroupById(realm, parentGroupWithChildId);
            GroupModel searchedGroup = session.groups().getGroupByName(realm, parentGroupWithChild, conflictingGroupName);
            assertThat(searchedGroup, notNullValue());
            assertThat(searchedGroup.getParentId(), equalTo(parentGroupWithChildId));
            return null;
        });

        withRealm(realmId, (session, realm) -> {
            GroupModel parentGroupWithoutChildren = session.groups().getGroupById(realm, parentGroupWithoutChildrenId);
            GroupModel searchedGroup = session.groups().getGroupByName(realm, parentGroupWithoutChildren, conflictingGroupName);
            assertThat(searchedGroup, nullValue());
            return null;
        });
    }

    @Test
    public void testGroupByNameCacheInvalidation() {
        String subGroupId1 = withRealm(realmId, (session, realm) -> {
            GroupModel group = session.groups().createGroup(realm, "parent-1");
            GroupModel subGroup = session.groups().createGroup(realm, "sub-group-1", group);
            return subGroup.getId();
        });

        withRealm(realmId, (session, realm) -> {
            GroupModel group1 = session.groups().getGroupByName(realm, null, "parent-1");
            GroupModel subGroup1 = session.groups().getGroupByName(realm, group1, "sub-group-1");
            assertThat(subGroup1.getId(), equalTo(subGroupId1));
            return null;
        });

        withRealm(realmId, (session, realm) -> {
            GroupModel group1 = session.groups().getGroupByName(realm, null, "parent-1");
            GroupModel subGroup1 = session.groups().getGroupByName(realm, group1, "sub-group-1");
            session.groups().removeGroup(realm, subGroup1);
            return null;
        });

        withRealm(realmId, (session, realm) -> {
            GroupModel group1 = session.groups().getGroupByName(realm, null, "parent-1");
            GroupModel subGroup1 = session.groups().getGroupByName(realm, group1, "sub-group-1");
            assertThat(subGroup1, nullValue());
            return null;
        });

    }
    @Test
    public void testFindGroupByPath() {
        String subGroupId1 = withRealm(realmId, (session, realm) -> {
            GroupModel group = session.groups().createGroup(realm, "parent-1");
            GroupModel subGroup = session.groups().createGroup(realm, "sub-group-1", group);
            return subGroup.getId();
        });

        String subGroupIdWithSlash = withRealm(realmId, (session, realm) -> {
            GroupModel group = session.groups().createGroup(realm, "parent-2");
            GroupModel subGroup = session.groups().createGroup(realm, "sub-group/1", group);
            return subGroup.getId();
        });

        withRealm(realmId, (session, realm) -> {
            GroupModel group1 = KeycloakModelUtils.findGroupByPath(session, realm, "/parent-1");
            GroupModel group2 = KeycloakModelUtils.findGroupByPath(session, realm, "/parent-2");
            assertThat(group1.getName(), equalTo("parent-1"));
            assertThat(group2.getName(), equalTo("parent-2"));

            GroupModel subGroup1 = KeycloakModelUtils.findGroupByPath(session, realm, "/parent-1/sub-group-1");
            GroupModel subGroup2 = KeycloakModelUtils.findGroupByPath(session, realm, "/parent-2/sub-group/1");
            assertThat(subGroup1.getId(), equalTo(subGroupId1));
            assertThat(subGroup2.getId(), equalTo(subGroupIdWithSlash));
            return null;
        });
    }
}
