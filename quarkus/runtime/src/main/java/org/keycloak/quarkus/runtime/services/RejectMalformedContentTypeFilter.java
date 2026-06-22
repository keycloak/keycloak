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

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.util.ObjectMapperResolver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RejectMalformedContentTypeFilter implements Handler<RoutingContext> {
    private static final Logger LOGGER = Logger.getLogger(RejectMalformedContentTypeFilter.class);
    private static final String ERROR_DESCRIPTION = "Invalid Content-Type header";
    private final ObjectMapper MAPPER = ObjectMapperResolver.createStreamSerializer();

    @Override
    public void handle(RoutingContext routingContext) {
        String contentType = routingContext.request().getHeader("Content-Type");

        if (contentType != null && !isParseable(contentType)) {
            LOGGER.debugf("Rejecting request with malformed Content-Type header: %s", contentType);

            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("Cache-Control", "no-store")
                    .putHeader("Pragma", "no-cache")
                    .putHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .end(errorResponse());
            return;
        }

        routingContext.next();
    }

    private String errorResponse() {
        OAuth2ErrorRepresentation error = new OAuth2ErrorRepresentation("invalid_request", ERROR_DESCRIPTION);
        try {
            return MAPPER.writeValueAsString(error);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    private static boolean isParseable(String contentType) {
        try {
            MediaType.valueOf(contentType);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
