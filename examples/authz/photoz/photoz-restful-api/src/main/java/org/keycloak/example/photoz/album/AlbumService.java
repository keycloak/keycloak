package org.keycloak.example.photoz.album;

import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.ClientAuthorizationContext;
import org.keycloak.authorization.client.representation.ResourceRepresentation;
import org.keycloak.authorization.client.representation.ScopeRepresentation;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.example.photoz.ErrorResponse;
import org.keycloak.example.photoz.entity.Album;
import org.keycloak.example.photoz.util.Transaction;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;

@Path("/album")
@Transaction
public class AlbumService {

    public static final String SCOPE_ALBUM_VIEW = "album:view";
    public static final String SCOPE_ALBUM_DELETE = "album:delete";

    @Inject
    private EntityManager entityManager;

    @Context
    private HttpServletRequest request;

    @POST
    @Consumes("application/json")
    public Response create(Album newAlbum) {
        Principal userPrincipal = request.getUserPrincipal();

        newAlbum.setId(UUID.randomUUID().toString());
        newAlbum.setUserId(userPrincipal.getName());

        Query queryDuplicatedAlbum = this.entityManager.createQuery("from Album where name = :name and userId = :userId");

        queryDuplicatedAlbum.setParameter("name", newAlbum.getName());
        queryDuplicatedAlbum.setParameter("userId", userPrincipal.getName());

        if (!queryDuplicatedAlbum.getResultList().isEmpty()) {
            throw new ErrorResponse("Name [" + newAlbum.getName() + "] already taken. Choose another one.", Status.CONFLICT);
        }

        try {
            this.entityManager.persist(newAlbum);
            createProtectedResource(newAlbum);
        } catch (Exception e) {
            getAuthzClient().protection().resource().delete(newAlbum.getExternalId());
        }

        return Response.ok(newAlbum).build();
    }

    @Path("{id}")
    @DELETE
    public Response delete(@PathParam("id") String id) {
        Album album = this.entityManager.find(Album.class, id);

        try {
            deleteProtectedResource(album);
            this.entityManager.remove(album);
        } catch (Exception e) {
            throw new RuntimeException("Could not delete album.", e);
        }

        return Response.ok().build();
    }

    @GET
    @Produces("application/json")
    public Response findAll() {
        return Response.ok(this.entityManager.createQuery("from Album where userId = :id").setParameter("id", request.getUserPrincipal().getName()).getResultList()).build();
    }

    @GET
    @Path("/shares")
    @Produces("application/json")
    public Response findShares() {
        List<PermissionTicketRepresentation> permissions = getAuthzClient().protection().permission().find(null, null, null, getKeycloakSecurityContext().getToken().getSubject(), true, true, null, null);
        Map<String, SharedAlbum> shares = new HashMap<>();

        for (PermissionTicketRepresentation permission : permissions) {
            SharedAlbum share = shares.get(permission.getResource());

            if (share == null) {
                share = new SharedAlbum(Album.class.cast(entityManager.createQuery("from Album where externalId = :externalId").setParameter("externalId", permission.getResource()).getSingleResult()));
                shares.put(permission.getResource(), share);
            }

            if (permission.getScope() != null) {
                share.addScope(permission.getScopeName());
            }
        }

        return Response.ok(shares.values()).build();
    }

    @GET
    @Path("{id}")
    @Produces("application/json")
    public Response findById(@PathParam("id") String id) {
        List result = this.entityManager.createQuery("from Album where id = :id").setParameter("id", id).getResultList();

        if (result.isEmpty()) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(result.get(0)).build();
    }

    private void createProtectedResource(Album album) {
        try {
            HashSet<ScopeRepresentation> scopes = new HashSet<>();

            scopes.add(new ScopeRepresentation(SCOPE_ALBUM_VIEW));
            scopes.add(new ScopeRepresentation(SCOPE_ALBUM_DELETE));

            ResourceRepresentation albumResource = new ResourceRepresentation(album.getName(), scopes, "/album/" + album.getId(), "http://photoz.com/album");

            albumResource.setOwner(album.getUserId());
            albumResource.setOwnerManagedAccess(true);

            ResourceRepresentation response = getAuthzClient().protection().resource().create(albumResource);

            album.setExternalId(response.getId());
        } catch (Exception e) {
            throw new RuntimeException("Could not register protected resource.", e);
        }
    }

    private void deleteProtectedResource(Album album) {
        String uri = "/album/" + album.getId();

        try {
            ProtectionResource protection = getAuthzClient().protection();
            List<ResourceRepresentation> search = protection.resource().findByUri(uri);

            if (search.isEmpty()) {
                throw new RuntimeException("Could not find protected resource with URI [" + uri + "]");
            }

            protection.resource().delete(search.get(0).getId());
        } catch (Exception e) {
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
}
