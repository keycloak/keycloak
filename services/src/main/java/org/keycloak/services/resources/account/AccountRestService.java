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
package org.keycloak.services.resources.account;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.account.ClientRepresentation;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.storage.ReadOnlyException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountRestService {

    @Context
    private HttpRequest request;
    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpHeaders headers;
    @Context
    protected ClientConnection clientConnection;

    private final KeycloakSession session;
    private final ClientModel client;
    private final EventBuilder event;
    private EventStoreProvider eventStore;
    private Auth auth;
    
    private final RealmModel realm;
    private final UserModel user;

    public AccountRestService(KeycloakSession session, Auth auth, ClientModel client, EventBuilder event) {
        this.session = session;
        this.auth = auth;
        this.realm = auth.getRealm();
        this.user = auth.getUser();
        this.client = client;
        this.event = event;
    }
    
    public void init() {
        eventStore = session.getProvider(EventStoreProvider.class);
    }

    /**
     * CORS preflight
     *
     * @return
     */
    @Path("/")
    @OPTIONS
    @NoCache
    public Response preflight() {
        return Cors.add(request, Response.ok()).auth().preflight().build();
    }

    /**
     * Get account information.
     *
     * @return
     */
    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response account() {
        auth.requireOneOf(AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE);

        UserModel user = auth.getUser();

        UserRepresentation rep = new UserRepresentation();
        rep.setUsername(user.getUsername());
        rep.setFirstName(user.getFirstName());
        rep.setLastName(user.getLastName());
        rep.setEmail(user.getEmail());
        rep.setEmailVerified(user.isEmailVerified());
        rep.setAttributes(user.getAttributes());

        return Cors.add(request, Response.ok(rep)).auth().allowedOrigins(auth.getToken()).build();
    }

    @Path("/")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response updateAccount(UserRepresentation userRep) {
        auth.require(AccountRoles.MANAGE_ACCOUNT);

        event.event(EventType.UPDATE_PROFILE).client(auth.getClient()).user(user);

        try {
            RealmModel realm = session.getContext().getRealm();

            boolean usernameChanged = userRep.getUsername() != null && !userRep.getUsername().equals(user.getUsername());
            if (realm.isEditUsernameAllowed()) {
                if (usernameChanged) {
                    UserModel existing = session.users().getUserByUsername(userRep.getUsername(), realm);
                    if (existing != null) {
                        return ErrorResponse.exists(Errors.USERNAME_EXISTS);
                    }

                    user.setUsername(userRep.getUsername());
                }
            } else if (usernameChanged) {
                return ErrorResponse.error(Errors.READ_ONLY_USERNAME, Response.Status.BAD_REQUEST);
            }

            boolean emailChanged = userRep.getEmail() != null && !userRep.getEmail().equals(user.getEmail());
            if (emailChanged && !realm.isDuplicateEmailsAllowed()) {
                UserModel existing = session.users().getUserByEmail(userRep.getEmail(), realm);
                if (existing != null) {
                    return ErrorResponse.exists(Errors.EMAIL_EXISTS);
                }
            }

            if (realm.isRegistrationEmailAsUsername() && !realm.isDuplicateEmailsAllowed()) {
                UserModel existing = session.users().getUserByUsername(userRep.getEmail(), realm);
                if (existing != null) {
                    return ErrorResponse.exists(Errors.USERNAME_EXISTS);
                }
            }

            if (emailChanged) {
                String oldEmail = user.getEmail();
                user.setEmail(userRep.getEmail());
                user.setEmailVerified(false);
                event.clone().event(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, oldEmail).detail(Details.UPDATED_EMAIL, userRep.getEmail()).success();

                if (realm.isRegistrationEmailAsUsername()) {
                    user.setUsername(userRep.getEmail());
                }
            }

            user.setFirstName(userRep.getFirstName());
            user.setLastName(userRep.getLastName());

            if (userRep.getAttributes() != null) {
                for (String k : user.getAttributes().keySet()) {
                    if (!userRep.getAttributes().containsKey(k)) {
                        user.removeAttribute(k);
                    }
                }

                for (Map.Entry<String, List<String>> e : userRep.getAttributes().entrySet()) {
                    user.setAttribute(e.getKey(), e.getValue());
                }
            }

            event.success();

            return Cors.add(request, Response.ok()).auth().allowedOrigins(auth.getToken()).build();
        } catch (ReadOnlyException e) {
            return ErrorResponse.error(Errors.READ_ONLY_USER, Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Get session information.
     *
     * @return
     */
    @Path("/sessions")
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

    /**
     * Remove sessions
     *
     * @param removeCurrent remove current session (default is false)
     * @return
     */
    @Path("/sessions")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response sessionsLogout(@QueryParam("current") boolean removeCurrent) {
        UserSessionModel userSession = auth.getSession();

        List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, user);
        for (UserSessionModel s : userSessions) {
            if (removeCurrent || !s.getId().equals(userSession.getId())) {
                AuthenticationManager.backchannelLogout(session, s, true);
            }
        }

        return Cors.add(request, Response.ok()).auth().allowedOrigins(auth.getToken()).build();
    }

    // TODO Federated identities
    // TODO Applications
    // TODO Logs

}
