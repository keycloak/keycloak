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

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserConsentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.account.AccountFormService;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.validation.Validation;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.utils.ProfileHelper;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
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

/**
 * Base resource for managing users
 *
 * @resource Users
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserResource {
    private static final Logger logger = Logger.getLogger(UserResource.class);

    protected RealmModel realm;

    private AdminPermissionEvaluator auth;

    private AdminEventBuilder adminEvent;
    private UserModel user;

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakSession session;

    @Context
    protected HttpHeaders headers;

    public UserResource(RealmModel realm, UserModel user, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.user = user;
        this.adminEvent = adminEvent.resource(ResourceType.USER);
    }

    /**
     * Update the user
     *
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(final UserRepresentation rep) {

        auth.users().requireManage(user);
        try {
            Set<String> attrsToRemove;
            if (rep.getAttributes() != null) {
                attrsToRemove = new HashSet<>(user.getAttributes().keySet());
                attrsToRemove.removeAll(rep.getAttributes().keySet());
            } else {
                attrsToRemove = Collections.emptySet();
            }

            if (rep.isEnabled() != null && rep.isEnabled()) {
                UserLoginFailureModel failureModel = session.sessions().getUserLoginFailure(realm, user.getId());
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
        } catch (ReadOnlyException re) {
            return ErrorResponse.exists("User is read only!");
        } catch (ModelException me) {
            logger.warn("Could not update user!", me);
            return ErrorResponse.exists("Could not update user!");
        } catch (ForbiddenException fe) {
            throw fe;
        } catch (Exception me) { // JPA
            logger.warn("Could not update user!", me);// may be committed by JTA which can't
            return ErrorResponse.exists("Could not update user!");
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
        if (rep.isEmailVerified() != null) user.setEmailVerified(rep.isEmailVerified());

        if (rep.getFederationLink() != null) user.setFederationLink(rep.getFederationLink());

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

        if (rep.getAttributes() != null) {
            for (Map.Entry<String, List<String>> attr : rep.getAttributes().entrySet()) {
                user.setAttribute(attr.getKey(), attr.getValue());
            }

            for (String attr : attrsToRemove) {
                user.removeAttribute(attr);
            }
        }
    }

    /**
     * Get representation of the user
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getUser() {
        auth.users().requireView(user);

        UserRepresentation rep = ModelToRepresentation.toRepresentation(session, realm, user);

        if (realm.isIdentityFederationEnabled()) {
            List<FederatedIdentityRepresentation> reps = getFederatedIdentities(user);
            rep.setFederatedIdentities(reps);
        }

        if (session.getProvider(BruteForceProtector.class).isTemporarilyDisabled(session, realm, user)) {
            rep.setEnabled(false);
        }
        rep.setAccess(auth.users().getAccess(user));

        return rep;
    }

    /**
     * Impersonate the user
     *
     * @return
     */
    @Path("impersonation")
    @POST
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> impersonate() {
        ProfileHelper.requireFeature(Profile.Feature.IMPERSONATION);

        auth.users().requireImpersonate(user);
        RealmModel authenticatedRealm = auth.adminAuth().getRealm();
        // if same realm logout before impersonation
        boolean sameRealm = false;
        if (authenticatedRealm.getId().equals(realm.getId())) {
            sameRealm = true;
            UserSessionModel userSession = session.sessions().getUserSession(authenticatedRealm, auth.adminAuth().getToken().getSessionState());
            AuthenticationManager.expireIdentityCookie(realm, uriInfo, clientConnection);
            AuthenticationManager.expireRememberMeCookie(realm, uriInfo, clientConnection);
            AuthenticationManager.backchannelLogout(session, authenticatedRealm, userSession, uriInfo, clientConnection, headers, true);
        }
        EventBuilder event = new EventBuilder(realm, session, clientConnection);

        String sessionId = KeycloakModelUtils.generateId();
        UserSessionModel userSession = session.sessions().createUserSession(sessionId, realm, user, user.getUsername(), clientConnection.getRemoteAddr(), "impersonate", false, null, null);
        AuthenticationManager.createLoginCookie(session, realm, userSession.getUser(), userSession, uriInfo, clientConnection);
        URI redirect = AccountFormService.accountServiceApplicationPage(uriInfo).build(realm.getName());
        Map<String, Object> result = new HashMap<>();
        result.put("sameRealm", sameRealm);
        result.put("redirect", redirect.toString());
        event.event(EventType.IMPERSONATE)
             .session(userSession)
             .user(user)
             .detail(Details.IMPERSONATOR_REALM,authenticatedRealm.getName())
             .detail(Details.IMPERSONATOR, auth.adminAuth().getUser().getUsername()).success();

        return result;
    }


    /**
     * Get sessions associated with the user
     *
     * @return
     */
    @Path("sessions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserSessionRepresentation> getSessions() {
        auth.users().requireView(user);
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
     * @return
     */
    @Path("offline-sessions/{clientId}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserSessionRepresentation> getOfflineSessions(final @PathParam("clientId") String clientId) {
        auth.users().requireView(user);
        ClientModel client = realm.getClientById(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        List<UserSessionModel> sessions = new UserSessionManager(session).findOfflineSessions(realm, user);
        List<UserSessionRepresentation> reps = new ArrayList<UserSessionRepresentation>();
        for (UserSessionModel session : sessions) {
            UserSessionRepresentation rep = ModelToRepresentation.toRepresentation(session);

            // Update lastSessionRefresh with the timestamp from clientSession
            AuthenticatedClientSessionModel clientSession = session.getAuthenticatedClientSessions().get(clientId);

            // Skip if userSession is not for this client
            if (clientSession == null) {
                continue;
            }

            rep.setLastAccess(clientSession.getTimestamp());

            reps.add(rep);
        }
        return reps;
    }

    /**
     * Get social logins associated with the user
     *
     * @return
     */
    @Path("federated-identity")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<FederatedIdentityRepresentation> getFederatedIdentity() {
        auth.users().requireView(user);

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
     * @param provider Social login provider id
     * @param rep
     * @return
     */
    @Path("federated-identity/{provider}")
    @POST
    @NoCache
    public Response addFederatedIdentity(final @PathParam("provider") String provider, FederatedIdentityRepresentation rep) {
        auth.users().requireManage(user);
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
     * @param provider Social login provider id
     */
    @Path("federated-identity/{provider}")
    @DELETE
    @NoCache
    public void removeFederatedIdentity(final @PathParam("provider") String provider) {
        auth.users().requireManage(user);
        if (!session.users().removeFederatedIdentity(realm, user, provider)) {
            throw new NotFoundException("Link not found");
        }
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
    }

    /**
     * Get consents granted by the user
     *
     * @return
     */
    @Path("consents")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getConsents() {
        auth.users().requireView(user);
        List<Map<String, Object>> result = new LinkedList<>();

        Set<ClientModel> offlineClients = new UserSessionManager(session).findClientsWithOfflineToken(realm, user);

        for (ClientModel client : realm.getClients()) {
            UserConsentModel consent = session.users().getConsentByClient(realm, user.getId(), client.getId());
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
            currentRep.put("createdDate", (rep==null ? null : rep.getCreatedDate()));
            currentRep.put("lastUpdatedDate", (rep==null ? null : rep.getLastUpdatedDate()));

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
     * @param clientId Client id
     */
    @Path("consents/{client}")
    @DELETE
    @NoCache
    public void revokeConsent(final @PathParam("client") String clientId) {
        auth.users().requireManage(user);

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        boolean revokedConsent = session.users().revokeConsentForClient(realm, user.getId(), client.getId());
        boolean revokedOfflineToken = new UserSessionManager(session).revokeOfflineToken(user, client);

        if (revokedConsent) {
            // Logout clientSessions for this user and client
            AuthenticationManager.backchannelLogoutUserFromClient(session, realm, user, client, uriInfo, headers);
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
     */
    @Path("logout")
    @POST
    public void logout() {
        auth.users().requireManage(user);

        session.users().setNotBeforeForUser(realm, user, Time.currentTime());

        List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, user);
        for (UserSessionModel userSession : userSessions) {
            AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, clientConnection, headers, true);
        }
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
    }

    /**
     * Delete the user
     */
    @DELETE
    @NoCache
    public Response deleteUser() {
        auth.users().requireManage(user);

        boolean removed = new UserManager(session).removeUser(realm, user);
        if (removed) {
            adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
            return Response.noContent().build();
        } else {
            return ErrorResponse.error("User couldn't be deleted", Status.BAD_REQUEST);
        }
    }

    @Path("role-mappings")
    public RoleMapperResource getRoleMappings() {
        AdminPermissionEvaluator.RequirePermissionCheck manageCheck = () -> auth.users().requireMapRoles(user);
        AdminPermissionEvaluator.RequirePermissionCheck viewCheck = () -> auth.users().requireView(user);
        RoleMapperResource resource =  new RoleMapperResource(realm, auth, user, adminEvent, manageCheck, viewCheck);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;

    }

    /**
     * Disable all credentials for a user of a specific type
     *
     * @param credentialTypes
     */
    @Path("disable-credential-types")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void disableCredentialType(List<String> credentialTypes) {
        auth.users().requireManage(user);
        if (credentialTypes == null) return;
        for (String type : credentialTypes) {
            session.userCredentialManager().disableCredentialType(realm, user, type);

        }


    }

    /**
     * Set up a temporary password for the user
     *
     * User will have to reset the temporary password next time they log in.
     *
     * @param pass A Temporary password
     */
    @Path("reset-password")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void resetPassword(CredentialRepresentation pass) {
        auth.users().requireManage(user);
        if (pass == null || pass.getValue() == null || !CredentialRepresentation.PASSWORD.equals(pass.getType())) {
            throw new BadRequestException("No password provided");
        }
        if (Validation.isBlank(pass.getValue())) {
            throw new BadRequestException("Empty password not allowed");
        }

        UserCredentialModel cred = UserCredentialModel.password(pass.getValue(), true);
        try {
            session.userCredentialManager().updateCredential(realm, user, cred);
        } catch (IllegalStateException ise) {
            throw new BadRequestException("Resetting to N old passwords is not allowed.");
        } catch (ReadOnlyException mre) {
            throw new BadRequestException("Can't reset password as account is read only");
        } catch (ModelException e) {
            Properties messages = AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale());
            throw new ErrorResponseException(e.getMessage(), MessageFormat.format(messages.getProperty(e.getMessage(), e.getMessage()), e.getParameters()),
                    Status.BAD_REQUEST);
        }
        if (pass.isTemporary() != null && pass.isTemporary()) user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);

        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
    }

    /**
     * Remove TOTP from the user
     *
     */
    @Path("remove-totp")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeTotp() {
        auth.users().requireManage(user);

        session.userCredentialManager().disableCredentialType(realm, user, CredentialModel.OTP);
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
     * @param redirectUri redirect uri
     * @param clientId client id
     * @return
     */
    @Deprecated
    @Path("reset-password-email")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resetPasswordEmail(@QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String redirectUri,
                                       @QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId) {
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        return executeActionsEmail(redirectUri, clientId, null, actions);
    }


    /**
     * Send a update account email to the user
     *
     * An email contains a link the user can click to perform a set of required actions.
     * The redirectUri and clientId parameters are optional. If no redirect is given, then there will
     * be no link back to click after actions have completed.  Redirect uri must be a valid uri for the
     * particular clientId.
     *
     * @param redirectUri Redirect uri
     * @param clientId Client id
     * @param lifespan Number of seconds after which the generated token expires
     * @param actions required actions the user needs to complete
     * @return
     */
    @Path("execute-actions-email")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response executeActionsEmail(@QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String redirectUri,
                                        @QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId,
                                        @QueryParam("lifespan") Integer lifespan,
                                        List<String> actions) {
        auth.users().requireManage(user);

        if (user.getEmail() == null) {
            return ErrorResponse.error("User email missing", Status.BAD_REQUEST);
        }

        if (!user.isEnabled()) {
            throw new WebApplicationException(
                ErrorResponse.error("User is disabled", Status.BAD_REQUEST));
        }

        if (redirectUri != null && clientId == null) {
            throw new WebApplicationException(
                ErrorResponse.error("Client id missing", Status.BAD_REQUEST));
        }

        if (clientId == null) {
            clientId = Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
        }

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null || !client.isEnabled()) {
            throw new WebApplicationException(
                ErrorResponse.error(clientId + " not enabled", Status.BAD_REQUEST));
        }

        String redirect;
        if (redirectUri != null) {
            redirect = RedirectUtils.verifyRedirectUri(uriInfo, redirectUri, realm, client);
            if (redirect == null) {
                throw new WebApplicationException(
                    ErrorResponse.error("Invalid redirect uri.", Status.BAD_REQUEST));
            }
        }

        if (lifespan == null) {
            lifespan = realm.getActionTokenGeneratedByAdminLifespan();
        }
        int expiration = Time.currentTime() + lifespan;
        ExecuteActionsActionToken token = new ExecuteActionsActionToken(user.getId(), expiration, actions, redirectUri, clientId);

        try {
            UriBuilder builder = LoginActionsService.actionTokenProcessor(uriInfo);
            builder.queryParam("key", token.serialize(session, realm, uriInfo));

            String link = builder.build(realm.getName()).toString();

            this.session.getProvider(EmailTemplateProvider.class)
              .setAttribute(Constants.TEMPLATE_ATTR_REQUIRED_ACTIONS, token.getRequiredActions())
              .setRealm(realm)
              .setUser(user)
              .sendExecuteActions(link, TimeUnit.SECONDS.toMinutes(lifespan));

            //audit.user(user).detail(Details.EMAIL, user.getEmail()).detail(Details.CODE_ID, accessCode.getCodeId()).success();

            adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();

            return Response.ok().build();
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendActionsEmail(e);
            return ErrorResponse.error("Failed to send execute actions email", Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Send an email-verification email to the user
     *
     * An email contains a link the user can click to verify their email address.
     * The redirectUri and clientId parameters are optional. The default for the
     * redirect is the account client.
     *
     * @param redirectUri Redirect uri
     * @param clientId Client id
     * @return
     */
    @Path("send-verify-email")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendVerifyEmail(@QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String redirectUri, @QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId) {
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.VERIFY_EMAIL.name());
        return executeActionsEmail(redirectUri, clientId, null, actions);
    }

    @GET
    @Path("groups")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<GroupRepresentation> groupMembership() {
        auth.users().requireView(user);
        List<GroupRepresentation> memberships = new LinkedList<>();
        for (GroupModel group : user.getGroups()) {
            memberships.add(ModelToRepresentation.toRepresentation(group, false));
        }
        return memberships;
    }

    @DELETE
    @Path("groups/{groupId}")
    @NoCache
    public void removeMembership(@PathParam("groupId") String groupId) {
        auth.users().requireManageGroupMembership(user);

        GroupModel group = session.realms().getGroupById(groupId, realm);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        auth.groups().requireManageMembership(group);

        try {
            if (user.isMemberOf(group)){
                user.leaveGroup(group);
                adminEvent.operation(OperationType.DELETE).resource(ResourceType.GROUP_MEMBERSHIP).representation(ModelToRepresentation.toRepresentation(group, true)).resourcePath(uriInfo).success();
            }
        } catch (ModelException me) {
            Properties messages = AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale());
            throw new ErrorResponseException(me.getMessage(), MessageFormat.format(messages.getProperty(me.getMessage(), me.getMessage()), me.getParameters()),
                    Status.BAD_REQUEST);
        }
    }

    @PUT
    @Path("groups/{groupId}")
    @NoCache
    public void joinGroup(@PathParam("groupId") String groupId) {
        auth.users().requireManageGroupMembership(user);
        GroupModel group = session.realms().getGroupById(groupId, realm);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        auth.groups().requireManageMembership(group);
        if (!user.isMemberOf(group)){
            user.joinGroup(group);
            adminEvent.operation(OperationType.CREATE).resource(ResourceType.GROUP_MEMBERSHIP).representation(ModelToRepresentation.toRepresentation(group, true)).resourcePath(uriInfo).success();
        }
    }

}
