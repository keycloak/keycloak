package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.RealmRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
@Path("/admin/realms")
@Consumes(MediaType.APPLICATION_JSON)
public interface RealmsResource {

    @Path("/{realm}")
    public RealmResource realm(@PathParam("realm") String realm);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void create(RealmRepresentation realmRepresentation);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<RealmRepresentation> findAll();

}
