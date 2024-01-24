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
package org.keycloak.services.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.common.Version;
import org.keycloak.common.util.MimeTypeUtil;
import org.keycloak.encoding.ResourceEncodingHelper;
import org.keycloak.encoding.ResourceEncodingProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.LocaleUtil;
import org.keycloak.theme.Theme;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Theme resource
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/resources")
public class ThemeResource {

    @Context
    private KeycloakSession session;

    /**
     * Get theme content
     *
     * @param themType
     * @param themeName
     * @param path
     * @return
     */
    @GET
    @Path("/{version}/{themeType}/{themeName}/{path:.*}")
    public Response getResource(@PathParam("version") String version, @PathParam("themeType") String themType, @PathParam("themeName") String themeName, @PathParam("path") String path) {
        if (!version.equals(Version.RESOURCES_VERSION)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            String contentType = MimeTypeUtil.getContentType(path);
            Theme theme = session.theme().getTheme(themeName, Theme.Type.valueOf(themType.toUpperCase()));
            ResourceEncodingProvider encodingProvider = session.theme().isCacheEnabled() ? ResourceEncodingHelper.getResourceEncodingProvider(session, contentType) : null;

            InputStream resource;
            if (encodingProvider != null) {
                resource = encodingProvider.getEncodedStream(() -> theme.getResourceAsStream(path), themType, themeName, path.replace('/', File.separatorChar));
            } else {
                resource = theme.getResourceAsStream(path);
            }

            if (resource != null) {
                Response.ResponseBuilder rb = Response.ok(resource).type(contentType).cacheControl(CacheControlUtil.getDefaultCacheControl());
                if (encodingProvider != null) {
                    rb.encoding(encodingProvider.getEncoding());
                }
                return rb.build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            ServicesLogger.LOGGER.failedToGetThemeRequest(e);
            return Response.serverError().build();
        }
    }

    @Path("/{realm}/{themeType}/{locale}")
    @OPTIONS
    public Response localizationTextPreflight() {
        return Cors.add(session.getContext().getHttpRequest(), Response.ok()).auth().preflight().build();
    }

    @GET
    @Path("/{realm}/{themeType}/{locale}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLocalizationTexts(@PathParam("realm") String realmName, @QueryParam("theme") String theme,
                                         @PathParam("locale") String localeString, @PathParam("themeType") String themeType,
                                         @QueryParam("source") boolean showSource) throws IOException {
        final RealmModel realm = session.realms().getRealmByName(realmName);
        if (realm == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        session.getContext().setRealm(realm);
        List<KeySource> result;

        Theme theTheme;
        final Theme.Type type = Theme.Type.valueOf(themeType.toUpperCase());
        if (theme == null) {
            theTheme = session.theme().getTheme(type);
        } else {
            theTheme = session.theme().getTheme(theme, type);
        }

        final Locale locale = Locale.forLanguageTag(localeString);
        if (showSource) {
            Properties messagesByLocale = theTheme.getMessages("messages", locale);
            Set<KeySource> resultSet = messagesByLocale.entrySet().stream().map(e ->
                    new KeySource((String) e.getKey(), (String) e.getValue(), Source.THEME)).collect(toSet());

            Map<Locale, Properties> realmLocalizationMessages = LocaleUtil.getRealmLocalizationTexts(realm, locale);
            for (Locale currentLocale = locale; currentLocale != null; currentLocale = LocaleUtil.getParentLocale(currentLocale)) {
                final List<KeySource> realmOverride = realmLocalizationMessages.get(currentLocale).entrySet().stream().map(e ->
                        new KeySource((String) e.getKey(), (String) e.getValue(), Source.REALM)).collect(toList());
                resultSet.addAll(realmOverride);
            }
            result = new ArrayList<>(resultSet);
        } else {
            result = theTheme.getEnhancedMessages(realm, locale).entrySet().stream().map(e ->
                    new KeySource((String) e.getKey(), (String) e.getValue())).collect(toList());
        }

        Response.ResponseBuilder responseBuilder = Response.ok(result);
        return Cors.add(session.getContext().getHttpRequest(), responseBuilder).allowedOrigins("*").auth().build();
    }
}

enum Source {
    THEME,
    REALM
}

class KeySource {
    private String key;
    private String value;
    private Source source;

    public KeySource(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public KeySource(String key, String value, Source source) {
        this(key, value);
        this.source = source;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Source getSource() {
        return source;
    }
}