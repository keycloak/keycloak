package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ClientInitialAccessResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ClientInitialAccessPresentation create(ClientInitialAccessCreatePresentation rep);
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ClientInitialAccessPresentation> list();

    @DELETE
    @Path("{id}")
    void delete(final @PathParam("id") String id);

}
