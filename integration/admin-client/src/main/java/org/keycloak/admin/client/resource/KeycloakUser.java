package org.keycloak.admin.client.resource;

import org.keycloak.representations.adapters.action.UserStats;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.SocialLinkRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface KeycloakUser {

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
    @Path("reset-password-email")
    public void resetPasswordEmail();

    @GET
    @Path("session-stats")
    public Map<String, UserStats> getUserStats();

    @GET
    @Path("sessions")
    public List<UserSessionRepresentation> getUserSessions();

    @GET
    @Path("social-links")
    public List<SocialLinkRepresentation> getSocialLinks();

    @Path("role-mappings")
    public KeycloakRoleMappings roles();

}
