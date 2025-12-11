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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.http.FormPartValue;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class RealmLocalizationResource {
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;

    protected final KeycloakSession session;

    public RealmLocalizationResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.auth = auth;
    }

    @Path("{locale}/{key}")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public void saveRealmLocalizationText(@PathParam("locale") String locale, @PathParam("key") String key,
            String text) {
        this.auth.realm().requireManageRealm();
        try {
            session.realms().saveLocalizationText(realm, locale, key, text);
        } catch (ModelDuplicateException e) {
            throw new BadRequestException(
                    String.format("Localization text %s for the locale %s and realm %s already exists.",
                            key, locale, realm.getId()));
        }
    }


    /**
     * Import localization from uploaded JSON file
     */
    @POST
    @Path("{locale}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Import localization from uploaded JSON file")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public void createOrUpdateRealmLocalizationTextsFromFile(@PathParam("locale") String locale) {
        this.auth.realm().requireManageRealm();

        MultivaluedMap<String, FormPartValue> formDataMap = session.getContext().getHttpRequest().getMultiPartFormParameters();
        if (!formDataMap.containsKey("file")) {
            throw new BadRequestException();
        }
        try (InputStream inputStream = formDataMap.getFirst("file").asInputStream()) {
            TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {
            };
            Map<String, String> rep = JsonSerialization.readValue(inputStream, typeRef);
            realm.createOrUpdateRealmLocalizationTexts(locale, rep);
        } catch (IOException e) {
            throw new BadRequestException("Could not read file.");
        }
    }

    @POST
    @Path("{locale}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public void createOrUpdateRealmLocalizationTexts(@PathParam("locale") String locale,
            Map<String, String> localizationTexts) {
        this.auth.realm().requireManageRealm();
        realm.createOrUpdateRealmLocalizationTexts(locale, localizationTexts);
    }

    @Path("{locale}")
    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void deleteRealmLocalizationTexts(@PathParam("locale") String locale) {
        this.auth.realm().requireManageRealm();
        if(!realm.removeRealmLocalizationTexts(locale)) {
            throw new NotFoundException("No localization texts for locale " + locale + " found.");
        }
    }

    @Path("{locale}/{key}")
    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void deleteRealmLocalizationText(@PathParam("locale") String locale, @PathParam("key") String key) {
        this.auth.realm().requireManageRealm();
        if (!session.realms().deleteLocalizationText(realm, locale, key)) {
            throw new NotFoundException("Localization text not found");
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<String> getRealmLocalizationLocales() {
        auth.requireAnyAdminRole();

        return realm.getRealmLocalizationTexts().keySet().stream().sorted();
    }

    @Path("{locale}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Map<String, String> getRealmLocalizationTexts(@PathParam("locale") String locale,
            @Deprecated @QueryParam("useRealmDefaultLocaleFallback") Boolean useFallback) {
        if (!AdminPermissions.realms(session, auth.adminAuth()).isAdmin()) {
            throw new ForbiddenException();
        }

        // this fallback is no longer needed since the fix for #15845, don't forget to remove it from the API
        if (useFallback != null && useFallback) {
            Map<String, String> realmLocalizationTexts = new HashMap<>();
            if (StringUtil.isNotBlank(realm.getDefaultLocale())) {
                realmLocalizationTexts.putAll(realm.getRealmLocalizationTextsByLocale(realm.getDefaultLocale()));
            }

            realmLocalizationTexts.putAll(realm.getRealmLocalizationTextsByLocale(locale));

            return realmLocalizationTexts;
        }

        return realm.getRealmLocalizationTextsByLocale(locale);
    }

    @Path("{locale}/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public String getRealmLocalizationText(@PathParam("locale") String locale, @PathParam("key") String key) {
        auth.requireAnyAdminRole();

        String text = session.realms().getLocalizationTextsById(realm, locale, key);
        if (text != null) {
            return text;
        } else {
            throw new NotFoundException("Localization text not found");
        }
    }
}
