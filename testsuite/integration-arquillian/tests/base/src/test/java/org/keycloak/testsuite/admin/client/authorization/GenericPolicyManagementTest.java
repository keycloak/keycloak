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

import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
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
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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

    private static final String[] EXPECTED_BUILTIN_POLICY_PROVIDERS = {"test", "user", "role", "time", "aggregate", "scope", "resource"};

    @Test
    public void testCreate() {
        PolicyRepresentation newPolicy = createTestingPolicy().toRepresentation();

        assertEquals("Test Generic Policy", newPolicy.getName());
        assertEquals("scope", newPolicy.getType());
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
        PolicyRepresentation policy = policyResource.toRepresentation();

        policy.setName("changed");
        policy.setLogic(Logic.NEGATIVE);
        policy.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        policy.getConfig().put("configA", "changed configuration for A");
        policy.getConfig().remove("configB");
        policy.getConfig().put("configC", "changed configuration for C");

        policyResource.update(policy);

        policy = policyResource.toRepresentation();

        assertEquals("changed", policy.getName());
        assertEquals(Logic.NEGATIVE, policy.getLogic());

        assertEquals(DecisionStrategy.AFFIRMATIVE, policy.getDecisionStrategy());
        assertEquals("changed configuration for A", policy.getConfig().get("configA"));
        assertNull(policy.getConfig().get("configB"));
        assertEquals("changed configuration for C", policy.getConfig().get("configC"));

        Map<String, String> config = policy.getConfig();

        config.put("applyPolicies", buildConfigOption(findPolicyByName("Test Associated C").getId()));

        config.put("resources", buildConfigOption(findResourceByName("Test Resource B").getId()));

        config.put("scopes", buildConfigOption(findScopeByName("Test Scope A").getId()));

        policyResource.update(policy);

        policy = policyResource.toRepresentation();
        config = policy.getConfig();

        assertAssociatedPolicy("Test Associated C", policy);
        List<PolicyRepresentation> associatedPolicies = getClientResource().authorization().policies().policy(policy.getId()).associatedPolicies();
        assertFalse(associatedPolicies.stream().filter(associated -> associated.getId().equals(findPolicyByName("Test Associated A").getId())).findFirst().isPresent());
        assertFalse(associatedPolicies.stream().filter(associated -> associated.getId().equals(findPolicyByName("Test Associated B").getId())).findFirst().isPresent());

        assertAssociatedResource("Test Resource B", policy);
        List<ResourceRepresentation> resources = policyResource.resources();
        assertFalse(resources.contains(findResourceByName("Test Resource A")));
        assertFalse(resources.contains(findResourceByName("Test Resource C")));

        assertAssociatedScope("Test Scope A", policy);
        List<ScopeRepresentation> scopes = getClientResource().authorization().policies().policy(policy.getId()).scopes();
        assertFalse(scopes.contains(findScopeByName("Test Scope B").getId()));
        assertFalse(scopes.contains(findScopeByName("Test Scope C").getId()));
    }

    @Test
    public void testDefaultPolicyProviders() {
        List<String> providers = getClientResource().authorization().policies()
                .policyProviders().stream().map(PolicyProviderRepresentation::getType).collect(Collectors.toList());

        assertFalse(providers.isEmpty());
        List expected = new ArrayList(Arrays.asList(EXPECTED_BUILTIN_POLICY_PROVIDERS));

        assertTrue(providers.containsAll(expected));
    }

    @Test
    public void testQueryPolicyByIdAllFields() {
        PolicyResource policy = createTestingPolicy();
        PolicyRepresentation representation = policy.toRepresentation("*");
        Set<ResourceRepresentation> resources = representation.getResourcesData();
        
        assertEquals(3, resources.size());

        representation = policy.toRepresentation();
        assertNull(representation.getResourcesData());
    }

    @Test
    public void testQueryPolicyAllFields() {
        AuthorizationResource authorization = getClientResource().authorization();

        authorization.resources().create(new ResourceRepresentation("Resource A"));
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        permission.setName("Permission A");
        permission.addResource("Resource A");
        authorization.permissions().resource().create(permission);
        
        List<PolicyRepresentation> policies = authorization.policies()
                .policies(null, "Permission A", null, null, null, true, null, "*", -1, -1);
        
        assertEquals(1, policies.size());
        assertEquals(1, policies.get(0).getResourcesData().size());

        policies = authorization.policies()
                .policies(null, "Permission A", null, null, null, true, null, null, -1, -1);

        assertEquals(1, policies.size());
        assertNull(policies.get(0).getResourcesData());
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
        newPolicy.setType("scope");
        newPolicy.setConfig(config);

        PoliciesResource policies = getClientResource().authorization().policies();

        try (Response response = policies.create(newPolicy)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            PolicyRepresentation stored = response.readEntity(PolicyRepresentation.class);

            return policies.policy(stored.getId());
        }
    }

    private ResourceResource createResource(String name) {
        ResourceRepresentation newResource = new ResourceRepresentation();

        newResource.setName(name);

        ResourcesResource resources = getClientResource().authorization().resources();

        try (Response response = resources.create(newResource)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            ResourceRepresentation stored = response.readEntity(ResourceRepresentation.class);

            return resources.resource(stored.getId());
        }
    }

    private ResourceScopeResource createScope(String name) {
        ScopeRepresentation newScope = new ScopeRepresentation();

        newScope.setName(name);

        ResourceScopesResource scopes = getClientResource().authorization().scopes();

        try (Response response = scopes.create(newScope)) {

            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            ScopeRepresentation stored = response.readEntity(ScopeRepresentation.class);

            return scopes.scope(stored.getId());
        }
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
        PoliciesResource policies = getClientResource().authorization().policies();
        associatedPolicy = policies.policy(associatedPolicy.getId()).toRepresentation();
        assertNotNull(associatedPolicy);
        PolicyRepresentation finalAssociatedPolicy = associatedPolicy;
        PolicyResource policyResource = policies.policy(dependentPolicy.getId());
        List<PolicyRepresentation> associatedPolicies = policyResource.associatedPolicies();
        assertTrue(associatedPolicies.stream().filter(associated -> associated.getId().equals(finalAssociatedPolicy.getId())).findFirst().isPresent());
        List<PolicyRepresentation> dependentPolicies = policies.policy(associatedPolicy.getId()).dependentPolicies();
        assertEquals(1, dependentPolicies.size());
        assertEquals(dependentPolicy.getId(), dependentPolicies.get(0).getId());
    }

    private void assertAssociatedResource(String resourceName, PolicyRepresentation policy) {
        ResourceRepresentation resource = findResourceByName(resourceName);
        assertNotNull(resource);
        List<ResourceRepresentation> resources = getClientResource().authorization().policies().policy(policy.getId()).resources();
        assertTrue(resources.contains(resource));
        List<PolicyRepresentation> policies = getClientResource().authorization().resources().resource(resource.getId()).permissions();
        assertEquals(1, policies.size());
        assertTrue(policies.stream().map(PolicyRepresentation::getId).collect(Collectors.toList())
                .contains(policy.getId()));
    }

    private void assertAssociatedScope(String scopeName, PolicyRepresentation policy) {
        ScopeRepresentation scope =  findScopeByName(scopeName);
        scope = getClientResource().authorization().scopes().scope(scope.getId()).toRepresentation();
        assertNotNull(scope);
        List<ScopeRepresentation> scopes = getClientResource().authorization().policies().policy(policy.getId()).scopes();
        assertTrue(scopes.stream().map((Function<ScopeRepresentation, String>) rep -> rep.getId()).collect(Collectors.toList()).contains(scope.getId()));
        List<PolicyRepresentation> permissions = getClientResource().authorization().scopes().scope(scope.getId()).permissions();
        assertEquals(1, permissions.size());
        assertTrue(permissions.stream().map(PolicyRepresentation::getId).collect(Collectors.toList())
                .contains(policy.getId()));
    }
}
