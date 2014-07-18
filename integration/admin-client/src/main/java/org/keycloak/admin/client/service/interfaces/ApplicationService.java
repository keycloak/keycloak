package org.keycloak.admin.client.service.interfaces;

import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.ClaimRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface ApplicationService {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationRepresentation getRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(ApplicationRepresentation applicationRepresentation);

    @DELETE
    public void remove();

    @GET
    @Path("allowed-origins")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getAllowedOrigins();

    @PUT
    @Path("allowed-origins")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateAllowedOrigins(Set<String> allowedOrigins);

    @DELETE
    @Path("allowed-origins")
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeAllowedOrigins(Set<String> originsToRemove);

    @GET
    @Path("claims")
    @Produces(MediaType.APPLICATION_JSON)
    public ClaimRepresentation getClaims();

    @PUT
    @Path("claims")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateClaims(ClaimRepresentation claimRepresentation);

    @POST
    @Path("client-secret")
    @Produces(MediaType.APPLICATION_JSON)
    public CredentialRepresentation generateNewSecret();

    @GET
    @Path("client-secret")
    @Produces(MediaType.APPLICATION_JSON)
    public CredentialRepresentation getSecret();

    @GET
    @Path("installation/jboss")
    @Produces(MediaType.APPLICATION_XML)
    public String getInstallationJbossXml();

    @GET
    @Path("installation/json")
    @Produces(MediaType.APPLICATION_JSON)
    public String getInstallationJson();

    @POST
    @Path("logout-all")
    public void logoutAllUsers();

    @POST
    @Path("logout-user/{username}")
    public void logoutUser(@PathParam("username") String username);

    @POST
    @Path("push-revocation")
    public void pushRevocation();

    @GET
    @Path("scope-mappings")
    @Produces(MediaType.APPLICATION_JSON)
    public MappingsRepresentation getScopeMappings();

    @Path("/roles")
    public ApplicationRolesService roles();

}