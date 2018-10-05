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
package org.keycloak.testsuite.authz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.resource.PermissionResource;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserManagedAccessTest extends AbstractResourceServerTest {

    private ResourceRepresentation resource;

    @Before
    public void configureAuthorization() throws Exception {
        ClientResource client = getClient(getRealm());
        AuthorizationResource authorization = client.authorization();

        JSPolicyRepresentation policy = new JSPolicyRepresentation();

        policy.setName("Only Owner Policy");
        policy.setCode("if ($evaluation.getContext().getIdentity().getId() == $evaluation.getPermission().getResource().getOwner()) {$evaluation.grant();}");

        Response response = authorization.policies().js().create(policy);
        response.close();
    }

    @Test
    public void testOnlyOwnerCanAccess() throws Exception {
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        resource = addResource("Resource A", "marta", true, "ScopeA", "ScopeB");

        permission.setName(resource.getName() + " Permission");
        permission.addResource(resource.getId());
        permission.addPolicy("Only Owner Policy");

        getClient(getRealm()).authorization().permissions().resource().create(permission).close();

        AuthorizationResponse response = authorize("marta", "password", resource.getName(), new String[] {"ScopeA", "ScopeB"});
        String rpt = response.getToken();

        assertNotNull(rpt);
        assertFalse(response.isUpgraded());

        AccessToken accessToken = toAccessToken(rpt);
        AccessToken.Authorization authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        Collection<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, resource.getName(), "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());

        try {
            response = authorize("kolo", "password", resource.getId(), new String[] {"ScopeA", "ScopeB"});
            fail("User should have access to resource from another user");
        } catch (AuthorizationDeniedException ade) {

        }
    }

    /**
     * Makes sure permissions granted to a typed resource instance does not grant access to resource instances with the same type.
     *
     * @throws Exception
     */
    @Test
    public void testOnlyOwnerCanAccessResourceWithType() throws Exception {
        ResourceRepresentation typedResource = addResource("Typed Resource", getClient(getRealm()).toRepresentation().getId(), false, "ScopeA", "ScopeB");

        typedResource.setType("my:resource");

        getClient(getRealm()).authorization().resources().resource(typedResource.getId()).update(typedResource);

        resource = addResource("Resource A", "marta", true, "ScopeA", "ScopeB");

        resource.setType(typedResource.getType());

        getClient(getRealm()).authorization().resources().resource(resource.getId()).update(resource);

        ResourceRepresentation resourceB = addResource("Resource B", "marta", true, "ScopeA", "ScopeB");

        resourceB.setType(typedResource.getType());

        getClient(getRealm()).authorization().resources().resource(resourceB.getId()).update(resourceB);

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(resource.getType() + " Permission");
        permission.setResourceType(resource.getType());
        permission.addPolicy("Only Owner Policy");

        getClient(getRealm()).authorization().permissions().resource().create(permission).close();

        AuthorizationResponse response = authorize("marta", "password", resource.getName(), new String[] {"ScopeA", "ScopeB"});
        String rpt = response.getToken();

        assertNotNull(rpt);
        assertFalse(response.isUpgraded());

        AccessToken accessToken = toAccessToken(rpt);
        AccessToken.Authorization authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        Collection<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, resource.getName(), "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());

        try {
            response = authorize("kolo", "password", resource.getId(), new String[] {"ScopeA", "ScopeB"});
            fail("User should not have access to resource from another user");
        } catch (AuthorizationDeniedException ade) {

        }

        List<PermissionTicketRepresentation> tickets = getAuthzClient().protection().permission().find(resource.getId(), null, null, null, null, null, null, null);

        for (PermissionTicketRepresentation ticket : tickets) {
            ticket.setGranted(true);
            getAuthzClient().protection().permission().update(ticket);
        }

        try {
            response = authorize("kolo", "password", resource.getId(), new String[] {"ScopeA", "ScopeB"});
        } catch (AuthorizationDeniedException ade) {
            fail("User should have access to resource from another user");
        }

        permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, resource.getName(), "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());

        try {
            response = authorize("kolo", "password", resourceB.getId(), new String[] {"ScopeA", "ScopeB"});
            fail("User should not have access to resource from another user");
        } catch (AuthorizationDeniedException ade) {

        }
    }

    @Test
    public void testUserGrantsAccessToResource() throws Exception {
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        resource = addResource("Resource A", "marta", true, "ScopeA", "ScopeB");

        permission.setName(resource.getName() + " Permission");
        permission.addResource(resource.getId());
        permission.addPolicy("Only Owner Policy");

        getClient(getRealm()).authorization().permissions().resource().create(permission).close();

        AuthorizationResponse response = authorize("marta", "password", "Resource A", new String[] {"ScopeA", "ScopeB"});
        String rpt = response.getToken();

        assertNotNull(rpt);
        assertFalse(response.isUpgraded());

        AccessToken accessToken = toAccessToken(rpt);
        AccessToken.Authorization authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        Collection<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());

        try {
            response = authorize("kolo", "password", resource.getId(), new String[] {});
            fail("User should have access to resource from another user");
        } catch (AuthorizationDeniedException ade) {

        }

        PermissionResource permissionResource = getAuthzClient().protection().permission();
        List<PermissionTicketRepresentation> permissionTickets = permissionResource.findByResource(resource.getId());

        assertFalse(permissionTickets.isEmpty());
        assertEquals(2, permissionTickets.size());

        for (PermissionTicketRepresentation ticket : permissionTickets) {
            assertFalse(ticket.isGranted());

            ticket.setGranted(true);

            permissionResource.update(ticket);
        }

        permissionTickets = permissionResource.findByResource(resource.getId());

        assertFalse(permissionTickets.isEmpty());
        assertEquals(2, permissionTickets.size());

        for (PermissionTicketRepresentation ticket : permissionTickets) {
            assertTrue(ticket.isGranted());
        }

        response = authorize("kolo", "password", resource.getId(), new String[] {"ScopeA", "ScopeB"});
        rpt = response.getToken();

        assertNotNull(rpt);
        assertFalse(response.isUpgraded());

        accessToken = toAccessToken(rpt);
        authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, resource.getName(), "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testUserGrantsAccessToResourceWithoutScopes() throws Exception {
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        resource = addResource("Resource A", "marta", true);

        permission.setName(resource.getName() + " Permission");
        permission.addResource(resource.getId());
        permission.addPolicy("Only Owner Policy");

        getClient(getRealm()).authorization().permissions().resource().create(permission).close();

        AuthorizationResponse response = authorize("marta", "password", "Resource A", new String[] {});
        String rpt = response.getToken();

        assertNotNull(rpt);
        assertFalse(response.isUpgraded());

        AccessToken accessToken = toAccessToken(rpt);
        AccessToken.Authorization authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        Collection<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A");
        assertTrue(permissions.isEmpty());

        try {
            response = authorize("kolo", "password", resource.getId(), new String[] {});
            fail("User should have access to resource from another user");
        } catch (AuthorizationDeniedException ade) {

        }

        PermissionResource permissionResource = getAuthzClient().protection().permission();
        List<PermissionTicketRepresentation> permissionTickets = permissionResource.findByResource(resource.getId());

        assertFalse(permissionTickets.isEmpty());
        assertEquals(1, permissionTickets.size());

        for (PermissionTicketRepresentation ticket : permissionTickets) {
            assertFalse(ticket.isGranted());

            ticket.setGranted(true);

            permissionResource.update(ticket);
        }

        permissionTickets = permissionResource.findByResource(resource.getId());

        assertFalse(permissionTickets.isEmpty());
        assertEquals(1, permissionTickets.size());

        for (PermissionTicketRepresentation ticket : permissionTickets) {
            assertTrue(ticket.isGranted());
        }

        response = authorize("kolo", "password", resource.getId(), new String[] {});
        rpt = response.getToken();

        assertNotNull(rpt);
        assertFalse(response.isUpgraded());

        accessToken = toAccessToken(rpt);
        authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, resource.getName());
        assertTrue(permissions.isEmpty());

        response = authorize("kolo", "password", resource.getId(), new String[] {});
        rpt = response.getToken();

        assertNotNull(rpt);
        assertFalse(response.isUpgraded());

        accessToken = toAccessToken(rpt);
        authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, resource.getName());
        assertTrue(permissions.isEmpty());

        permissionTickets = permissionResource.findByResource(resource.getId());

        assertFalse(permissionTickets.isEmpty());
        assertEquals(1, permissionTickets.size());

        for (PermissionTicketRepresentation ticket : permissionTickets) {
            assertTrue(ticket.isGranted());
        }
    }

    @Test
    public void testUserGrantsAccessToScope() throws Exception {
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        resource = addResource("Resource A", "marta", true, "ScopeA", "ScopeB");

        permission.setName(resource.getName() + " Permission");
        permission.addResource(resource.getId());
        permission.addPolicy("Only Owner Policy");

        getClient(getRealm()).authorization().permissions().resource().create(permission).close();

        AuthorizationResponse response = authorize("marta", "password", "Resource A", new String[] {"ScopeA", "ScopeB"});
        String rpt = response.getToken();

        assertNotNull(rpt);
        assertFalse(response.isUpgraded());

        AccessToken accessToken = toAccessToken(rpt);
        AccessToken.Authorization authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        Collection<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A", "ScopeA", "ScopeB");
        assertTrue(permissions.isEmpty());

        try {
            response = authorize("kolo", "password", resource.getId(), new String[] {"ScopeA"});
            fail("User should not have access to resource from another user");
        } catch (AuthorizationDeniedException ade) {

        }

        PermissionResource permissionResource = getAuthzClient().protection().permission();
        List<PermissionTicketRepresentation> permissionTickets = permissionResource.findByResource(resource.getId());

        assertFalse(permissionTickets.isEmpty());
        assertEquals(1, permissionTickets.size());

        PermissionTicketRepresentation ticket = permissionTickets.get(0);
        assertFalse(ticket.isGranted());

        ticket.setGranted(true);

        permissionResource.update(ticket);

        response = authorize("kolo", "password", resource.getId(), new String[] {"ScopeA", "ScopeB"});
        rpt = response.getToken();

        assertNotNull(rpt);
        assertFalse(response.isUpgraded());

        accessToken = toAccessToken(rpt);
        authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, resource.getName(), "ScopeA");
        assertTrue(permissions.isEmpty());

        permissionTickets = permissionResource.findByResource(resource.getId());

        assertFalse(permissionTickets.isEmpty());
        // must have two permission tickets, one persisted during the first authorize call for ScopeA and another for the second call to authorize for ScopeB
        assertEquals(2, permissionTickets.size());

        for (PermissionTicketRepresentation representation : new ArrayList<>(permissionTickets)) {
            if (representation.isGranted()) {
                permissionTickets.remove(representation);
            }
        }

        assertEquals(1, permissionTickets.size());
    }

    @Test
    public void testPermissiveModePermissions() throws Exception {
        resource = addResource("Resource A");

        try {
            authorize("kolo", "password", resource.getId(), null);
            fail("Access should be denied, server in enforcing mode");
        } catch (AuthorizationDeniedException ade) {

        }

        AuthorizationResource authorizationResource = getClient(getRealm()).authorization();
        ResourceServerRepresentation settings = authorizationResource.getSettings();

        settings.setPolicyEnforcementMode(PolicyEnforcementMode.PERMISSIVE);

        authorizationResource.update(settings);

        AuthorizationResponse response = authorize("marta", "password", "Resource A", null);
        String rpt = response.getToken();

        assertNotNull(rpt);
        assertFalse(response.isUpgraded());

        AccessToken accessToken = toAccessToken(rpt);
        AccessToken.Authorization authorization = accessToken.getAuthorization();

        assertNotNull(authorization);

        Collection<Permission> permissions = authorization.getPermissions();

        assertNotNull(permissions);
        assertPermissions(permissions, "Resource A");
        assertTrue(permissions.isEmpty());
    }
}
