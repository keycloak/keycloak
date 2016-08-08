/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.ClientConnection;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelReadOnlyException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserConsentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.UserManager;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.WebApplicationException;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.AccountService;
import org.keycloak.common.util.Time;
import org.keycloak.services.validation.Validation;

import static org.keycloak.events.admin.ResourceType.GROUP_MEMBERSHIP;

/**
 * Base resource for managing users
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UsersResource {
    protected static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    protected RealmModel realm;

    private RealmAuth auth;

    private AdminEventBuilder adminEvent;

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakSession session;

    @Context
    protected HttpHeaders headers;

    public UsersResource(RealmModel realm, RealmAuth auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent.resource(ResourceType.USER);

        auth.init(RealmAuth.Resource.USER);
    }

    /**
     * Update the user
     *
     * @param id User id
     * @param rep
     * @return
     */
    @Path("{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(final @PathParam("id") String id, final UserRepresentation rep) {
        auth.requireManage();

        try {
            UserModel user = session.users().getUserById(id, realm);
            if (user == null) {
                throw new NotFoundException("User not found");
            }

             Set<String> attrsToRemove;
            if (rep.getAttributes() != null) {
                attrsToRemove = new HashSet<>(user.getAttributes().keySet());
                attrsToRemove.removeAll(rep.getAttributes().keySet());
            } else {
                attrsToRemove = Collections.emptySet();
            }

            if (rep.isEnabled() != null && rep.isEnabled()) {
                UserLoginFailureModel failureModel = session.sessions().getUserLoginFailure(realm, id);
                if (failureModel != null) {
                    failureModel.clearFailures();
                }
            }

            updateUserFromRep(user, rep, attrsToRemove, realm, session, true);
            adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo).representation(rep).success();

            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().commit();
            }
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("User exists with same username or email");
        } catch (ModelReadOnlyException re) {
            return ErrorResponse.exists("User is read only!");
        } catch (ModelException me) {
            return ErrorResponse.exists("Could not update user!");
        }
    }

    /**
     * Create a new user
     *
     * Username must be unique.
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
            Set<String> emptySet = Collections.emptySet();
            updateUserFromRep(user, rep, emptySet, realm, session, false);

            adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo, user.getId()).representation(rep).success();

            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().commit();
            }

            return Response.created(uriInfo.getAbsolutePathBuilder().path(user.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            return ErrorResponse.exists("User exists with same username or email");
        } catch (ModelException me){
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            return ErrorResponse.exists("Could not create user");
        }
    }

    public static void updateUserFromRep(UserModel user, UserRepresentation rep, Set<String> attrsToRemove, RealmModel realm, KeycloakSession session, boolean removeMissingRequiredActions) {
        if (rep.getUsername() != null && realm.isEditUsernameAllowed()) {
            user.setUsername(rep.getUsername());
        }
        if (rep.getEmail() != null) user.setEmail(rep.getEmail());
        if (rep.getFirstName() != null) user.setFirstName(rep.getFirstName());
        if (rep.getLastName() != null) user.setLastName(rep.getLastName());

        if (rep.isEnabled() != null) user.setEnabled(rep.isEnabled());
        if (rep.isTotp() != null) user.setOtpEnabled(rep.isTotp());
        if (rep.isEmailVerified() != null) user.setEmailVerified(rep.isEmailVerified());

        List<String> reqActions = rep.getRequiredActions();

        if (reqActions != null) {
            Set<String> allActions = new HashSet<>();
            for (ProviderFactory factory : session.getKeycloakSessionFactory().getProviderFactories(RequiredActionProvider.class)) {
                allActions.add(factory.getId());
            }
            for (String action : allActions) {
                if (reqActions.contains(action)) {
                    user.addRequiredAction(action);
                } else if (removeMissingRequiredActions) {
                    user.removeRequiredAction(action);
                }
            }
        }

        if (rep.getAttributesAsListValues() != null) {
            for (Map.Entry<String, List<String>> attr : rep.getAttributesAsListValues().entrySet()) {
                user.setAttribute(attr.getKey(), attr.getValue());
            }

            for (String attr : attrsToRemove) {
                user.removeAttribute(attr);
            }
        }
    }

    /**
     * Get represenation of the user
     *
     * @param id User id
     * @return
     */
    @Path("{id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getUser(final @PathParam("id") String id) {
        auth.requireView();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        UserRepresentation rep = ModelToRepresentation.toRepresentation(user);

        if (realm.isIdentityFederationEnabled()) {
            List<FederatedIdentityRepresentation> reps = getFederatedIdentities(user);
            rep.setFederatedIdentities(reps);
        }

        if (session.getProvider(BruteForceProtector.class).isTemporarilyDisabled(session, realm, user)) {
            rep.setEnabled(false);
        }

        return rep;
    }

    /**
     * Impersonate the user
     *
     * @param id User id
     * @return
     */
    @Path("{id}/impersonation")
    @POST
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> impersonate(final @PathParam("id") String id) {
        auth.init(RealmAuth.Resource.IMPERSONATION);
        auth.requireManage();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        RealmModel authenticatedRealm = auth.getAuth().getRealm();
        // if same realm logout before impersonation
        boolean sameRealm = false;
        if (authenticatedRealm.getId().equals(realm.getId())) {
            sameRealm = true;
            UserSessionModel userSession = session.sessions().getUserSession(authenticatedRealm, auth.getAuth().getToken().getSessionState());
            AuthenticationManager.expireIdentityCookie(realm, uriInfo, clientConnection);
            AuthenticationManager.expireRememberMeCookie(realm, uriInfo, clientConnection);
            AuthenticationManager.backchannelLogout(session, authenticatedRealm, userSession, uriInfo, clientConnection, headers, true);
        }
        EventBuilder event = new EventBuilder(realm, session, clientConnection);

        UserSessionModel userSession = session.sessions().createUserSession(realm, user, user.getUsername(), clientConnection.getRemoteAddr(), "impersonate", false, null, null);
        AuthenticationManager.createLoginCookie(session, realm, userSession.getUser(), userSession, uriInfo, clientConnection);
        URI redirect = AccountService.accountServiceApplicationPage(uriInfo).build(realm.getName());
        Map<String, Object> result = new HashMap<>();
        result.put("sameRealm", sameRealm);
        result.put("redirect", redirect.toString());
        event.event(EventType.IMPERSONATE)
             .session(userSession)
             .user(user)
             .detail(Details.IMPERSONATOR_REALM,authenticatedRealm.getName())
             .detail(Details.IMPERSONATOR, auth.getAuth().getUser().getUsername()).success();

        return result;
    }


    /**
     * Get sessions associated with the user
     *
     * @param id User id
     * @return
     */
    @Path("{id}/sessions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserSessionRepresentation> getSessions(final @PathParam("id") String id) {
        auth.requireView();

        UserModel user = session.users().getUserById(id, realm);
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
     * Get offline sessions associated with the user and client
     *
     * @param id User id
     * @return
     */
    @Path("{id}/offline-sessions/{clientId}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserSessionRepresentation> getSessions(final @PathParam("id") String id, final @PathParam("clientId") String clientId) {
        auth.requireView();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        ClientModel client = realm.getClientById(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        List<UserSessionModel> sessions = new UserSessionManager(session).findOfflineSessions(realm, client, user);
        List<UserSessionRepresentation> reps = new ArrayList<UserSessionRepresentation>();
        for (UserSessionModel session : sessions) {
            UserSessionRepresentation rep = ModelToRepresentation.toRepresentation(session);

            // Update lastSessionRefresh with the timestamp from clientSession
            for (ClientSessionModel clientSession : session.getClientSessions()) {
                if (clientId.equals(clientSession.getClient().getId())) {
                    rep.setLastAccess(Time.toMillis(clientSession.getTimestamp()));
                    break;
                }
            }

            reps.add(rep);
        }
        return reps;
    }

    /**
     * Get social logins associated with the user
     *
     * @param id User id
     * @return
     */
    @Path("{id}/federated-identity")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<FederatedIdentityRepresentation> getFederatedIdentity(final @PathParam("id") String id) {
        auth.requireView();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        return getFederatedIdentities(user);
    }

    private List<FederatedIdentityRepresentation> getFederatedIdentities(UserModel user) {
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

    /**
     * Add a social login provider to the user
     *
     * @param id User id
     * @param provider Social login provider id
     * @param rep
     * @return
     */
    @Path("{id}/federated-identity/{provider}")
    @POST
    @NoCache
    public Response addFederatedIdentity(final @PathParam("id") String id, final @PathParam("provider") String provider, FederatedIdentityRepresentation rep) {
        auth.requireManage();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        if (session.users().getFederatedIdentity(user, provider, realm) != null) {
            return ErrorResponse.exists("User is already linked with provider");
        }

        FederatedIdentityModel socialLink = new FederatedIdentityModel(provider, rep.getUserId(), rep.getUserName());
        session.users().addFederatedIdentity(realm, user, socialLink);
        adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo).representation(rep).success();
        return Response.noContent().build();
    }

    /**
     * Remove a social login provider from user
     *
     * @param id User id
     * @param provider Social login provider id
     */
    @Path("{id}/federated-identity/{provider}")
    @DELETE
    @NoCache
    public void removeFederatedIdentity(final @PathParam("id") String id, final @PathParam("provider") String provider) {
        auth.requireManage();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        if (!session.users().removeFederatedIdentity(realm, user, provider)) {
            throw new NotFoundException("Link not found");
        }
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
    }

    /**
     * Get consents granted by the user
     *
     * @param id User id
     * @return
     */
    @Path("{id}/consents")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getConsents(final @PathParam("id") String id) {
        auth.requireView();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        List<Map<String, Object>> result = new LinkedList<>();

        Set<ClientModel> offlineClients = new UserSessionManager(session).findClientsWithOfflineToken(realm, user);

        for (ClientModel client : realm.getClients()) {
            UserConsentModel consent = session.users().getConsentByClient(realm, user, client.getId());
            boolean hasOfflineToken = offlineClients.contains(client);

            if (consent == null && !hasOfflineToken) {
                continue;
            }

            UserConsentRepresentation rep = (consent == null) ? null : ModelToRepresentation.toRepresentation(consent);

            Map<String, Object> currentRep = new HashMap<>();
            currentRep.put("clientId", client.getClientId());
            currentRep.put("grantedProtocolMappers", (rep==null ? Collections.emptyMap() : rep.getGrantedProtocolMappers()));
            currentRep.put("grantedRealmRoles", (rep==null ? Collections.emptyList() : rep.getGrantedRealmRoles()));
            currentRep.put("grantedClientRoles", (rep==null ? Collections.emptyMap() : rep.getGrantedClientRoles()));

            List<Map<String, String>> additionalGrants = new LinkedList<>();
            if (hasOfflineToken) {
                Map<String, String> offlineTokens = new HashMap<>();
                offlineTokens.put("client", client.getId());
                // TODO: translate
                offlineTokens.put("key", "Offline Token");
                additionalGrants.add(offlineTokens);
            }
            currentRep.put("additionalGrants", additionalGrants);

            result.add(currentRep);
        }

        return result;
    }

    /**
     * Revoke consent and offline tokens for particular client from user
     *
     * @param id User id
     * @param clientId Client id
     */
    @Path("{id}/consents/{client}")
    @DELETE
    @NoCache
    public void revokeConsent(final @PathParam("id") String id, final @PathParam("client") String clientId) {
        auth.requireManage();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        ClientModel client = realm.getClientByClientId(clientId);
        boolean revokedConsent = session.users().revokeConsentForClient(realm, user, client.getId());
        boolean revokedOfflineToken = new UserSessionManager(session).revokeOfflineToken(user, client);

        if (revokedConsent) {
            // Logout clientSessions for this user and client
            AuthenticationManager.backchannelUserFromClient(session, realm, user, client, uriInfo, headers);
        }

        if (!revokedConsent && !revokedOfflineToken) {
            throw new NotFoundException("Consent nor offline token not found");
        }
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
    }

    /**
     * Remove all user sessions associated with the user
     *
     * Also send notification to all clients that have an admin URL to invalidate the sessions for the particular user.
     *
     * @param id User id
     */
    @Path("{id}/logout")
    @POST
    public void logout(final @PathParam("id") String id) {
        auth.requireManage();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, user);
        for (UserSessionModel userSession : userSessions) {
            AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, clientConnection, headers, true);
        }
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
    }

    /**
     * Delete the user
     *
     * @param id User id
     */
    @Path("{id}")
    @DELETE
    @NoCache
    public Response deleteUser(final @PathParam("id") String id) {
        auth.requireManage();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        boolean removed = new UserManager(session).removeUser(realm, user);
        if (removed) {
            adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
            return Response.noContent().build();
        } else {
            return ErrorResponse.error("User couldn't be deleted", Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Get users
     *
     * Returns a list of users, filtered according to query parameters
     *
     * @param search A String contained in username, first or last name, or email
     * @param last
     * @param first
     * @param email
     * @param username
     * @param first Pagination offset
     * @param maxResults Pagination size
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
            userModels = session.users().searchForUser(attributes, realm, firstResult, maxResults);
        } else {
            userModels = session.users().getUsers(realm, firstResult, maxResults, false);
        }

        for (UserModel user : userModels) {
            results.add(ModelToRepresentation.toRepresentation(user));
        }
        return results;
    }

    @Path("count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Integer getUsersCount() {
        auth.requireView();

        return session.users().getUsersCount(realm);
    }

    @Path("{id}/role-mappings")
    public RoleMapperResource getRoleMappings(@PathParam("id") String id) {
        auth.init(RealmAuth.Resource.USER);

        UserModel user = session.users().getUserById(id, realm);

        RoleMapperResource resource =  new RoleMapperResource(realm, auth, user, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;

    }

     /**
     * Set up a temporary password for the user
     *
     * User will have to reset the temporary password next time they log in.
     *
     * @param id User id
     * @param pass A Temporary password
     */
    @Path("{id}/reset-password")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void resetPassword(@PathParam("id") String id, CredentialRepresentation pass) {
        auth.requireManage();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        if (pass == null || pass.getValue() == null || !CredentialRepresentation.PASSWORD.equals(pass.getType())) {
            throw new BadRequestException("No password provided");
        }
        if (Validation.isBlank(pass.getValue())) {
            throw new BadRequestException("Empty password not allowed");
        }

        UserCredentialModel cred = RepresentationToModel.convertCredential(pass);
        try {
            session.users().updateCredential(realm, user, cred);
        } catch (IllegalStateException ise) {
            throw new BadRequestException("Resetting to N old passwords is not allowed.");
        } catch (ModelReadOnlyException mre) {
            throw new BadRequestException("Can't reset password as account is read only");
        } catch (ModelException e) {
            Properties messages = AdminRoot.getMessages(session, realm, auth.getAuth().getToken().getLocale());
            throw new ErrorResponseException(e.getMessage(), MessageFormat.format(messages.getProperty(e.getMessage(), e.getMessage()), e.getParameters()),
                    Status.BAD_REQUEST);
        }
        if (pass.isTemporary() != null && pass.isTemporary()) user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);

        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
    }

    /**
     * Remove TOTP from the user
     *
     * @param id User id
     */
    @Path("{id}/remove-totp")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeTotp(@PathParam("id") String id) {
        auth.requireManage();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        user.setOtpEnabled(false);
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
    }

    /**
     * Send an email to the user with a link they can click to reset their password.
     * The redirectUri and clientId parameters are optional. The default for the
     * redirect is the account client.
     *
     * This endpoint has been deprecated.  Please use the execute-actions-email passing a list with
     * UPDATE_PASSWORD within it.
     *
     * @param id
     * @param redirectUri redirect uri
     * @param clientId client id
     * @return
     */
    @Deprecated
    @Path("{id}/reset-password-email")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resetPasswordEmail(@PathParam("id") String id,
                                        @QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String redirectUri,
                                        @QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId) {
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        return executeActionsEmail(id, redirectUri, clientId, actions);
    }


    /**
     * Send a update account email to the user
     *
     * An email contains a link the user can click to perform a set of required actions.
     * The redirectUri and clientId parameters are optional. The default for the
     * redirect is the account client.
     *
     * @param id User is
     * @param redirectUri Redirect uri
     * @param clientId Client id
     * @param actions required actions the user needs to complete
     * @return
     */
    @Path("{id}/execute-actions-email")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response executeActionsEmail(@PathParam("id") String id,
                                        @QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String redirectUri,
                                        @QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId,
                                        List<String> actions) {
        auth.requireManage();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            return ErrorResponse.error("User not found", Response.Status.NOT_FOUND);
        }

        if (user.getEmail() == null) {
            return ErrorResponse.error("User email missing", Response.Status.BAD_REQUEST);
        }

        ClientSessionModel clientSession = createClientSession(user, redirectUri, clientId);
        for (String action : actions) {
            clientSession.addRequiredAction(action);
        }
        ClientSessionCode accessCode = new ClientSessionCode(realm, clientSession);
        accessCode.setAction(ClientSessionModel.Action.EXECUTE_ACTIONS.name());

        try {
            UriBuilder builder = Urls.executeActionsBuilder(uriInfo.getBaseUri());
            builder.queryParam("key", accessCode.getCode());

            String link = builder.build(realm.getName()).toString();
            long expiration = TimeUnit.SECONDS.toMinutes(realm.getAccessCodeLifespanUserAction());

            this.session.getProvider(EmailTemplateProvider.class).setRealm(realm).setUser(user).sendExecuteActions(link, expiration);

            //audit.user(user).detail(Details.EMAIL, user.getEmail()).detail(Details.CODE_ID, accessCode.getCodeId()).success();

            adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();

            return Response.ok().build();
        } catch (EmailException e) {
            logger.failedToSendActionsEmail(e);
            return ErrorResponse.error("Failed to send execute actions email", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Send an email-verification email to the user
     *
     * An email contains a link the user can click to verify their email address.
     * The redirectUri and clientId parameters are optional. The default for the
     * redirect is the account client.
     *
     * @param id User id
     * @param redirectUri Redirect uri
     * @param clientId Client id
     * @return
     */
    @Path("{id}/send-verify-email")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendVerifyEmail(@PathParam("id") String id, @QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String redirectUri, @QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId) {
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.VERIFY_EMAIL.name());
        return executeActionsEmail(id, redirectUri, clientId, actions);
    }

    private ClientSessionModel createClientSession(UserModel user, String redirectUri, String clientId) {

        if (!user.isEnabled()) {
            throw new WebApplicationException(
                ErrorResponse.error("User is disabled", Response.Status.BAD_REQUEST));
        }

        if (redirectUri != null && clientId == null) {
            throw new WebApplicationException(
                ErrorResponse.error("Client id missing", Response.Status.BAD_REQUEST));
        }

        if (clientId == null) {
            clientId = Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
        }

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null || !client.isEnabled()) {
            throw new WebApplicationException(
                ErrorResponse.error(clientId + " not enabled", Response.Status.BAD_REQUEST));
        }

        String redirect;
        if (redirectUri != null) {
            redirect = RedirectUtils.verifyRedirectUri(uriInfo, redirectUri, realm, client);
            if (redirect == null) {
                throw new WebApplicationException(
                    ErrorResponse.error("Invalid redirect uri.", Response.Status.BAD_REQUEST));
            }
        } else {
            redirect = Urls.accountBase(uriInfo.getBaseUri()).path("/").build(realm.getName()).toString();
        }


        UserSessionModel userSession = session.sessions().createUserSession(realm, user, user.getUsername(), clientConnection.getRemoteAddr(), "form", false, null, null);
        //audit.session(userSession);
        ClientSessionModel clientSession = session.sessions().createClientSession(realm, client);
        clientSession.setAuthMethod(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientSession.setRedirectUri(redirect);
        clientSession.setUserSession(userSession);

        return clientSession;
    }

    @GET
    @Path("{id}/groups")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<GroupRepresentation> groupMembership(@PathParam("id") String id) {
        auth.requireView();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        List<GroupRepresentation> memberships = new LinkedList<>();
        for (GroupModel group : user.getGroups()) {
            memberships.add(ModelToRepresentation.toRepresentation(group, false));
        }
        return memberships;
    }

    @DELETE
    @Path("{id}/groups/{groupId}")
    @NoCache
    public void removeMembership(@PathParam("id") String id, @PathParam("groupId") String groupId) {
        auth.requireManage();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        GroupModel group = session.realms().getGroupById(groupId, realm);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }

        try {
            if (user.isMemberOf(group)){
                user.leaveGroup(group);
                adminEvent.operation(OperationType.DELETE).resource(ResourceType.GROUP_MEMBERSHIP).representation(ModelToRepresentation.toRepresentation(group, true)).resourcePath(uriInfo).success();
            }
        } catch (ModelException me) {
            Properties messages = AdminRoot.getMessages(session, realm, auth.getAuth().getToken().getLocale());
            throw new ErrorResponseException(me.getMessage(), MessageFormat.format(messages.getProperty(me.getMessage(), me.getMessage()), me.getParameters()),
                    Response.Status.BAD_REQUEST);
        }
    }

    @PUT
    @Path("{id}/groups/{groupId}")
    @NoCache
    public void joinGroup(@PathParam("id") String id, @PathParam("groupId") String groupId) {
        auth.requireManage();

        UserModel user = session.users().getUserById(id, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        GroupModel group = session.realms().getGroupById(groupId, realm);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        if (!user.isMemberOf(group)){
            user.joinGroup(group);
            adminEvent.operation(OperationType.CREATE).resource(ResourceType.GROUP_MEMBERSHIP).representation(ModelToRepresentation.toRepresentation(group, true)).resourcePath(uriInfo).success();
        }
    }

}
