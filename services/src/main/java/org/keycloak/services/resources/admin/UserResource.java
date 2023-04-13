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
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.authentication.requiredactions.util.RequiredActionsValidator;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.util.CollectionUtil;
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
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
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
import org.keycloak.services.managers.UserConsentManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.account.AccountFormService;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.validation.Validation;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.utils.ProfileHelper;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;
import static org.keycloak.userprofile.UserProfileContext.USER_API;
import static org.keycloak.utils.LockObjectsForModification.lockUserSessionsForModification;

/**
 * Base resource for managing users
 *
 * @resource Users
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserResource {
    private static final Logger logger = Logger.getLogger(UserResource.class);

    protected final RealmModel realm;

    private final AdminPermissionEvaluator auth;

    private final AdminEventBuilder adminEvent;
    private final UserModel user;

    protected final ClientConnection clientConnection;

    protected final KeycloakSession session;

    protected final HttpHeaders headers;
    
    public UserResource(KeycloakSession session, UserModel user, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.auth = auth;
        this.realm = session.getContext().getRealm();
        this.clientConnection = session.getContext().getConnection();
        this.user = user;
        this.adminEvent = adminEvent.resource(ResourceType.USER);
        this.headers = session.getContext().getRequestHeaders();
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

            boolean wasPermanentlyLockedOut = false;
            if (rep.isEnabled() != null && rep.isEnabled()) {
                UserLoginFailureModel failureModel = session.loginFailures().getUserLoginFailure(realm, user.getId());
                if (failureModel != null) {
                    failureModel.clearFailures();
                }
                wasPermanentlyLockedOut = session.getProvider(BruteForceProtector.class).isPermanentlyLockedOut(session, realm, user);
            }

            Map<String, List<String>> attributes = new HashMap<>(rep.toAttributes());

            if (rep.getAttributes() == null) {
                // include existing attributes in case no attributes are set so that validation takes into account the existing
                // attributes associated with the user
                for (Map.Entry<String, List<String>> entry : user.getAttributes().entrySet()) {
                    attributes.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }

            UserProfile profile = session.getProvider(UserProfileProvider.class).create(USER_API, attributes, user);

            Response response = validateUserProfile(profile, session, auth.adminAuth());
            if (response != null) {
                return response;
            }
            profile.update(rep.getAttributes() != null);
            updateUserFromRep(profile, user, rep, session, true);
            RepresentationToModel.createCredentials(rep, session, realm, user, true);

            // we need to do it here as the attributes would be overwritten by what is in the rep
            if (wasPermanentlyLockedOut) {
                session.getProvider(BruteForceProtector.class).cleanUpPermanentLockout(session, realm, user);
            }

            adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();

            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().commit();
            }
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            session.getTransactionManager().setRollbackOnly();
            throw ErrorResponse.exists("User exists with same username or email");
        } catch (ReadOnlyException re) {
            session.getTransactionManager().setRollbackOnly();
            throw ErrorResponse.error("User is read only!", Status.BAD_REQUEST);
        } catch (ModelException me) {
            logger.warn("Could not update user!", me);
            session.getTransactionManager().setRollbackOnly();
            throw ErrorResponse.error("Could not update user!", Status.BAD_REQUEST);
        } catch (ForbiddenException | ErrorResponseException e) {
            session.getTransactionManager().setRollbackOnly();
            throw e;
        } catch (Exception me) { // JPA
            session.getTransactionManager().setRollbackOnly();
            logger.warn("Could not update user!", me);// may be committed by JTA which can't
            throw ErrorResponse.error("Could not update user!", Status.BAD_REQUEST);
        }
    }

    public static Response validateUserProfile(UserProfile profile, KeycloakSession session, AdminAuth adminAuth) {
        try {
            profile.validate();
        } catch (ValidationException pve) {
            List<ErrorRepresentation> errors = new ArrayList<>();
            AdminMessageFormatter adminMessageFormatter = createAdminMessageFormatter(session, adminAuth);

            for (ValidationException.Error error : pve.getErrors()) {
                errors.add(new ErrorRepresentation(error.getFormattedMessage(adminMessageFormatter)));
            }

            throw ErrorResponse.errors(errors, Status.BAD_REQUEST);
        }

        return null;
    }

    private static AdminMessageFormatter createAdminMessageFormatter(KeycloakSession session, AdminAuth adminAuth) {
        // the authenticated user is used to resolve the locale for the messages. It can be null.
        UserModel authenticatedUser = adminAuth == null ? null : adminAuth.getUser();

        return new AdminMessageFormatter(session, authenticatedUser);
    }

    public static void updateUserFromRep(UserProfile profile, UserModel user, UserRepresentation rep, KeycloakSession session, boolean isUpdateExistingUser) {
        boolean removeMissingRequiredActions = isUpdateExistingUser;

        if (rep.isEnabled() != null) user.setEnabled(rep.isEnabled());
        if (rep.isEmailVerified() != null) user.setEmailVerified(rep.isEmailVerified());

        if (rep.getFederationLink() != null) user.setFederationLink(rep.getFederationLink());

        List<String> reqActions = rep.getRequiredActions();

        if (reqActions != null) {
            session.getKeycloakSessionFactory()
                    .getProviderFactoriesStream(RequiredActionProvider.class)
                    .map(ProviderFactory::getId)
                    .distinct()
                    .forEach(action -> {
                        if (reqActions.contains(action)) {
                            user.addRequiredAction(action);
                        } else if (removeMissingRequiredActions) {
                            user.removeRequiredAction(action);
                        }
                    });
        }

        List<CredentialRepresentation> credentials = rep.getCredentials();
        if (credentials != null) {
            for (CredentialRepresentation credential : credentials) {
                if (CredentialRepresentation.PASSWORD.equals(credential.getType()) && credential.isTemporary() != null
                        && credential.isTemporary()) {
                    user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                }
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
            List<FederatedIdentityRepresentation> reps = getFederatedIdentities(user).collect(Collectors.toList());
            rep.setFederatedIdentities(reps);
        }

        if (session.getProvider(BruteForceProtector.class).isTemporarilyDisabled(session, realm, user)) {
            rep.setEnabled(false);
        }
        rep.setAccess(auth.users().getAccess(user));

        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        UserProfile profile = provider.create(USER_API, user);
        Map<String, List<String>> readableAttributes = profile.getAttributes().getReadable(false);

        if (rep.getAttributes() != null) {
            rep.setAttributes(readableAttributes);
        }

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
        String sessionState = auth.adminAuth().getToken().getSessionState();
        if (authenticatedRealm.getId().equals(realm.getId()) && sessionState != null) {
            sameRealm = true;
            UserSessionModel userSession = lockUserSessionsForModification(session, () -> session.sessions().getUserSession(authenticatedRealm, sessionState));
            AuthenticationManager.expireIdentityCookie(realm, session.getContext().getUri(), session);
            AuthenticationManager.expireRememberMeCookie(realm, session.getContext().getUri(), session);
            AuthenticationManager.backchannelLogout(session, authenticatedRealm, userSession, session.getContext().getUri(), clientConnection, headers, true);
        }
        EventBuilder event = new EventBuilder(realm, session, clientConnection);

        UserSessionModel userSession = new UserSessionManager(session).createUserSession(realm, user, user.getUsername(), clientConnection.getRemoteAddr(), "impersonate", false, null, null);

        UserModel adminUser = auth.adminAuth().getUser();
        String impersonatorId = adminUser.getId();
        String impersonator = adminUser.getUsername();
        userSession.setNote(IMPERSONATOR_ID.toString(), impersonatorId);
        userSession.setNote(IMPERSONATOR_USERNAME.toString(), impersonator);

        AuthenticationManager.createLoginCookie(session, realm, userSession.getUser(), userSession, session.getContext().getUri(), clientConnection);
        URI redirect = AccountFormService.accountServiceBaseUrl(session.getContext().getUri()).build(realm.getName());
        Map<String, Object> result = new HashMap<>();
        result.put("sameRealm", sameRealm);
        result.put("redirect", redirect.toString());
        event.event(EventType.IMPERSONATE)
                .session(userSession)
                .user(user)
                .detail(Details.IMPERSONATOR_REALM, authenticatedRealm.getName())
                .detail(Details.IMPERSONATOR, impersonator).success();

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
    public Stream<UserSessionRepresentation> getSessions() {
        auth.users().requireView(user);
        return session.sessions().getUserSessionsStream(realm, user).map(ModelToRepresentation::toRepresentation);
    }

    /**
     * Get offline sessions associated with the user and client
     *
     * @return
     */
    @Path("offline-sessions/{clientUuid}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<UserSessionRepresentation> getOfflineSessions(final @PathParam("clientUuid") String clientUuid) {
        auth.users().requireView(user);
        ClientModel client = realm.getClientById(clientUuid);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        return new UserSessionManager(session).findOfflineSessionsStream(realm, user)
                .map(session -> toUserSessionRepresentation(session, clientUuid))
                .filter(Objects::nonNull);
    }

    /**
     * Get social logins associated with the user
     *
     * @return a non-null {@code Stream} of social logins (federated identities).
     */
    @Path("federated-identity")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<FederatedIdentityRepresentation> getFederatedIdentity() {
        auth.users().requireView(user);
        return getFederatedIdentities(user);
    }

    private Stream<FederatedIdentityRepresentation> getFederatedIdentities(UserModel user) {
        Set<String> idps = realm.getIdentityProvidersStream().map(IdentityProviderModel::getAlias).collect(Collectors.toSet());
        return session.users().getFederatedIdentitiesStream(realm, user)
                .filter(identity -> idps.contains(identity.getIdentityProvider()))
                .map(ModelToRepresentation::toRepresentation);
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
        if (session.users().getFederatedIdentity(realm, user, provider) != null) {
            throw ErrorResponse.exists("User is already linked with provider");
        }

        FederatedIdentityModel socialLink = new FederatedIdentityModel(provider, rep.getUserId(), rep.getUserName());
        session.users().addFederatedIdentity(realm, user, socialLink);
        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri()).representation(rep).success();
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
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
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
    public Stream<Map<String, Object>> getConsents() {
        auth.users().requireView(user);

        Set<ClientModel> offlineClients = new UserSessionManager(session).findClientsWithOfflineToken(realm, user);

        Set<ClientModel> clientsWithUserConsents = new HashSet<>();
        List<UserConsentModel> userConsents = session.users().getConsentsStream(realm, user.getId())
                 // collect clients with explicit user consents for later filtering
                .peek(ucm -> clientsWithUserConsents.add(ucm.getClient()))
                .collect(Collectors.toList());

        return Stream.concat(
                userConsents.stream().map(consent -> toConsent(consent, offlineClients)),
                offlineClients.stream()
                        // filter out clients with explicit user consents to avoid rendering them twice
                        .filter(c -> !clientsWithUserConsents.contains(c))
                        .map(this::toConsent)
        );
    }

    private Map<String, Object> toConsent(ClientModel client) {
        Map<String, Object> currentRep = new HashMap<>();
        currentRep.put("clientId", client.getClientId());
        currentRep.put("grantedClientScopes", Collections.emptyList());
        currentRep.put("createdDate", null);
        currentRep.put("lastUpdatedDate", null);

        List<Map<String, String>> additionalGrants = new LinkedList<>();

        Map<String, String> offlineTokens = new HashMap<>();
        offlineTokens.put("client", client.getId());
        offlineTokens.put("key", "Offline Token");
        additionalGrants.add(offlineTokens);

        currentRep.put("additionalGrants", additionalGrants);
        return currentRep;
    }

    private Map<String, Object> toConsent(UserConsentModel consent, Set<ClientModel> offlineClients) {

        UserConsentRepresentation rep = ModelToRepresentation.toRepresentation(consent);

        Map<String, Object> currentRep = new HashMap<>();
        currentRep.put("clientId", consent.getClient().getClientId());
        currentRep.put("grantedClientScopes", rep.getGrantedClientScopes());
        currentRep.put("createdDate", rep.getCreatedDate());
        currentRep.put("lastUpdatedDate", rep.getLastUpdatedDate());

        List<Map<String, String>> additionalGrants = new LinkedList<>();
        if (offlineClients.contains(consent.getClient())) {
            Map<String, String> offlineTokens = new HashMap<>();
            offlineTokens.put("client", consent.getClient().getId());
            offlineTokens.put("key", "Offline Token");
            additionalGrants.add(offlineTokens);
        }
        currentRep.put("additionalGrants", additionalGrants);
        return currentRep;
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
        boolean revokedConsent = UserConsentManager.revokeConsentToClient(session, client, user);

        if (!revokedConsent) {
            throw new NotFoundException("Consent nor offline token not found");
        }
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
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

        session.sessions().getUserSessionsStream(realm, user)
                .collect(Collectors.toList()) // collect to avoid concurrent modification as backchannelLogout removes the user sessions.
                .forEach(userSession -> AuthenticationManager.backchannelLogout(session, realm, userSession,
                        session.getContext().getUri(), clientConnection, headers, true));
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
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
            adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
            return Response.noContent().build();
        } else {
            throw ErrorResponse.error("User couldn't be deleted", Status.BAD_REQUEST);
        }
    }

    @Path("role-mappings")
    public RoleMapperResource getRoleMappings() {
        AdminPermissionEvaluator.RequirePermissionCheck manageCheck = () -> auth.users().requireMapRoles(user);
        AdminPermissionEvaluator.RequirePermissionCheck viewCheck = () -> auth.users().requireView(user);
        return new RoleMapperResource(session, auth, user, adminEvent, manageCheck, viewCheck);
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
            user.credentialManager().disableCredentialType(type);

        }
    }

    /**
     * Set up a new password for the user.
     *
     * @param cred The representation must contain a rawPassword with the plain-text password
     */
    @Path("reset-password")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void resetPassword(CredentialRepresentation cred) {
        auth.users().requireManage(user);
        if (cred == null || cred.getValue() == null) {
            throw new BadRequestException("No password provided");
        }
        if (Validation.isBlank(cred.getValue())) {
            throw new BadRequestException("Empty password not allowed");
        }

        try {
            user.credentialManager().updateCredential(UserCredentialModel.password(cred.getValue(), false));
        } catch (IllegalStateException ise) {
            throw new BadRequestException("Resetting to N old passwords is not allowed.");
        } catch (ReadOnlyException mre) {
            throw new BadRequestException("Can't reset password as account is read only");
        } catch (ModelException e) {
            logger.warn("Could not update user password.", e);
            Properties messages = AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale());
            throw new ErrorResponseException(e.getMessage(), MessageFormat.format(messages.getProperty(e.getMessage(), e.getMessage()), e.getParameters()),
                    Status.BAD_REQUEST);
        }
        if (cred.isTemporary() != null && cred.isTemporary()) {
            user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        } else {
            // Remove a potentially existing UPDATE_PASSWORD action when explicitly assigning a non-temporary password.
            user.removeRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        }

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
    }


    @GET
    @Path("credentials")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<CredentialRepresentation> credentials(){
        auth.users().requireView(user);
        return user.credentialManager().getStoredCredentialsStream()
                .map(ModelToRepresentation::toRepresentation)
                .peek(credentialRepresentation -> credentialRepresentation.setSecretData(null));
    }


    /**
     * Return credential types, which are provided by the user storage where user is stored. Returned values can contain for example "password", "otp" etc.
     * This will always return empty list for "local" users, which are not backed by any user storage
     *
     * @return
     */
    @GET
    @Path("configured-user-storage-credential-types")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<String> getConfiguredUserStorageCredentialTypes() {
        // This has "requireManage" due the compatibility with "credentials()" endpoint. Strictly said, it is reading endpoint, not writing,
        // so may be revisited if to rather use "requireView" here in the future.
        auth.users().requireManage(user);
        return user.credentialManager().getConfiguredUserStorageCredentialTypesStream();
    }


    /**
     * Remove a credential for a user
     *
     */
    @Path("credentials/{credentialId}")
    @DELETE
    @NoCache
    public void removeCredential(final @PathParam("credentialId") String credentialId) {
        auth.users().requireManage(user);
        CredentialModel credential = user.credentialManager().getStoredCredentialById(credentialId);
        if (credential == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery()) throw new NotFoundException("Credential not found");
            else throw new ForbiddenException();
        }
        user.credentialManager().removeStoredCredentialById(credentialId);
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Update a credential label for a user
     */
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("credentials/{credentialId}/userLabel")
    public void setCredentialUserLabel(final @PathParam("credentialId") String credentialId, String userLabel) {
        auth.users().requireManage(user);
        CredentialModel credential = user.credentialManager().getStoredCredentialById(credentialId);
        if (credential == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery()) throw new NotFoundException("Credential not found");
            else throw new ForbiddenException();
        }
        user.credentialManager().updateCredentialLabel(credentialId, userLabel);
    }

    /**
     * Move a credential to a first position in the credentials list of the user
     * @param credentialId The credential to move
     */
    @Path("credentials/{credentialId}/moveToFirst")
    @POST
    public void moveCredentialToFirst(final @PathParam("credentialId") String credentialId){
        moveCredentialAfter(credentialId, null);
    }

    /**
     * Move a credential to a position behind another credential
     * @param credentialId The credential to move
     * @param newPreviousCredentialId The credential that will be the previous element in the list. If set to null, the moved credential will be the first element in the list.
     */
    @Path("credentials/{credentialId}/moveAfter/{newPreviousCredentialId}")
    @POST
    public void moveCredentialAfter(final @PathParam("credentialId") String credentialId, final @PathParam("newPreviousCredentialId") String newPreviousCredentialId){
        auth.users().requireManage(user);
        CredentialModel credential = user.credentialManager().getStoredCredentialById(credentialId);
        if (credential == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery()) throw new NotFoundException("Credential not found");
            else throw new ForbiddenException();
        }
        user.credentialManager().moveStoredCredentialTo(credentialId, newPreviousCredentialId);
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
     * Send an email to the user with a link they can click to execute particular actions.
     *
     * An email contains a link the user can click to perform a set of required actions.
     * The redirectUri and clientId parameters are optional. If no redirect is given, then there will
     * be no link back to click after actions have completed.  Redirect uri must be a valid uri for the
     * particular clientId.
     *
     * @param redirectUri Redirect uri
     * @param clientId Client id
     * @param lifespan Number of seconds after which the generated token expires
     * @param actions Required actions the user needs to complete
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
            throw ErrorResponse.error("User email missing", Status.BAD_REQUEST);
        }

        if (!user.isEnabled()) {
            throw ErrorResponse.error("User is disabled", Status.BAD_REQUEST);
        }

        if (redirectUri != null && clientId == null) {
            throw ErrorResponse.error("Client id missing", Status.BAD_REQUEST);
        }

        if (clientId == null) {
            clientId = Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
        }

        if (CollectionUtil.isNotEmpty(actions) && !RequiredActionsValidator.validRequiredActions(session, actions)) {
            throw ErrorResponse.error("Provided invalid required actions", Status.BAD_REQUEST);
        }

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            logger.debugf("Client %s doesn't exist", clientId);
            throw ErrorResponse.error("Client doesn't exist", Status.BAD_REQUEST);
        }
        if (!client.isEnabled()) {
            logger.debugf("Client %s is not enabled", clientId);
            throw ErrorResponse.error("Client is not enabled", Status.BAD_REQUEST);
        }

        String redirect;
        if (redirectUri != null) {
            redirect = RedirectUtils.verifyRedirectUri(session, redirectUri, client);
            if (redirect == null) {
                throw ErrorResponse.error("Invalid redirect uri.", Status.BAD_REQUEST);
            }
        }

        if (lifespan == null) {
            lifespan = realm.getActionTokenGeneratedByAdminLifespan();
        }
        int expiration = Time.currentTime() + lifespan;
        ExecuteActionsActionToken token = new ExecuteActionsActionToken(user.getId(), user.getEmail(), expiration, actions, redirectUri, clientId);

        try {
            UriBuilder builder = LoginActionsService.actionTokenProcessor(session.getContext().getUri());
            builder.queryParam("key", token.serialize(session, realm, session.getContext().getUri()));

            String link = builder.build(realm.getName()).toString();

            this.session.getProvider(EmailTemplateProvider.class)
              .setAttribute(Constants.TEMPLATE_ATTR_REQUIRED_ACTIONS, token.getRequiredActions())
              .setRealm(realm)
              .setUser(user)
              .sendExecuteActions(link, TimeUnit.SECONDS.toMinutes(lifespan));

            //audit.user(user).detail(Details.EMAIL, user.getEmail()).detail(Details.CODE_ID, accessCode.getCodeId()).success();

            adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();

            return Response.noContent().build();
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendActionsEmail(e);
            throw ErrorResponse.error("Failed to send execute actions email", Status.INTERNAL_SERVER_ERROR);
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
    public Stream<GroupRepresentation> groupMembership(@QueryParam("search") String search,
                                                       @QueryParam("first") Integer firstResult,
                                                       @QueryParam("max") Integer maxResults,
                                                       @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation) {
        auth.users().requireView(user);

        if (Objects.nonNull(search)) {
            return ModelToRepresentation.searchForGroupByName(user, !briefRepresentation, search.trim(), firstResult, maxResults);
        } else {
            return ModelToRepresentation.toGroupHierarchy(user, !briefRepresentation, firstResult, maxResults);
        }
    }

    @GET
    @NoCache
    @Path("groups/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> getGroupMembershipCount(@QueryParam("search") String search) {
        auth.users().requireView(user);
        Long results;

        if (Objects.nonNull(search)) {
            results = user.getGroupsCountByNameContaining(search);
        } else {
            results = user.getGroupsCount();
        }
        Map<String, Long> map = new HashMap<>();
        map.put("count", results);
        return map;
    }

    @DELETE
    @Path("groups/{groupId}")
    @NoCache
    public void removeMembership(@PathParam("groupId") String groupId) {
        auth.users().requireManageGroupMembership(user);

        GroupModel group = session.groups().getGroupById(realm, groupId);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        auth.groups().requireManageMembership(group);

        try {
            if (user.isMemberOf(group)){
                user.leaveGroup(group);
                adminEvent.operation(OperationType.DELETE).resource(ResourceType.GROUP_MEMBERSHIP).representation(ModelToRepresentation.toRepresentation(group, true)).resourcePath(session.getContext().getUri()).success();
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
        GroupModel group = session.groups().getGroupById(realm, groupId);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        auth.groups().requireManageMembership(group);
        if (!RoleUtils.isDirectMember(user.getGroupsStream(),group)){
            user.joinGroup(group);
            adminEvent.operation(OperationType.CREATE).resource(ResourceType.GROUP_MEMBERSHIP).representation(ModelToRepresentation.toRepresentation(group, true)).resourcePath(session.getContext().getUri()).success();
        }
    }

    /**
     * Converts the specified {@link UserSessionModel} into a {@link UserSessionRepresentation}.
     *
     * @param userSession the model to be converted.
     * @param clientUuid the client's UUID.
     * @return a reference to the constructed representation or {@code null} if the session is not associated with the specified
     * client.
     */
    private UserSessionRepresentation toUserSessionRepresentation(final UserSessionModel userSession, final String clientUuid) {
        UserSessionRepresentation rep = ModelToRepresentation.toRepresentation(userSession);
        // Update lastSessionRefresh with the timestamp from clientSession
        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientUuid);
        if (clientSession == null) {
            return null;
        }
        rep.setLastAccess(Time.toMillis(clientSession.getTimestamp()));
        return rep;
    }
}
