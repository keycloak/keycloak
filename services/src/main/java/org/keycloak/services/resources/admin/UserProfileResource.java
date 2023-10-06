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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.representations.idm.UserProfileAttributeGroupMetadata;
import org.keycloak.representations.idm.UserProfileAttributeMetadata;
import org.keycloak.representations.idm.UserProfileMetadata;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.AttributeValidatorMetadata;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.config.UPConfig;
import org.keycloak.userprofile.config.UPGroup;
import org.keycloak.util.JsonSerialization;
import org.keycloak.validate.Validators;

/**
 * @author Vlastimil Elias <velias@redhat.com>
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class UserProfileResource {

    protected final KeycloakSession session;

    protected final RealmModel realm;
    private final AdminPermissionEvaluator auth;

    public UserProfileResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.auth = auth;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation()
    public String getConfiguration() {
        auth.requireAnyAdminRole();
        return session.getProvider(UserProfileProvider.class).getConfiguration();
    }

    @GET
    @Path("/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation()
    public UserProfileMetadata getMetadata() {
        auth.requireAnyAdminRole();
        UserProfile profile = session.getProvider(UserProfileProvider.class).create(UserProfileContext.USER_API, Collections.emptyMap());
        return createUserProfileMetadata(session, profile);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation()
    public Response update(String text) {
        auth.realm().requireManageRealm();
        UserProfileProvider t = session.getProvider(UserProfileProvider.class);

        try {
            t.setConfiguration(text);
        } catch (ComponentValidationException e) {
            //show validation result containing details about error
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }

        return Response.ok(t.getConfiguration()).type(MediaType.APPLICATION_JSON).build();
    }

    public static UserProfileMetadata createUserProfileMetadata(KeycloakSession session, UserProfile profile) {
        Map<String, List<String>> am = profile.getAttributes().getReadable();

        if(am == null)
            return null;

        List<UserProfileAttributeMetadata> attributes = am.keySet().stream()
                .map(name -> profile.getAttributes().getMetadata(name))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(AttributeMetadata::getGuiOrder))
                .map(sam -> toRestMetadata(sam, session, profile))
                .collect(Collectors.toList());

        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        UPConfig config;

        try {
            config = JsonSerialization.readValue(provider.getConfiguration(), UPConfig.class);
        } catch (Exception cause) {
            throw new RuntimeException("Failed to parse configuration", cause);
        }

        List<UserProfileAttributeGroupMetadata> groups = config.getGroups().stream().map(new Function<UPGroup, UserProfileAttributeGroupMetadata>() {
            @Override
            public UserProfileAttributeGroupMetadata apply(UPGroup upGroup) {
                return new UserProfileAttributeGroupMetadata(upGroup.getName(), upGroup.getDisplayHeader(), upGroup.getDisplayDescription(), upGroup.getAnnotations());
            }
        }).collect(Collectors.toList());

        return new UserProfileMetadata(attributes, groups);
    }

    private static UserProfileAttributeMetadata toRestMetadata(AttributeMetadata am, KeycloakSession session, UserProfile profile) {
        String group = null;

        if (am.getAttributeGroupMetadata() != null) {
            group = am.getAttributeGroupMetadata().getName();
        }

        return new UserProfileAttributeMetadata(am.getName(),
                am.getAttributeDisplayName(),
                profile.getAttributes().isRequired(am.getName()),
                profile.getAttributes().isReadOnly(am.getName()),
                group,
                am.getAnnotations(),
                toValidatorMetadata(am, session));
    }

    private static Map<String, Map<String, Object>> toValidatorMetadata(AttributeMetadata am, KeycloakSession session){
        // we return only validators which are instance of ConfiguredProvider. Others are expected as internal.
        return am.getValidators() == null ? null : am.getValidators().stream()
                .filter(avm -> (Validators.validator(session, avm.getValidatorId()) instanceof ConfiguredProvider))
                .collect(Collectors.toMap(AttributeValidatorMetadata::getValidatorId, AttributeValidatorMetadata::getValidatorConfig));
    }
}
