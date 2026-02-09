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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.ws.rs.ForbiddenException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionEffect;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse.EvaluationResultRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_MEMBERS;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW_MEMBERS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class UserResourceTypeEvaluationSpecTest extends AbstractPermissionTest {

    @InjectUser(ref = "alice")
    ManagedUser userAlice;

    @InjectUser(ref = "bob")
    ManagedUser userBob;

    @InjectUser(ref = "jdoe")
    ManagedUser userJdoe;

    @InjectUser(ref = "tom")
    ManagedUser userTom;

    @InjectUser(ref = "mary")
    ManagedUser userMary;

    ManagedUser myadmin;

    private List<ManagedUser> ALL_GROUP_MEMBERS;
    private List<ManagedUser> ALL_USERS;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    private UserPolicyRepresentation allowPolicy;
    private UserPolicyRepresentation denyPolicy;

    @BeforeEach
    public void onBefore() {
        Map<String, List<ManagedUser>> groupMembers = new HashMap<>();

        groupMembers.put("group-0", List.of(userAlice));
        groupMembers.put("group-1", List.of(userBob));
        groupMembers.put("group-2", List.of(userMary, userTom));

        for (Entry<String, List<ManagedUser>> group : groupMembers.entrySet()) {
            String name = group.getKey();

            GroupRepresentation rep = new GroupRepresentation();
            rep.setName(name);
            realm.admin().groups().add(rep).close();

            List<ManagedUser> members = group.getValue();

            for (ManagedUser member : members) {
                joinGroup(member, name);
            }
        }

        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        allowPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        denyPolicy = createUserPolicy(Logic.NEGATIVE, realm, client,"Not My Admin User Policy", myadmin.getId());
        this.myadmin = new ManagedUser(myadmin, realm.admin().users().get(myadmin.getId()));

        ALL_USERS = new ArrayList<>();
        groupMembers.values().forEach(ALL_USERS::addAll);
        ALL_USERS.add(this.myadmin);
        ALL_USERS.add(userJdoe);
        ALL_GROUP_MEMBERS = groupMembers.values().stream().flatMap(Collection::stream).toList();
    }

    @Test
    public void test_01() {
        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_02() {
        allowAllGroups();

        assertFilter(ALL_GROUP_MEMBERS);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_03() {
        denyAllGroups();

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_04() {
        allowGroup("group-0");

        assertFilter(userAlice);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_05() {
        allowGroup("group-0");
        allowAllGroups();

        assertFilter(ALL_GROUP_MEMBERS);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_06() {
        allowGroup("group-0");
        denyAllGroups();

        assertFilter(userAlice);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_07() {
        denyGroup("group-0");

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_08() {
        denyGroup("group-0");
        allowAllGroups();

        assertFilter(userBob, userTom, userMary);

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

        assertFilter(ALL_USERS);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_11() {
        allowAllUsers();
        allowAllGroups();

        assertFilter(ALL_USERS);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_12() {
        allowAllUsers();
        denyAllGroups();

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(userJdoe.getUsername(), "myadmin"));

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_13() {
        allowAllUsers();
        allowGroup("group-0");

        assertFilter(ALL_USERS);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_14() {
        allowAllUsers();
        allowGroup("group-0");
        allowAllGroups();

        assertFilter(ALL_USERS);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_15() {
        allowAllUsers();
        allowGroup("group-0");
        denyAllGroups();

        assertFilter(userAlice, userJdoe, myadmin);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_16() {
        allowAllUsers();
        denyGroup("group-0");

        assertFilter(userBob, userJdoe, myadmin, userTom, userMary);

        assertUpdate(userAlice, false);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_17() {
        allowAllUsers();
        denyGroup("group-0");
        allowAllGroups();

        assertFilter(userBob, userJdoe, myadmin, userTom, userMary);

        assertUpdate(userAlice, false);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_18() {
        allowAllUsers();
        denyGroup("group-0");
        denyAllGroups();

        assertFilter(userJdoe, myadmin);

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_19() {
        denyAllUsers();

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_20() {
        denyAllUsers();
        allowAllGroups();

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_21() {
        denyAllUsers();
        denyAllGroups();

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_22() {
        denyAllUsers();
        allowGroup("group-0");

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_23() {
        denyAllUsers();
        allowGroup("group-0");
        allowAllGroups();

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_24() {
        denyAllUsers();
        allowGroup("group-0");
        denyAllGroups();

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_25() {
        denyAllUsers();
        denyGroup("group-0");

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_26() {
        denyAllUsers();
        denyGroup("group-0");
        allowAllGroups();

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_27() {
        denyAllUsers();
        denyGroup("group-0");
        denyAllGroups();

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_28() {
        allowUser(userAlice);

        assertFilter(userAlice);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_29() {
        allowUser(userAlice);
        allowAllGroups();

        assertFilter(userAlice, userBob, userTom, userMary);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_30() {
        allowUser(userAlice);
        denyAllGroups();

        assertFilter(userAlice);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_31() {
        allowUser(userAlice);
        allowGroup("group-1");

        assertFilter(userAlice, userBob);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_32() {
        allowUser(userAlice);
        allowGroup("group-0");
        allowAllGroups();

        assertFilter(userAlice, userBob, userTom, userMary);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_33() {
        allowUser(userAlice);
        allowGroup("group-0");
        denyAllGroups();

        assertFilter(userAlice);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_33_a() {
        allowUser(userAlice);
        allowGroup("group-1");
        denyAllGroups();

        assertFilter(userAlice, userBob);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_34() {
        allowUser(userAlice);
        denyGroup("group-0");

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_35() {
        allowUser(userAlice);
        denyGroup("group-0");
        allowAllGroups();

        assertFilter(userBob, userTom, userMary);

        assertUpdate(userAlice, false);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_35_a() {
        allowUser(userAlice);
        denyGroup("group-1");
        allowAllGroups();

        assertFilter(userAlice, userTom, userMary);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_36() {
        allowUser(userAlice);
        denyGroup("group-0");
        denyAllGroups();

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_37() {
        allowUser(userAlice);
        allowAllUsers();

        assertFilter(ALL_USERS);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_38() {
        allowUser(userAlice);
        allowAllUsers();
        allowAllGroups();

        assertFilter(ALL_USERS);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_39() {
        allowUser(userAlice);
        allowAllUsers();
        denyAllGroups();

        assertFilter(userAlice, userJdoe, myadmin);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_40() {
        allowUser(userAlice);
        allowAllUsers();
        allowGroup("group-0");

        assertFilter(ALL_USERS);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_40_a() {
        allowUser(userAlice);
        allowAllUsers();
        allowGroup("group-1");

        assertFilter(ALL_USERS);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_41() {
        allowUser(userAlice);
        allowAllUsers();
        allowGroup("group-1");
        allowAllGroups();

        assertFilter(ALL_USERS);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_42() {
        allowUser(userAlice);
        allowAllUsers();
        allowGroup("group-0");
        denyAllGroups();

        assertFilter(userAlice, userJdoe, myadmin);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_42_a() {
        allowUser(userAlice);
        allowAllUsers();
        allowGroup("group-1");

        assertFilter(ALL_USERS);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_43() {
        allowUser(userAlice);
        allowAllUsers();
        denyGroup("group-0");

        assertFilter(userBob, userJdoe, myadmin, userTom, userMary);

        assertUpdate(userAlice, false);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_44() {
        allowUser(userAlice);
        allowAllUsers();
        denyGroup("group-0");
        allowAllGroups();

        assertFilter(userBob, userJdoe, myadmin, userTom, userMary);

        assertUpdate(userAlice, false);
        assertUpdate(userBob, true);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_45() {
        allowUser(userAlice);
        allowAllUsers();
        denyGroup("group-0");
        denyAllGroups();

        assertFilter(userJdoe, myadmin);

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, true);
    }

    @Test
    public void test_46() {
        allowUser(userAlice);
        denyAllUsers();

        assertFilter(userAlice);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_47() {
        allowUser(userAlice);
        denyAllUsers();
        allowAllGroups();

        assertFilter(userAlice);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_48() {
        allowUser(userAlice);
        denyAllUsers();
        denyAllGroups();

        assertFilter(userAlice);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_49() {
        allowUser(userAlice);
        denyAllUsers();
        allowGroup("group-0");

        assertFilter(userAlice);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_49_a() {
        allowUser(userAlice);
        denyAllUsers();
        allowGroup("group-1");

        assertFilter(userAlice);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_50() {
        allowUser(userAlice);
        denyAllUsers();
        allowGroup("group-0");
        allowAllGroups();

        assertFilter(userAlice);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_51() {
        allowUser(userAlice);
        denyAllUsers();
        allowGroup("group-0");
        denyAllGroups();

        assertFilter(userAlice);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_52() {
        allowUser(userAlice);
        denyAllUsers();
        denyGroup("group-0");

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_52_a() {
        allowUser(userAlice);
        denyAllUsers();
        denyGroup("group-1");

        assertFilter(userAlice);

        assertUpdate(userAlice, true);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_53() {
        allowUser(userAlice);
        denyAllUsers();
        denyGroup("group-0");
        allowAllGroups();

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_54() {
        allowUser(userAlice);
        denyAllUsers();
        denyGroup("group-0");
        denyAllGroups();

        assertFilter();

        assertUpdate(userAlice, false);
        assertUpdate(userBob, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_55() {
        allowAllUsers();
        denyUser(userAlice);
        denyUser(userJdoe);

        assertFilter(userBob, myadmin, userMary, userTom);

        assertUpdate(userBob, true);
        assertUpdate(userAlice, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_56() {
        allowUser(userJdoe);
        denyUser(userBob);

        assertFilter(userJdoe);

        assertUpdate(userJdoe, true);
        assertUpdate(userBob, false);
        assertUpdate(userAlice, false);
    }

    @Test
    public void test_57() {
        denyUser(userAlice);
        denyUser(userJdoe);

        assertFilter();

        assertUpdate(userBob, false);
        assertUpdate(userAlice, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_58() {
        denyUser(userTom);
        allowGroup("group-2");

        assertFilter(userMary);

        assertUpdate(userMary, true);
        assertUpdate(userTom, false);
        assertUpdate(userJdoe, false);
    }

    @Test
    public void test_59() {
        allowAllUsers();
        denyUser(userTom);
        allowGroup("group-1");

        assertFilter(userAlice, userBob, userMary, userJdoe, myadmin);

        assertUpdate(userMary, true);
        assertUpdate(userJdoe, true);
        assertUpdate(userTom, false);
    }

    @Test
    public void test_60() {
        allowAllUsers();
        denyUser(userTom);
        allowGroup("group-1");
        denyGroup("group-2");

        assertFilter(userAlice, userBob, userJdoe, myadmin);

        assertUpdate(userJdoe, true);
        assertUpdate(userMary, false);
        assertUpdate(userTom, false);
    }

    private void denyUser(ManagedUser user) {
        createPermission(client, user.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), denyPolicy);
    }

    private void allowAllUsers() {
        createAllPermission(client, USERS_RESOURCE_TYPE, allowPolicy, Set.of(VIEW, MANAGE));
    }

    private void allowUser(ManagedUser user) {
        createPermission(client, user.getId() , USERS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), allowPolicy);
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

    private void assertFilter(List<ManagedUser> expected) {
        assertFilter(expected.toArray(new ManagedUser[0]));
    }

    private void assertFilter(ManagedUser... expected) {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);

        if (expected.length == 0) {
            assertThat(search.isEmpty(), is(true));
        } else {
            assertThat(search.stream().map(UserRepresentation::getUsername).toList(), containsInAnyOrder(Stream.of(expected).map(ManagedUser::getUsername).toArray()));
        }
    }
}
