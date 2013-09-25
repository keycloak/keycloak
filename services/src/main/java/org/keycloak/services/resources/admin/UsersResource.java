package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.representations.idm.*;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.*;

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
public class UsersResource {

    protected RealmModel realm;

    public UsersResource(RealmModel realm) {
        this.realm = realm;
    }

    @Context
    protected ResourceContext resourceContext;

    @Context
    protected KeycloakSession session;


    @Path("{username}")
    @PUT
    @Consumes("application/json")
    public void updateUser(final @PathParam("username") String username, final UserRepresentation rep) {
        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException();
        }
        user.setEmail(rep.getEmail());
        user.setFirstName(rep.getFirstName());
        user.setLastName(rep.getLastName());
        for (Map.Entry<String, String> attr : rep.getAttributes().entrySet()) {
            user.setAttribute(attr.getKey(), attr.getValue());
        }
    }

    @POST
    @Consumes("application/json")
    public Response createUser(final @Context UriInfo uriInfo, final UserRepresentation rep) {
        if (realm.getUser(rep.getUsername()) != null) {
            throw new InternalServerErrorException(); // todo appropriate status here.
        }
        UserModel user = realm.addUser(rep.getUsername());
        if (user == null) {
            throw new NotFoundException();
        }
        user.setEmail(rep.getEmail());
        user.setFirstName(rep.getFirstName());
        user.setLastName(rep.getLastName());
        if (rep.getAttributes() != null) {
            for (Map.Entry<String, String> attr : rep.getAttributes().entrySet()) {
                user.setAttribute(attr.getKey(), attr.getValue());
            }
        }
        return Response.created(uriInfo.getAbsolutePathBuilder().path(user.getLoginName()).build()).build();
    }

    @Path("{username}")
    @GET
    @NoCache
    @Produces("application/json")
    public UserRepresentation getUser(final @PathParam("username") String username) {
        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException();
        }
        return new RealmManager(session).toRepresentation(user);
    }

    @GET
    @NoCache
    @Produces("application/json")
    public List<UserRepresentation> getUsers(@QueryParam("search") String search,
                                             @QueryParam("lastName") String last,
                                             @QueryParam("firstName") String first,
                                             @QueryParam("email") String email,
                                             @QueryParam("username") String username) {
        RealmManager manager = new RealmManager(session);
        List<UserRepresentation> results = new ArrayList<UserRepresentation>();
        if (search != null) {
            List<UserModel> userModels = manager.searchUsers(search, realm);
            for (UserModel user : userModels) {
                results.add(manager.toRepresentation(user));
            }
        } else {
            Map<String, String> attributes = new HashMap<String, String>();
            if (last != null) {
                attributes.put(UserModel.LAST_NAME, last);
            }
            if (first != null) {
                attributes.put(UserModel.FIRST_NAME, first);
            }
            if (email != null) {
                attributes.put(UserModel.EMAIL, email);
            }
            if (username != null) {
                attributes.put(UserModel.LOGIN_NAME, username);
            }
            List<UserModel> userModels = realm.searchForUserByAttributes(attributes);
            for (UserModel user : userModels) {
                results.add(manager.toRepresentation(user));
            }

        }
        return results;
    }

    @Path("{username}/role-mappings")
    @GET
    public AllRoleMappingsRepresentation getRoleMappings(@PathParam("username") String username) {
        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException();
        }

        AllRoleMappingsRepresentation all = new AllRoleMappingsRepresentation();
        all.setRealmId(realm.getId());
        all.setRealmName(realm.getName());
        all.setUsername(username);
        List<RoleModel> realmMappings = realm.getRoleMappings(user);
        RealmManager manager = new RealmManager(session);
        if (realmMappings.size() > 0) {
            List<RoleRepresentation> realmRep = new ArrayList<RoleRepresentation>();
            for (RoleModel roleModel : realmMappings) {
                realmRep.add(manager.toRepresentation(roleModel));
            }
            all.setRealmMappings(realmRep);
        }

        List<ApplicationModel> applications = realm.getApplications();
        if (applications.size() > 0) {
            Map<String, ApplicationRoleMappings> appMappings = new HashMap<String, ApplicationRoleMappings>();
            for (ApplicationModel application : applications) {
                List<RoleModel> roleMappings = application.getRoleMappings(user);
                if (roleMappings.size() > 0) {
                    ApplicationRoleMappings mappings = new ApplicationRoleMappings();
                    mappings.setUsername(user.getLoginName());
                    mappings.setApplicationId(application.getId());
                    mappings.setApplication(application.getName());
                    List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();
                    mappings.setMappings(roles);
                    for (RoleModel role : roleMappings) {
                        roles.add(manager.toRepresentation(role));
                    }
                    appMappings.put(application.getName(), mappings);
                    all.setApplicationMappings(appMappings);
                }
            }
        }
        return all;
    }

    @Path("{username}/role-mappings/realm")
    @GET
    public RealmRoleMappingsRepresentation getRealmRoleMappings(@PathParam("username") String username) {
        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException();
        }

        RealmRoleMappingsRepresentation rep = new RealmRoleMappingsRepresentation();
        List<RoleModel> realmMappings = realm.getRoleMappings(user);
        if (realmMappings.size() > 0) {
            RealmManager manager = new RealmManager(session);
            List<RoleRepresentation> realmRep = new ArrayList<RoleRepresentation>();
            for (RoleModel roleModel : realmMappings) {
                realmRep.add(manager.toRepresentation(roleModel));
            }
            rep.setMappings(realmRep);
        }
        return rep;
    }

    @Path("{username}/role-mappings/realm")
    @POST
    public void addRealmRoleMappings(@PathParam("username") String username, List<RoleRepresentation> roles) {
        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException();
        }

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = realm.getRoleById(role.getId());
            if (roleModel == null) {
                throw new NotFoundException();
            }
            realm.grantRole(user, roleModel);
        }


    }

    @Path("{username}/role-mappings/realm")
    @DELETE
    public void deleteRoleMapping(@PathParam("username") String username, List<RoleRepresentation> roles) {
        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException();
        }

        if (roles == null) {
            List<RoleModel> roleModels = realm.getRoleMappings(user);
            for (RoleModel roleModel : roleModels) {
                realm.deleteRoleMapping(user, roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = realm.getRoleById(role.getId());
                if (roleModel == null) {
                    throw new NotFoundException();
                }
                realm.deleteRoleMapping(user, roleModel);
            }
        }

    }

    @Path("{username}/role-mappings/applications/{appId}")
    @GET
    public ApplicationRoleMappings getApplicationRoleMappings(@PathParam("username") String username, @PathParam("appId") String appId) {
        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException();
        }

        ApplicationModel application = realm.getApplicationById(appId);

        if (application == null) {
            throw new NotFoundException();
        }

        ApplicationRoleMappings rep = new ApplicationRoleMappings();
        List<RoleModel> mappings = application.getRoleMappings(user);
        if (mappings.size() > 0) {
            RealmManager manager = new RealmManager(session);
            List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
            for (RoleModel roleModel : mappings) {
                mapRep.add(manager.toRepresentation(roleModel));
            }
            rep.setMappings(mapRep);
        }
        return rep;
    }

    @Path("{username}/role-mappings/applications/{appId}")
    @POST
    public void addApplicationRoleMapping(@PathParam("username") String username, @PathParam("appId") String appId, List<RoleRepresentation> roles) {
        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException();
        }

        ApplicationModel application = realm.getApplicationById(appId);

        if (application == null) {
            throw new NotFoundException();
        }


    }

    @Path("{username}/role-mappings/applications/{appId}")
    @DELETE
    public void deleteApplicationRoleMapping(@PathParam("username") String username, @PathParam("appId") String appId, List<RoleRepresentation> roles) {
        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException();
        }

        ApplicationModel application = realm.getApplicationById(appId);

        if (application == null) {
            throw new NotFoundException();
        }

        if (roles == null) {
            List<RoleModel> roleModels = application.getRoleMappings(user);
            for (RoleModel roleModel : roleModels) {
                application.deleteRoleMapping(user, roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = application.getRoleById(role.getId());
                if (roleModel == null) {
                    throw new NotFoundException();
                }
                application.deleteRoleMapping(user, roleModel);
            }
        }
    }
}
