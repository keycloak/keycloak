package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface ClientTemplateResource {

    @Path("protocol-mappers")
    public ProtocolMappersResource getProtocolMappers();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ClientTemplateRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(ClientTemplateRepresentation rep);

    @DELETE
    public void remove();
}