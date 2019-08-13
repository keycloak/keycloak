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
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.DeviceModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserDeviceStore;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.account.ClientRepresentation;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.representations.idm.DeviceRepresentation;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.Cors;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SessionResource {

    private HttpRequest request;
    private final KeycloakSession session;
    private final Auth auth;
    private final RealmModel realm;
    private final UserModel user;

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
    public Response sessions() {
        List<SessionRepresentation> reps = new LinkedList<>();
        List<UserSessionModel> sessions = session.sessions().getUserSessions(realm, user);

        for (UserSessionModel s : sessions) {
            SessionRepresentation rep = new SessionRepresentation();
            rep.setId(s.getId());
            rep.setIpAddress(s.getIpAddress());
            rep.setStarted(s.getStarted());
            rep.setLastAccess(s.getLastSessionRefresh());
            rep.setExpires(s.getStarted() + realm.getSsoSessionMaxLifespan());
            rep.setDevice(toRepresentation(getDevice(s)));

            rep.setClients(new LinkedList());

            for (String clientUUID : s.getAuthenticatedClientSessions().keySet()) {
                ClientModel client = realm.getClientById(clientUUID);
                ClientRepresentation clientRep = new ClientRepresentation();
                clientRep.setClientId(client.getClientId());
                clientRep.setClientName(client.getName());
                rep.getClients().add(clientRep);
            }

            reps.add(rep);
        }

        return Cors.add(request, Response.ok(reps)).auth().allowedOrigins(auth.getToken()).build();
    }

    private DeviceModel getDevice(UserSessionModel s) {
        String id = s.getNote(DeviceModel.DEVICE_ID);

        if (id != null) {
            return getDeviceProvider().getDeviceById(auth.getUser(), id);
        }

        return null;
    }

    /**
     * Get all devices from where sessions were started
     *
     * @return
     */
    @Path("/devices")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response history() {
        return Cors.add(request, Response.ok(getDeviceProvider().getDevices(user).stream().map(this::toRepresentation).collect(
                Collectors.toList())))
                .auth().allowedOrigins(auth.getToken()).build();
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
        UserSessionModel userSession = auth.getSession();

        List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, user);
        for (UserSessionModel s : userSessions) {
            if (removeCurrent || !s.getId().equals(userSession.getId())) {
                AuthenticationManager.backchannelLogout(session, s, true);
            }
        }

        return Cors.add(request, Response.ok()).auth().allowedOrigins(auth.getToken()).build();
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
        return Cors.add(request, Response.ok()).auth().allowedOrigins(auth.getToken()).build();
    }

    private DeviceRepresentation toRepresentation(DeviceModel device) {
        if (device == null) {
            return null;
        }

        DeviceRepresentation representation = new DeviceRepresentation();

        representation.setCreated(device.getCreated());
        representation.setLastAccess(device.getLastAccess());
        representation.setBrowser(device.getBrowser());
        representation.setDevice(device.getDevice());
        representation.setOs(device.getOs());
        representation.setOsVersion(device.getOsVersion());
        representation.setIp(device.getIp());

        DeviceModel attached = getDevice(auth.getSession());

        if (attached != null) {
            representation.setCurrent(attached.getId().equals(device.getId()));
        }

        return representation;
    }

    private UserDeviceStore getDeviceProvider() {
        if (session.users() instanceof UserDeviceStore) {
            return (UserDeviceStore) session.users();
        }

        return null;
    }
}
