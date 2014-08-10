package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.ClaimRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface OAuthClientResource {

    @GET
    public OAuthClientRepresentation toRepresentation();

    @PUT
    public void update(OAuthClientRepresentation oAuthClientRepresentation);

    @DELETE
    public void remove();

    @GET
    @Path("claims")
    public ClaimRepresentation getClaims();

    @PUT
    @Path("claims")
    public ClaimRepresentation updateClaims(ClaimRepresentation claimRepresentation);

    @POST
    @Path("client-secret")
    public CredentialRepresentation generateNewSecret();

    @GET
    @Path("client-secret")
    public CredentialRepresentation getSecret();

    @GET
    @Path("installation")
    public String getInstallationJson();

    @Path("/scope-mappings")
    public RoleMappingResource getScopeMappings();

}
