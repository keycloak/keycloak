package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface UserResource {

    @GET
    public UserRepresentation toRepresentation();

    @PUT
    public void update(UserRepresentation userRepresentation);

    @DELETE
    public void remove();

    @POST
    @Path("logout")
    public void logout();

    @PUT
    @Path("remove-totp")
    public void removeTotp();

    @PUT
    @Path("reset-password")
    public void resetPassword(CredentialRepresentation credentialRepresentation);

    @PUT
    @Path("execute-actions-email")
    public void executeActionsEmail(List<String> actions);

    @PUT
    @Path("execute-actions-email")
    public void executeActionsEmail(@QueryParam("client_id") String clientId, List<String> actions);

    @PUT
    @Path("send-verify-email")
    public void sendVerifyEmail();

    @PUT
    @Path("send-verify-email")
    public void sendVerifyEmail(@QueryParam("client_id") String clientId);

    @GET
    @Path("sessions")
    public List<UserSessionRepresentation> getUserSessions();

    @GET
    @Path("federated-identity")
    public List<FederatedIdentityRepresentation> getFederatedIdentity();

    @POST
    @Path("federated-identity/{provider}")
    public Response addFederatedIdentity(@PathParam("provider") String provider, FederatedIdentityRepresentation rep);

    @Path("federated-identity/{provider}")
    @DELETE
    public void removeFederatedIdentity(final @PathParam("provider") String provider);

    @Path("role-mappings")
    public RoleMappingResource roles();

}
