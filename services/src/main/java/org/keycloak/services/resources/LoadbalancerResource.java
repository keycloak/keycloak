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

import io.smallrye.common.annotation.NonBlocking;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.health.LoadbalancerCheckCommand;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.MediaType;

/**
 * Prepare information for the loadbalancer (possibly in a multi-site setup) whether this Keycloak cluster should receive traffic.
 *
 * This is non-blocking, so that the loadbalancer can still retrieve the status even if the Keycloak instance is
 * trying to withstand a high load.
 *
 * @author <a href="mailto:aschwart@redhat.com">Alexander Schwartz</a>
 */
@Path("/lb-check")
@NonBlocking
public class LoadbalancerResource {

    protected static final Logger logger = Logger.getLogger(LoadbalancerResource.class);

    @Context
    KeycloakSession session;

    @GET
    @Produces(MediaType.TEXT_PLAIN_UTF_8)
    public Response getStatusForLoadbalancer() {
        LoadbalancerCheckCommand event = new LoadbalancerCheckCommand();
        session.getKeycloakSessionFactory().publish(event);
        if (event.isDown()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("DOWN").build();
        } else {
            return Response.ok().entity("UP").build();
        }
    }

}
