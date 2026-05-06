package org.keycloak.admin.client.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.representations.idm.oid4vc.UserVerifiableCredentialRepresentation;

/**
 * @since Keycloak 26.7.0 All the child endpoints are also available since that version<p>
 *
 * This endpoint including all the child endpoints requires feature {@link org.keycloak.common.Profile.Feature#OID4VC_VCI} to be enabled and also requires "verifiable credentials" to be enabled for the realm<p>
 */
public interface UserVerifiableCredentialResource {

    @POST
    @Path("credentials")
    @Consumes({MediaType.APPLICATION_JSON})
    UserVerifiableCredentialRepresentation createCredential(UserVerifiableCredentialRepresentation representation);

    @GET
    @Path("credentials")
    @Produces(MediaType.APPLICATION_JSON)
    List<UserVerifiableCredentialRepresentation> getCredentials();

    @DELETE
    @Path("credentials/{credentialScopeName}")
    void revokeCredential(@PathParam("credentialScopeName") String credentialScopeName);

    // TODO: Issued credentials
}
