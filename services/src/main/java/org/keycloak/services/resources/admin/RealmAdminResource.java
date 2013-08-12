package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.UserManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.resources.Transaction;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
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

    public RealmAdminResource(UserModel admin, RealmModel realm) {
        this.admin = admin;
        this.realm = realm;
    }

    @Path("applications")
    public ApplicationsResource getResources() {
        return new ApplicationsResource(admin, realm);
    }

    @GET
    @NoCache
    @Produces("application/json")
    public RealmRepresentation getRealm() {
        return new Transaction() {
            @Override
            protected RealmRepresentation callImpl() {
                return new RealmManager(session).toRepresentation(realm);
            }
        }.call();
    }


    @Path("roles")
    @GET
    @NoCache
    @Produces("application/json")
    public List<RoleRepresentation> queryRoles() {
        return new Transaction() {
            @Override
            protected List<RoleRepresentation> callImpl() {
                List<RoleModel> roleModels = realm.getRoles();
                List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();
                for (RoleModel roleModel : roleModels) {
                    RoleRepresentation role = new RoleRepresentation(roleModel.getName(), roleModel.getDescription());
                    role.setId(roleModel.getId());
                    roles.add(role);
                }
                return roles;
            }
        }.call();
    }

    @PUT
    @Consumes("application/json")
    public void updateRealm(final RealmRepresentation rep) {
        new Transaction() {
            @Override
            protected void runImpl() {
                logger.info("updating realm: " + rep.getRealm());
                new RealmManager(session).updateRealm(rep, realm);
            }
        }.run();

    }

    @Path("roles/{id}")
    @GET
    @NoCache
    @Produces("application/json")
    public RoleRepresentation getRole(final @PathParam("id") String id) {
        return new Transaction() {
            @Override
            protected RoleRepresentation callImpl() {
                RoleModel roleModel = realm.getRoleById(id);
                if (roleModel == null) {
                    throw new NotFoundException();
                }
                RoleRepresentation rep = new RoleRepresentation(roleModel.getName(), roleModel.getDescription());
                rep.setId(roleModel.getId());
                return rep;
            }
        }.call();
    }


    @Path("roles/{id}")
    @PUT
    @Consumes("application/json")
    public void updateRole(final @PathParam("id") String id, final RoleRepresentation rep) {
        new Transaction() {
            @Override
            protected void runImpl() {
                RoleModel role = realm.getRoleById(id);
                if (role == null) {
                   throw new NotFoundException();
                }
                role.setName(rep.getName());
                role.setDescription(rep.getDescription());
            }
        }.run();

    }

    @Path("roles")
    @POST
    @Consumes("application/json")
    public Response createRole(final @Context UriInfo uriInfo, final RoleRepresentation rep) {
        return new Transaction() {
            @Override
            protected Response callImpl() {
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
        }.call();

    }


    @Path("users")
    @GET
    @NoCache
    @Produces("application/json")
    public List<UserRepresentation> queryUsers(final @Context UriInfo uriInfo) {
        return new Transaction() {
            @Override
            protected List<UserRepresentation> callImpl() {
                logger.info("queryUsers");
                Map<String, String> params = new HashMap<String, String>();
                MultivaluedMap<String,String> queryParameters = uriInfo.getQueryParameters();
                for (String key : queryParameters.keySet()) {
                    logger.info("   " + key + "=" + queryParameters.getFirst(key));
                    params.put(key, queryParameters.getFirst(key));
                }
                List<UserModel> userModels = realm.queryUsers(params);
                List<UserRepresentation> users = new ArrayList<UserRepresentation>();
                for (UserModel userModel : userModels) {
                    users.add(UserManager.toRepresentation(userModel));
                }
                logger.info("   resultSet: " + users.size());
                return users;
            }
        }.call();
    }

    @Path("users/{loginName}")
    @GET
    @NoCache
    @Produces("application/json")
    public UserRepresentation getUser(final @PathParam("loginName") String loginName) {
        return new Transaction() {
            @Override
            protected UserRepresentation callImpl() {
                UserModel userModel = realm.getUser(loginName);
                if (userModel == null) {
                    throw new NotFoundException();
                }
                return UserManager.toRepresentation(userModel);
            }
        }.call();
    }

    @Path("users")
    @POST
    @NoCache
    @Consumes("application/json")
    public Response createUser(final @Context UriInfo uriInfo, final UserRepresentation rep) {
        return new Transaction() {
            @Override
            protected Response callImpl() {
                if (realm.getUser(rep.getUsername()) != null) {
                    return Response.status(Response.Status.FOUND).build();
                }
                rep.setCredentials(null); // don't allow credential creation
                UserManager userManager = new UserManager();
                UserModel userModel = userManager.createUser(realm, rep);
                return Response.created(uriInfo.getAbsolutePathBuilder().path(userModel.getLoginName()).build()).build();
            }
        }.call();
    }

    @Path("users/{loginName}")
    @PUT
    @NoCache
    @Consumes("application/json")
    public void updateUser(final @PathParam("loginName") String loginName, final UserRepresentation rep) {
        new Transaction() {
            @Override
            protected void runImpl() {
                UserModel userModel = realm.getUser(loginName);
                if (userModel == null) {
                    throw new NotFoundException();
                }
                UserManager userManager = new UserManager();
                userManager.updateUserAsAdmin(userModel, rep);
            }
        }.run();
    }







}
