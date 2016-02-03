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

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.representations.VersionRepresentation;
import org.keycloak.services.ServicesLogger;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/version")
public class ServerVersionResource {

    protected static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    @Context
    protected HttpRequest request;

    @Context
    protected HttpResponse response;

    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersionPreflight() {
        logger.debugv("cors request from: {0}", request.getHttpHeaders().getRequestHeaders().getFirst("Origin"));
        return Cors.add(request, Response.ok()).allowedMethods("GET").auth().preflight().build();
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public VersionRepresentation getVersion() {
        Cors.add(request).allowedOrigins("*").allowedMethods("GET").auth().build(response);
        return VersionRepresentation.SINGLETON;
    }
}
