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

import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.health.LoadBalancerCheckProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.MediaType;

import io.smallrye.common.annotation.NonBlocking;
import org.jboss.logging.Logger;

/**
 * Prepare information for the load balancer (possibly in a multi-site setup) whether this Keycloak cluster should receive traffic.
 * <p>
 * This is non-blocking, so that the load balancer can still retrieve the status even if the Keycloak instance is
 * trying to withstand a high load. See {@link LoadBalancerCheckProvider#isDown()} for a longer explanation.
 *
 * @author <a href="mailto:aschwart@redhat.com">Alexander Schwartz</a>
 */
@Provider
@Path("/lb-check")
@NonBlocking
public class LoadBalancerResource {

    protected static final Logger logger = Logger.getLogger(LoadBalancerResource.class);

    @Context
    KeycloakSession session;

    /**
     * Return the status for a load balancer in a multi-site setup if this Keycloak site should receive traffic.
     * <p />
     * While a loadbalancer will usually check for the returned status code, the additional text <code>UP</code> or <code>DOWN</down>
     * is returned for humans to see the status in the browser.
     * <p />
     * In contrast to other management endpoints of Quarkus, no information is returned to the caller about the internal state of Keycloak
     * as this endpoint might be publicly available from the internet and should return as little information as possible.
     *
     * @return HTTP status 503 and DOWN when down, and HTTP status 200 and UP when up.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN_UTF_8)
    public Response getStatusForLoadBalancer() {
        Set<LoadBalancerCheckProvider> healthStatusProviders = session.getAllProviders(LoadBalancerCheckProvider.class);
        if (healthStatusProviders.stream().anyMatch(LoadBalancerCheckProvider::isDown)) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("DOWN").build();
        } else {
            return Response.ok().entity("UP").build();
        }
    }

}
