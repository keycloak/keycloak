package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.ClientRepresentation;
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
public interface ClientResource {

    @Path("protocol-mappers")
    public ProtocolMappersResource getProtocolMappers();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(ClientRepresentation clientRepresentation);

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

    @Path("session-count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Integer> getApplicationSessionCount();

    @Path("user-sessions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserSessionRepresentation> getUserSessions(@QueryParam("first") Integer firstResult, @QueryParam("max") Integer maxResults);

    @POST
    @Path("push-revocation")
    public void pushRevocation();

    @Path("/scope-mappings")
    public RoleMappingResource getScopeMappings();

    @Path("/roles")
    public RolesResource roles();

}