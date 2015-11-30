package org.keycloak.admin.client.resource;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface RealmResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    RealmRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void update(RealmRepresentation realmRepresentation);

    @Path("clients")
    ClientsResource clients();

    @Path("client-description-converter")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation convertClientDescription(String description);

    @Path("users")
    UsersResource users();

    @Path("roles")
    RolesResource roles();

    @Path("groups")
    GroupsResource groups();

    @GET
    @Path("group-by-path/{path: .*}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GroupRepresentation getGroupByPath(@PathParam("path") String path);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-groups")
    public List<GroupRepresentation> getDefaultGroups();

    @PUT
    @Path("default-groups/{groupId}")
    public void addDefaultGroup(@PathParam("groupId") String groupId);

    @DELETE
    @Path("default-groups/{groupId}")
    public void removeDefaultGroup(@PathParam("groupId") String groupId);

    @Path("identity-provider")
    IdentityProvidersResource identityProviders();

    @DELETE
    void remove();

    @Path("client-session-stats")
    @GET
    List<Map<String, String>> getClientSessionStats();

    @Path("clients-initial-access")
    ClientInitialAccessResource clientInitialAccess();

}
