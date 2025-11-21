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

package org.keycloak.quarkus.runtime.services;

import java.util.Objects;

import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.util.ObjectMapperResolver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.jboss.logging.Logger;

/**
 * This filter rejects all paths that need normalization as of RFC3986 or that have double slashes.
 * This prevents path traversal that would circumvent path filtering applied by a proxy if that proxy would not apply
 * normalization of the path. In addition to that, the reverse proxy might not be aware of the additional path
 * of the double slashes that Keycloak performs.
 */
public class RejectNonNormalizedPathFilter implements Handler<RoutingContext> {
    private static final Logger LOGGER = Logger.getLogger(RejectNonNormalizedPathFilter.class);
    private final ObjectMapper MAPPER = ObjectMapperResolver.createStreamSerializer();

    @Override
    public void handle(RoutingContext routingContext) {
        if (!Objects.equals(routingContext.request().path(), routingContext.normalizedPath())) {
            LOGGER.debugf("Request with a non-normalized path blocked: %s vs. %s", routingContext.request().path(), routingContext.normalizedPath());
            OAuth2ErrorRepresentation error = new OAuth2ErrorRepresentation("missingNormalization", "Request path not normalized");
            routingContext.response().headers().add("Content-Type", "application/json; charset=UTF-8");
            String jsonString;
            try {
                jsonString = MAPPER.writeValueAsString(error);
            } catch (JsonProcessingException e) {
                jsonString = "";
            }
            routingContext.response().setStatusCode(400).end(jsonString);
        } else {
            routingContext.next();
        }
    }

}
