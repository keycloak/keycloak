package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailProvider;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.adapters.action.UserStats;
import org.keycloak.representations.idm.ApplicationMappingsRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.SocialLinkRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.services.managers.AccessCode;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.managers.UserManager;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.Urls;

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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Base resource for managing users
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UsersResource {
    protected static final Logger logger = Logger.getLogger(UsersResource.class);

    protected RealmModel realm;

    private RealmAuth auth;

    private TokenManager tokenManager;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakSession session;

    public UsersResource(RealmModel realm, RealmAuth auth, TokenManager tokenManager) {
        this.auth = auth;
        this.realm = realm;
        this.tokenManager = tokenManager;

        auth.init(RealmAuth.Resource.USER);
    }

    /**
     * Update the user
     *
     * @param username user name (not id!)
     * @param rep
     * @return
     */
    @Path("{username}")
    @PUT
    @Consumes("application/json")
    public Response updateUser(final @PathParam("username") String username, final UserRepresentation rep) {
        auth.requireManage();

        try {
            UserModel user = session.users().getUserByUsername(username, realm);
            if (user == null) {
                throw new NotFoundException("User not found");
            }
            updateUserFromRep(user, rep);

            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return Flows.errors().exists("User exists with same username or email");
        }
    }

    /**
     * Create a new user.  Must be a unique username!
     *
     * @param uriInfo
     * @param rep
     * @return
     */
    @POST
    @Consumes("application/json")
    public Response createUser(final @Context UriInfo uriInfo, final UserRepresentation rep) {
        auth.requireManage();

        try {
            UserModel user = session.users().addUser(realm, rep.getUsername());
            updateUserFromRep(user, rep);

            return Response.created(uriInfo.getAbsolutePathBuilder().path(user.getUsername()).build()).build();
        } catch (ModelDuplicateException e) {
            return Flows.errors().exists("User exists with same username or email");
        }
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

    /**
     * Get represenation of the user
     *
     * @param username username (not id!)
     * @return
     */
    @Path("{username}")
    @GET
    @NoCache
    @Produces("application/json")
    public UserRepresentation getUser(final @PathParam("username") String username) {
        auth.requireView();

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        return ModelToRepresentation.toRepresentation(user);
    }

    /**
     * For each application with an admin URL, query them for the set of users logged in.  This not as reliable
     * as getSessions().
     *
     * @See getSessions
     *
     * @param username
     * @return
     */
    @Path("{username}/session-stats")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, UserStats> getSessionStats(final @PathParam("username") String username) {
        logger.info("session-stats");
        auth.requireView();
        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        Map<String, UserStats> stats = new HashMap<String, UserStats>();
        for (ApplicationModel applicationModel : realm.getApplications()) {
            if (applicationModel.getManagementUrl() == null) continue;
            UserStats appStats = new ResourceAdminManager().getUserStats(uriInfo.getRequestUri(), realm, applicationModel, user);
            if (appStats == null) continue;
            if (appStats.isLoggedIn()) stats.put(applicationModel.getName(), appStats);
        }
        return stats;
    }

    /**
     * List set of sessions associated with this user.
     *
     * @param username
     * @return
     */
    @Path("{username}/sessions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserSessionRepresentation> getSessions(final @PathParam("username") String username) {
        logger.info("sessions");
        auth.requireView();
        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        List<UserSessionModel> sessions = session.sessions().getUserSessions(realm, user);
        List<UserSessionRepresentation> reps = new ArrayList<UserSessionRepresentation>();
        for (UserSessionModel session : sessions) {
            UserSessionRepresentation rep = ModelToRepresentation.toRepresentation(session);
            reps.add(rep);
        }
        return reps;
    }

    /**
     * List set of social logins associated with this user.
     *
     * @param username
     * @return
     */
    @Path("{username}/social-links")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<SocialLinkRepresentation> getSocialLinks(final @PathParam("username") String username) {
        auth.requireView();
        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        Set<SocialLinkModel> socialLinks = session.users().getSocialLinks(user, realm);
        List<SocialLinkRepresentation> result = new ArrayList<SocialLinkRepresentation>();
        for (SocialLinkModel socialLink : socialLinks) {
            SocialLinkRepresentation rep = ModelToRepresentation.toRepresentation(socialLink);
            result.add(rep);
        }
        return result;
    }

    /**
     * Remove all user sessions associated with this user.  And, for all applications that have an admin URL, tell
     * them to invalidate the sessions for this particular user.
     *
     * @param username username (not id!)
     */
    @Path("{username}/logout")
    @POST
    public void logout(final @PathParam("username") String username) {
        auth.requireManage();
        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        session.sessions().removeUserSessions(realm, user);
        new ResourceAdminManager().logoutUser(uriInfo.getRequestUri(), realm, user.getId(), null);
    }

    /**
     * delete this user
     *
     * @param username username (not id!)
     */
    @Path("{username}")
    @DELETE
    @NoCache
    public void deleteUser(final @PathParam("username") String username) {
        auth.requireManage();

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        new UserManager(session).removeUser(realm, user);
    }

    /**
     * Query list of users.  May pass in query criteria
     *
     * @param search string contained in username, first or last name, or email
     * @param last
     * @param first
     * @param email
     * @param username
     * @return
     */
    @GET
    @NoCache
    @Produces("application/json")
    public List<UserRepresentation> getUsers(@QueryParam("search") String search,
                                             @QueryParam("lastName") String last,
                                             @QueryParam("firstName") String first,
                                             @QueryParam("email") String email,
                                             @QueryParam("username") String username,
                                             @QueryParam("first") Integer firstResult,
                                             @QueryParam("max") Integer maxResults) {
        auth.requireView();

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : -1;

        List<UserRepresentation> results = new ArrayList<UserRepresentation>();
        List<UserModel> userModels;
        if (search != null) {
            userModels = session.users().searchForUser(search.trim(), realm, firstResult, maxResults);
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
                attributes.put(UserModel.USERNAME, username);
            }
            userModels = session.users().searchForUserByAttributes(attributes, realm, firstResult, maxResults);
            for (UserModel user : userModels) {
                results.add(ModelToRepresentation.toRepresentation(user));
            }
        } else {
            userModels = session.users().getUsers(realm, firstResult, maxResults);
        }

        for (UserModel user : userModels) {
            results.add(ModelToRepresentation.toRepresentation(user));
        }
        return results;
    }

    /**
     * Get role mappings for this user
     *
     * @param username username (not id!)
     * @return
     */
    @Path("{username}/role-mappings")
    @GET
    @Produces("application/json")
    @NoCache
    public MappingsRepresentation getRoleMappings(@PathParam("username") String username) {
        auth.requireView();

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        MappingsRepresentation all = new MappingsRepresentation();
        Set<RoleModel> realmMappings = user.getRoleMappings();
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
                Set<RoleModel> roleMappings = user.getApplicationRoleMappings(application);
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

    /**
     * Get realm-level role mappings for this user
     *
     * @param username username (not id!)
     * @return
     */
    @Path("{username}/role-mappings/realm")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getRealmRoleMappings(@PathParam("username") String username) {
        auth.requireView();

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        Set<RoleModel> realmMappings = user.getRealmRoleMappings();
        List<RoleRepresentation> realmMappingsRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : realmMappings) {
            realmMappingsRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return realmMappingsRep;
    }

    /**
     * Effective realm-level role mappings for this user.  Will recurse all composite roles to get this list.
     *
     * @param username username (not id!)
     * @return
     */
    @Path("{username}/role-mappings/realm/composite")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getCompositeRealmRoleMappings(@PathParam("username") String username) {
        auth.requireView();

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        Set<RoleModel> roles = realm.getRoles();
        List<RoleRepresentation> realmMappingsRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roles) {
            if (user.hasRole(roleModel)) {
               realmMappingsRep.add(ModelToRepresentation.toRepresentation(roleModel));
            }
        }
        return realmMappingsRep;
    }

    /**
     * Realm-level roles that can be mapped to this user
     *
     * @param username username (not id!)
     * @return
     */
    @Path("{username}/role-mappings/realm/available")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getAvailableRealmRoleMappings(@PathParam("username") String username) {
        auth.requireView();

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        Set<RoleModel> available = realm.getRoles();
        return getAvailableRoles(user, available);
    }

    /**
     * Add realm-level role mappings
     *
     * @param username username (not id!)
     * @param roles
     */
    @Path("{username}/role-mappings/realm")
    @POST
    @Consumes("application/json")
    public void addRealmRoleMappings(@PathParam("username") String username, List<RoleRepresentation> roles) {
        auth.requireManage();

        logger.debugv("** addRealmRoleMappings: {0}", roles);
        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = realm.getRole(role.getName());
            if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                throw new NotFoundException("Role not found");
            }
            user.grantRole(roleModel);
        }


    }

    /**
     * Delete realm-level role mappings
     *
     * @param username username (not id!)
     * @param roles
     */
    @Path("{username}/role-mappings/realm")
    @DELETE
    @Consumes("application/json")
    public void deleteRealmRoleMappings(@PathParam("username") String username, List<RoleRepresentation> roles) {
        auth.requireManage();

        logger.debug("deleteRealmRoleMappings");
        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (roles == null) {
            Set<RoleModel> roleModels = user.getRealmRoleMappings();
            for (RoleModel roleModel : roleModels) {
                user.deleteRoleMapping(roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = realm.getRole(role.getName());
                if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                    throw new NotFoundException("Role not found");
                }
                user.deleteRoleMapping(roleModel);
            }
        }
    }

    /**
     * Get application-level role mappings for this user for a specific app
     *
     * @param username username (not id!)
     * @param appName app name (not id!)
     * @return
     */
    @Path("{username}/role-mappings/applications/{app}")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getApplicationRoleMappings(@PathParam("username") String username, @PathParam("app") String appName) {
        auth.requireView();

        logger.debug("getApplicationRoleMappings");

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        ApplicationModel application = realm.getApplicationByName(appName);

        if (application == null) {
            throw new NotFoundException("Application not found");
        }

        Set<RoleModel> mappings = user.getApplicationRoleMappings(application);
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : mappings) {
            mapRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        logger.debugv("getApplicationRoleMappings.size() = {0}", mapRep.size());
        return mapRep;
    }

    /**
     * Get effective application-level role mappings.  This recurses any composite roles
     *
     * @param username username (not id!)
     * @param appName app name (not id!)
     * @return
     */
    @Path("{username}/role-mappings/applications/{app}/composite")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getCompositeApplicationRoleMappings(@PathParam("username") String username, @PathParam("app") String appName) {
        auth.requireView();

        logger.debug("getCompositeApplicationRoleMappings");

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        ApplicationModel application = realm.getApplicationByName(appName);

        if (application == null) {
            throw new NotFoundException("Application not found");
        }

        Set<RoleModel> roles = application.getRoles();
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roles) {
            if (user.hasRole(roleModel)) mapRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        logger.debugv("getCompositeApplicationRoleMappings.size() = {0}", mapRep.size());
        return mapRep;
    }

    /**
     * Get available application-level roles that can be mapped to the user
     *
     * @param username username (not id!)
     * @param appName app name (not id!)
     * @return
     */
    @Path("{username}/role-mappings/applications/{app}/available")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getAvailableApplicationRoleMappings(@PathParam("username") String username, @PathParam("app") String appName) {
        auth.requireView();

        logger.debug("getApplicationRoleMappings");

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        ApplicationModel application = realm.getApplicationByName(appName);

        if (application == null) {
            throw new NotFoundException("Application not found");
        }
        Set<RoleModel> available = application.getRoles();
        return getAvailableRoles(user, available);
    }

    protected List<RoleRepresentation> getAvailableRoles(UserModel user, Set<RoleModel> available) {
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (RoleModel roleModel : available) {
            if (user.hasRole(roleModel)) continue;
            roles.add(roleModel);
        }

        List<RoleRepresentation> mappings = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roles) {
            mappings.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return mappings;
    }

    /**
     * Add applicaiton-level roles to the user role mapping.
     *
     * @param username username (not id!)
     * @param appName app name (not id!)
     * @param roles
     */
    @Path("{username}/role-mappings/applications/{app}")
    @POST
    @Consumes("application/json")
    public void addApplicationRoleMapping(@PathParam("username") String username, @PathParam("app") String appName, List<RoleRepresentation> roles) {
        auth.requireManage();

        logger.debug("addApplicationRoleMapping");
        UserModel user = session.users().getUserByUsername(username, realm);
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
            user.grantRole(roleModel);
        }

    }

    /**
     * Delete application-level roles from user role mapping.
     *
     * @param username username (not id!)
     * @param appName app name (not id!)
     * @param roles
     */
    @Path("{username}/role-mappings/applications/{app}")
    @DELETE
    @Consumes("application/json")
    public void deleteApplicationRoleMapping(@PathParam("username") String username, @PathParam("app") String appName, List<RoleRepresentation> roles) {
        auth.requireManage();

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        ApplicationModel application = realm.getApplicationByName(appName);

        if (application == null) {
            throw new NotFoundException("Application not found");
        }

        if (roles == null) {
            Set<RoleModel> roleModels = user.getApplicationRoleMappings(application);
            for (RoleModel roleModel : roleModels) {
                if (!(roleModel.getContainer() instanceof ApplicationModel)) {
                    ApplicationModel app = (ApplicationModel) roleModel.getContainer();
                    if (!app.getId().equals(application.getId())) continue;
                }
                user.deleteRoleMapping(roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = application.getRole(role.getName());
                if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                    throw new NotFoundException("Role not found");
                }
                user.deleteRoleMapping(roleModel);
            }
        }
    }

    /**
     *  Set up a temporary password for this user.  User will have to reset this temporary password when they log
     *  in next.
     *
     * @param username username (not id!)
     * @param pass temporary password
     */
    @Path("{username}/reset-password")
    @PUT
    @Consumes("application/json")
    public void resetPassword(@PathParam("username") String username, CredentialRepresentation pass) {
        auth.requireManage();

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        if (pass == null || pass.getValue() == null || !CredentialRepresentation.PASSWORD.equals(pass.getType())) {
            throw new BadRequestException("No password provided");
        }

        UserCredentialModel cred = RepresentationToModel.convertCredential(pass);
        user.updateCredential(cred);
        user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
    }

    /**
     *
     *
     * @param username username (not id!)
     */
    @Path("{username}/remove-totp")
    @PUT
    @Consumes("application/json")
    public void removeTotp(@PathParam("username") String username) {
        auth.requireManage();

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        user.setTotp(false);
    }

    /**
     * Send an email to the user with a link they can click to reset their password
     *
     * @param username username (not id!)
     * @return
     */
    @Path("{username}/reset-password-email")
    @PUT
    @Consumes("application/json")
    public Response resetPasswordEmail(@PathParam("username") String username) {
        auth.requireManage();

        UserModel user = session.users().getUserByUsername(username, realm);
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
            return Flows.errors().error("AccountProvider management not enabled", Response.Status.INTERNAL_SERVER_ERROR);
        }

        AccessCode accessCode = tokenManager.createAccessCode(scope, state, redirect, session, realm, client, user, null);
        accessCode.setRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);

        try {
            UriBuilder builder = Urls.loginPasswordResetBuilder(uriInfo.getBaseUri());
            builder.queryParam("key", accessCode.getCode());

            String link = builder.build(realm.getName()).toString();
            long expiration = TimeUnit.SECONDS.toMinutes(realm.getAccessCodeLifespanUserAction());

            session.getProvider(EmailProvider.class).setRealm(realm).setUser(user).sendPasswordReset(link, expiration);

            return Response.ok().build();
        } catch (EmailException e) {
            logger.error("Failed to send password reset email", e);
            return Flows.errors().error("Failed to send email", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}
