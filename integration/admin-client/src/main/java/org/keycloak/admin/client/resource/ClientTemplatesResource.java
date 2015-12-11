package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;

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
 */
public interface ClientTemplatesResource {

    @Path("{id}")
    public ClientTemplatesResource get(@PathParam("id") String id);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(ClientTemplateRepresentation clientRepresentation);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ClientTemplateRepresentation> findAll();



}
