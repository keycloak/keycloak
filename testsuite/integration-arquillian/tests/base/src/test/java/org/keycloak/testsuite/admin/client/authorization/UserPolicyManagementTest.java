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
package org.keycloak.testsuite.admin.client.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.PolicyResource;
import org.keycloak.admin.client.resource.UserPoliciesResource;
import org.keycloak.admin.client.resource.UserPolicyResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserPolicyManagementTest extends AbstractPermissionManagementTest {

    @Override
    protected RealmBuilder createTestRealm() {
        return super.createTestRealm()
                .user(UserBuilder.create().username("User A"))
                .user(UserBuilder.create().username("User B"))
                .user(UserBuilder.create().username("User C"));
    }

    @Test
    public void testCreateUserPolicy() {
        AuthorizationResource authorization = getClient().authorization();
        UserPolicyRepresentation representation = new UserPolicyRepresentation();

        representation.setName("Realm User Policy");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.addUser("User A");
        representation.addUser("User B");

        assertCreated(authorization, representation);
    }

    @Test
    public void testUpdate() {
        AuthorizationResource authorization = getClient().authorization();
        UserPolicyRepresentation representation = new UserPolicyRepresentation();

        representation.setName("Update Test User Policy");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.addUser("User A");
        representation.addUser("User B");
        representation.addUser("User C");

        assertCreated(authorization, representation);

        representation.setName("changed");
        representation.setDescription("changed");
        representation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        representation.setLogic(Logic.POSITIVE);
        representation.setUsers(representation.getUsers().stream().filter(userName -> !userName.equals("User A")).collect(Collectors.toSet()));

        UserPoliciesResource policies = authorization.policies().users();
        UserPolicyResource permission = policies.findById(representation.getId());

        permission.update(representation);
        assertRepresentation(representation, permission);

        representation.setUsers(representation.getUsers().stream().filter(userName -> !userName.equals("User C")).collect(Collectors.toSet()));

        permission.update(representation);
        assertRepresentation(representation, permission);
    }

    @Test
    public void testDelete() {
        AuthorizationResource authorization = getClient().authorization();
        UserPolicyRepresentation representation = new UserPolicyRepresentation();

        representation.setName("Test Delete Permission");
        representation.addUser("User A");

        UserPoliciesResource policies = authorization.policies().users();
        Response response = policies.create(representation);
        UserPolicyRepresentation created = response.readEntity(UserPolicyRepresentation.class);

        policies.findById(created.getId()).remove();

        UserPolicyResource removed = policies.findById(created.getId());

        try {
            removed.toRepresentation();
            fail("Permission not removed");
        } catch (NotFoundException ignore) {

        }
    }

    @Test
    public void testGenericConfig() {
        AuthorizationResource authorization = getClient().authorization();
        UserPolicyRepresentation representation = new UserPolicyRepresentation();

        representation.setName("Test Generic Config Permission");
        representation.addUser("User A");

        UserPoliciesResource policies = authorization.policies().users();
        Response response = policies.create(representation);
        UserPolicyRepresentation created = response.readEntity(UserPolicyRepresentation.class);

        PolicyResource policy = authorization.policies().policy(created.getId());
        PolicyRepresentation genericConfig = policy.toRepresentation();

        assertNotNull(genericConfig.getConfig());
        assertNotNull(genericConfig.getConfig().get("users"));

        UserRepresentation user = getRealm().users().search("User A").get(0);

        assertTrue(genericConfig.getConfig().get("users").contains(user.getId()));
    }

    private void assertCreated(AuthorizationResource authorization, UserPolicyRepresentation representation) {
        UserPoliciesResource permissions = authorization.policies().users();
        Response response = permissions.create(representation);
        UserPolicyRepresentation created = response.readEntity(UserPolicyRepresentation.class);
        UserPolicyResource permission = permissions.findById(created.getId());
        assertRepresentation(representation, permission);
    }

    private void assertRepresentation(UserPolicyRepresentation representation, UserPolicyResource permission) {
        UserPolicyRepresentation actual = permission.toRepresentation();
        assertRepresentation(representation, actual, () -> permission.resources(), () -> Collections.emptyList(), () -> permission.associatedPolicies());
        assertEquals(representation.getUsers().size(), actual.getUsers().size());
        assertEquals(0, actual.getUsers().stream().filter(userId -> !representation.getUsers().stream()
                .filter(userName -> getUserName(userId).equalsIgnoreCase(userName))
                .findFirst().isPresent())
                .count());
    }

    private String getUserName(String id) {
        return getRealm().users().get(id).toRepresentation().getUsername();
    }
}
