/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.integration.jaxrs;

import java.io.IOException;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

@Provider
@PreMatching
@Priority(1)
public class BadRequestFilter implements ContainerRequestFilter {

    private static final Logger logger = Logger.getLogger(BadRequestFilter.class);

    private static final String IPV4_ANY_LOCAL = "0.0.0.0";

    @Inject
    KeycloakSession session;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String remoteAddr = session.getContext().getConnection().getRemoteAddr();
        String host = session.getContext().getHttpRequest().getUri().getRequestUri().getHost();
        // we don't expect 0.0.0.0 to reach us for any request in any way that we'd us
        if (IPV4_ANY_LOCAL.equals(remoteAddr) || IPV4_ANY_LOCAL.equals(host)) {
            logger.debugf("Denying request because it's using %s", IPV4_ANY_LOCAL);
            throw new BadRequestException();
        }
    }
}
