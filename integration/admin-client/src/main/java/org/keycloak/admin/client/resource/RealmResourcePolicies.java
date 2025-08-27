package org.keycloak.admin.client.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;

public interface RealmResourcePolicies {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(ResourcePolicyRepresentation representation);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(List<ResourcePolicyRepresentation> representation);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ResourcePolicyRepresentation> list();

    @Path("{id}")
    RealmResourcePolicy policy(@PathParam("id") String id);
}
