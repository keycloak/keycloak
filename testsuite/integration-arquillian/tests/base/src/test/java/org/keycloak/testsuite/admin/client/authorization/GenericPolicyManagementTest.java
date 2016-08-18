/*
  Copyright 2016 Red Hat, Inc. and/or its affiliates
  and other contributors as indicated by the @author tags.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

 */
package org.keycloak.testsuite.admin.client.authorization;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.PoliciesResource;
import org.keycloak.admin.client.resource.PolicyResource;
import org.keycloak.admin.client.resource.ResourceResource;
import org.keycloak.admin.client.resource.ResourceScopeResource;
import org.keycloak.admin.client.resource.ResourceScopesResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyProviderRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class GenericPolicyManagementTest extends AbstractAuthorizationTest {

    private static final String[] EXPECTED_BUILTIN_POLICY_PROVIDERS = {"test", "user", "role", "drools", "js", "time", "aggregate", "scope", "resource"};

    @Before
    @Override
    public void onBeforeAuthzTests() {
        super.onBeforeAuthzTests();
        enableAuthorizationServices();
    }

    @After
    @Override
    public void onAfterAuthzTests() {
        super.onAfterAuthzTests();
    }

    @Test
    public void testCreate() {
        PolicyRepresentation newPolicy = createTestingPolicy().toRepresentation();

        assertEquals("Test Generic Policy", newPolicy.getName());
        assertEquals("test", newPolicy.getType());
        assertEquals(Logic.POSITIVE, newPolicy.getLogic());
        assertEquals(DecisionStrategy.UNANIMOUS, newPolicy.getDecisionStrategy());
        assertEquals("configuration for A", newPolicy.getConfig().get("configA"));
        assertEquals("configuration for B", newPolicy.getConfig().get("configB"));
        assertEquals("configuration for C", newPolicy.getConfig().get("configC"));

        List<PolicyRepresentation> policies = getClientResource().authorization().policies().policies();

        assertEquals(6, policies.size());

        assertAssociatedPolicy("Test Associated A", newPolicy);
        assertAssociatedPolicy("Test Associated B", newPolicy);
        assertAssociatedPolicy("Test Associated C", newPolicy);

        assertAssociatedResource("Test Resource A", newPolicy);
        assertAssociatedResource("Test Resource B", newPolicy);
        assertAssociatedResource("Test Resource C", newPolicy);

        assertAssociatedScope("Test Scope A", newPolicy);
        assertAssociatedScope("Test Scope B", newPolicy);
        assertAssociatedScope("Test Scope C", newPolicy);
    }

    @Test
    public void testUpdate() {
        PolicyResource policyResource = createTestingPolicy();
        PolicyRepresentation resource = policyResource.toRepresentation();

        resource.setName("changed");
        resource.setLogic(Logic.NEGATIVE);
        resource.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        resource.getConfig().put("configA", "changed configuration for A");
        resource.getConfig().remove("configB");
        resource.getConfig().put("configC", "changed configuration for C");

        policyResource.update(resource);

        resource = policyResource.toRepresentation();

        assertEquals("changed", resource.getName());
        assertEquals(Logic.NEGATIVE, resource.getLogic());

        assertEquals(DecisionStrategy.AFFIRMATIVE, resource.getDecisionStrategy());
        assertEquals("changed configuration for A", resource.getConfig().get("configA"));
        assertNull(resource.getConfig().get("configB"));
        assertEquals("changed configuration for C", resource.getConfig().get("configC"));

        Map<String, String> config = resource.getConfig();

        config.put("applyPolicies", buildConfigOption(findPolicyByName("Test Associated C").getId()));

        config.put("resources", buildConfigOption(findResourceByName("Test Resource B").getId()));

        config.put("scopes", buildConfigOption(findScopeByName("Test Scope A").getId()));

        policyResource.update(resource);

        resource = policyResource.toRepresentation();
        config = resource.getConfig();

        assertAssociatedPolicy("Test Associated C", resource);
        assertFalse(config.get("applyPolicies").contains(findPolicyByName("Test Associated A").getId()));
        assertFalse(config.get("applyPolicies").contains(findPolicyByName("Test Associated B").getId()));

        assertAssociatedResource("Test Resource B", resource);
        assertFalse(config.get("resources").contains(findResourceByName("Test Resource A").getId()));
        assertFalse(config.get("resources").contains(findResourceByName("Test Resource C").getId()));

        assertAssociatedScope("Test Scope A", resource);
        assertFalse(config.get("scopes").contains(findScopeByName("Test Scope B").getId()));
        assertFalse(config.get("scopes").contains(findScopeByName("Test Scope C").getId()));
    }

    @Test
    public void testDefaultPolicyProviders() {
        List<String> providers = getClientResource().authorization().policies()
                .policyProviders().stream().map(PolicyProviderRepresentation::getType).collect(Collectors.toList());

        assertFalse(providers.isEmpty());
        assertTrue(providers.containsAll(Arrays.asList(EXPECTED_BUILTIN_POLICY_PROVIDERS)));
    }

    private PolicyResource createTestingPolicy() {
        Map<String, String> config = new HashMap<>();

        config.put("configA", "configuration for A");
        config.put("configB", "configuration for B");
        config.put("configC", "configuration for C");

        config.put("applyPolicies", buildConfigOption(
                createPolicy("Test Associated A", new HashMap<>()).toRepresentation().getId(),
                createPolicy("Test Associated B", new HashMap<>()).toRepresentation().getId(),
                createPolicy("Test Associated C", new HashMap<>()).toRepresentation().getId()
        ));

        config.put("resources", buildConfigOption(
                createResource("Test Resource A").toRepresentation().getId(),
                createResource("Test Resource B").toRepresentation().getId(),
                createResource("Test Resource C").toRepresentation().getId()
        ));

        config.put("scopes", buildConfigOption(
                createScope("Test Scope A").toRepresentation().getId(),
                createScope("Test Scope B").toRepresentation().getId(),
                createScope("Test Scope C").toRepresentation().getId()
        ));

        return createPolicy("Test Generic Policy", config);
    }

    private PolicyResource createPolicy(String name, Map<String, String> config) {
        PolicyRepresentation newPolicy = new PolicyRepresentation();

        newPolicy.setName(name);
        newPolicy.setType("test");
        newPolicy.setConfig(config);

        PoliciesResource policies = getClientResource().authorization().policies();
        Response response = policies.create(newPolicy);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        PolicyRepresentation stored = response.readEntity(PolicyRepresentation.class);

        return policies.policy(stored.getId());
    }

    private ResourceResource createResource(String name) {
        ResourceRepresentation newResource = new ResourceRepresentation();

        newResource.setName(name);

        ResourcesResource resources = getClientResource().authorization().resources();

        Response response = resources.create(newResource);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        ResourceRepresentation stored = response.readEntity(ResourceRepresentation.class);

        return resources.resource(stored.getId());
    }

    private ResourceScopeResource createScope(String name) {
        ScopeRepresentation newScope = new ScopeRepresentation();

        newScope.setName(name);

        ResourceScopesResource scopes = getClientResource().authorization().scopes();

        Response response = scopes.create(newScope);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        ScopeRepresentation stored = response.readEntity(ScopeRepresentation.class);

        return scopes.scope(stored.getId());
    }

    private String buildConfigOption(String... values) {
        StringBuilder builder = new StringBuilder();

        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append("\"" + value + "\"");
        }

        return builder.insert(0, "[").append("]").toString();
    }

    private PolicyRepresentation findPolicyByName(String name) {
        return getClientResource().authorization().policies().policies()
                .stream().filter(policyRepresentation -> policyRepresentation.getName().equals(name))
                .findFirst().orElse(null);
    }

    private ResourceRepresentation findResourceByName(String name) {
        return getClientResource().authorization().resources().resources()
                .stream().filter(resource -> resource.getName().equals(name))
                .findFirst().orElse(null);
    }

    private ScopeRepresentation findScopeByName(String name) {
        return getClientResource().authorization().scopes().scopes()
                .stream().filter(scope -> scope.getName().equals(name))
                .findFirst().orElse(null);
    }

    private void assertAssociatedPolicy(String associatedPolicyName, PolicyRepresentation dependentPolicy) {
        PolicyRepresentation associatedPolicy = findPolicyByName(associatedPolicyName);
        assertNotNull(associatedPolicy);
        assertTrue(dependentPolicy.getConfig().get("applyPolicies").contains(associatedPolicy.getId()));
        assertEquals(1, associatedPolicy.getDependentPolicies().size());
        assertEquals(dependentPolicy.getId(), associatedPolicy.getDependentPolicies().get(0).getId());
    }

    private void assertAssociatedResource(String resourceName, PolicyRepresentation policy) {
        ResourceRepresentation resource = findResourceByName(resourceName);
        assertNotNull(resource);
        assertTrue(policy.getConfig().get("resources").contains(resource.getId()));
        assertEquals(1, resource.getPolicies().size());
        assertTrue(resource.getPolicies().stream().map(PolicyRepresentation::getId).collect(Collectors.toList())
                .contains(policy.getId()));
    }

    private void assertAssociatedScope(String scopeName, PolicyRepresentation policy) {
        ScopeRepresentation scope =  findScopeByName(scopeName);
        assertNotNull(scope);
        assertTrue(policy.getConfig().get("scopes").contains(scope.getId()));
        assertEquals(1, scope.getPolicies().size());
        assertTrue(scope.getPolicies().stream().map(PolicyRepresentation::getId).collect(Collectors.toList())
                .contains(policy.getId()));
    }
}
