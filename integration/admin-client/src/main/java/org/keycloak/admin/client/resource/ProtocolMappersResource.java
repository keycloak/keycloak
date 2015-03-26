package org.keycloak.admin.client.resource;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.keycloak.representations.idm.ProtocolMapperRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ProtocolMappersResource {

    @GET
    @Path("protocol/{protocol}")
    @Produces("application/json")
    public List<ProtocolMapperRepresentation> getMappersPerProtocol(@PathParam("protocol") String protocol);

    @Path("models")
    @POST
    @Consumes("application/json")
    public Response createMapper(ProtocolMapperRepresentation rep);

    @Path("add-models")
    @POST
    @Consumes("application/json")
    public void createMapper(List<ProtocolMapperRepresentation> reps);

    @GET
    @Path("models")
    @Produces("application/json")
    public List<ProtocolMapperRepresentation> getMappers();

    @GET
    @Path("models/{id}")
    @Produces("application/json")
    public ProtocolMapperRepresentation getMapperById(@PathParam("id") String id);

    @PUT
    @Path("models/{id}")
    @Consumes("application/json")
    public void update(@PathParam("id") String id, ProtocolMapperRepresentation rep);

    @DELETE
    @Path("models/{id}")
    public void delete(@PathParam("id") String id);
}
