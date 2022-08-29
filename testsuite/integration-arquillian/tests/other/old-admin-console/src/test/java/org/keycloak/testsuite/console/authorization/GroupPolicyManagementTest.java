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
package org.keycloak.testsuite.console.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.console.page.clients.authorization.policy.GroupPolicy;
import org.keycloak.testsuite.console.page.clients.authorization.policy.RolePolicy;
import org.keycloak.testsuite.console.page.clients.authorization.policy.UserPolicy;
import org.keycloak.testsuite.util.GroupBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class GroupPolicyManagementTest extends AbstractAuthorizationSettingsTest {

    @Before
    public void configureTest() {
        super.configureTest();
        RealmResource realmResource = testRealmResource();
        String groupAId = ApiUtil.getCreatedId(realmResource.groups().add(GroupBuilder.create().name("Group A").build()));
        String groupBId = ApiUtil.getCreatedId(realmResource.groups().group(groupAId).subGroup(GroupBuilder.create().name("Group B").build()));
        realmResource.groups().group(groupBId).subGroup(GroupBuilder.create().name("Group D").build());
        realmResource.groups().group(groupBId).subGroup(GroupBuilder.create().name("Group E").build());
        realmResource.groups().group(groupAId).subGroup(GroupBuilder.create().name("Group C").build());
        realmResource.groups().add(GroupBuilder.create().name("Group F").build());
    }

    @Test
    public void testCreateWithoutGroupClaims() throws InterruptedException {
        authorizationPage.navigateTo();
        GroupPolicyRepresentation expected = new GroupPolicyRepresentation();

        expected.setName("Test Group Policy");
        expected.setDescription("description");
        expected.addGroupPath("/Group A", true);
        expected.addGroupPath("/Group A/Group B/Group D");
        expected.addGroupPath("Group F");

        createPolicy(expected);
    }

    @Test
    public void testUpdate() throws InterruptedException {
        authorizationPage.navigateTo();
        GroupPolicyRepresentation expected = new GroupPolicyRepresentation();

        expected.setName("Test Group Policy");
        expected.setDescription("description");
        expected.setGroupsClaim("groups");
        expected.addGroupPath("/Group A", true);
        expected.addGroupPath("/Group A/Group B/Group D");
        expected.addGroupPath("Group F");

        expected = createPolicy(expected);

        String previousName = expected.getName();

        expected.setName("Changed Test Group Policy");
        expected.setDescription("Changed description");
        expected.setLogic(Logic.NEGATIVE);
        expected.setGroupsClaim(null);

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(previousName, expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        GroupPolicy actual = authorizationPage.authorizationTabs().policies().name(expected.getName());

        assertPolicy(expected, actual);

        expected.getGroups().clear();
        expected.addGroupPath("/Group A", false);
        expected.addGroupPath("/Group A/Group B/Group D");

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(expected.getName(), expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        actual = authorizationPage.authorizationTabs().policies().name(expected.getName());

        assertPolicy(expected, actual);

        expected.getGroups().clear();
        expected.addGroupPath("/Group E");
        expected.addGroupPath("/Group A/Group B", true);
        expected.addGroupPath("/Group A/Group C");


        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(expected.getName(), expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        actual = authorizationPage.authorizationTabs().policies().name(expected.getName());

        assertPolicy(expected, actual);
    }

    @Test
    public void testDelete() throws InterruptedException {
        authorizationPage.navigateTo();
        GroupPolicyRepresentation expected = new GroupPolicyRepresentation();

        expected.setName("Test Delete Group Policy");
        expected.setDescription("description");
        expected.setGroupsClaim("groups");
        expected.addGroupPath("/Group A", true);
        expected.addGroupPath("/Group A/Group B/Group D");
        expected.addGroupPath("Group F");

        expected = createPolicy(expected);
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().delete(expected.getName());
        assertAlertSuccess();
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().policies().policies().findByName(expected.getName()));
    }

    @Test
    public void testDeleteFromList() throws InterruptedException {
        authorizationPage.navigateTo();
        GroupPolicyRepresentation expected = new GroupPolicyRepresentation();

        expected.setName("Test Delete Group Policy");
        expected.setDescription("description");
        expected.setGroupsClaim("groups");
        expected.addGroupPath("/Group A", true);
        expected.addGroupPath("/Group A/Group B/Group D");
        expected.addGroupPath("Group F");

        expected = createPolicy(expected);
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().deleteFromList(expected.getName());
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().policies().policies().findByName(expected.getName()));
    }

    @Test
    public void testSaveWithInInvalidGroup() throws InterruptedException {
        authorizationPage.navigateTo();
        GroupPolicyRepresentation expected = new GroupPolicyRepresentation();

        expected.setName("Test Invalid Group Policy");
        expected.setDescription("description");
        expected.addGroupPath("/Groups", true);

        authorizationPage.authorizationTabs().policies().create(expected, false);
        
        alert.assertDanger("Error! You must choose a group");
    }

    private GroupPolicyRepresentation createPolicy(GroupPolicyRepresentation expected) {
        GroupPolicy policy = authorizationPage.authorizationTabs().policies().create(expected);
        assertAlertSuccess();
        return assertPolicy(expected, policy);
    }

    private GroupPolicyRepresentation assertPolicy(GroupPolicyRepresentation expected, GroupPolicy policy) {
        GroupPolicyRepresentation actual = policy.toRepresentation();

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getLogic(), actual.getLogic());
        assertEquals(expected.getGroupsClaim(), actual.getGroupsClaim());

        assertNotNull(actual.getGroups());
        assertEquals(expected.getGroups().size(), actual.getGroups().size());
        assertEquals(0, actual.getGroups().stream().filter(actualDefinition -> !expected.getGroups().stream()
                .filter(groupDefinition -> actualDefinition.getPath().contains(groupDefinition.getPath()) && actualDefinition.isExtendChildren() == groupDefinition.isExtendChildren())
                .findFirst().isPresent())
                .count());
        return actual;
    }
}
