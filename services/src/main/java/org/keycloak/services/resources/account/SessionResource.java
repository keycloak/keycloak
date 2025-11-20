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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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

import org.jboss.resteasy.reactive.NoCache;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class SessionResource {

    private final KeycloakSession session;
    private final Auth auth;
    private final RealmModel realm;
    private final UserModel user;

    public SessionResource(KeycloakSession session, Auth auth) {
        this.session = session;
        this.auth = auth;
        this.realm = auth.getRealm();
        this.user = auth.getUser();
    }

    /**
     * Get session information.
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Stream<SessionRepresentation> toRepresentation() {
        return session.sessions().getUserSessionsStream(realm, user).map(this::toRepresentation);
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
    public Collection<DeviceRepresentation> devices() {
        Map<String, DeviceRepresentation> reps = new HashMap<>();
        session.sessions().getUserSessionsStream(realm, user).forEach(s -> {
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
            });

        return reps.values();
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
        session.sessions().getUserSessionsStream(realm, user).filter(s -> removeCurrent || !isCurrentSession(s))
                .collect(Collectors.toList()) // collect to avoid concurrent modification as backchannelLogout removes the user sessions.
                .forEach(s -> AuthenticationManager.backchannelLogout(session, s, true));

        return Response.noContent().build();
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
        return Response.noContent().build();
    }

    private SessionRepresentation createSessionRepresentation(UserSessionModel s, DeviceRepresentation device) {
        SessionRepresentation sessionRep = new SessionRepresentation();

        sessionRep.setId(s.getId());
        sessionRep.setIpAddress(s.getIpAddress());
        sessionRep.setStarted(s.getStarted());
        sessionRep.setLastAccess(s.getLastSessionRefresh());
        int maxLifespan = s.isRememberMe() && realm.getSsoSessionMaxLifespanRememberMe() > 0
                ? realm.getSsoSessionMaxLifespanRememberMe() : realm.getSsoSessionMaxLifespan();
        int expires = s.getStarted() + maxLifespan;
        sessionRep.setExpires(expires);
        sessionRep.setBrowser(device.getBrowser());

        if (isCurrentSession(s)) {
            sessionRep.setCurrent(true);
        }

        sessionRep.setClients(new LinkedList());

        for (String clientUUID : s.getAuthenticatedClientSessions().keySet()) {
            ClientModel client = realm.getClientById(clientUUID);
            if (client != null) {
                ClientRepresentation clientRep = new ClientRepresentation();
                clientRep.setClientId(client.getClientId());
                clientRep.setClientName(client.getName());
                sessionRep.getClients().add(clientRep);
            }
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
        if (auth.getSession() == null) return false;
        return session.getId().equals(auth.getSession().getId());
    }

    private SessionRepresentation toRepresentation(UserSessionModel s) {
        return createSessionRepresentation(s, getAttachedDevice(s));
    }
}
