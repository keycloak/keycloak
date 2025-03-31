/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.authz.fgap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.keycloak.authorization.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.AdminPermissionsSchema.MANAGE_MEMBERS;
import static org.keycloak.authorization.AdminPermissionsSchema.USERS_RESOURCE_TYPE;
import static org.keycloak.authorization.AdminPermissionsSchema.VIEW;
import static org.keycloak.authorization.AdminPermissionsSchema.VIEW_MEMBERS;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.authorization.AdminPermissionsSchema;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionEffect;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse.EvaluationResultRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;

@KeycloakIntegrationTest(config = KeycloakAdminPermissionsServerConfig.class)
public class UserResourceTypeEvaluationSpecTest extends AbstractPermissionTest {

    @InjectUser(ref = "alice")
    ManagedUser userAlice;

    @InjectUser(ref = "bob")
    ManagedUser userBob;

    @InjectUser(ref = "jdoe")
    ManagedUser userJdoe;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    private UserPolicyRepresentation allowPolicy;
    private UserPolicyRepresentation denyPolicy;

    @BeforeEach
    public void onBefore() {
        for (int i = 0; i < 2; i++) {
            GroupRepresentation group = new GroupRepresentation();
            group.setName("group-" + i);
            realm.admin().groups().add(group).close();
        }

        joinGroup(userAlice, "group-0");
        joinGroup(userBob, "group-1");

        allowPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        denyPolicy = createUserPolicy(Logic.NEGATIVE, realm, client,"Not My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
    }

    @AfterEach
    public void onAfter() {
        ScopePermissionsResource permissions = getScopePermissionsResource(client);

        for (ScopePermissionRepresentation permission : permissions.findAll(null, null, null, -1, -1)) {
            permissions.findById(permission.getId()).remove();
        }

        realm.admin().groups().groups().forEach(group -> realm.admin().groups().group(group.getId()).remove());
    }

    @Test
    public void test_01() {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_02() {
        allowAllGroups();

        // TODO: should see only user members of groups
        //assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder("alice", "bob"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_03() {
        denyAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_04() {
        allowGroup("group-0");

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder("alice"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_05() {
        allowGroup("group-0");
        allowAllGroups();

        // TODO: should only see users that are members of groups
        //List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        //assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder("alice", "bob"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_06() {
        allowGroup("group-0");
        denyAllGroups();

        // TODO: should see only members of single group
        //List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        //assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder("alice", "bob"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_07() {
        denyGroup("group-0");

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_08() {
        denyGroup("group-0");
        allowAllGroups();

        // TODO: should see only members of groups except those from single group
        //List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        //assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder("bob"));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_09() {
        denyGroup("group-0");
        denyAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_10() {
        allowAllUsers();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_11() {
        allowAllUsers();
        allowAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_12() {
        allowAllUsers();
        denyAllGroups();

        // TODO: should not see users members of a group
//        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
//        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder("jdoe", "myadmin"));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_13() {
        allowAllUsers();
        allowGroup("group-0");

        // TODO: should return all users
//        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
//        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_14() {
        allowAllUsers();
        allowGroup("group-0");
        allowAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_15() {
        allowAllUsers();
        allowGroup("group-0");
        denyAllGroups();

        // TODO: should see user that are not a member of a group or members of the single group
        //List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        //assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_16() {
        allowAllUsers();
        denyGroup("group-0");

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_17() {
        allowAllUsers();
        denyGroup("group-0");
        allowAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_18() {
        allowAllUsers();
        denyGroup("group-0");
        denyAllGroups();

        // TODO: should see only users not members of groups
        //List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        //assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_19() {
        denyAllUsers();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_20() {
        denyAllUsers();
        allowAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_21() {
        denyAllUsers();
        denyAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_22() {
        denyAllUsers();
        allowGroup("group-0");

        // NOK denying all users permissions should have precedence over group permissions and not return users
        //List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        //assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_23() {
        denyAllUsers();
        allowGroup("group-0");
        allowAllGroups();

        // NOK denying all users permissions should have precedence over group permissions and not return users
        //List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        //assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_24() {
        denyAllUsers();
        allowGroup("group-0");
        denyAllGroups();

        // NOK denying all users permissions should have precedence over group permissions and not return users
//        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
//        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_25() {
        denyAllUsers();
        denyGroup("group-0");

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_26() {
        denyAllUsers();
        denyGroup("group-0");
        allowAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_27() {
        denyAllUsers();
        denyGroup("group-0");
        denyAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_28() {
        allowUser(userAlice.getId());

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_29() {
        allowUser(userAlice.getId());
        allowAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_30() {
        allowUser(userAlice.getId());
        denyAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_31() {
        allowUser(userAlice.getId());
        allowGroup("group-1");

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_32() {
        allowUser(userAlice.getId());
        allowGroup("group-0");
        allowAllGroups();

        //NOK all group members should be granted
        //List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        //assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_33() {
        allowUser(userAlice.getId());
        allowGroup("group-0");
        denyAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_33_a() {
        allowUser(userAlice.getId());
        allowGroup("group-1");
        denyAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_34() {
        allowUser(userAlice.getId());
        denyGroup("group-0");

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_35() {
        allowUser(userAlice.getId());
        denyGroup("group-0");
        allowAllGroups();

        //NOK should grant access to users from other groups
        //List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        //assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userBob.getUsername()));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_35_a() {
        allowUser(userAlice.getId());
        denyGroup("group-1");
        allowAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_36() {
        allowUser(userAlice.getId());
        denyGroup("group-0");
        denyAllGroups();

        //NOK should grant access to users from other groups
//        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
//        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_37() {
        allowUser(userAlice.getId());
        allowAllUsers();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_38() {
        allowUser(userAlice.getId());
        allowAllUsers();
        allowAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_39() {
        allowUser(userAlice.getId());
        allowAllUsers();
        denyAllGroups();

        //NOK should not return members from groups other than the user that was granted
        //List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        //assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_40() {
        allowUser(userAlice.getId());
        allowAllUsers();
        allowGroup("group-0");

        //NOK not sure if it should restrict or grant access
//        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
//        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_40_a() {
        allowUser(userAlice.getId());
        allowAllUsers();
        allowGroup("group-1");

        //NOK not sure if it should restrict or grant access
        //List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        //assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_41() {
        allowUser(userAlice.getId());
        allowAllUsers();
        allowGroup("group-1");
        allowAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_42() {
        allowUser(userAlice.getId());
        allowAllUsers();
        allowGroup("group-0");
        denyAllGroups();

        //TODO should return single resource and users not members of groups
//        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
//        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_42_a() {
        allowUser(userAlice.getId());
        allowAllUsers();
        allowGroup("group-1");
        denyAllGroups();

        //NOK not sure if it should restrict or grant access
        //List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        //assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_43() {
        allowUser(userAlice.getId());
        allowAllUsers();
        denyGroup("group-0");

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_44() {
        allowUser(userAlice.getId());
        allowAllUsers();
        denyGroup("group-0");
        allowAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_45() {
        allowUser(userAlice.getId());
        allowAllUsers();
        denyGroup("group-0");
        denyAllGroups();

        //NOK filtering is not taking into account when group membership is denied to all groups
        //List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        //assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userBob.getUsername(), userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_46() {
        allowUser(userAlice.getId());
        denyAllUsers();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_47() {
        allowUser(userAlice.getId());
        denyAllUsers();
        allowAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_48() {
        allowUser(userAlice.getId());
        denyAllUsers();
        denyAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_49() {
        allowUser(userAlice.getId());
        denyAllUsers();
        allowGroup("group-0");

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_49_a() {
        allowUser(userAlice.getId());
        denyAllUsers();
        allowGroup("group-1");

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername(), userBob.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_50() {
        allowUser(userAlice.getId());
        denyAllUsers();
        allowGroup("group-0");
        allowAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_51() {
        allowUser(userAlice.getId());
        denyAllUsers();
        allowGroup("group-0");
        denyAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_52() {
        allowUser(userAlice.getId());
        denyAllUsers();
        denyGroup("group-0");

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_52_a() {
        allowUser(userAlice.getId());
        denyAllUsers();
        denyGroup("group-1");

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userAlice.getUsername()));

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_53() {
        allowUser(userAlice.getId());
        denyAllUsers();
        denyGroup("group-0");
        allowAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_54() {
        allowUser(userAlice.getId());
        denyAllUsers();
        denyGroup("group-0");
        denyAllGroups();

        // NOK should return alice because the resource is granted
//        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
//        assertThat(search.isEmpty(), is(true));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    private void allowAllUsers() {
        createAllPermission(client, USERS_RESOURCE_TYPE, allowPolicy, Set.of(VIEW, MANAGE));
    }

    private void allowUser(String id) {
        createPermission(client, id , USERS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), allowPolicy);
    }

    private void denyAllUsers() {
        createAllPermission(client, USERS_RESOURCE_TYPE, denyPolicy, Set.of(VIEW, MANAGE));
    }

    private void allowGroup(String name) {
        createPermission(client, getGroup(name).getId() ,GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS, MANAGE_MEMBERS), allowPolicy);
    }

    private void denyAllGroups() {
        createAllPermission(client, GROUPS_RESOURCE_TYPE, denyPolicy, Set.of(VIEW_MEMBERS, MANAGE_MEMBERS));
    }

    private void allowAllGroups() {
        createAllPermission(client, GROUPS_RESOURCE_TYPE, allowPolicy, Set.of(VIEW_MEMBERS, MANAGE_MEMBERS));
    }

    private void denyGroup(String groupName) {
        createPermission(client, getGroup(groupName).getId() ,GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS, MANAGE_MEMBERS), denyPolicy);
    }

    private GroupRepresentation getGroup(String groupName) {
        return realm.admin().groups().groups(groupName, -1, -1).get(0);
    }

    private void joinGroup(ManagedUser user, String groupName) {
        String groupId = getGroup(groupName).getId();
        realm.admin().users().get(user.getId()).joinGroup(groupId);
    }

    private void assertUpdate(ManagedUser user, boolean success) {
        if (success) {
            realmAdminClient.realm(realm.getName()).users().get(user.getId()).update(user.admin().toRepresentation());
            assertEvaluation(user, DecisionEffect.PERMIT, Set.of(VIEW, MANAGE));
        } else {
            try {
                realmAdminClient.realm(realm.getName()).users().get(user.getId()).update(user.admin().toRepresentation());
                fail("Should have thrown an exception");
            } catch (ForbiddenException expected) {
                assertEvaluation(user, DecisionEffect.DENY, Set.of(VIEW, MANAGE));
            }
        }
    }

    private void assertEvaluation(ManagedUser managedUser, DecisionEffect effect, Set<String> grantedScopes) {
        PolicyEvaluationRequest request = new PolicyEvaluationRequest();

        request.setUserId(realm.admin().users().search("myadmin").get(0).getId());
        request.setResourceType(USERS_RESOURCE_TYPE);
        ResourceRepresentation resource = new ResourceRepresentation();
        resource.setName(managedUser.getId());
        request.setResources(List.of(resource));

        PolicyEvaluationResponse response = client.admin().authorization().policies().evaluate(request);
        assertThat(response.getStatus(), is(effect));

        if (response.getResults().isEmpty()) {
            return;
        }

        EvaluationResultRepresentation result = response.getResults().get(0);

        if (!grantedScopes.isEmpty()) {
            if (DecisionEffect.PERMIT.equals(effect)) {
                assertThat(result.getResource().getName(), is(managedUser.getUsername() + " with scopes " + grantedScopes.stream().sorted().toList()));
                assertThat(result.getAllowedScopes().stream().map(ScopeRepresentation::getName).toList(), containsInAnyOrder(grantedScopes.toArray(new String[0])));
                assertThat(result.getDeniedScopes().stream().map(ScopeRepresentation::getName).toList(), containsInAnyOrder(AdminPermissionsSchema.USERS.getScopes().stream().filter(Predicate.not(grantedScopes::contains)).toArray()));
            } else {
                assertThat(result.getDeniedScopes().stream().map(ScopeRepresentation::getName).toList(), containsInAnyOrder(AdminPermissionsSchema.USERS.getScopes().toArray()));
            }
        }
    }
}
