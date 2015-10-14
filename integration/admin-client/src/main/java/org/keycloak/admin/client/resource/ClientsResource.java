package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.ClientRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 * @author <a href="mailto:tom@tutorials.de">Thomas Darimont</a>
 */
public interface ClientsResource {

    @Path("{id}")
    public ClientResource get(@PathParam("id") String id);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(ClientRepresentation clientRepresentation);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ClientRepresentation> findAll();

    @Path("/by-client-id/{clientId}")
    ClientResource getByClientId(@PathParam("clientId") String clientId);
}
