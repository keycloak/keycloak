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

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.BruteForceProtector;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;

/**
 * Base resource class for the admin REST api of one realm
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AttackDetectionResource {
    protected static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;
    protected RealmAuth auth;
    protected RealmModel realm;
    private AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected ClientConnection connection;

    @Context
    protected HttpHeaders headers;

    public AttackDetectionResource(RealmAuth auth, RealmModel realm, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent.realm(realm).resource(ResourceType.USER_LOGIN_FAILURE);

        auth.init(RealmAuth.Resource.USER);
    }

    /**
     * Get status of a username in brute force detection
     *
     * @param userId
     * @return
     */
    @GET
    @Path("brute-force/users/{userId}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> bruteForceUserStatus(@PathParam("userId") String userId) {
        auth.requireView();

        Map<String, Object> data = new HashMap<>();
        data.put("disabled", false);
        data.put("numFailures", 0);
        data.put("lastFailure", 0);
        data.put("lastIPFailure", "n/a");
        if (!realm.isBruteForceProtected()) return data;

        UserModel user = session.users().getUserById(userId, realm);

        UserLoginFailureModel model = session.sessions().getUserLoginFailure(realm, userId);
        if (model == null) return data;
        if (session.getProvider(BruteForceProtector.class).isTemporarilyDisabled(session, realm, user)) {
            data.put("disabled", true);
        }
        data.put("numFailures", model.getNumFailures());
        data.put("lastFailure", model.getLastFailure());
        data.put("lastIPFailure", model.getLastIPFailure());
        return data;
    }

    /**
     * Clear any user login failures for the user
     *
     * This can release temporary disabled user
     *
     * @param userId
     */
    @Path("brute-force/users/{userId}")
    @DELETE
    public void clearBruteForceForUser(@PathParam("userId") String userId) {
        auth.requireManage();

        UserLoginFailureModel model = session.sessions().getUserLoginFailure(realm, userId);
        if (model != null) {
            session.sessions().removeUserLoginFailure(realm, userId);
            adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
        }
    }

    /**
     * Clear any user login failures for all users
     *
     * This can release temporary disabled users
     *
     */
    @Path("brute-force/users")
    @DELETE
    public void clearAllBruteForce() {
        auth.requireManage();

        session.sessions().removeAllUserLoginFailures(realm);
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
    }


}
