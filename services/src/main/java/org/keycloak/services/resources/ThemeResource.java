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

import org.jboss.logging.Logger;
import org.keycloak.common.Version;
import org.keycloak.common.util.MimeTypeUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.theme.Theme;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Theme resource
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/resources")
public class ThemeResource {

    protected static final Logger logger = Logger.getLogger(ThemeResource.class);

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
            Theme theme = session.theme().getTheme(themeName, Theme.Type.valueOf(themType.toUpperCase()));
            InputStream resource = theme.getResourceAsStream(path);
            if (resource != null) {
                return Response.ok(resource).type(MimeTypeUtil.getContentType(path)).cacheControl(CacheControlUtil.getDefaultCacheControl()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            ServicesLogger.LOGGER.failedToGetThemeRequest(e);
            return Response.serverError().build();
        }
    }

}
