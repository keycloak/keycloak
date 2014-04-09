package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.adapters.action.UserStats;
import org.keycloak.representations.idm.ApplicationMappingsRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.email.EmailException;
import org.keycloak.services.email.EmailSender;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.ModelToRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.util.Time;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UsersResource {
    protected static final Logger logger = Logger.getLogger(UsersResource.class);

    protected RealmModel realm;

    private RealmAuth auth;
    private TokenManager tokenManager;

    public UsersResource(RealmModel realm, RealmAuth auth, TokenManager tokenManager) {
        this.auth = auth;
        this.realm = realm;
        this.tokenManager = tokenManager;

        auth.init(RealmAuth.Resource.USER);
    }

    @Context
    protected UriInfo uriInfo;

    /*
    @Context
    protected ResourceContext resourceContext;
    */

    @Context
    protected KeycloakSession session;


    @Path("{username}")
    @PUT
    @Consumes("application/json")
    public void updateUser(final @PathParam("username") String username, final UserRepresentation rep) {
        auth.requireManage();

        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        updateUserFromRep(user, rep);
    }

    @POST
    @Consumes("application/json")
    public Response createUser(final @Context UriInfo uriInfo, final UserRepresentation rep) {
        auth.requireManage();

        if (realm.getUser(rep.getUsername()) != null) {
            return Flows.errors().exists("User with username " + rep.getUsername() + " already exists");
        }
        UserModel user = realm.addUser(rep.getUsername());
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        updateUserFromRep(user, rep);

        return Response.created(uriInfo.getAbsolutePathBuilder().path(user.getLoginName()).build()).build();
    }

    private void updateUserFromRep(UserModel user, UserRepresentation rep) {
        user.setEmail(rep.getEmail());
        user.setFirstName(rep.getFirstName());
        user.setLastName(rep.getLastName());

        user.setEnabled(rep.isEnabled());
        user.setTotp(rep.isTotp());
        user.setEmailVerified(rep.isEmailVerified());

        List<String> reqActions = rep.getRequiredActions();

        if (reqActions != null) {
            for (UserModel.RequiredAction ra : UserModel.RequiredAction.values()) {
                if (reqActions.contains(ra.name())) {
                    user.addRequiredAction(ra);
                } else {
                    user.removeRequiredAction(ra);
                }
            }
        }

        if (rep.getAttributes() != null) {
            for (Map.Entry<String, String> attr : rep.getAttributes().entrySet()) {
                user.setAttribute(attr.getKey(), attr.getValue());
            }
        }
    }

    @Path("{username}")
    @GET
    @NoCache
    @Produces("application/json")
    public UserRepresentation getUser(final @PathParam("username") String username) {
        auth.requireView();

        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        return ModelToRepresentation.toRepresentation(user);
    }

    @Path("{username}/session-stats")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, UserStats> getSessionStats(final @PathParam("username") String username) {
        logger.info("session-stats");
        auth.requireView();
        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        Map<String, UserStats> stats = new HashMap<String, UserStats>();
        for (ApplicationModel applicationModel : realm.getApplications()) {
            if (applicationModel.getManagementUrl() == null) continue;
            UserStats appStats = new ResourceAdminManager().getUserStats(realm, applicationModel, user);
            if (appStats.isLoggedIn()) stats.put(applicationModel.getName(), appStats);
        }
        return stats;
    }

    @Path("{username}/logout")
    @POST
    public void logout(final @PathParam("username") String username) {
        auth.requireManage();
        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        // set notBefore so that user will be forced to log in.
        user.setNotBefore(Time.currentTime());
        new ResourceAdminManager().logoutUser(realm, user);
    }


    @Path("{username}")
    @DELETE
    @NoCache
    public void deleteUser(final @PathParam("username") String username) {
        auth.requireManage();

        realm.removeUser(username);
    }

    @GET
    @NoCache
    @Produces("application/json")
    public List<UserRepresentation> getUsers(@QueryParam("search") String search,
                                             @QueryParam("lastName") String last,
                                             @QueryParam("firstName") String first,
                                             @QueryParam("email") String email,
                                             @QueryParam("username") String username) {
        auth.requireView();

        RealmManager manager = new RealmManager(session);
        List<UserRepresentation> results = new ArrayList<UserRepresentation>();
        List<UserModel> userModels;
        if (search != null) {
            userModels = manager.searchUsers(search, realm);
        } else if (last != null || first != null || email != null || username != null) {
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
            userModels = realm.searchForUserByAttributes(attributes);
            for (UserModel user : userModels) {
                results.add(ModelToRepresentation.toRepresentation(user));
            }
        } else {
            userModels = realm.getUsers();
        }

        for (UserModel user : userModels) {
            results.add(ModelToRepresentation.toRepresentation(user));
        }
        return results;
    }

    @Path("{username}/role-mappings")
    @GET
    @Produces("application/json")
    @NoCache
    public MappingsRepresentation getRoleMappings(@PathParam("username") String username) {
        auth.requireView();

        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        MappingsRepresentation all = new MappingsRepresentation();
        Set<RoleModel> realmMappings = realm.getRoleMappings(user);
        RealmManager manager = new RealmManager(session);
        if (realmMappings.size() > 0) {
            List<RoleRepresentation> realmRep = new ArrayList<RoleRepresentation>();
            for (RoleModel roleModel : realmMappings) {
                realmRep.add(ModelToRepresentation.toRepresentation(roleModel));
            }
            all.setRealmMappings(realmRep);
        }

        List<ApplicationModel> applications = realm.getApplications();
        if (applications.size() > 0) {
            Map<String, ApplicationMappingsRepresentation> appMappings = new HashMap<String, ApplicationMappingsRepresentation>();
            for (ApplicationModel application : applications) {
                Set<RoleModel> roleMappings = application.getApplicationRoleMappings(user);
                if (roleMappings.size() > 0) {
                    ApplicationMappingsRepresentation mappings = new ApplicationMappingsRepresentation();
                    mappings.setApplicationId(application.getId());
                    mappings.setApplication(application.getName());
                    List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();
                    mappings.setMappings(roles);
                    for (RoleModel role : roleMappings) {
                        roles.add(ModelToRepresentation.toRepresentation(role));
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
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getRealmRoleMappings(@PathParam("username") String username) {
        auth.requireView();

        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        Set<RoleModel> realmMappings = realm.getRealmRoleMappings(user);
        List<RoleRepresentation> realmMappingsRep = new ArrayList<RoleRepresentation>();
        RealmManager manager = new RealmManager(session);
        for (RoleModel roleModel : realmMappings) {
            realmMappingsRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return realmMappingsRep;
    }

    @Path("{username}/role-mappings/realm")
    @POST
    @Consumes("application/json")
    public void addRealmRoleMappings(@PathParam("username") String username, List<RoleRepresentation> roles) {
        auth.requireManage();

        logger.debug("** addRealmRoleMappings: {0}", roles);
        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = realm.getRole(role.getName());
            if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                throw new NotFoundException("Role not found");
            }
            realm.grantRole(user, roleModel);
        }


    }

    @Path("{username}/role-mappings/realm")
    @DELETE
    @Consumes("application/json")
    public void deleteRealmRoleMappings(@PathParam("username") String username, List<RoleRepresentation> roles) {
        auth.requireManage();

        logger.debug("deleteRealmRoleMappings");
        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (roles == null) {
            Set<RoleModel> roleModels = realm.getRealmRoleMappings(user);
            for (RoleModel roleModel : roleModels) {
                realm.deleteRoleMapping(user, roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = realm.getRole(role.getName());
                if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                    throw new NotFoundException("Role not found");
                }
                realm.deleteRoleMapping(user, roleModel);
            }
        }
    }

    @Path("{username}/role-mappings/applications/{app}")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getApplicationRoleMappings(@PathParam("username") String username, @PathParam("app") String appName) {
        auth.requireView();

        logger.debug("getApplicationRoleMappings");

        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        ApplicationModel application = realm.getApplicationByName(appName);

        if (application == null) {
            throw new NotFoundException("Application not found");
        }

        Set<RoleModel> mappings = application.getApplicationRoleMappings(user);
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : mappings) {
            mapRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        logger.debug("getApplicationRoleMappings.size() = {0}", mapRep.size());
        return mapRep;
    }

    @Path("{username}/role-mappings/applications/{app}")
    @POST
    @Consumes("application/json")
    public void addApplicationRoleMapping(@PathParam("username") String username, @PathParam("app") String appName, List<RoleRepresentation> roles) {
        auth.requireManage();

        logger.debug("addApplicationRoleMapping");
        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        ApplicationModel application = realm.getApplicationByName(appName);

        if (application == null) {
            throw new NotFoundException("Application not found");
        }

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = application.getRole(role.getName());
            if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                throw new NotFoundException("Role not found");
            }
            realm.grantRole(user, roleModel);
        }

    }

    @Path("{username}/role-mappings/applications/{app}")
    @DELETE
    @Consumes("application/json")
    public void deleteApplicationRoleMapping(@PathParam("username") String username, @PathParam("app") String appName, List<RoleRepresentation> roles) {
        auth.requireManage();

        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        ApplicationModel application = realm.getApplicationByName(appName);

        if (application == null) {
            throw new NotFoundException("Application not found");
        }

        if (roles == null) {
            Set<RoleModel> roleModels = application.getApplicationRoleMappings(user);
            for (RoleModel roleModel : roleModels) {
                if (!(roleModel.getContainer() instanceof ApplicationModel)) {
                    ApplicationModel app = (ApplicationModel) roleModel.getContainer();
                    if (!app.getId().equals(application.getId())) continue;
                }
                realm.deleteRoleMapping(user, roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = application.getRole(role.getName());
                if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                    throw new NotFoundException("Role not found");
                }
                realm.deleteRoleMapping(user, roleModel);
            }
        }
    }

    @Path("{username}/reset-password")
    @PUT
    @Consumes("application/json")
    public void resetPassword(@PathParam("username") String username, CredentialRepresentation pass) {
        auth.requireManage();

        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        if (pass == null || pass.getValue() == null || !CredentialRepresentation.PASSWORD.equals(pass.getType())) {
            throw new BadRequestException("No password provided");
        }

        UserCredentialModel cred = RealmManager.fromRepresentation(pass);
        realm.updateCredential(user, cred);
        user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
    }

    @Path("{username}/remove-totp")
    @PUT
    @Consumes("application/json")
    public void removeTotp(@PathParam("username") String username) {
        auth.requireManage();

        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        user.setTotp(false);
    }

    @Path("{username}/reset-password-email")
    @PUT
    @Consumes("application/json")
    public Response resetPasswordEmail(@PathParam("username") String username) {
        auth.requireManage();

        UserModel user = realm.getUser(username);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (user.getEmail() == null) {
            return Flows.errors().error("User email missing", Response.Status.BAD_REQUEST);
        }

        String redirect = Urls.accountBase(uriInfo.getBaseUri()).path("/").build(realm.getName()).toString();
        String clientId = Constants.ACCOUNT_MANAGEMENT_APP;
        String state = null;
        String scope = null;

        ClientModel client = realm.findClient(clientId);
        if (client == null || !client.isEnabled()) {
            return Flows.errors().error("Account management not enabled", Response.Status.INTERNAL_SERVER_ERROR);
        }

        Set<UserModel.RequiredAction> requiredActions = new HashSet<UserModel.RequiredAction>(user.getRequiredActions());
        requiredActions.add(UserModel.RequiredAction.UPDATE_PASSWORD);

        AccessCodeEntry accessCode = tokenManager.createAccessCode(scope, state, redirect, realm, client, user);
        accessCode.setRequiredActions(requiredActions);
        accessCode.setExpiration(Time.currentTime() + realm.getAccessCodeLifespanUserAction());

        try {
            new EmailSender(realm.getSmtpConfig()).sendPasswordReset(user, realm, accessCode, uriInfo);
            return Response.ok().build();
        } catch (EmailException e) {
            logger.error("Failed to send password reset email", e);
            return Flows.errors().error("Failed to send email", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}
