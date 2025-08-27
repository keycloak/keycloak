package org.keycloak.admin.client.resource;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;

public interface RealmResourcePolicy {

    @DELETE
    Response delete();

    @PUT
    @Consumes(APPLICATION_JSON)
    Response update(ResourcePolicyRepresentation policy);

    @GET
    @Produces(APPLICATION_JSON)
    ResourcePolicyRepresentation toRepresentation();
}
