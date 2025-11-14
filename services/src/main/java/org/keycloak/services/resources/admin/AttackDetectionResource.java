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

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

/**
 * Base resource class for the admin REST api of one realm
 *
 * @resource Attack Detection
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class AttackDetectionResource {
    protected static final Logger logger = Logger.getLogger(AttackDetectionResource.class);
    protected final AdminPermissionEvaluator auth;
    protected final RealmModel realm;
    private final AdminEventBuilder adminEvent;

    protected final KeycloakSession session;

    protected final ClientConnection connection;

    protected final HttpHeaders headers;

    public AttackDetectionResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.auth = auth;
        this.realm = session.getContext().getRealm();
        this.connection = session.getContext().getConnection();
        this.adminEvent = adminEvent.realm(realm).resource(ResourceType.USER_LOGIN_FAILURE);
        this.headers = session.getContext().getRequestHeaders();
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ATTACK_DETECTION)
    @Operation( summary = "Get status of a username in brute force detection")
    public Map<String, Object> bruteForceUserStatus(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            auth.users().requireView();
        } else {
            auth.users().requireView(user);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("disabled", false);
        data.put("numFailures", 0);
        data.put("numTemporaryLockouts", 0);
        data.put("lastFailure", 0);
        data.put("lastIPFailure", "n/a");
        data.put("failedLoginNotBefore", 0);
        if (!realm.isBruteForceProtected()) return data;


        UserLoginFailureModel model = session.loginFailures().getUserLoginFailure(realm, userId);
        if (model == null) return data;

        boolean disabled = isUserDisabled(model, user);
        if (disabled) {
            data.put("disabled", true);
            if(session.getProvider(BruteForceProtector.class).isTemporarilyDisabled(session, realm, user)) {
                data.put("failedLoginNotBefore", model.getFailedLoginNotBefore());
            } else {
                data.put("failedLoginNotBefore", Long.MAX_VALUE);
            }
        }

        data.put("numFailures", model.getNumFailures());
        data.put("numTemporaryLockouts", model.getNumTemporaryLockouts());
        data.put("lastFailure", model.getLastFailure());
        data.put("lastIPFailure", model.getLastIPFailure());
        return data;
    }

    private boolean isUserDisabled(UserLoginFailureModel model, UserModel user) {
        if(user == null) {
            return Time.currentTime() < model.getFailedLoginNotBefore();
        }

        return isUserDisabledOrLockedByBruteForce(session, realm, user);
    }

    private boolean isUserDisabledOrLockedByBruteForce(KeycloakSession session, RealmModel realm, UserModel user) {
        return session.getProvider(BruteForceProtector.class).isPermanentlyLockedOut(session, realm, user) 
        || session.getProvider(BruteForceProtector.class).isTemporarilyDisabled(session, realm, user);
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ATTACK_DETECTION)
    @Operation( summary="Clear any user login failures for the user This can release temporary disabled user")
    public void clearBruteForceForUser(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            auth.users().requireManage();
        } else {
            auth.users().requireManage(user);
        }
        UserLoginFailureModel model = session.loginFailures().getUserLoginFailure(realm, userId);
        if (model != null) {
            session.loginFailures().removeUserLoginFailure(realm, userId);
            adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ATTACK_DETECTION)
    @Operation( summary = "Clear any user login failures for all users This can release temporary disabled users")
    public void clearAllBruteForce() {
        auth.users().requireManage();

        session.loginFailures().removeAllUserLoginFailures(realm);
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }


}
