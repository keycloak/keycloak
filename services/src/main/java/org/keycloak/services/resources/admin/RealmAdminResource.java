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

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.Config;
import org.keycloak.KeyPairVerifier;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.exportimport.ClientDescriptionConverter;
import org.keycloak.exportimport.ClientDescriptionConverterFactory;
import org.keycloak.exportimport.util.ExportOptions;
import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.models.*;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.partialimport.PartialImportManager;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.LDAPConnectionTestManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.managers.UserStorageSyncManager;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.storage.UserStorageProviderModel;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
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
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.keycloak.models.utils.StripSecretsUtils.stripForExport;
import static org.keycloak.util.JsonSerialization.readValue;

/**
 * Base resource class for the admin REST api of one realm
 *
 * @resource Realms Admin
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdminResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected AdminPermissionEvaluator auth;
    protected RealmModel realm;
    private TokenManager tokenManager;
    private AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    @Context
    protected ClientConnection connection;

    @Context
    protected HttpHeaders headers;

    public RealmAdminResource(AdminPermissionEvaluator auth, RealmModel realm, TokenManager tokenManager, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.tokenManager = tokenManager;
        this.adminEvent = adminEvent.realm(realm).resource(ResourceType.REALM);
    }

    /**
     * Base path for importing clients under this realm.
     *
     * @return
     */
    @Path("client-description-converter")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation convertClientDescription(String description) {
        auth.clients().requireManage();

        if (realm == null) {
            throw new NotFoundException("Realm not found.");
        }

        for (ProviderFactory<ClientDescriptionConverter> factory : session.getKeycloakSessionFactory().getProviderFactories(ClientDescriptionConverter.class)) {
            if (((ClientDescriptionConverterFactory) factory).isSupported(description)) {
                return factory.create(session).convertToInternal(description);
            }
        }

        throw new BadRequestException("Unsupported format");
    }

    /**
     * Base path for managing attack detection.
     *
     * @return
     */
    @Path("attack-detection")
    public AttackDetectionResource getAttackDetection() {
        AttackDetectionResource resource = new AttackDetectionResource(auth, realm, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    /**
     * Base path for managing clients under this realm.
     *
     * @return
     */
    @Path("clients")
    public ClientsResource getClients() {
        ClientsResource clientsResource = new ClientsResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(clientsResource);
        return clientsResource;
    }

    /**
     * This endpoint is deprecated. It's here just because of backwards compatibility. Use {@link #getClientScopes()} instead
     *
     * @return
     */
    @Deprecated
    @Path("client-templates")
    public ClientScopesResource getClientTemplates() {
        return getClientScopes();
    }

    /**
     * Base path for managing client scopes under this realm.
     *
     * @return
     */
    @Path("client-scopes")
    public ClientScopesResource getClientScopes() {
        ClientScopesResource clientScopesResource = new ClientScopesResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(clientScopesResource);
        return clientScopesResource;
    }


    /**
     * Get realm default client scopes.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-default-client-scopes")
    public List<ClientScopeRepresentation> getDefaultDefaultClientScopes() {
        return getDefaultClientScopes(true);
    }

    private List<ClientScopeRepresentation> getDefaultClientScopes(boolean defaultScope) {
        auth.clients().requireViewClientScopes();

        List<ClientScopeRepresentation> defaults = new LinkedList<>();
        for (ClientScopeModel clientScope : realm.getDefaultClientScopes(defaultScope)) {
            ClientScopeRepresentation rep = new ClientScopeRepresentation();
            rep.setId(clientScope.getId());
            rep.setName(clientScope.getName());
            defaults.add(rep);
        }
        return defaults;
    }


    @PUT
    @NoCache
    @Path("default-default-client-scopes/{clientScopeId}")
    public void addDefaultDefaultClientScope(@PathParam("clientScopeId") String clientScopeId) {
        addDefaultClientScope(clientScopeId,true);
    }

    private void addDefaultClientScope(String clientScopeId, boolean defaultScope) {
        auth.clients().requireManageClientScopes();

        ClientScopeModel clientScope = realm.getClientScopeById(clientScopeId);
        if (clientScope == null) {
            throw new NotFoundException("Client scope not found");
        }
        realm.addDefaultClientScope(clientScope, defaultScope);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.CLIENT_SCOPE).resourcePath(session.getContext().getUri()).success();
    }


    @DELETE
    @NoCache
    @Path("default-default-client-scopes/{clientScopeId}")
    public void removeDefaultDefaultClientScope(@PathParam("clientScopeId") String clientScopeId) {
        auth.clients().requireManageClientScopes();

        ClientScopeModel clientScope = realm.getClientScopeById(clientScopeId);
        if (clientScope == null) {
            throw new NotFoundException("Client scope not found");
        }
        realm.removeDefaultClientScope(clientScope);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.CLIENT_SCOPE).resourcePath(session.getContext().getUri()).success();
    }


    /**
     * Get realm optional client scopes.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-optional-client-scopes")
    public List<ClientScopeRepresentation> getDefaultOptionalClientScopes() {
        return getDefaultClientScopes(false);
    }

    @PUT
    @NoCache
    @Path("default-optional-client-scopes/{clientScopeId}")
    public void addDefaultOptionalClientScope(@PathParam("clientScopeId") String clientScopeId) {
        addDefaultClientScope(clientScopeId, false);
    }

    @DELETE
    @NoCache
    @Path("default-optional-client-scopes/{clientScopeId}")
    public void removeDefaultOptionalClientScope(@PathParam("clientScopeId") String clientScopeId) {
        removeDefaultDefaultClientScope(clientScopeId);
    }

    /**
     * Base path for managing client initial access tokens
     *
     * @return
     */
    @Path("clients-initial-access")
    public ClientInitialAccessResource getClientInitialAccess() {
        ClientInitialAccessResource resource = new ClientInitialAccessResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("client-registration-policy")
    public ClientRegistrationPolicyResource getClientRegistrationPolicy() {
        ClientRegistrationPolicyResource resource = new ClientRegistrationPolicyResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    /**
     * Base path for managing components under this realm.
     *
     * @return
     */
    @Path("components")
    public ComponentResource getComponents() {
        ComponentResource resource = new ComponentResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    /**
     * base path for managing realm-level roles of this realm
     *
     * @return
     */
    @Path("roles")
    public RoleContainerResource getRoleContainerResource() {
        return new RoleContainerResource(session, session.getContext().getUri(), realm, auth, realm, adminEvent);
    }

    /**
     * Get the top-level representation of the realm
     *
     * It will not include nested information like User and Client representations.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public RealmRepresentation getRealm() {
        if (auth.realm().canViewRealm()) {
            return ModelToRepresentation.toRepresentation(realm, false);
        } else {
            auth.realm().requireViewRealmNameList();

            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm(realm.getName());

            if (auth.realm().canViewIdentityProviders()) {
                RealmRepresentation r = ModelToRepresentation.toRepresentation(realm, false);
                rep.setIdentityProviders(r.getIdentityProviders());
                rep.setIdentityProviderMappers(r.getIdentityProviderMappers());
            }

            return rep;
        }
    }

    /**
     * Update the top-level information of the realm
     *
     * Any user, roles or client information in the representation
     * will be ignored.  This will only update top-level attributes of the realm.
     *
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRealm(final RealmRepresentation rep) {
        auth.realm().requireManageRealm();

        logger.debug("updating realm: " + realm.getName());

        if (Config.getAdminRealm().equals(realm.getName()) && (rep.getRealm() != null && !rep.getRealm().equals(Config.getAdminRealm()))) {
            return ErrorResponse.error("Can't rename master realm", Status.BAD_REQUEST);
        }

        try {
            if (!Constants.GENERATE.equals(rep.getPublicKey()) && (rep.getPrivateKey() != null && rep.getPublicKey() != null)) {
                try {
                    KeyPairVerifier.verify(rep.getPrivateKey(), rep.getPublicKey());
                } catch (VerificationException e) {
                    return ErrorResponse.error(e.getMessage(), Status.BAD_REQUEST);
                }
            }

            if (!Constants.GENERATE.equals(rep.getPublicKey()) && (rep.getCertificate() != null)) {
                try {
                    X509Certificate cert = PemUtils.decodeCertificate(rep.getCertificate());
                    if (cert == null) {
                        return ErrorResponse.error("Failed to decode certificate", Status.BAD_REQUEST);
                    }
                } catch (Exception e)  {
                    return ErrorResponse.error("Failed to decode certificate", Status.BAD_REQUEST);
                }
            }

            boolean wasDuplicateEmailsAllowed = realm.isDuplicateEmailsAllowed();
            RepresentationToModel.updateRealm(rep, realm, session);

            // Refresh periodic sync tasks for configured federationProviders
            List<UserStorageProviderModel> federationProviders = realm.getUserStorageProviders();
            UserStorageSyncManager usersSyncManager = new UserStorageSyncManager();
            for (final UserStorageProviderModel fedProvider : federationProviders) {
                usersSyncManager.notifyToRefreshPeriodicSync(session, realm, fedProvider, false);
            }

            adminEvent.operation(OperationType.UPDATE).representation(StripSecretsUtils.strip(rep)).success();
            
            if (rep.isDuplicateEmailsAllowed() != null && rep.isDuplicateEmailsAllowed() != wasDuplicateEmailsAllowed) {
                UserCache cache = session.getProvider(UserCache.class);
                if (cache != null) cache.clear();
            }
            
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Realm with same name exists");
        } catch (ModelException e) {
            return ErrorResponse.error(e.getMessage(), Status.BAD_REQUEST);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ErrorResponse.error("Failed to update realm", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete the realm
     *
     */
    @DELETE
    public void deleteRealm() {
        auth.realm().requireManageRealm();

        if (!new RealmManager(session).removeRealm(realm)) {
            throw new NotFoundException("Realm doesn't exist");
        }
    }

    /**
     * Base path for managing users in this realm.
     *
     * @return
     */
    @Path("users")
    public UsersResource users() {
        UsersResource users = new UsersResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(users);
        //resourceContext.initResource(users);
        return users;
    }

    @NoCache
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("users-management-permissions")
    public ManagementPermissionReference getUserMgmtPermissions() {
        auth.realm().requireViewRealm();

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        if (permissions.users().isPermissionsEnabled()) {
            return toUsersMgmtRef(permissions);
        } else {
            return new ManagementPermissionReference();
        }

    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    @Path("users-management-permissions")
    public ManagementPermissionReference setUsersManagementPermissionsEnabled(ManagementPermissionReference ref) {
        auth.realm().requireManageRealm();

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        permissions.users().setPermissionsEnabled(ref.isEnabled());
        if (ref.isEnabled()) {
            return toUsersMgmtRef(permissions);
        } else {
            return new ManagementPermissionReference();
        }
    }


    public static ManagementPermissionReference toUsersMgmtRef(AdminPermissionManagement permissions) {
        ManagementPermissionReference ref = new ManagementPermissionReference();
        ref.setEnabled(true);
        ref.setResource(permissions.users().resource().getId());
        Map<String, String> scopes = permissions.users().getPermissions();
        ref.setScopePermissions(scopes);
        return ref;
    }


    @Path("user-storage")
    public UserStorageProviderResource userStorage() {
        UserStorageProviderResource fed = new UserStorageProviderResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(fed);
        //resourceContext.initResource(fed);
        return fed;
    }

    @Path("authentication")
    public AuthenticationManagementResource flows() {
        AuthenticationManagementResource resource = new AuthenticationManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        //resourceContext.initResource(resource);
        return resource;

    }

    /**
     * Path for managing all realm-level or client-level roles defined in this realm by its id.
     *
     * @return
     */
    @Path("roles-by-id")
    public RoleByIdResource rolesById() {
         RoleByIdResource resource = new RoleByIdResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        //resourceContext.initResource(resource);
        return resource;
    }

    /**
     * Push the realm's revocation policy to any client that has an admin url associated with it.
     *
     */
    @Path("push-revocation")
    @POST
    public GlobalRequestResult pushRevocation() {
        auth.realm().requireManageRealm();

        GlobalRequestResult result = new ResourceAdminManager(session).pushRealmRevocationPolicy(realm);
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(result).success();
        return result;
    }

    /**
     * Removes all user sessions.  Any client that has an admin url will also be told to invalidate any sessions
     * they have.
     *
     */
    @Path("logout-all")
    @POST
    public GlobalRequestResult logoutAll() {
        auth.users().requireManage();

        session.sessions().removeUserSessions(realm);
        GlobalRequestResult result = new ResourceAdminManager(session).logoutAll(realm);
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(result).success();
        return result;
    }

    /**
     * Remove a specific user session. Any client that has an admin url will also be told to invalidate this
     * particular session.
     *
     * @param sessionId
     */
    @Path("sessions/{session}")
    @DELETE
    public void deleteSession(@PathParam("session") String sessionId) {
        auth.users().requireManage();

        UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
        if (userSession == null) throw new NotFoundException("Sesssion not found");
        AuthenticationManager.backchannelLogout(session, realm, userSession, session.getContext().getUri(), connection, headers, true);
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.USER_SESSION).resourcePath(session.getContext().getUri()).success();

    }

    /**
     * Get client session stats
     *
     * Returns a JSON map.  The key is the client id, the value is the number of sessions that currently are active
     * with that client.  Only clients that actually have a session associated with them will be in this map.
     *
     * @return
     */
    @Path("client-session-stats")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, String>> getClientSessionStats() {
        auth.realm().requireViewRealm();

        Map<String, Map<String, String>> data = new HashMap<>();
        {
            Map<String, Long> activeCount = session.sessions().getActiveClientSessionStats(realm, false);
            for (Map.Entry<String, Long> entry : activeCount.entrySet()) {
                Map<String, String> map = new HashMap<>();
                ClientModel client = realm.getClientById(entry.getKey());
                if (client == null)
                    continue;
                map.put("id", client.getId());
                map.put("clientId", client.getClientId());
                map.put("active", entry.getValue().toString());
                map.put("offline", "0");
                data.put(client.getId(), map);
            }
        }
        {
            Map<String, Long> offlineCount = session.sessions().getActiveClientSessionStats(realm, true);
            for (Map.Entry<String, Long> entry : offlineCount.entrySet()) {
                Map<String, String> map = data.get(entry.getKey());
                if (map == null) {
                    map = new HashMap<>();
                    ClientModel client = realm.getClientById(entry.getKey());
                    if (client == null)
                        continue;
                    map.put("id", client.getId());
                    map.put("clientId", client.getClientId());
                    map.put("active", "0");
                    data.put(client.getId(), map);
                }
                map.put("offline", entry.getValue().toString());
            }
        }
        List<Map<String, String>> result = new LinkedList<>();
        for (Map<String, String> item : data.values())
            result.add(item);
        return result;
    }

    /**
     * Get the events provider configuration
     *
     * Returns JSON object with events provider configuration
     *
     * @return
     */
    @GET
    @NoCache
    @Path("events/config")
    @Produces(MediaType.APPLICATION_JSON)
    public RealmEventsConfigRepresentation getRealmEventsConfig() {
        auth.realm().requireViewEvents();

        RealmEventsConfigRepresentation config = ModelToRepresentation.toEventsConfigReprensetation(realm);
        if (config.getEnabledEventTypes() == null || config.getEnabledEventTypes().isEmpty()) {
            config.setEnabledEventTypes(new LinkedList<String>());
            for (EventType e : EventType.values()) {
                if (e.isSaveByDefault()) {
                    config.getEnabledEventTypes().add(e.name());
                }
            }
        }
        return config;
    }

    /**
     * Update the events provider
     *
     * Change the events provider and/or its configuration
     *
     * @param rep
     */
    @PUT
    @Path("events/config")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateRealmEventsConfig(final RealmEventsConfigRepresentation rep) {
        auth.realm().requireManageEvents();

        logger.debug("updating realm events config: " + realm.getName());
        new RealmManager(session).updateRealmEventsConfig(rep, realm);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.REALM).realm(realm)
                .resourcePath(session.getContext().getUri()).representation(rep)
                // refresh the builder to consider old and new config
                .refreshRealmEventsConfig(session)
                .success();
    }

    /**
     * Get events
     *
     * Returns all events, or filters them based on URL query parameters listed here
     *
     * @param types The types of events to return
     * @param client App or oauth client name
     * @param user User id
     * @param ipAddress IP address
     * @param dateTo To date
     * @param dateFrom From date
     * @param firstResult Paging offset
     * @param maxResults Maximum results size (defaults to 100)
     * @return
     */
    @Path("events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<EventRepresentation> getEvents(@QueryParam("type") List<String> types, @QueryParam("client") String client,
                                               @QueryParam("user") String user, @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo,
                                               @QueryParam("ipAddress") String ipAddress, @QueryParam("first") Integer firstResult,
                                               @QueryParam("max") Integer maxResults) {
        auth.realm().requireViewEvents();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

        EventQuery query = eventStore.createQuery().realm(realm.getId());
        if (client != null) {
            query.client(client);
        }

        if (types != null && !types.isEmpty()) {
            EventType[] t = new EventType[types.size()];
            for (int i = 0; i < t.length; i++) {
                t[i] = EventType.valueOf(types.get(i));
            }
            query.type(t);
        }

        if (user != null) {
            query.user(user);
        }

        if(dateFrom != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date from = null;
            try {
                from = df.parse(dateFrom);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for 'Date(From)', expected format is yyyy-MM-dd");
            }
            query.fromDate(from);
        }

        if(dateTo != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date to = null;
            try {
                to = df.parse(dateTo);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for 'Date(To)', expected format is yyyy-MM-dd");
            }
            query.toDate(to);
        }

        if (ipAddress != null) {
            query.ipAddress(ipAddress);
        }
        if (firstResult != null) {
            query.firstResult(firstResult);
        }
        if (maxResults != null) {
            query.maxResults(maxResults);
        } else {
            query.maxResults(Constants.DEFAULT_MAX_RESULTS);
        }

        return toEventListRep(query.getResultList());
    }

    private List<EventRepresentation> toEventListRep(List<Event> events) {
        List<EventRepresentation> reps = new ArrayList<>();
        for (Event event : events) {
            reps.add(ModelToRepresentation.toRepresentation(event));
        }
        return reps;
    }

    /**
     * Get admin events
     *
     * Returns all admin events, or filters events based on URL query parameters listed here
     *
     * @param operationTypes
     * @param authRealm
     * @param authClient
     * @param authUser user id
     * @param authIpAddress
     * @param resourcePath
     * @param dateTo
     * @param dateFrom
     * @param firstResult
     * @param maxResults Maximum results size (defaults to 100)
     * @return
     */
    @Path("admin-events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<AdminEventRepresentation> getEvents(@QueryParam("operationTypes") List<String> operationTypes, @QueryParam("authRealm") String authRealm, @QueryParam("authClient") String authClient,
                                                    @QueryParam("authUser") String authUser, @QueryParam("authIpAddress") String authIpAddress,
                                                    @QueryParam("resourcePath") String resourcePath, @QueryParam("dateFrom") String dateFrom,
                                                    @QueryParam("dateTo") String dateTo, @QueryParam("first") Integer firstResult,
                                                    @QueryParam("max") Integer maxResults,
                                                    @QueryParam("resourceTypes") List<String> resourceTypes) {
        auth.realm().requireViewEvents();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        AdminEventQuery query = eventStore.createAdminQuery().realm(realm.getId());;

        if (authRealm != null) {
            query.authRealm(authRealm);
        }

        if (authClient != null) {
            query.authClient(authClient);
        }

        if (authUser != null) {
            query.authUser(authUser);
        }

        if (authIpAddress != null) {
            query.authIpAddress(authIpAddress);
        }

        if (resourcePath != null) {
            query.resourcePath(resourcePath);
        }

        if (operationTypes != null && !operationTypes.isEmpty()) {
            OperationType[] t = new OperationType[operationTypes.size()];
            for (int i = 0; i < t.length; i++) {
                t[i] = OperationType.valueOf(operationTypes.get(i));
            }
            query.operation(t);
        }

        if (resourceTypes != null && !resourceTypes.isEmpty()) {
            ResourceType[] t = new ResourceType[resourceTypes.size()];
            for (int i = 0; i < t.length; i++) {
                t[i] = ResourceType.valueOf(resourceTypes.get(i));
            }
            query.resourceType(t);
        }



        if(dateFrom != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date from = null;
            try {
                from = df.parse(dateFrom);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for 'Date(From)', expected format is yyyy-MM-dd");
            }
            query.fromTime(from);
        }

        if(dateTo != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date to = null;
            try {
                to = df.parse(dateTo);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for 'Date(To)', expected format is yyyy-MM-dd");
            }
            query.toTime(to);
        }

        if (firstResult != null) {
            query.firstResult(firstResult);
        }
        if (maxResults != null) {
            query.maxResults(maxResults);
        } else {
            query.maxResults(Constants.DEFAULT_MAX_RESULTS);
        }

        return toAdminEventRep(query.getResultList());
    }

    private List<AdminEventRepresentation> toAdminEventRep(List<AdminEvent> events) {
        List<AdminEventRepresentation> reps = new ArrayList<>();
        for (AdminEvent event : events) {
            reps.add(ModelToRepresentation.toRepresentation(event));
        }

        return reps;
    }

    /**
     * Delete all events
     *
     */
    @Path("events")
    @DELETE
    public void clearEvents() {
        auth.realm().requireManageEvents();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clear(realm.getId());
    }

    /**
     * Delete all admin events
     *
     */
    @Path("admin-events")
    @DELETE
    public void clearAdminEvents() {
        auth.realm().requireManageEvents();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clearAdmin(realm.getId());
    }

    /**
     * Test LDAP connection
     *
     * @param action
     * @param connectionUrl
     * @param bindDn
     * @param bindCredential
     * @return
     */
    @Path("testLDAPConnection")
    @POST
    @NoCache
    public Response testLDAPConnection(@FormParam("action") String action, @FormParam("connectionUrl") String connectionUrl,
                                       @FormParam("bindDn") String bindDn, @FormParam("bindCredential") String bindCredential,
                                       @FormParam("useTruststoreSpi") String useTruststoreSpi, @FormParam("connectionTimeout") String connectionTimeout,
                                       @FormParam("componentId") String componentId, @FormParam("startTls") String startTls) {
        auth.realm().requireManageRealm();

        if (componentId != null && bindCredential.equals(ComponentRepresentation.SECRET_VALUE)) {
            bindCredential = realm.getComponent(componentId).getConfig().getFirst(LDAPConstants.BIND_CREDENTIAL);
        }

        boolean result = LDAPConnectionTestManager.testLDAP(session, action, connectionUrl, bindDn, bindCredential, useTruststoreSpi, connectionTimeout, startTls);
        return result ? Response.noContent().build() : ErrorResponse.error("LDAP test error", Response.Status.BAD_REQUEST);
    }

    /**
     * Test SMTP connection with current logged in user
     *
     * @param config SMTP server configuration
     * @return
     * @throws Exception
     */
    @Path("testSMTPConnection")
    @POST
    @NoCache
    public Response testSMTPConnection(final @FormParam("config") String config) throws Exception {
        Map<String, String> settings = readValue(config, new TypeReference<Map<String, String>>() {
        });

        try {
            UserModel user = auth.adminAuth().getUser();
            if (user.getEmail() == null) {
                return ErrorResponse.error("Logged in user does not have an e-mail.", Response.Status.INTERNAL_SERVER_ERROR);
            }
            if (ComponentRepresentation.SECRET_VALUE.equals(settings.get("password"))) {
                settings.put("password", realm.getSmtpConfig().get("password"));
            }
            session.getProvider(EmailTemplateProvider.class).sendSmtpTestEmail(settings, user);
        } catch (Exception e) {
            e.printStackTrace();
            logger.errorf("Failed to send email \n %s", e.getCause());
            return ErrorResponse.error("Failed to send email", Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.noContent().build();
    }

    @Path("identity-provider")
    public IdentityProvidersResource getIdentityProviderResource() {
        return new IdentityProvidersResource(realm, session, this.auth, adminEvent);
    }

    /**
     * Get group hierarchy.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-groups")
    public List<GroupRepresentation> getDefaultGroups() {
        auth.realm().requireViewRealm();

        List<GroupRepresentation> defaults = new LinkedList<>();
        for (GroupModel group : realm.getDefaultGroups()) {
            defaults.add(ModelToRepresentation.toRepresentation(group, false));
        }
        return defaults;
    }
    @PUT
    @NoCache
    @Path("default-groups/{groupId}")
    public void addDefaultGroup(@PathParam("groupId") String groupId) {
        auth.realm().requireManageRealm();

        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        realm.addDefaultGroup(group);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.GROUP).resourcePath(session.getContext().getUri()).success();
    }

    @DELETE
    @NoCache
    @Path("default-groups/{groupId}")
    public void removeDefaultGroup(@PathParam("groupId") String groupId) {
        auth.realm().requireManageRealm();

        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        realm.removeDefaultGroup(group);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.GROUP).resourcePath(session.getContext().getUri()).success();
    }


    @Path("groups")
    public GroupsResource getGroups() {
        GroupsResource resource =  new GroupsResource(realm, session, this.auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }


    @GET
    @Path("group-by-path/{path: .*}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GroupRepresentation getGroupByPath(@PathParam("path") String path) {
        GroupModel found = KeycloakModelUtils.findGroupByPath(realm, path);
        if (found == null) {
            throw new NotFoundException("Group path does not exist");

        }
        auth.groups().requireView(found);
        return ModelToRepresentation.toGroupHierarchy(found, true);
    }

    /**
     * Partial import from a JSON file to an existing realm.
     *
     * @param rep
     * @return
     */
    @Path("partialImport")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response partialImport(PartialImportRepresentation rep) {
        auth.realm().requireManageRealm();

        PartialImportManager partialImport = new PartialImportManager(rep, session, realm, adminEvent);
        return partialImport.saveResources();
    }

    /**
     * Partial export of existing realm into a JSON file.
     *
     * @param exportGroupsAndRoles
     * @param exportClients
     * @return
     */
    @Path("partial-export")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public RealmRepresentation partialExport(@QueryParam("exportGroupsAndRoles") Boolean exportGroupsAndRoles,
                                                     @QueryParam("exportClients") Boolean exportClients) {
        auth.realm().requireViewRealm();

        boolean groupsAndRolesExported = exportGroupsAndRoles != null && exportGroupsAndRoles;
        boolean clientsExported = exportClients != null && exportClients;

        if (groupsAndRolesExported) {
            auth.groups().requireList();
        }
        if (clientsExported) {
            auth.clients().requireView();
        }

        ExportOptions options = new ExportOptions(false, clientsExported, groupsAndRolesExported);
        RealmRepresentation rep = ExportUtils.exportRealm(session, realm, options, false);
        return stripForExport(session, rep);
    }

    /**
     * Clear realm cache
     *
     */
    @Path("clear-realm-cache")
    @POST
    public void clearRealmCache() {
        auth.realm().requireManageRealm();

        CacheRealmProvider cache = session.getProvider(CacheRealmProvider.class);
        if (cache != null) {
            cache.clear();
        }

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Clear user cache
     *
     */
    @Path("clear-user-cache")
    @POST
    public void clearUserCache() {
        auth.realm().requireManageRealm();

        UserCache cache = session.getProvider(UserCache.class);
        if (cache != null) {
            cache.clear();
        }

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Clear cache of external public keys (Public keys of clients or Identity providers)
     *
     */
    @Path("clear-keys-cache")
    @POST
    public void clearKeysCache() {
        auth.realm().requireManageRealm();

        PublicKeyStorageProvider cache = session.getProvider(PublicKeyStorageProvider.class);
        if (cache != null) {
            cache.clearCache();
        }

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
    }

    @Path("keys")
    public KeyResource keys() {
        KeyResource resource =  new KeyResource(realm, session, this.auth);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @GET
    @Path("credential-registrators")
    @NoCache
    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public List<String> getCredentialRegistrators(){
        auth.realm().requireViewRealm();
        return session.getContext().getRealm().getRequiredActionProviders().stream()
                .filter(ra -> ra.isEnabled())
                .map(RequiredActionProviderModel::getProviderId)
                .filter(providerId ->  session.getProvider(RequiredActionProvider.class, providerId) instanceof CredentialRegistrator)
                .collect(Collectors.toList());
    }

}
