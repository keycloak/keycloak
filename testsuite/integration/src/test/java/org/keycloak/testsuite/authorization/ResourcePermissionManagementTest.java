/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.authorization;

import org.apache.commons.collections.map.HashedMap;
import org.junit.Test;
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.DefaultEvaluation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourcePermissionManagementTest extends AbstractPhotozAdminTest {

    @Test
    public void testCreateForTypeWithSinglePolicy() throws Exception {
        PolicyRepresentation newPermission = new PolicyRepresentation();

        newPermission.setName("Admin Resource Policy");
        newPermission.setType("resource");

        HashedMap config = new HashedMap();

        config.put("defaultResourceType", "http://photoz.com/admin");
        config.put("applyPolicies", JsonSerialization.writeValueAsString(new String[] {this.administrationPolicy.getId()}));

        newPermission.setConfig(config);

        Response response = newPermissionRequest().post(Entity.entity(newPermission, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        PolicyRepresentation permission = response.readEntity(PolicyRepresentation.class);

        onAuthorizationSession(authorizationProvider -> {
            Policy policyModel = authorizationProvider.getStoreFactory().getPolicyStore().findById(permission.getId());

            assertNotNull(policyModel);
            assertEquals(permission.getId(), policyModel.getId());
            assertEquals(newPermission.getName(), policyModel.getName());
            assertEquals(newPermission.getType(), policyModel.getType());
            assertEquals(resourceServer.getId(), policyModel.getResourceServer().getId());
        });

        Set<String> roles = new HashSet<>();

        roles.add("admin");

        Map<String, DefaultEvaluation> evaluationsAdminRole = performEvaluation(
                Arrays.asList(new ResourcePermission(adminResource, Collections.emptyList(), resourceServer)),
                createAccessToken(roles),
                createClientConnection("127.0.0.1"));

        assertEquals(1, evaluationsAdminRole.size());
        assertTrue(evaluationsAdminRole.containsKey(this.administrationPolicy.getId()));
        assertEquals(Effect.PERMIT, evaluationsAdminRole.get(this.administrationPolicy.getId()).getEffect());

        evaluationsAdminRole = performEvaluation(
                Arrays.asList(new ResourcePermission(adminResource, Collections.emptyList(), resourceServer)),
                createAccessToken(roles),
                createClientConnection("127.0.0.10"));

        assertEquals(1, evaluationsAdminRole.size());
        assertTrue(evaluationsAdminRole.containsKey(this.administrationPolicy.getId()));
        assertEquals(Effect.DENY, evaluationsAdminRole.get(this.administrationPolicy.getId()).getEffect());

        roles.clear();
        roles.add("user");

        Map<String, DefaultEvaluation> evaluationsUserRole = performEvaluation(
                Arrays.asList(new ResourcePermission(adminResource, Collections.emptyList(), resourceServer)),
                createAccessToken(roles),
                createClientConnection("127.0.0.1"));

        assertEquals(1, evaluationsUserRole.size());
        assertTrue(evaluationsUserRole.containsKey(this.administrationPolicy.getId()));
        assertEquals(Effect.DENY, evaluationsUserRole.get(this.administrationPolicy.getId()).getEffect());
    }

    @Test
    public void testCreateForTypeWithMultiplePolicies() throws Exception {
        createAlbumResourceTypePermission();

        HashSet<String> roles = new HashSet<>();

        roles.add("admin");

        Map<String, DefaultEvaluation> evaluationsAdminRole = performEvaluation(
                Arrays.asList(new ResourcePermission(albumResource, Collections.emptyList(), resourceServer)),
                createAccessToken(roles),
                createClientConnection("127.0.0.1"));

        assertEquals(2, evaluationsAdminRole.size());
        assertTrue(evaluationsAdminRole.containsKey(this.administrationPolicy.getId()));
        assertTrue(evaluationsAdminRole.containsKey(this.anyUserPolicy.getId()));
        assertEquals(Effect.DENY, evaluationsAdminRole.get(this.anyUserPolicy.getId()).getEffect());
        assertEquals(Effect.PERMIT, evaluationsAdminRole.get(this.administrationPolicy.getId()).getEffect());

        evaluationsAdminRole = performEvaluation(
                Arrays.asList(new ResourcePermission(albumResource, Collections.emptyList(), resourceServer)),
                createAccessToken(roles),
                createClientConnection("127.0.0.10"));

        assertEquals(2, evaluationsAdminRole.size());
        assertTrue(evaluationsAdminRole.containsKey(this.administrationPolicy.getId()));
        assertTrue(evaluationsAdminRole.containsKey(this.anyUserPolicy.getId()));
        assertEquals(Effect.DENY, evaluationsAdminRole.get(this.anyUserPolicy.getId()).getEffect());
        assertEquals(Effect.DENY, evaluationsAdminRole.get(this.administrationPolicy.getId()).getEffect());

        roles.clear();
        roles.add("user");

        Map<String, DefaultEvaluation> evaluationsUserRole = performEvaluation(
                Arrays.asList(new ResourcePermission(albumResource, Collections.emptyList(), resourceServer)),
                createAccessToken(roles),
                createClientConnection("127.0.0.1"));

        assertEquals(2, evaluationsUserRole.size());
        assertTrue(evaluationsUserRole.containsKey(this.administrationPolicy.getId()));
        assertTrue(evaluationsUserRole.containsKey(this.anyUserPolicy.getId()));
        assertEquals(Effect.PERMIT, evaluationsUserRole.get(this.anyUserPolicy.getId()).getEffect());
        assertEquals(Effect.DENY, evaluationsUserRole.get(this.administrationPolicy.getId()).getEffect());
    }

    @Test
    public void testUpdate() throws Exception {
        PolicyRepresentation permission = createAlbumResourceTypePermission();
        Map<String, String> config = permission.getConfig();

        config.put("applyPolicies", JsonSerialization.writeValueAsString(new String[] {this.anyUserPolicy.getId()}));

        permission.setConfig(config);

        newPermissionRequest(permission.getId()).put(Entity.entity(permission, MediaType.APPLICATION_JSON_TYPE));

        HashSet<String> roles = new HashSet<>();

        roles.add("admin");

        Map<String, DefaultEvaluation> evaluationsAdminRole = performEvaluation(
                Arrays.asList(new ResourcePermission(albumResource, Collections.emptyList(), resourceServer)),
                createAccessToken(roles),
                createClientConnection("127.0.0.1"));

        assertEquals(1, evaluationsAdminRole.size());
        assertTrue(evaluationsAdminRole.containsKey(this.anyUserPolicy.getId()));
        assertEquals(Effect.DENY, evaluationsAdminRole.get(this.anyUserPolicy.getId()).getEffect());
    }

    @Test
    public void testDelete() throws Exception {
        PolicyRepresentation newPermission = createAlbumResourceTypePermission();

        Response delete = newPermissionRequest(newPermission.getId()).delete();

        assertEquals(Status.NO_CONTENT.getStatusCode(), delete.getStatus());
    }

    @Test
    public void testFindById() throws Exception {
        PolicyRepresentation newPermission = createAlbumResourceTypePermission();

        Response response = newPermissionRequest(newPermission.getId()).get();

        PolicyRepresentation permission = response.readEntity(PolicyRepresentation.class);

        assertEquals(newPermission.getId(), permission.getId());
        assertEquals(newPermission.getName(), permission.getName());
        assertEquals(newPermission.getType(), permission.getType());
    }

    @Test
    public void testCreatePolicyForResource() throws Exception {
        PolicyRepresentation newPermission = new PolicyRepresentation();

        newPermission.setName("Multiple Resource Policy");
        newPermission.setType("resource");

        HashedMap config = new HashedMap();

        config.put("resources", JsonSerialization.writeValueAsString(new String[] {this.albumResource.getId(), this.adminResource.getId()}));
        config.put("applyPolicies", JsonSerialization.writeValueAsString(new String[] {this.onlyFromSpecificAddressPolicy.getId()}));

        newPermission.setConfig(config);

        Response response = newPermissionRequest().post(Entity.entity(newPermission, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        List<ResourcePermission> permissions = new ArrayList<>();

        permissions.add(new ResourcePermission(this.albumResource, Collections.emptyList(), this.resourceServer));

        Map<String, DefaultEvaluation> evaluations = performEvaluation(
                permissions,
                createAccessToken(Collections.emptySet()),
                createClientConnection("127.0.0.1"));

        assertEquals(1, evaluations.size());
        assertTrue(evaluations.containsKey(this.onlyFromSpecificAddressPolicy.getId()));
        assertEquals(Effect.PERMIT, evaluations.get(this.onlyFromSpecificAddressPolicy.getId()).getEffect());

        permissions = new ArrayList<>();

        permissions.add(new ResourcePermission(this.adminResource, Collections.emptyList(), this.resourceServer));

        evaluations = performEvaluation(
                permissions,
                createAccessToken(Collections.emptySet()),
                createClientConnection("127.0.0.1"));

        assertEquals(1, evaluations.size());
        assertTrue(evaluations.containsKey(this.onlyFromSpecificAddressPolicy.getId()));
        assertEquals(Effect.PERMIT, evaluations.get(this.onlyFromSpecificAddressPolicy.getId()).getEffect());

        permissions = new ArrayList<>();

        permissions.add(new ResourcePermission(this.adminResource, Collections.emptyList(), this.resourceServer));
        permissions.add(new ResourcePermission(this.albumResource, Collections.emptyList(), this.resourceServer));

        evaluations = performEvaluation(
                permissions,
                createAccessToken(Collections.emptySet()),
                createClientConnection("127.0.0.1"));

        assertEquals(1, evaluations.size());
        assertTrue(evaluations.containsKey(this.onlyFromSpecificAddressPolicy.getId()));
        assertEquals(Effect.PERMIT, evaluations.get(this.onlyFromSpecificAddressPolicy.getId()).getEffect());

        permissions = new ArrayList<>();

        permissions.add(new ResourcePermission(this.adminResource, Collections.emptyList(), this.resourceServer));
        permissions.add(new ResourcePermission(this.albumResource, Collections.emptyList(), this.resourceServer));

        evaluations = performEvaluation(
                permissions,
                createAccessToken(Collections.emptySet()),
                createClientConnection("127.0.0.10"));

        assertEquals(1, evaluations.size());
        assertTrue(evaluations.containsKey(this.onlyFromSpecificAddressPolicy.getId()));
        assertEquals(Effect.DENY, evaluations.get(this.onlyFromSpecificAddressPolicy.getId()).getEffect());
    }

    /**
     * Tests if a resource can inherit the policies defined for another resource based on its type
     *
     * @throws Exception
     */
    @Test
    public void testInheritPoliciesBasedOnResourceType() throws Exception {
        createAlbumResourceTypePermission();
        ResourceRepresentation representation = new ResourceRepresentation();

        representation.setName("Alice Family Album");
        representation.setType(this.albumResource.getType());

        Resource resource = createResource(representation);

        Set<String> roles = new HashSet<>();

        roles.add("user");

        Map<String, DefaultEvaluation> evaluationsUserRole = performEvaluation(
                Arrays.asList(new ResourcePermission(resource, Collections.emptyList(), resourceServer)),
                createAccessToken(roles),
                createClientConnection("127.0.0.1"));

        assertEquals(2, evaluationsUserRole.size());
        assertTrue(evaluationsUserRole.containsKey(this.administrationPolicy.getId()));
        assertTrue(evaluationsUserRole.containsKey(this.anyUserPolicy.getId()));
        assertEquals(Effect.PERMIT, evaluationsUserRole.get(this.anyUserPolicy.getId()).getEffect());
        assertEquals(Effect.DENY, evaluationsUserRole.get(this.administrationPolicy.getId()).getEffect());

        ResourceRepresentation someResourceRep = new ResourceRepresentation();

        someResourceRep.setName("Some Resource");
        someResourceRep.setType("Some non-existent type");

        Resource someResource = createResource(someResourceRep);

        evaluationsUserRole = performEvaluation(
                Arrays.asList(new ResourcePermission(someResource, Collections.emptyList(), resourceServer)),
                createAccessToken(roles),
                createClientConnection("127.0.0.1"));

        // no policies can be applied given that there is no policy defined for this resource or its type
        assertEquals(0, evaluationsUserRole.size());
    }

    private PolicyRepresentation createAlbumResourceTypePermission() throws Exception {
        PolicyRepresentation newPermission = new PolicyRepresentation();

        newPermission.setName("Album Resource Policy");
        newPermission.setType("resource");
        newPermission.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);

        HashedMap config = new HashedMap();

        config.put("defaultResourceType",  albumResource.getType());

        String applyPolicies = JsonSerialization.writeValueAsString(new String[]{this.anyUserPolicy.getId(), this.administrationPolicy.getId()});

        config.put("applyPolicies", applyPolicies);

        newPermission.setConfig(config);

        Response response = newPermissionRequest().post(Entity.entity(newPermission, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        PolicyRepresentation permission = response.readEntity(PolicyRepresentation.class);

        onAuthorizationSession(authorizationProvider -> {
            Policy policyModel = authorizationProvider.getStoreFactory().getPolicyStore().findById(permission.getId());

            assertNotNull(policyModel);
            assertEquals(permission.getId(), policyModel.getId());
            assertEquals(permission.getName(), policyModel.getName());
            assertEquals(permission.getType(), policyModel.getType());
            assertTrue(permission.getConfig().containsValue(albumResource.getType()));
            assertTrue(permission.getConfig().containsValue(applyPolicies));
            assertEquals(resourceServer.getId(), policyModel.getResourceServer().getId());
        });

        return permission;
    }
}
