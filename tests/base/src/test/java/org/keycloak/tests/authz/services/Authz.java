package org.keycloak.tests.authz.services;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.PoliciesResource;
import org.keycloak.admin.client.resource.ResourcePermissionsResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testframework.realm.ManagedClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class Authz {

    public static PolicyRepresentation createAlwaysGrantPolicy(ManagedClient resourceServer) {
        AuthorizationResource authorization = resourceServer.admin().authorization();
        PoliciesResource policiesApi = authorization.policies();
        PolicyRepresentation policy = policiesApi.findByName("Always Grant");

        if (policy != null) {
            return policy;
        }

        PolicyRepresentation expected = new PolicyRepresentation();

        expected.setName("Always Grant");
        expected.setType("always-grant");

        try (Response response = policiesApi.create(expected)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }

        policy = policiesApi.findByName(expected.getName());

        assertNotNull(policy);

        return policy;
    }

    public static ResourceRepresentation create(ManagedClient resourceServer, ResourceRepresentation resource) {
        AuthorizationResource authorization = resourceServer.admin().authorization();
        ResourcesResource resourcesApi = authorization.resources();

        try (Response response = resourcesApi.create(resource)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }

        List<ResourceRepresentation> resources = resourcesApi.findByName(resource.getName());

        assertEquals(1, resources.size());

        return resources.get(0);
    }

    public static ResourcePermissionRepresentation create(ManagedClient resourceServer, ResourcePermissionRepresentation expected) {
        AuthorizationResource authorization = resourceServer.admin().authorization();
        ResourcePermissionsResource permissionsApi = authorization.permissions().resource();

        try (Response response = permissionsApi.create(expected)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }

        ResourcePermissionRepresentation policy = permissionsApi.findByName(expected.getName());

        assertNotNull(policy);

        return policy;
    }
}
