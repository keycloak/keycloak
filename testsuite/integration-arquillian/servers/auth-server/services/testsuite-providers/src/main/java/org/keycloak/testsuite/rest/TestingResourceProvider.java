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

package org.keycloak.testsuite.rest;

import org.infinispan.Cache;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.testsuite.events.EventsListenerProvider;
import org.keycloak.testsuite.forms.PassThroughAuthenticator;
import org.keycloak.testsuite.forms.PassThroughClientAuthenticator;
import org.keycloak.testsuite.rest.representation.AuthenticatorState;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TestingResourceProvider implements RealmResourceProvider {

    private KeycloakSession session;

    @Override
    public Object getResource() {
        return this;
    }

    public TestingResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @POST
    @Path("/remove-user-session")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUserSession(@QueryParam("realm") final String name, @QueryParam("session") final String sessionId) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) {
            throw new NotFoundException("Realm not found");
        }

        UserSessionModel sessionModel = session.sessions().getUserSession(realm, sessionId);
        if (sessionModel == null) {
            throw new NotFoundException("Session not found");
        }

        session.sessions().removeUserSession(realm, sessionModel);
        return Response.ok().build();
    }

    @POST
    @Path("/remove-user-sessions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUserSessions(@QueryParam("realm") final String realmName) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotFoundException("Realm not found");
        }

        session.sessions().removeUserSessions(realm);
        return Response.ok().build();
    }

    @GET
    @Path("/get-user-session")
    @Produces(MediaType.APPLICATION_JSON)
    public Integer getLastSessionRefresh(@QueryParam("realm") final String name, @QueryParam("session") final String sessionId) {

        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) {
            throw new NotFoundException("Realm not found");
        }

        UserSessionModel sessionModel = session.sessions().getUserSession(realm, sessionId);
        if (sessionModel == null) {
            throw new NotFoundException("Session not found");
        }

        return sessionModel.getLastSessionRefresh();
    }

    @POST
    @Path("/remove-expired")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeExpired(@QueryParam("realm") final String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) {
            throw new NotFoundException("Realm not found");
        }

        session.sessions().removeExpired(realm);
        return Response.ok().build();
    }

    @GET
    @Path("/time-offset")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getTimeOffset() {
        Map<String, String> response = new HashMap<>();
        response.put("currentTime", String.valueOf(Time.currentTime()));
        response.put("offset", String.valueOf(Time.getOffset()));
        return response;
    }

    @PUT
    @Path("/time-offset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> setTimeOffset(Map<String, String> time) {
        int offset = Integer.parseInt(time.get("offset"));
        Time.setOffset(offset);
        return getTimeOffset();
    }

    @POST
    @Path("/poll-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    public EventRepresentation getEvent() {
        Event event = EventsListenerProvider.poll();
        if (event != null) {
            return ModelToRepresentation.toRepresentation(event);
        } else {
            return null;
        }
    }

    @POST
    @Path("/poll-admin-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    public AdminEventRepresentation getAdminEvent() {
        AdminEvent adminEvent = EventsListenerProvider.pollAdminEvent();
        if (adminEvent != null) {
            return ModelToRepresentation.toRepresentation(adminEvent);
        } else {
            return null;
        }
    }

    @POST
    @Path("/clear-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearEventQueue() {
        EventsListenerProvider.clear();
        return Response.ok().build();
    }

    @POST
    @Path("/clear-admin-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearAdminEventQueue() {
        EventsListenerProvider.clearAdminEvents();
        return Response.ok().build();
    }

    @GET
    @Path("/cache/{cache}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean isCached(@PathParam("cache") String cacheName, @PathParam("id") String id) {
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        Cache<Object, Object> cache = provider.getCache(cacheName);
        return cache.containsKey(id);
    }

    @Override
    public void close() {
    }

    /*
     * Migration from KeycloakRule#verifyCode
     */
    @GET
    @Path("/verify-code")
    @Produces(MediaType.APPLICATION_JSON)
    public String verifyCode(@QueryParam("realm") String realmName, @QueryParam("code") String code) {
        RealmModel realm = session.realms().getRealm(realmName);
        try {
            ClientSessionCode accessCode = ClientSessionCode.parse(code, session, realm);
            if (accessCode == null) {
                throw new AssertionError("Invalid code");
            }
            return accessCode.getClientSession().getId();
        } catch (Throwable t) {
            throw new AssertionError("Failed to parse code", t);
        }
    }

    @POST
    @Path("/update-pass-through-auth-state")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticatorState updateAuthenticator(AuthenticatorState state) {
        if (state.getClientId() != null) {
            PassThroughClientAuthenticator.clientId = state.getClientId();
        }
        if (state.getUsername() != null) {
            PassThroughAuthenticator.username = state.getUsername();
        }

        AuthenticatorState result = new AuthenticatorState();
        result.setClientId(PassThroughClientAuthenticator.clientId);
        result.setUsername(PassThroughAuthenticator.username);
        return result;
    }
}
