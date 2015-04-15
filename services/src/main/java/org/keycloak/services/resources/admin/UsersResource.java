package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.ClientConnection;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelReadOnlyException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.idm.ClientMappingsRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.UserManager;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.Urls;

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
import javax.ws.rs.core.HttpHeaders;
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
    protected ClientConnection clientConnection;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakSession session;

    @Context
    protected HttpHeaders headers;

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
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(final @PathParam("username") String username, final UserRepresentation rep) {
        auth.requireManage();

        try {
            UserModel user = session.users().getUserByUsername(username, realm);
            if (user == null) {
                throw new NotFoundException("User not found");
            }
            updateUserFromRep(user, rep);

            if (session.getTransaction().isActive()) {
                session.getTransaction().commit();
            }

            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("User exists with same username or email");
        } catch (ModelReadOnlyException re) {
            return ErrorResponse.exists("User is read only!");
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
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUser(final @Context UriInfo uriInfo, final UserRepresentation rep) {
        auth.requireManage();

        // Double-check duplicated username and email here due to federation
        if (session.users().getUserByUsername(rep.getUsername(), realm) != null) {
            return ErrorResponse.exists("User exists with same username");
        }
        if (rep.getEmail() != null && session.users().getUserByEmail(rep.getEmail(), realm) != null) {
            return ErrorResponse.exists("User exists with same email");
        }

        try {
            UserModel user = session.users().addUser(realm, rep.getUsername());
            updateUserFromRep(user, rep);

            if (session.getTransaction().isActive()) {
                session.getTransaction().commit();
            }

            return Response.created(uriInfo.getAbsolutePathBuilder().path(user.getUsername()).build()).build();
        } catch (ModelDuplicateException e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().setRollbackOnly();
            }
            return ErrorResponse.exists("User exists with same username or email");
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

            Set<String> attrToRemove = new HashSet<String>(user.getAttributes().keySet());
            attrToRemove.removeAll(rep.getAttributes().keySet());
            for (String attr : attrToRemove) {
                user.removeAttribute(attr);
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
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getUser(final @PathParam("username") String username) {
        auth.requireView();

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        return ModelToRepresentation.toRepresentation(user);
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
    @Path("{username}/federated-identity")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<FederatedIdentityRepresentation> getFederatedIdentity(final @PathParam("username") String username) {
        auth.requireView();
        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        Set<FederatedIdentityModel> identities = session.users().getFederatedIdentities(user, realm);
        List<FederatedIdentityRepresentation> result = new ArrayList<FederatedIdentityRepresentation>();

        for (FederatedIdentityModel identity : identities) {
            for (IdentityProviderModel identityProviderModel : realm.getIdentityProviders()) {
                if (identityProviderModel.getAlias().equals(identity.getIdentityProvider())) {
                    FederatedIdentityRepresentation rep = ModelToRepresentation.toRepresentation(identity);
                    result.add(rep);
                }
            }
        }
        return result;
    }

    @Path("{username}/federated-identity/{provider}")
    @POST
    @NoCache
    public Response addFederatedIdentity(final @PathParam("username") String username, final @PathParam("provider") String provider, FederatedIdentityRepresentation rep) {
        auth.requireManage();
        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        if (session.users().getFederatedIdentity(user, provider, realm) != null) {
            return ErrorResponse.exists("User is already linked with provider");
        }

        FederatedIdentityModel socialLink = new FederatedIdentityModel(provider, rep.getUserId(), rep.getUserName());
        session.users().addFederatedIdentity(realm, user, socialLink);

        return Response.noContent().build();
    }

    @Path("{username}/federated-identity/{provider}")
    @DELETE
    @NoCache
    public void removeFederatedIdentity(final @PathParam("username") String username, final @PathParam("provider") String provider) {
        auth.requireManage();
        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        if (!session.users().removeFederatedIdentity(realm, user, provider)) {
            throw new NotFoundException("Link not found");
        }
    }

    /**
     * Remove all user sessions associated with this user.  And, for all client that have an admin URL, tell
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

        List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, user);
        for (UserSessionModel userSession : userSessions) {
            AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, clientConnection, headers);
        }
    }

    /**
     * delete this user
     *
     * @param username username (not id!)
     */
    @Path("{username}")
    @DELETE
    @NoCache
    public Response deleteUser(final @PathParam("username") String username) {
        auth.requireManage();

        UserRepresentation rep = getUser(username);
        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        boolean removed = new UserManager(session).removeUser(realm, user);
        if (removed) {
            return Response.noContent().build();
        } else {
            return ErrorResponse.error("User couldn't be deleted", Response.Status.BAD_REQUEST);
        }
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
    @Produces(MediaType.APPLICATION_JSON)
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
    @Produces(MediaType.APPLICATION_JSON)
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

        List<ClientModel> clients = realm.getClients();
        if (clients.size() > 0) {
            Map<String, ClientMappingsRepresentation> appMappings = new HashMap<String, ClientMappingsRepresentation>();
            for (ClientModel client : clients) {
                Set<RoleModel> roleMappings = user.getClientRoleMappings(client);
                if (roleMappings.size() > 0) {
                    ClientMappingsRepresentation mappings = new ClientMappingsRepresentation();
                    mappings.setId(client.getId());
                    mappings.setClient(client.getClientId());
                    List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();
                    mappings.setMappings(roles);
                    for (RoleModel role : roleMappings) {
                        roles.add(ModelToRepresentation.toRepresentation(role));
                    }
                    appMappings.put(client.getClientId(), mappings);
                    all.setClientMappings(appMappings);
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
    @Produces(MediaType.APPLICATION_JSON)
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
    @Produces(MediaType.APPLICATION_JSON)
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
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getAvailableRealmRoleMappings(@PathParam("username") String username) {
        auth.requireView();

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        Set<RoleModel> available = realm.getRoles();
        return UserClientRoleMappingsResource.getAvailableRoles(user, available);
    }

    /**
     * Add realm-level role mappings
     *
     * @param username username (not id!)
     * @param roles
     */
    @Path("{username}/role-mappings/realm")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
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
    @Consumes(MediaType.APPLICATION_JSON)
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

    @Path("{username}/role-mappings/clients/{clientId}")
    public UserClientRoleMappingsResource getUserClientRoleMappingsResource(@PathParam("username") String username, @PathParam("clientId") String clientId) {
        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        ClientModel client = realm.getClientByClientId(clientId);

        if (client == null) {
            throw new NotFoundException("Client not found");
        }

        return new UserClientRoleMappingsResource(realm, auth, user, client);

    }
    @Path("{username}/role-mappings/clients-by-id/{id}")
    public UserClientRoleMappingsResource getUserClientRoleMappingsResourceById(@PathParam("username") String username, @PathParam("id") String id) {
        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        ClientModel client = realm.getClientById(id);

        if (client == null) {
            throw new NotFoundException("Client not found");
        }

        return new UserClientRoleMappingsResource(realm, auth, user, client);

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
    @Consumes(MediaType.APPLICATION_JSON)
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
        try {
            session.users().updateCredential(realm, user, cred);
        } catch (IllegalStateException ise) {
            throw new BadRequestException("Resetting to N old passwords is not allowed.");
        } catch (ModelReadOnlyException mre) {
            throw new BadRequestException("Can't reset password as account is read only");
        }
        if (pass.isTemporary()) user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
    }

    /**
     *
     *
     * @param username username (not id!)
     */
    @Path("{username}/remove-totp")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeTotp(@PathParam("username") String username) {
        auth.requireManage();

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        user.setTotp(false);
    }

    /**
     * Send an email to the user with a link they can click to reset their password.
     * The redirectUri and clientId parameters are optional. The default for the
     * redirect is the account client.
     *
     * @param username username (not id!)
     * @param redirectUri redirect uri
     * @param clientId client id
     * @return
     */
    @Path("{username}/reset-password-email")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resetPasswordEmail(@PathParam("username") String username, @QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String redirectUri, @QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId) {
        auth.requireManage();

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            return ErrorResponse.error("User not found", Response.Status.NOT_FOUND);
        }

        if (!user.isEnabled()) {
            return ErrorResponse.error("User is disabled", Response.Status.BAD_REQUEST);
        }

        if (user.getEmail() == null) {
            return ErrorResponse.error("User email missing", Response.Status.BAD_REQUEST);
        }

        if(redirectUri != null && clientId == null){
            return ErrorResponse.error("Client id missing", Response.Status.BAD_REQUEST);
        }

        if(clientId == null){
            clientId = Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
        }

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null || !client.isEnabled()) {
            return ErrorResponse.error(clientId + " not enabled", Response.Status.INTERNAL_SERVER_ERROR);
        }

        String redirect;
        if(redirectUri != null){
            redirect = RedirectUtils.verifyRedirectUri(uriInfo, redirectUri, realm, client);
            if(redirect == null){
                return ErrorResponse.error("Invalid redirect uri.", Response.Status.BAD_REQUEST);
            }
        }else{
            redirect = Urls.accountBase(uriInfo.getBaseUri()).path("/").build(realm.getName()).toString();
        }


        UserSessionModel userSession = session.sessions().createUserSession(realm, user, username, clientConnection.getRemoteAddr(), "form", false, null, null);
        //audit.session(userSession);
        ClientSessionModel clientSession = session.sessions().createClientSession(realm, client);
        clientSession.setAuthMethod(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientSession.setRedirectUri(redirect);
        clientSession.setUserSession(userSession);
        ClientSessionCode accessCode = new ClientSessionCode(realm, clientSession);

        accessCode.setAction(ClientSessionModel.Action.RECOVER_PASSWORD);

        try {
            UriBuilder builder = Urls.loginPasswordResetBuilder(uriInfo.getBaseUri());
            builder.queryParam("key", accessCode.getCode());

            String link = builder.build(realm.getName()).toString();
            long expiration = TimeUnit.SECONDS.toMinutes(realm.getAccessCodeLifespanUserAction());

            this.session.getProvider(EmailProvider.class).setRealm(realm).setUser(user).sendPasswordReset(link, expiration);

            //audit.user(user).detail(Details.EMAIL, user.getEmail()).detail(Details.CODE_ID, accessCode.getCodeId()).success();
            return Response.ok().build();
        } catch (EmailException e) {
            logger.error("Failed to send password reset email", e);
            return ErrorResponse.error("Failed to send email", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}
