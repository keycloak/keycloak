package org.keycloak.admin.client.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.representations.idm.oid4vc.IssuedVerifiableCredentialRepresentation;
import org.keycloak.representations.idm.oid4vc.UserVerifiableCredentialRepresentation;
import org.keycloak.representations.idm.oid4vc.VerifiableCredentialOfferActionConfig;

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

    @PUT
    @Path("credentials/{credentialScopeName}")
    @Produces(MediaType.APPLICATION_JSON)
    UserVerifiableCredentialRepresentation updateCredential(@PathParam("credentialScopeName") String credentialScopeName);

    @GET
    @Path("issued-credentials")
    @Produces(MediaType.APPLICATION_JSON)
    List<IssuedVerifiableCredentialRepresentation> getIssuedCredentials();

    @DELETE
    @Path("issued-credentials/{id}")
    void revokeIssuedCredential(@PathParam("id") String credentialId);

    @PUT
    @Path("credentials/send-credential-offer")
    @Consumes(MediaType.APPLICATION_JSON)
    void sendCredentialOffer(@QueryParam("client_id") String clientId,
                             @QueryParam("redirect_uri") String redirectUri,
                             @QueryParam("lifespan") Integer lifespan,
                             VerifiableCredentialOfferActionConfig credentialOfferConfig);
}
