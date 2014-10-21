package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.SocialLinkRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
    @Path("reset-password-email")
    public void resetPasswordEmail();

    @GET
    @Path("sessions")
    public List<UserSessionRepresentation> getUserSessions();

    @GET
    @Path("social-links")
    public List<SocialLinkRepresentation> getSocialLinks();

    @POST
    @Path("social-links/{provider}")
    public Response addSocialLink(@PathParam("provider") String provider, SocialLinkRepresentation rep);

    @Path("social-links/{provider}")
    @DELETE
    public void removeSocialLink(final @PathParam("provider") String provider);

    @Path("role-mappings")
    public RoleMappingResource roles();

}
