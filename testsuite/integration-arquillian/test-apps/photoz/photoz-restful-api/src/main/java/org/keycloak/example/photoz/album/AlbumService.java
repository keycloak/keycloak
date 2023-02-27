package org.keycloak.example.photoz.album;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.ClientAuthorizationContext;
import org.keycloak.example.photoz.CustomDatabase;
import org.keycloak.example.photoz.entity.Album;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.authorization.client.resource.ProtectionResource;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashSet;
import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import org.jboss.logging.Logger;

@Path("/album")
public class AlbumService {

    private final Logger log = Logger.getLogger(AlbumService.class);

    public static final String SCOPE_ALBUM_VIEW = "album:view";
    public static final String SCOPE_ALBUM_DELETE = "album:delete";

    private CustomDatabase customDatabase = CustomDatabase.create();

    @Context
    private HttpServletRequest request;

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response create(Album newAlbum, @QueryParam("user") String invalidUser, @Context HttpHeaders headers) {
        printAuthHeaders(headers);
        
        String userId = request.getUserPrincipal().getName();
        
        if (invalidUser != null) {
            userId = invalidUser;
        }
        
        newAlbum.setUserId(userId);

        log.debug("PERSISTING " + newAlbum);
        customDatabase.addAlbum(newAlbum);
        try {
            createProtectedResource(newAlbum);
        } catch (RuntimeException e) {
            log.debug("ERROR " + e);
            customDatabase.remove(newAlbum);
            return Response.status(500).entity(e.getMessage()).build(); //
        }

        return Response.ok(newAlbum).build();
    }

    @Path("{name}")
    @DELETE
    public Response delete(@PathParam("name") String name, @Context HttpHeaders headers) {
        printAuthHeaders(headers);
        
        Album album = this.customDatabase.findByName(name);

        try {
            deleteProtectedResource(album);
            this.customDatabase.remove(album);
        } catch (Exception e) {
            throw new RuntimeException("Could not delete album.", e);
        }

        return Response.ok().build();
    }

    @GET
    @Produces("application/json")
    public Response findAll(@QueryParam("getAll") Boolean getAll) {
        if (getAll != null && getAll) {
            return Response.ok(this.customDatabase.getAll()).build();
        } else {
            return Response.ok(this.customDatabase.findByUserId(request.getUserPrincipal().getName())).build();
        }
    }

    @GET
    @Path("{name}")
    @Produces("application/json")
    public Response findById(@PathParam("name") String name) {
        Album result = this.customDatabase.findByName(name);

        if (result == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(result).build();
    }

    private void createProtectedResource(Album album) {
        log.debug("Creating ProtectedResource for " + album);
        try {
            HashSet<ScopeRepresentation> scopes = new HashSet<>();

            scopes.add(new ScopeRepresentation(SCOPE_ALBUM_VIEW));
            scopes.add(new ScopeRepresentation(SCOPE_ALBUM_DELETE));

            ResourceRepresentation albumResource = new ResourceRepresentation(album.getName(), scopes, "/album/" + album.getName(), "http://photoz.com/album");

            albumResource.setOwner(album.getUserId());

            if (album.isUserManaged()) {
                albumResource.setOwnerManagedAccess(true);
            }

            getAuthzClient().protection().resource().create(albumResource);
        } catch (Exception e) {
            throw new RuntimeException("Could not register protected resource.", e);
        }
    }

    private void deleteProtectedResource(Album album) {
        String uri = "/album/" + album.getName();

        try {
            ProtectionResource protection = getAuthzClient().protection();
            List<ResourceRepresentation> search = protection.resource().findByUri(uri);

            if (search.isEmpty()) {
                throw new RuntimeException("Could not find protected resource with URI [" + uri + "]");
            }

            protection.resource().delete(search.get(0).getId());
        } catch (RuntimeException e) {
            throw new RuntimeException("Could not search protected resource.", e);
        }
    }

    private AuthzClient getAuthzClient() {
        return getAuthorizationContext().getClient();
    }

    private ClientAuthorizationContext getAuthorizationContext() {
        return ClientAuthorizationContext.class.cast(getKeycloakSecurityContext().getAuthorizationContext());
    }

    private KeycloakSecurityContext getKeycloakSecurityContext() {
        return KeycloakSecurityContext.class.cast(request.getAttribute(KeycloakSecurityContext.class.getName()));
    }

    private void printAuthHeaders(HttpHeaders headers) {
        log.debug("-----------------Authorization headers--------------------------");
        for (String authHeader : headers.getRequestHeader(HttpHeaders.AUTHORIZATION)) {
            log.debug(authHeader);
        }
    }
}