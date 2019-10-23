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

import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.RulePoliciesResource;
import org.keycloak.admin.client.resource.RulePolicyResource;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.RulePolicyRepresentation;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.RestartContainer;
import org.keycloak.testsuite.util.ContainerAssume;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@EnableFeature(Profile.Feature.AUTHZ_DROOLS_POLICY)
public class RulesPolicyManagementTest extends AbstractPolicyManagementTest {

    @BeforeClass
    public static void verifyEnvironment() {
        ContainerAssume.assumeNotAuthServerUndertow();
        ContainerAssume.assumeNotAuthServerRemote();
    }

    @Test
    public void testCreate() {
        assertCreated(getClient().authorization(), createDefaultRepresentation("Rule Policy"));
    }

    @Test
    public void testUpdate() {
        AuthorizationResource authorization = getClient().authorization();
        RulePolicyRepresentation representation = createDefaultRepresentation("Update Rule Policy");

        assertCreated(authorization, representation);

        representation.setName("changed");
        representation.setDescription("changed");
        representation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        representation.setLogic(Logic.POSITIVE);
        representation.setScannerPeriod("12");
        representation.setScannerPeriodUnit("Days");
        representation.setModuleName("PhotozAuthzContextualPolicy");
        representation.setSessionName("MainContextualSession");

        RulePoliciesResource policies = authorization.policies().rule();
        RulePolicyResource policy = policies.findById(representation.getId());

        policy.update(representation);

        assertRepresentation(representation, policy);
    }

    @Test
    public void testDelete() {
        AuthorizationResource authorization = getClient().authorization();
        RulePolicyRepresentation representation = createDefaultRepresentation("Delete Rule Policy");

        RulePoliciesResource policies = authorization.policies().rule();

        try (Response response = policies.create(representation)) {
            RulePolicyRepresentation created = response.readEntity(RulePolicyRepresentation.class);

            policies.findById(created.getId()).remove();

            RulePolicyResource removed = policies.findById(created.getId());

            try {
                removed.toRepresentation();
                fail("Policy not removed");
            } catch (NotFoundException ignore) {

            }
        }
    }

    private RulePolicyRepresentation createDefaultRepresentation(String name) {
        RulePolicyRepresentation representation = new RulePolicyRepresentation();

        representation.setName(name);
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.setArtifactGroupId("org.keycloak.testsuite");
        representation.setArtifactId("photoz-authz-policy");
        representation.setArtifactVersion(System.getProperty("project.version"));
        representation.setModuleName("PhotozAuthzOwnerPolicy");
        representation.setSessionName("MainOwnerSession");
        representation.setScannerPeriod("1");
        representation.setScannerPeriodUnit("Minutes");

        return representation;
    }

    private void assertCreated(AuthorizationResource authorization, RulePolicyRepresentation representation) {
        RulePoliciesResource permissions = authorization.policies().rule();

        try (Response response = permissions.create(representation)) {
            RulePolicyRepresentation created = response.readEntity(RulePolicyRepresentation.class);
            RulePolicyResource permission = permissions.findById(created.getId());
            assertRepresentation(representation, permission);
        }
    }

    private void assertRepresentation(RulePolicyRepresentation expected, RulePolicyResource policy) {
        RulePolicyRepresentation actual = policy.toRepresentation();
        assertRepresentation(expected, actual, policy::resources, Collections::emptyList, policy::associatedPolicies);
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getLogic(), actual.getLogic());
        assertEquals(expected.getArtifactGroupId(), actual.getArtifactGroupId());
        assertEquals(expected.getArtifactId(), actual.getArtifactId());
        assertEquals(expected.getArtifactVersion(), actual.getArtifactVersion());
        assertEquals(expected.getModuleName(), actual.getModuleName());
        assertEquals(expected.getSessionName(), actual.getSessionName());
        assertEquals(expected.getScannerPeriod(), actual.getScannerPeriod());
        assertEquals(expected.getScannerPeriodUnit(), actual.getScannerPeriodUnit());
    }
}
