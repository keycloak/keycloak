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

package org.keycloak.testsuite.client.resources;

import java.util.Date;
import java.util.List;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testsuite.rest.representation.AuthenticatorState;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import org.jboss.resteasy.annotations.cache.NoCache;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */

@Path("/realms/master/testing")
@Consumes(MediaType.APPLICATION_JSON)
public interface TestingResource {

    @GET
    @Path("/time-offset")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, String> getTimeOffset();

    @PUT
    @Path("/time-offset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, String> setTimeOffset(Map<String, String> time);

    @POST
    @Path("/poll-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    EventRepresentation pollEvent();

    @POST
    @Path("/poll-admin-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    AdminEventRepresentation pollAdminEvent();

    @POST
    @Path("/clear-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    Response clearEventQueue();

    @POST
    @Path("/clear-admin-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    Response clearAdminEventQueue();

    @GET
    @Path("/clear-event-store")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearEventStore();

    @GET
    @Path("/clear-event-store-for-realm")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearEventStore(@QueryParam("realmId") String realmId);

    @GET
    @Path("/clear-event-store-older-than")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearEventStore(@QueryParam("realmId") String realmId, @QueryParam("olderThan") long olderThan);

    /**
     * Query events
     *
     * Returns all events, or filters them based on URL query parameters listed here
     *
     * @param realmId The realm
     * @param types The types of events to return
     * @param client App or oauth client name
     * @param user User id
     * @param dateFrom From date
     * @param dateTo To date
     * @param ipAddress IP address
     * @param firstResult Paging offset
     * @param maxResults Paging size
     * @return
     */
    @Path("query-events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<EventRepresentation> queryEvents(@QueryParam("realmId") String realmId, @QueryParam("type") List<String> types, @QueryParam("client") String client,
            @QueryParam("user") String user, @QueryParam("dateFrom") Date dateFrom, @QueryParam("dateTo") Date dateTo,
            @QueryParam("ipAddress") String ipAddress, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults);

    @PUT
    @Path("/on-event")
    @Consumes(MediaType.APPLICATION_JSON)
    public void onEvent(final EventRepresentation rep);

    @GET
    @Path("/clear-admin-event-store")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearAdminEventStore();

    @GET
    @Path("/clear-admin-event-store-for-realm")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearAdminEventStore(@QueryParam("realmId") String realmId);

    @GET
    @Path("/clear-admin-event-store-older-than")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearAdminEventStore(@QueryParam("realmId") String realmId, @QueryParam("olderThan") long olderThan);

    /**
     * Get admin events
     *
     * Returns all admin events, or filters events based on URL query parameters listed here
     *
     * @param realmId
     * @param operationTypes
     * @param authRealm
     * @param authClient
     * @param authUser user id
     * @param authIpAddress
     * @param resourcePath
     * @param dateFrom
     * @param dateTo
     * @param firstResult
     * @param maxResults
     * @return
     */
    @Path("query-admin-events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<AdminEventRepresentation> getAdminEvents(@QueryParam("realmId") String realmId, @QueryParam("operationTypes") List<String> operationTypes, @QueryParam("authRealm") String authRealm, @QueryParam("authClient") String authClient,
            @QueryParam("authUser") String authUser, @QueryParam("authIpAddress") String authIpAddress,
            @QueryParam("resourcePath") String resourcePath, @QueryParam("dateFrom") Date dateFrom,
            @QueryParam("dateTo") Date dateTo, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults);

    @POST
    @Path("/on-admin-event")
    @Consumes(MediaType.APPLICATION_JSON)
    public void onAdminEvent(final AdminEventRepresentation rep, @QueryParam("includeRepresentation") boolean includeRepresentation);

    @POST
    @Path("/remove-user-session")
    @Produces(MediaType.APPLICATION_JSON)
    Response removeUserSession(@QueryParam("realm") final String realm, @QueryParam("session") final String sessionId);

    @POST
    @Path("/remove-user-sessions")
    @Produces(MediaType.APPLICATION_JSON)
    Response removeUserSessions(@QueryParam("realm") final String realm);

    @GET
    @Path("/get-user-session")
    @Produces(MediaType.APPLICATION_JSON)
    Integer getLastSessionRefresh(@QueryParam("realm") final String realm, @QueryParam("session") final String sessionId);

    @POST
    @Path("/remove-expired")
    @Produces(MediaType.APPLICATION_JSON)
    Response removeExpired(@QueryParam("realm") final String realm);

    @GET
    @Path("/cache/{cache}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    boolean isCached(@PathParam("cache") String cacheName, @PathParam("id") String id);

    @GET
    @Path("/verify-code")
    @Produces(MediaType.APPLICATION_JSON)
    String verifyCode(@QueryParam("realm") String realmName, @QueryParam("code") String code);

    @POST
    @Path("/update-pass-through-auth-state")
    @Produces(MediaType.APPLICATION_JSON)
    AuthenticatorState updateAuthenticator(AuthenticatorState state);
}
