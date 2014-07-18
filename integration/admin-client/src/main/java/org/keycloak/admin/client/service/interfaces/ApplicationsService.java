package org.keycloak.admin.client.service.interfaces;

import org.keycloak.representations.idm.ApplicationRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface ApplicationsService {

    @Path("{appName}")
    public ApplicationService get(@PathParam("appName") String appName);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void create(ApplicationRepresentation applicationRepresentation);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ApplicationRepresentation> findAll();



}
