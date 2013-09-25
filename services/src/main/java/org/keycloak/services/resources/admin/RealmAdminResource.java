package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;

import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdminResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected UserModel admin;
    protected RealmModel realm;

    @Context
    protected ResourceContext resourceContext;

    @Context
    protected KeycloakSession session;

    public RealmAdminResource(UserModel admin, RealmModel realm) {
        this.admin = admin;
        this.realm = realm;
    }

    @Path("applications")
    public ApplicationsResource getResources() {
        ApplicationsResource applicationsResource = new ApplicationsResource(admin, realm);
        resourceContext.initResource(applicationsResource);
        return applicationsResource;
    }

    @GET
    @NoCache
    @Produces("application/json")
    public RealmRepresentation getRealm() {
        return new RealmManager(session).toRepresentation(realm);
    }


    @Path("roles")
    @GET
    @NoCache
    @Produces("application/json")
    public List<RoleRepresentation> getRoles() {
        List<RoleModel> roleModels = realm.getRoles();
        List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roleModels) {
            RoleRepresentation role = new RoleRepresentation(roleModel.getName(), roleModel.getDescription());
            role.setId(roleModel.getId());
            roles.add(role);
        }
        return roles;
    }

    @PUT
    @Consumes("application/json")
    public void updateRealm(final RealmRepresentation rep) {
        logger.info("updating realm: " + rep.getRealm());
        new RealmManager(session).updateRealm(rep, realm);
    }

    @Path("roles/{id}")
    @GET
    @NoCache
    @Produces("application/json")
    public RoleRepresentation getRole(final @PathParam("id") String id) {
        RoleModel roleModel = realm.getRoleById(id);
        if (roleModel == null) {
            throw new NotFoundException();
        }
        RoleRepresentation rep = new RoleRepresentation(roleModel.getName(), roleModel.getDescription());
        rep.setId(roleModel.getId());
        return rep;
    }


    @Path("roles/{id}")
    @PUT
    @Consumes("application/json")
    public void updateRole(final @PathParam("id") String id, final RoleRepresentation rep) {
        RoleModel role = realm.getRoleById(id);
        if (role == null) {
            throw new NotFoundException();
        }
        role.setName(rep.getName());
        role.setDescription(rep.getDescription());
    }

    @Path("roles")
    @POST
    @Consumes("application/json")
    public Response createRole(final @Context UriInfo uriInfo, final RoleRepresentation rep) {
        if (realm.getRole(rep.getName()) != null) {
            throw new InternalServerErrorException(); // todo appropriate status here.
        }
        RoleModel role = realm.addRole(rep.getName());
        if (role == null) {
            throw new NotFoundException();
        }
        role.setDescription(rep.getDescription());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(role.getId()).build()).build();
    }

    @Path("users")
    public UsersResource users() {
        UsersResource users = new UsersResource(realm);
        resourceContext.initResource(users);
        return users;
    }



}
