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

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.LDAPCapabilityRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.TestLdapConnectionRepresentation;

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

    @Path("client-scopes")
    ClientScopesResource clientScopes();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-default-client-scopes")
    List<ClientScopeRepresentation> getDefaultDefaultClientScopes();

    @PUT
    @Path("default-default-client-scopes/{clientScopeId}")
    void addDefaultDefaultClientScope(@PathParam("clientScopeId") String clientScopeId);

    @DELETE
    @Path("default-default-client-scopes/{clientScopeId}")
    void removeDefaultDefaultClientScope(@PathParam("clientScopeId") String clientScopeId);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-optional-client-scopes")
    List<ClientScopeRepresentation> getDefaultOptionalClientScopes();

    @PUT
    @Path("default-optional-client-scopes/{clientScopeId}")
    void addDefaultOptionalClientScope(@PathParam("clientScopeId") String clientScopeId);

    @DELETE
    @Path("default-optional-client-scopes/{clientScopeId}")
    void removeDefaultOptionalClientScope(@PathParam("clientScopeId") String clientScopeId);

    @Path("client-description-converter")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
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
    @Produces(MediaType.APPLICATION_JSON)
    List<EventRepresentation> getEvents(@QueryParam("type") List<String> types, @QueryParam("client") String client,
            @QueryParam("user") String user, @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo,
            @QueryParam("ipAddress") String ipAddress, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults);

    /**
     * Get events
     *
     * Returns all events, or filters them based on URL query parameters listed here
     *
     * @param types The types of events to return
     * @param client App or oauth client name
     * @param user User id
     * @param ipAddress IP address
     * @param dateFrom From (inclusive) date (yyyy-MM-dd)
     * @param dateTo To (inclusive) date (yyyy-MM-dd)
     * @param firstResult Paging offset
     * @param maxResults Maximum results size (defaults to 100)
     * @param direction The direction to sort events by. Available values are "asc" or "desc". The parameter is supported since Keycloak 26.2
     * @return events
     * @since Keycloak 26.2. Use method {@link #getEvents(List, String, String, String, String, String, Integer, Integer)} for the older versions of the Keycloak server
     */
    @Path("events")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<EventRepresentation> getEvents(@QueryParam("type") List<String> types, @QueryParam("client") String client,
            @QueryParam("user") String user, @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo,
            @QueryParam("ipAddress") String ipAddress, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults,
            @QueryParam("direction") String direction);

    /**
     * Get events
     *
     * Returns all events, or filters them based on URL query parameters listed here
     *
     * @param types The types of events to return
     * @param client App or oauth client name
     * @param user User id
     * @param ipAddress IP address
     * @param dateFrom time in Epoch timestamp. The parameter is supported since Keycloak 26.2
     * @param dateTo time in Epoch timestamp. The parameter is supported since Keycloak 26.2
     * @param firstResult Paging offset
     * @param maxResults Maximum results size (defaults to 100)
     * @param direction The direction to sort events by. Available values are "asc" or "desc". The parameter is supported since Keycloak 26.2
     * @return events
     * @since Keycloak 26.2. Use method {@link #getEvents(List, String, String, String, String, String, Integer, Integer)} for the older versions of the Keycloak server
     */
    @Path("events")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<EventRepresentation> getEvents(@QueryParam("type") List<String> types, @QueryParam("client") String client,
            @QueryParam("user") String user, @QueryParam("dateFrom") long dateFrom, @QueryParam("dateTo") long dateTo,
            @QueryParam("ipAddress") String ipAddress, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults,
            @QueryParam("direction") String direction);

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
    @Path("admin-events")
    @Produces(MediaType.APPLICATION_JSON)
    List<AdminEventRepresentation> getAdminEvents(@QueryParam("operationTypes") List<String> operationTypes, @QueryParam("authRealm") String authRealm, @QueryParam("authClient") String authClient,
            @QueryParam("authUser") String authUser, @QueryParam("authIpAddress") String authIpAddress,
            @QueryParam("resourcePath") String resourcePath, @QueryParam("resourceTypes") List<String> resourceTypes, @QueryParam("dateFrom") String dateFrom,
            @QueryParam("dateTo") String dateTo, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults);

    /**
     * Get admin events
     *
     * Returns all admin events, or filters events based on URL query parameters listed here
     *
     * @param operationTypes operation types
     * @param authRealm realm, from where the user was authenticated
     * @param authClient client, which authenticated the event operation
     * @param authUser user, which did the particular admin event
     * @param authIpAddress IP address from which the event was done
     * @param resourcePath resource path
     * @param resourceTypes resource types
     * @param dateFrom time in Epoch timestamp. The parameter is supported since Keycloak 26.2
     * @param dateTo time in Epoch timestamp. The parameter is supported since Keycloak 26.2
     * @param firstResult Paging offset
     * @param maxResults Maximum results size (defaults to 100)
     * @param direction The direction to sort events by. Available values are "asc" or "desc". The parameter is supported since Keycloak 26.2
     * @return admin events
     * @since Keycloak 26.2. Use method {@link #getAdminEvents(List, String, String, String, String, String, List, String, String, Integer, Integer)} for the older versions of the Keycloak server
     */
    @GET
    @Path("admin-events")
    @Produces(MediaType.APPLICATION_JSON)
    List<AdminEventRepresentation> getAdminEvents(@QueryParam("operationTypes") List<String> operationTypes, @QueryParam("authRealm") String authRealm, @QueryParam("authClient") String authClient,
            @QueryParam("authUser") String authUser, @QueryParam("authIpAddress") String authIpAddress,
            @QueryParam("resourcePath") String resourcePath, @QueryParam("resourceTypes") List<String> resourceTypes, @QueryParam("dateFrom") String dateFrom,
            @QueryParam("dateTo") String dateTo, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults, @QueryParam("direction") String direction);

    /**
     * Get admin events
     *
     * Returns all admin events, or filters events based on URL query parameters listed here
     *
     * @param operationTypes operation types
     * @param authRealm realm, from where the user was authenticated
     * @param authClient client, which authenticated the event operation
     * @param authUser user, which did the particular admin event
     * @param authIpAddress IP address from which the event was done
     * @param resourcePath resource path
     * @param resourceTypes resource types
     * @param dateFrom From (inclusive) date (yyyy-MM-dd)
     * @param dateTo To (inclusive) date (yyyy-MM-dd)
     * @param firstResult Paging offset
     * @param maxResults Maximum results size (defaults to 100)
     * @param direction The direction to sort events by. Available values are "asc" or "desc". The parameter is supported since Keycloak 26.2
     * @return admin events
     * @since Keycloak 26.2. Use method {@link #getAdminEvents(List, String, String, String, String, String, List, String, String, Integer, Integer)} for the older versions of the Keycloak server
     */
    @GET
    @Path("admin-events")
    @Produces(MediaType.APPLICATION_JSON)
    List<AdminEventRepresentation> getAdminEvents(@QueryParam("operationTypes") List<String> operationTypes, @QueryParam("authRealm") String authRealm, @QueryParam("authClient") String authClient,
            @QueryParam("authUser") String authUser, @QueryParam("authIpAddress") String authIpAddress,
            @QueryParam("resourcePath") String resourcePath, @QueryParam("resourceTypes") List<String> resourceTypes, @QueryParam("dateFrom") long dateFrom,
            @QueryParam("dateTo") long dateTo, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults, @QueryParam("direction") String direction);

    @GET
    @Path("events/config")
    @Produces(MediaType.APPLICATION_JSON)
    RealmEventsConfigRepresentation getRealmEventsConfig();

    @PUT
    @Path("events/config")
    @Consumes(MediaType.APPLICATION_JSON)
    void updateRealmEventsConfig(RealmEventsConfigRepresentation rep);

    @GET
    @Path("group-by-path/{path: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    GroupRepresentation getGroupByPath(@PathParam("path") String path);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-groups")
    List<GroupRepresentation> getDefaultGroups();

    @PUT
    @Path("default-groups/{groupId}")
    void addDefaultGroup(@PathParam("groupId") String groupId);

    @DELETE
    @Path("default-groups/{groupId}")
    void removeDefaultGroup(@PathParam("groupId") String groupId);

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
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Deprecated
    Response testLDAPConnection(@FormParam("action") String action, @FormParam("connectionUrl") String connectionUrl,
                                @FormParam("bindDn") String bindDn, @FormParam("bindCredential") String bindCredential,
                                @FormParam("useTruststoreSpi") String useTruststoreSpi, @FormParam("connectionTimeout") String connectionTimeout);

    @Path("testLDAPConnection")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response testLDAPConnection(TestLdapConnectionRepresentation config);

    @POST
    @Path("ldap-server-capabilities")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    List<LDAPCapabilityRepresentation> ldapServerCapabilities(TestLdapConnectionRepresentation config);

    @Path("testSMTPConnection")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Deprecated
    Response testSMTPConnection(@FormParam("config") String config);

    @Path("testSMTPConnection")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response testSMTPConnection(Map<String, String> config);

    @Path("clear-realm-cache")
    @POST
    void clearRealmCache();

    @Path("clear-user-cache")
    @POST
    void clearUserCache();

    @Path("clear-keys-cache")
    @POST
    void clearKeysCache();

    /**
     * Clear the crl cache (CRLs loaded for X509 authentication
     * @since Keycloak 26.2
     */
    @Path("clear-crl-cache")
    @POST
    void clearCrlCache();

    @Path("push-revocation")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    GlobalRequestResult pushRevocation();

    @Path("logout-all")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    GlobalRequestResult logoutAll();

    /**
     * Delete given user session
     *
     * @param sessionId session ID
     * @param offline Parameter available since Keycloak server 24.0.2. Will be ignored on older Keycloak versions with the default value false.
     * @throws jakarta.ws.rs.NotFoundException if the user session is not found
     */
    @Path("sessions/{session}")
    @DELETE
    void deleteSession(@PathParam("session") String sessionId, @DefaultValue("false") @QueryParam("isOffline") boolean offline);

    @Path("components")
    ComponentsResource components();

    @Path("user-storage")
    UserStorageProviderResource userStorage();


    @Path("keys")
    KeyResource keys();

    @Path("localization")
    RealmLocalizationResource localization();

    @Path("client-policies/policies")
    ClientPoliciesPoliciesResource clientPoliciesPoliciesResource();

    @Path("client-policies/profiles")
    ClientPoliciesProfilesResource clientPoliciesProfilesResource();

    @Path("organizations")
    OrganizationsResource organizations();

    @Path("client-types")
    ClientTypesResource clientTypes();

    @Path("workflows")
    WorkflowsResource workflows();
}
