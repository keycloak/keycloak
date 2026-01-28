/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.services.resources.admin;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.client.clienttype.ClientTypeException;
import org.keycloak.client.clienttype.ClientTypeManager;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientTypesRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ClientTypesResource {
    protected static final Logger logger = Logger.getLogger(ClientTypesResource.class);

    protected final ClientTypeManager manager;
    protected final RealmModel realm;

    private final AdminPermissionEvaluator auth;

    public ClientTypesResource(ClientTypeManager manager, RealmModel realm, AdminPermissionEvaluator auth) {
        this.manager = manager;
        this.auth = auth;
        this.realm = realm;
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "List all client types available in the current realm",
            description = "This endpoint returns a list of both global and realm level client types and the attributes they set"
    )
    public ClientTypesRepresentation getClientTypes() {
        auth.realm().requireViewRealm();

        try {
            return manager.getClientTypes(realm);
        } catch (ClientTypeException e) {
            logger.error(e.getMessage(), e);
            throw new BadRequestException(ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST));
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Update a client type",
            description = "This endpoint allows you to update a realm level client type"
    )
    @APIResponse(responseCode = "204", description = "No Content")
    public Response updateClientTypes(final ClientTypesRepresentation clientTypes) {
        auth.realm().requireManageRealm();

        try {
            manager.updateClientTypes(realm, clientTypes);
        } catch (ClientTypeException e) {
            logger.error(e.getMessage(), e);
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }
        return Response.noContent().build();
    }
}