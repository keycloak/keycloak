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

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.Version;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.utils.MediaType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Get keycloak.js file for javascript clients
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/js")
public class JsResource {

    @Context
    private HttpRequest request;

    /**
     * Get keycloak.js file for javascript clients
     *
     * @return
     */
    @GET
    @Path("/keycloak.js")
    @Produces(MediaType.TEXT_PLAIN_JAVASCRIPT)
    public Response getKeycloakJs(@QueryParam("version") String version) {
        return getJs("keycloak.js", version);
    }

    @GET
    @Path("/{version}/keycloak.js")
    @Produces(MediaType.TEXT_PLAIN_JAVASCRIPT)
    public Response getKeycloakJsWithVersion(@PathParam("version") String version) {
        return getJs("keycloak.js", version);
    }

    @GET
    @Path("/keycloak.min.js")
    @Produces(MediaType.TEXT_PLAIN_JAVASCRIPT)
    public Response getKeycloakMinJs(@QueryParam("version") String version) {
        return getJs("keycloak.min.js", version);
    }

    @GET
    @Path("/{version}/keycloak.min.js")
    @Produces(MediaType.TEXT_PLAIN_JAVASCRIPT)
    public Response getKeycloakMinJsWithVersion(@PathParam("version") String version) {
        return getJs("keycloak.min.js", version);
    }

    /**
     * Get keycloak-authz.js file for javascript clients
     *
     * @return
     */
    @GET
    @Path("/keycloak-authz.js")
    @Produces(MediaType.TEXT_PLAIN_JAVASCRIPT)
    public Response getKeycloakAuthzJs(@QueryParam("version") String version) {
        return getJs("keycloak-authz.js", version);
    }

    @GET
    @Path("/{version}/keycloak-authz.js")
    @Produces(MediaType.TEXT_PLAIN_JAVASCRIPT)
    public Response getKeycloakAuthzJsWithVersion(@PathParam("version") String version) {
        return getJs("keycloak-authz.js", version);
    }

    @GET
    @Path("/keycloak-authz.min.js")
    @Produces(MediaType.TEXT_PLAIN_JAVASCRIPT)
    public Response getKeycloakAuthzMinJs(@QueryParam("version") String version) {
        return getJs("keycloak-authz.min.js", version);
    }

    @GET
    @Path("/{version}/keycloak-authz.min.js")
    @Produces(MediaType.TEXT_PLAIN_JAVASCRIPT)
    public Response getKeycloakAuthzMinJsWithVersion(@PathParam("version") String version) {
        return getJs("keycloak-authz.min.js", version);
    }

    private Response getJs(String name, String version) {
        CacheControl cacheControl;
        if (version != null) {
            if (!version.equals(Version.RESOURCES_VERSION)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            cacheControl = CacheControlUtil.getDefaultCacheControl();
        } else {
            cacheControl = CacheControlUtil.noCache();
        }

        Cors cors = Cors.add(request).allowAllOrigins();

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(name);
        if (inputStream != null) {
            return cors.builder(Response.ok(inputStream).type("text/javascript").cacheControl(cacheControl)).build();
        } else {
            return cors.builder(Response.status(Response.Status.NOT_FOUND)).build();
        }
    }
}
