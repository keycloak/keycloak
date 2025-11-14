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
package org.keycloak.testsuite.authz.admin;

import java.util.Collections;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AggregatePoliciesResource;
import org.keycloak.admin.client.resource.AggregatePolicyResource;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.UserPoliciesResource;
import org.keycloak.admin.client.resource.UserPolicyResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AggregatePolicyManagementTest extends AbstractPolicyManagementTest {

    @Override
    protected RealmBuilder createTestRealm() {
        return super.createTestRealm()
                .user(UserBuilder.create().username("AggregatePolicyManagementTestUser"));
    }

    @Test
    public void testCreate() {
        AuthorizationResource authorization = getClient().authorization();
        AggregatePolicyRepresentation representation = new AggregatePolicyRepresentation();

        representation.setName("Aggregate Policy");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.addPolicy("Only Marta Policy", "Only Kolo Policy");

        assertCreated(authorization, representation);
    }

    @Test
    public void testUpdate() {
        AuthorizationResource authorization = getClient().authorization();
        AggregatePolicyRepresentation representation = new AggregatePolicyRepresentation();

        representation.setName("Update Aggregate Policy");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.addPolicy("Only Marta Policy", "Only Kolo Policy");

        assertCreated(authorization, representation);

        representation.setName("changed");
        representation.setDescription("changed");
        representation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        representation.setLogic(Logic.POSITIVE);
        representation.getPolicies().clear();
        representation.addPolicy("Only Kolo Policy");

        AggregatePoliciesResource policies = authorization.policies().aggregate();
        AggregatePolicyResource policy = policies.findById(representation.getId());

        policy.update(representation);
        assertRepresentation(representation, policy);
    }

    @Test
    public void testDelete() {
        AuthorizationResource authorization = getClient().authorization();
        AggregatePolicyRepresentation representation = new AggregatePolicyRepresentation();

        representation.setName("Test Delete Policy");
        representation.addPolicy("Only Marta Policy");

        AggregatePoliciesResource policies = authorization.policies().aggregate();

        try (Response response = policies.create(representation)) {
            AggregatePolicyRepresentation created = response.readEntity(AggregatePolicyRepresentation.class);

            policies.findById(created.getId()).remove();

            AggregatePolicyResource removed = policies.findById(created.getId());

            try {
                removed.toRepresentation();
                fail("Policy not removed");
            } catch (NotFoundException ignore) {

            }
        }
    }

    //Issue #24651
    @Test
    public void testDeleteUser() {
        AuthorizationResource authorization = getClient().authorization();

        UsersResource users = getRealm().users();
        UserRepresentation user = users.search("AggregatePolicyManagementTestUser").get(0);

        UserPolicyRepresentation userPolicyRepresentation = new UserPolicyRepresentation();
        userPolicyRepresentation.setName("AggregatePolicyManagementTestUser Only");
        userPolicyRepresentation.setDescription("description");
        userPolicyRepresentation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        userPolicyRepresentation.setLogic(Logic.NEGATIVE);
        userPolicyRepresentation.addUser(user.getId());
        authorization.policies().user().create(userPolicyRepresentation);

        TimePolicyRepresentation timePolicyRepresentation = new TimePolicyRepresentation();
        timePolicyRepresentation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        timePolicyRepresentation.setLogic(Logic.NEGATIVE);
        timePolicyRepresentation.setName("Dayshift");
        timePolicyRepresentation.setHour("8");
        timePolicyRepresentation.setHourEnd("17");
        authorization.policies().time().create(timePolicyRepresentation);

        AggregatePolicyRepresentation representation = new AggregatePolicyRepresentation();
        representation.setName("AggregatePolicyManagementTestUser Only during dayshift");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.addPolicy("AggregatePolicyManagementTestUser Only", "Dayshift");
        assertCreated(authorization, representation);

        users.get(user.getId()).remove();

        UserPolicyRepresentation actualUserPolicy = authorization.policies().user().findByName(userPolicyRepresentation.getName());
        assertEquals(0, actualUserPolicy.getUsers().size());

        AggregatePolicyResource actual = authorization.policies().aggregate().findById(representation.getId());
        assertRepresentation(representation, actual);
    }

    private void assertCreated(AuthorizationResource authorization, AggregatePolicyRepresentation representation) {
        AggregatePoliciesResource permissions = authorization.policies().aggregate();
        try (Response response = permissions.create(representation)) {
            AggregatePolicyRepresentation created = response.readEntity(AggregatePolicyRepresentation.class);
            AggregatePolicyResource permission = permissions.findById(created.getId());
            assertRepresentation(representation, permission);
        }
    }

    private void assertCreated(AuthorizationResource authorization, UserPolicyRepresentation representation) {
        UserPoliciesResource permissions = authorization.policies().user();

        try (Response response = permissions.create(representation)) {
            UserPolicyRepresentation created = response.readEntity(UserPolicyRepresentation.class);
            UserPolicyResource permission = permissions.findById(created.getId());
            assertRepresentation(representation, permission);
        }
    }

    private void assertRepresentation(AggregatePolicyRepresentation representation, AggregatePolicyResource policy) {
        AggregatePolicyRepresentation actual = policy.toRepresentation();
        assertRepresentation(representation, actual, () -> policy.resources(), () -> Collections.emptyList(), () -> policy.associatedPolicies());
    }

    private void assertRepresentation(UserPolicyRepresentation representation, UserPolicyResource permission) {
        UserPolicyRepresentation actual = permission.toRepresentation();
        assertRepresentation(representation, actual, () -> permission.resources(), () -> Collections.emptyList(), () -> permission.associatedPolicies());
        assertEquals(representation.getUsers().size(), actual.getUsers().size());
        assertEquals(0, actual.getUsers().stream().filter(userId -> !representation.getUsers().stream()
                        .filter(userName -> getRealm().users().get(userId).toRepresentation().getUsername().equalsIgnoreCase(userName))
                        .findFirst().isPresent())
                .count());
    }

}
