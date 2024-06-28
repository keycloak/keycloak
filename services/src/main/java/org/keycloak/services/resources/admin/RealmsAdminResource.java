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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.policy.PasswordPolicyNotMetException;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.ExportImportManager;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static org.keycloak.utils.StreamsUtil.throwIfEmpty;

/**
 * Top level resource for Admin REST API
 *
 * @resource Realms Admin
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class RealmsAdminResource {
    protected static final Logger logger = Logger.getLogger(RealmsAdminResource.class);
    protected final AdminAuth auth;
    protected final TokenManager tokenManager;

    protected final KeycloakSession session;

    protected final ClientConnection clientConnection;

    public RealmsAdminResource(KeycloakSession session, AdminAuth auth, TokenManager tokenManager) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        this.auth = auth;
        this.tokenManager = tokenManager;
    }

    public static final CacheControl noCache = new CacheControl();

    static {
        noCache.setNoCache(true);
    }

    /**
     * Get accessible realms
     *
     * Returns a list of accessible realms. The list is filtered based on what realms the caller is allowed to view.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Get accessible realms Returns a list of accessible realms. The list is filtered based on what realms the caller is allowed to view.")
    public Stream<RealmRepresentation> getRealms(@DefaultValue("false") @QueryParam("briefRepresentation") boolean briefRepresentation) {
        Stream<RealmRepresentation> realms = session.realms().getRealmsStream()
                .map(realm -> toRealmRep(realm, briefRepresentation))
                .filter(Objects::nonNull);
        return throwIfEmpty(realms, new ForbiddenException());
    }

    protected RealmRepresentation toRealmRep(RealmModel realm, boolean briefRep) {
        if (AdminPermissions.realms(session, auth).canView(realm)) {
            return briefRep ? ModelToRepresentation.toBriefRepresentation(realm) : ModelToRepresentation.toRepresentation(session, realm, false);
        } else if (AdminPermissions.realms(session, auth).isAdmin(realm)) {
            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm(realm.getName());
            return rep;
        }
        return null;
    }

    /**
     * Import a realm.
     * <p>
     * Imports a realm from a full representation of that realm.  Realm name must be unique.
     *
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Import a realm. Imports a realm from a full representation of that realm.", description = "Realm name must be unique.")
    @APIResponse(responseCode = "201", description = "Created")
    public Response importRealm(InputStream requestBody) {
        AdminPermissions.realms(session, auth).requireCreateRealm();

        ExportImportManager exportImportManager = session.getProvider(DatastoreProvider.class).getExportImportManager();

        try {
            RealmModel realm = exportImportManager.importRealm(requestBody);

            grantPermissionsToRealmCreator(realm);

            URI location = AdminRoot.realmsUrl(session.getContext().getUri()).path(realm.getName()).build();
            logger.debugv("imported realm success, sending back: {0}", location.toString());

            // The create event is associated with the realm of the user executing the operation,
            // instead of the realm being created.
            AdminEventBuilder adminEvent = new AdminEventBuilder(auth.getRealm(), auth, session, clientConnection);
            adminEvent.resource(ResourceType.REALM).realm(auth.getRealm()).operation(OperationType.CREATE)
                    .resourcePath(realm.getName())
                    .representation(ModelToRepresentation.toRepresentation(session, realm, false))
                    .success();

            return Response.created(location).build();
        } catch (ModelDuplicateException e) {
            logger.error("Conflict detected", e);
            if (session.getTransactionManager().isActive()) session.getTransactionManager().setRollbackOnly();
            throw ErrorResponse.exists("Conflict detected. See logs for details");
        } catch (PasswordPolicyNotMetException e) {
            logger.error("Password policy not met for user " + e.getUsername(), e);
            if (session.getTransactionManager().isActive()) session.getTransactionManager().setRollbackOnly();
            throw ErrorResponse.error("Password policy not met. See logs for details", Response.Status.BAD_REQUEST);
        } catch (ModelIllegalStateException e) {
            logger.error(e.getMessage(), e);
            throw ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        } catch (ModelException e) {
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    private void grantPermissionsToRealmCreator(RealmModel realm) {
        if (auth.hasRealmRole(AdminRoles.ADMIN)) {
            return;
        }

        ClientModel realmAdminApp = realm.getMasterAdminClient();
        Arrays.stream(AdminRoles.ALL_REALM_ROLES)
                .map(realmAdminApp::getRole)
                .forEach(auth.getUser()::grantRole);
    }

    /**
     * Base path for the admin REST API for one particular realm.
     *
     * @param headers
     * @param name realm name (not id!)
     * @return
     */
    @Path("{realm}")
    public RealmAdminResource getRealmAdmin(@PathParam("realm") @Parameter(description = "realm name (not id!)") final String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) throw new NotFoundException("Realm not found.");

        if (!RealmManager.isAdministrationRealm(auth.getRealm())
                && !auth.getRealm().equals(realm)) {
            throw new ForbiddenException();
        }
        AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, auth);

        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, auth, session, clientConnection);
        session.getContext().setRealm(realm);

        return new RealmAdminResource(session, realmAuth, adminEvent);
    }

}
