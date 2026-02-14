/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.util.WellKnownProviderUtil;
import org.keycloak.wellknown.WellKnownProviderFactory;

import org.jboss.logging.Logger;

@Provider
@Path("/.well-known")
public class ServerMetadataResource {

    protected static final Logger logger = Logger.getLogger(ServerMetadataResource.class);

    @Context
    protected KeycloakSession session;

    @OPTIONS
    @Path("{provider}/realms/{realm}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWellKnownPreflight(final @PathParam("provider") String alias,
                                          final @PathParam("realm") String realm) {
        if (!isValidProvider(alias)) throw new NotFoundException();
        return Cors.builder().allowedMethods("GET").preflight().auth().add(Response.ok());
    }

    @GET
    @Path("{provider}/realms/{realm}")
    @Produces({MediaType.APPLICATION_JSON, org.keycloak.utils.MediaType.APPLICATION_JWT})
    public Response getWellKnown(final @PathParam("provider") String alias,
                                 final @PathParam("realm") String realm) {
        if (!isValidProvider(alias)) throw new NotFoundException();
        return RealmsResource.getWellKnownResponse(session, realm, alias, logger);
    }

    public static UriBuilder wellKnownOAuthProviderUrl(UriBuilder builder) {
        return builder.path(ServerMetadataResource.class).path("{provider}/realms/{realm}");
    }

    private boolean isValidProvider(String alias) {

        WellKnownProviderFactory factory = WellKnownProviderUtil.resolveFromAlias(session.getKeycloakSessionFactory(), alias)
                .orElse(null);
        if (factory == null) return false;
        return factory.isAvailableViaServerMetadata();
    }
}
