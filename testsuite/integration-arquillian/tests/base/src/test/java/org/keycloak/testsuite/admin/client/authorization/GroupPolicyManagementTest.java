/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.admin.client.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.GroupPoliciesResource;
import org.keycloak.admin.client.resource.GroupPolicyResource;
import org.keycloak.admin.client.resource.PolicyResource;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.testsuite.util.GroupBuilder;
import org.keycloak.testsuite.util.RealmBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class GroupPolicyManagementTest extends AbstractPolicyManagementTest {

    @Override
    protected RealmBuilder createTestRealm() {
        return super.createTestRealm().group(GroupBuilder.create().name("Group A")
                .subGroups(Arrays.asList("Group B", "Group D").stream().map(name -> {
                    if ("Group B".equals(name)) {
                        return GroupBuilder.create().name(name).subGroups(Arrays.asList("Group C", "Group E").stream().map(name1 -> GroupBuilder.create().name(name1).build()).collect(Collectors.toList())).build();
                    }
                    return GroupBuilder.create().name(name).build();
                }).collect(Collectors.toList()))
                .build()).group(GroupBuilder.create().name("Group F").build());
    }

    @Test
    public void testCreate() {
        AuthorizationResource authorization = getClient().authorization();
        GroupPolicyRepresentation representation = new GroupPolicyRepresentation();

        representation.setName("Group Policy");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.setGroupsClaim("groups");
        representation.addGroupPath("/Group A/Group B/Group C", true);
        representation.addGroupPath("Group F");

        assertCreated(authorization, representation);
    }

    @Test
    public void testCreateWithoutGroupsClaim() {
        AuthorizationResource authorization = getClient().authorization();
        GroupPolicyRepresentation representation = new GroupPolicyRepresentation();

        representation.setName(KeycloakModelUtils.generateId());
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.addGroupPath("/Group A/Group B/Group C", true);
        representation.addGroupPath("Group F");

        assertCreated(authorization, representation);
    }

    @Test
    public void testUpdate() {
        AuthorizationResource authorization = getClient().authorization();
        GroupPolicyRepresentation representation = new GroupPolicyRepresentation();

        representation.setName("Update Group Policy");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.setGroupsClaim("groups");
        representation.addGroupPath("/Group A/Group B/Group C", true);
        representation.addGroupPath("Group F");

        assertCreated(authorization, representation);

        representation.setName("changed");
        representation.setDescription("changed");
        representation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        representation.setLogic(Logic.POSITIVE);
        representation.setGroupsClaim(null);
        representation.removeGroup("/Group A/Group B");

        GroupPoliciesResource policies = authorization.policies().group();
        GroupPolicyResource permission = policies.findById(representation.getId());

        permission.update(representation);
        assertRepresentation(representation, permission);

        for (GroupPolicyRepresentation.GroupDefinition roleDefinition : representation.getGroups()) {
            if (roleDefinition.getPath().equals("Group F")) {
                roleDefinition.setExtendChildren(true);
            }
        }

        permission.update(representation);
        assertRepresentation(representation, permission);

        representation.getGroups().clear();
        representation.addGroupPath("/Group A/Group B");

        permission.update(representation);
        assertRepresentation(representation, permission);
    }

    @Test
    public void testDelete() {
        AuthorizationResource authorization = getClient().authorization();
        GroupPolicyRepresentation representation = new GroupPolicyRepresentation();

        representation.setName("Delete Group Policy");
        representation.setGroupsClaim("groups");
        representation.addGroupPath("/Group A/Group B/Group C", true);
        representation.addGroupPath("Group F");

        GroupPoliciesResource policies = authorization.policies().group();

        try (Response response = policies.create(representation)) {
            GroupPolicyRepresentation created = response.readEntity(GroupPolicyRepresentation.class);

            policies.findById(created.getId()).remove();

            GroupPolicyResource removed = policies.findById(created.getId());

            try {
                removed.toRepresentation();
                fail("Permission not removed");
            } catch (NotFoundException ignore) {

            }
        }
    }

    @Test
    public void testRemoveWithoutPath() {
        GroupPolicyRepresentation representation = new GroupPolicyRepresentation();

        representation.setName("Delete Group Path Policy");
        representation.setGroupsClaim("groups");
        representation.addGroup("Group A");

        representation.removeGroup("Group A");

        assertTrue(representation.getGroups().isEmpty());
    }

    @Test
    public void testGenericConfig() {
        AuthorizationResource authorization = getClient().authorization();
        GroupPolicyRepresentation representation = new GroupPolicyRepresentation();

        representation.setName("Test Generic Config Permission");
        representation.setGroupsClaim("groups");
        representation.addGroupPath("/Group A");

        GroupPoliciesResource policies = authorization.policies().group();

        try (Response response = policies.create(representation)) {
            GroupPolicyRepresentation created = response.readEntity(GroupPolicyRepresentation.class);

            PolicyResource policy = authorization.policies().policy(created.getId());
            PolicyRepresentation genericConfig = policy.toRepresentation();

            assertNotNull(genericConfig.getConfig());
            assertNotNull(genericConfig.getConfig().get("groups"));

            GroupRepresentation group = getRealm().groups().groups().stream().filter(groupRepresentation -> groupRepresentation.getName().equals("Group A")).findFirst().get();

            assertTrue(genericConfig.getConfig().get("groups").contains(group.getId()));
        }
    }

    private void assertCreated(AuthorizationResource authorization, GroupPolicyRepresentation representation) {
        GroupPoliciesResource policies = authorization.policies().group();
        Response response = policies.create(representation);
        GroupPolicyRepresentation created = response.readEntity(GroupPolicyRepresentation.class);
        GroupPolicyResource policy = policies.findById(created.getId());
        assertRepresentation(representation, policy);
    }

    private void assertRepresentation(GroupPolicyRepresentation representation, GroupPolicyResource permission) {
        GroupPolicyRepresentation actual = permission.toRepresentation();
        assertRepresentation(representation, actual, () -> permission.resources(), () -> Collections.emptyList(), () -> permission.associatedPolicies());
        assertEquals(representation.getGroups().size(), actual.getGroups().size());
        assertEquals(0, actual.getGroups().stream().filter(actualDefinition -> !representation.getGroups().stream()
                .filter(groupDefinition -> getGroupPath(actualDefinition.getId()).equals(getCanonicalGroupPath(groupDefinition.getPath())) && actualDefinition.isExtendChildren() == groupDefinition.isExtendChildren())
                .findFirst().isPresent())
                .count());
    }

    private String getGroupPath(String id) {
        return getRealm().groups().group(id).toRepresentation().getPath();
    }

    private String getCanonicalGroupPath(String path) {
        if (path.charAt(0) == '/') {
            return path;
        }
        return "/" + path;
    }
}
