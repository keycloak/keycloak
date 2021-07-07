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

import com.fasterxml.jackson.core.type.TypeReference;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.util.JsonSerialization;

public class RealmLocalizationResource {
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;

    @Context
    protected KeycloakSession session;

    public RealmLocalizationResource(RealmModel realm, AdminPermissionEvaluator auth) {
        this.realm = realm;
        this.auth = auth;
    }

    @Path("{locale}/{key}")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
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
    public void patchRealmLocalizationTextsFromFile(@PathParam("locale") String locale, MultipartFormDataInput input)
            throws IOException {
        this.auth.realm().requireManageRealm();

        Map<String, List<InputPart>> formDataMap = input.getFormDataMap();
        if (!formDataMap.containsKey("file")) {
            throw new BadRequestException();
        }
        InputPart file = formDataMap.get("file").get(0);
        try (InputStream inputStream = file.getBody(InputStream.class, null)) {
            TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {
            };
            Map<String, String> rep = JsonSerialization.readValue(inputStream, typeRef);
            realm.patchRealmLocalizationTexts(locale, rep);
        } catch (IOException e) {
            throw new BadRequestException("Could not read file.");
        }
    }

    @PATCH
    @Path("{locale}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void patchRealmLocalizationTexts(@PathParam("locale") String locale, Map<String, String> loclizationTexts) {
        this.auth.realm().requireManageRealm();
        realm.patchRealmLocalizationTexts(locale, loclizationTexts);
    }

    @Path("{locale}")
    @DELETE
    public void deleteRealmLocalizationTexts(@PathParam("locale") String locale) {
        this.auth.realm().requireManageRealm();
        if(!realm.removeRealmLocalizationTexts(locale)) {
            throw new NotFoundException("No localization texts for locale " + locale + " found.");
        }
    }

    @Path("{locale}/{key}")
    @DELETE
    public void deleteRealmLocalizationText(@PathParam("locale") String locale, @PathParam("key") String key) {
        this.auth.realm().requireManageRealm();
        if (!session.realms().deleteLocalizationText(realm, locale, key)) {
            throw new NotFoundException("Localization text not found");
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<String> getRealmLocalizationLocales() {
        if (!AdminPermissions.realms(session, auth.adminAuth()).isAdmin()) {
            throw new ForbiddenException();
        }

        return realm.getRealmLocalizationTexts().keySet().stream().sorted();
    }

    @Path("{locale}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getRealmLocalizationTexts(@PathParam("locale") String locale) {
        if (!AdminPermissions.realms(session, auth.adminAuth()).isAdmin()) {
            throw new ForbiddenException();
        }

        return realm.getRealmLocalizationTextsByLocale(locale);
    }

    @Path("{locale}/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getRealmLocalizationText(@PathParam("locale") String locale, @PathParam("key") String key) {
        if (!AdminPermissions.realms(session, auth.adminAuth()).isAdmin()) {
            throw new ForbiddenException();
        }

        String text = session.realms().getLocalizationTextsById(realm, locale, key);
        if (text != null) {
            return text;
        } else {
            throw new NotFoundException("Localization text not found");
        }
    }
}
