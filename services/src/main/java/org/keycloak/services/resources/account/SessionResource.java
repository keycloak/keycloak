/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.resources.account;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.device.DeviceActivityManager;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.account.ClientRepresentation;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.Cors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class SessionResource {

    private final KeycloakSession session;
    private final Auth auth;
    private final RealmModel realm;
    private final UserModel user;
    private HttpRequest request;

    public SessionResource(KeycloakSession session, Auth auth, HttpRequest request) {
        this.session = session;
        this.auth = auth;
        this.realm = auth.getRealm();
        this.user = auth.getUser();
        this.request = request;
    }

    /**
     * Get session information.
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response toRepresentation() {
        return Cors.add(request, Response.ok(session.sessions().getUserSessions(realm, user).stream()
                .map(this::toRepresentation).collect(Collectors.toList()))).auth().allowedOrigins(auth.getToken()).build();
    }

    /**
     * Get device activity information based on the active sessions.
     *
     * @return
     */
    @Path("devices")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response devices() {
        Map<String, DeviceRepresentation> reps = new HashMap<>();
        List<UserSessionModel> sessions = session.sessions().getUserSessions(realm, user);

        for (UserSessionModel s : sessions) {
            DeviceRepresentation device = getAttachedDevice(s);
            DeviceRepresentation rep = reps
                    .computeIfAbsent(device.getOs() + device.getOsVersion(), key -> {
                        DeviceRepresentation representation = new DeviceRepresentation();

                        representation.setLastAccess(device.getLastAccess());
                        representation.setOs(device.getOs());
                        representation.setOsVersion(device.getOsVersion());
                        representation.setDevice(device.getDevice());
                        representation.setMobile(device.isMobile());

                        return representation;
                    });

            if (isCurrentSession(s)) {
                rep.setCurrent(true);
            }

            if (rep.getLastAccess() == 0 || rep.getLastAccess() < s.getLastSessionRefresh()) {
                rep.setLastAccess(s.getLastSessionRefresh());
            }

            rep.addSession(createSessionRepresentation(s, device));
        }

        return Cors.add(request, Response.ok(reps.values())).auth().allowedOrigins(auth.getToken()).build();
    }

    /**
     * Remove sessions
     *
     * @param removeCurrent remove current session (default is false)
     * @return
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response logout(@QueryParam("current") boolean removeCurrent) {
        auth.require(AccountRoles.MANAGE_ACCOUNT);
        List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, user);

        for (UserSessionModel s : userSessions) {
            if (removeCurrent || !isCurrentSession(s)) {
                AuthenticationManager.backchannelLogout(session, s, true);
            }
        }

        return Cors.add(request, Response.noContent()).auth().allowedOrigins(auth.getToken()).build();
    }

    /**
     * Remove a specific session
     *
     * @param id a specific session to remove
     * @return
     */
    @Path("/{id}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response logout(@PathParam("id") String id) {
        auth.require(AccountRoles.MANAGE_ACCOUNT);
        UserSessionModel userSession = session.sessions().getUserSession(realm, id);
        if (userSession != null && userSession.getUser().equals(user)) {
            AuthenticationManager.backchannelLogout(session, userSession, true);
        }
        return Cors.add(request, Response.noContent()).auth().allowedOrigins(auth.getToken()).build();
    }

    private SessionRepresentation createSessionRepresentation(UserSessionModel s, DeviceRepresentation device) {
        SessionRepresentation sessionRep = new SessionRepresentation();

        sessionRep.setId(s.getId());
        sessionRep.setIpAddress(s.getIpAddress());
        sessionRep.setStarted(s.getStarted());
        sessionRep.setLastAccess(s.getLastSessionRefresh());
        sessionRep.setExpires(s.getStarted() + realm.getSsoSessionMaxLifespan());
        sessionRep.setBrowser(device.getBrowser());

        if (isCurrentSession(s)) {
            sessionRep.setCurrent(true);
        }

        sessionRep.setClients(new LinkedList());

        for (String clientUUID : s.getAuthenticatedClientSessions().keySet()) {
            ClientModel client = realm.getClientById(clientUUID);
            ClientRepresentation clientRep = new ClientRepresentation();
            clientRep.setClientId(client.getClientId());
            clientRep.setClientName(client.getName());
            sessionRep.getClients().add(clientRep);
        }
        return sessionRep;
    }

    private DeviceRepresentation getAttachedDevice(UserSessionModel s) {
        DeviceRepresentation device = DeviceActivityManager.getCurrentDevice(s);

        if (device == null) {
            device = DeviceRepresentation.unknown();
            device.setIpAddress(s.getIpAddress());
        }

        return device;
    }

    private boolean isCurrentSession(UserSessionModel session) {
        return session.getId().equals(auth.getSession().getId());
    }

    private SessionRepresentation toRepresentation(UserSessionModel s) {
        return createSessionRepresentation(s, getAttachedDevice(s));
    }
}
