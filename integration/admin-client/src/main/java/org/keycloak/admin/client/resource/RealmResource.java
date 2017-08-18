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

package org.keycloak.admin.client.resource;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface RealmResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    RealmRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void update(RealmRepresentation realmRepresentation);

    @Path("clients")
    ClientsResource clients();

    @Path("client-templates")
    ClientTemplatesResource clientTemplates();

    @Path("client-description-converter")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation convertClientDescription(String description);

    @Path("users")
    UsersResource users();

    @Path("roles")
    RolesResource roles();

    @Path("roles-by-id")
    RoleByIdResource rolesById();

    @Path("groups")
    GroupsResource groups();

    @DELETE
    @Path("events")
    void clearEvents();

    @GET
    @Path("events")
    @Produces(MediaType.APPLICATION_JSON)
    List<EventRepresentation> getEvents();

    @Path("events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<EventRepresentation> getEvents(@QueryParam("type") List<String> types, @QueryParam("client") String client,
            @QueryParam("user") String user, @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo,
            @QueryParam("ipAddress") String ipAddress, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults);

    @DELETE
    @Path("admin-events")
    void clearAdminEvents();

    @GET
    @Path("admin-events")
    @Produces(MediaType.APPLICATION_JSON)
    List<AdminEventRepresentation> getAdminEvents();

    @GET
    @Path("admin-events")
    @Produces(MediaType.APPLICATION_JSON)
    List<AdminEventRepresentation> getAdminEvents(@QueryParam("operationTypes") List<String> operationTypes, @QueryParam("authRealm") String authRealm, @QueryParam("authClient") String authClient,
            @QueryParam("authUser") String authUser, @QueryParam("authIpAddress") String authIpAddress,
            @QueryParam("resourcePath") String resourcePath, @QueryParam("dateFrom") String dateFrom,
            @QueryParam("dateTo") String dateTo, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults);

    @GET
    @Path("events/config")
    @Produces(MediaType.APPLICATION_JSON)
    public RealmEventsConfigRepresentation getRealmEventsConfig();

    @PUT
    @Path("events/config")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateRealmEventsConfig(RealmEventsConfigRepresentation rep);

    @GET
    @Path("group-by-path/{path: .*}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GroupRepresentation getGroupByPath(@PathParam("path") String path);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-groups")
    public List<GroupRepresentation> getDefaultGroups();

    @PUT
    @Path("default-groups/{groupId}")
    public void addDefaultGroup(@PathParam("groupId") String groupId);

    @DELETE
    @Path("default-groups/{groupId}")
    public void removeDefaultGroup(@PathParam("groupId") String groupId);

    @Path("identity-provider")
    IdentityProvidersResource identityProviders();

    @DELETE
    void remove();

    @Path("client-session-stats")
    @GET
    List<Map<String, String>> getClientSessionStats();

    @Path("clients-initial-access")
    ClientInitialAccessResource clientInitialAccess();

    @Path("client-registration-policy")
    ClientRegistrationPolicyResource clientRegistrationPolicy();

    @Path("partialImport")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response partialImport(PartialImportRepresentation rep);

    @Path("partial-export")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    RealmRepresentation partialExport(@QueryParam("exportGroupsAndRoles") Boolean exportGroupsAndRoles,
                                             @QueryParam("exportClients") Boolean exportClients);
    @Path("authentication")
    @Consumes(MediaType.APPLICATION_JSON)
    AuthenticationManagementResource flows();

    @Path("attack-detection")
    AttackDetectionResource attackDetection();

    @Path("testLDAPConnection")
    @GET
    @NoCache
    Response testLDAPConnection(@QueryParam("action") String action, @QueryParam("connectionUrl") String connectionUrl,
                                @QueryParam("bindDn") String bindDn, @QueryParam("bindCredential") String bindCredential,
                                @QueryParam("useTruststoreSpi") String useTruststoreSpi, @QueryParam("connectionTimeout") String connectionTimeout);

    @Path("testSMTPConnection/{config}")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    Response testSMTPConnection(final @PathParam("config") String config) throws Exception;

    @Path("clear-realm-cache")
    @POST
    void clearRealmCache();

    @Path("clear-user-cache")
    @POST
    void clearUserCache();

    @Path("clear-keys-cache")
    @POST
    void clearKeysCache();

    @Path("push-revocation")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    GlobalRequestResult pushRevocation();

    @Path("logout-all")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    GlobalRequestResult logoutAll();

    @Path("sessions/{session}")
    @DELETE
    void deleteSession(@PathParam("session") String sessionId);

    @Path("components")
    ComponentsResource components();

    @Path("user-storage")
    UserStorageProviderResource userStorage();


    @Path("keys")
    KeyResource keys();

}
