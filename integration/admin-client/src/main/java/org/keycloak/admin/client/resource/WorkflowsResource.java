package org.keycloak.admin.client.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.workflows.WorkflowRepresentation;

import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;

/**
 * @since Keycloak server 26.4.0. All the child endpoints are also available since that version<p>
 *
 * This endpoint including all the child endpoints require feature {@link org.keycloak.common.Profile.Feature#WORKFLOWS} to be enabled. Note that feature is experimental in 26.4.0 and there might be
 * backwards incompatible changes in the future versions of admin-client and Keycloak server<p>
 */
public interface WorkflowsResource {

    @POST
    @Consumes({MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML})
    Response create(WorkflowRepresentation representation);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<WorkflowRepresentation> list();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<WorkflowRepresentation> list(
            @QueryParam("search") String search,
            @QueryParam("exact") Boolean exact,
            @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults
    );

    @Path("{id}")
    WorkflowResource workflow(@PathParam("id") String id);
}
