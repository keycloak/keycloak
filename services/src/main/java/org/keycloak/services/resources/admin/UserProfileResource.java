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
 */
package org.keycloak.services.resources.admin;

import java.util.Collections;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.component.ComponentValidationException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.UserProfileMetadata;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import static org.keycloak.userprofile.UserProfileUtil.createUserProfileMetadata;

/**
 * @author Vlastimil Elias <velias@redhat.com>
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class UserProfileResource {

    protected final KeycloakSession session;
    protected final AdminEventBuilder adminEvent;
    protected final RealmModel realm;
    private final AdminPermissionEvaluator auth;

    public UserProfileResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.USER_PROFILE);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(description = "Get the configuration for the user profile")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UPConfig.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public UPConfig getConfiguration() {
        auth.requireAnyAdminRole();
        return session.getProvider(UserProfileProvider.class).getConfiguration();
    }

    @GET
    @Path("/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(description = "Get the UserProfileMetadata from the configuration")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UserProfileMetadata.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public UserProfileMetadata getMetadata() {
        auth.requireAnyAdminRole();
        UserProfile profile = session.getProvider(UserProfileProvider.class).create(UserProfileContext.USER_API, Collections.emptyMap());
        return createUserProfileMetadata(session, profile);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(description = "Set the configuration for the user profile")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UPConfig.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Response update(UPConfig config) {
        auth.realm().requireManageRealm();
        return Response.ok(setAndGetConfiguration(config)).type(MediaType.APPLICATION_JSON).build();
    }

    public UPConfig setAndGetConfiguration(UPConfig config) {
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);

        if (config != null && provider.getConfiguration().equals(config)) {
            return config;
        }

        try {
            provider.setConfiguration(config);
        } catch (ComponentValidationException e) {
            //show validation result containing details about error
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }

        adminEvent.operation(OperationType.UPDATE)
                .resourcePath(session.getContext().getUri())
                .representation(config)
                .success();

        return provider.getConfiguration();
    }
}
