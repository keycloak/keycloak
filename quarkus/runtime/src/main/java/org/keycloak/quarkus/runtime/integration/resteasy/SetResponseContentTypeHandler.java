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

package org.keycloak.quarkus.runtime.integration.resteasy;

import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.util.ServerMediaType;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 * <p>A {@link ServerRestHandler} that set the media type produced by a JAX-RS resource method.
 *
 * <p>The main reason behind this handler is to make the response media type available to {@link org.keycloak.headers.DefaultSecurityHeadersProvider}.
 *
 * <p>When the method produces multiple media types, the type is negotiated against the Accept header
 * using the same logic RESTEasy Reactive applies to {@code @Produces}. If nothing matches, the first produced type is used.
 */
public class SetResponseContentTypeHandler implements ServerRestHandler {

    private final MediaType defaultMediaType;
    private final ServerMediaType negotiator;

    public SetResponseContentTypeHandler(String[] producesMediaTypes) {
        if (producesMediaTypes.length == 0) {
            this.defaultMediaType = MediaType.APPLICATION_JSON_TYPE;
            this.negotiator = null;
        } else {
            this.defaultMediaType = MediaType.valueOf(producesMediaTypes[0]);
            this.negotiator = producesMediaTypes.length > 1
                    ? new ServerMediaType(ServerMediaType.mediaTypesFromArray(producesMediaTypes), null, false)
                    : null;
        }
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        requestContext.setResponseContentType(select(requestContext));
    }

    private MediaType select(ResteasyReactiveRequestContext requestContext) {
        if (negotiator != null) {
            Map.Entry<MediaType, MediaType> negotiated = negotiator
                    .negotiateProduces(requestContext.getHttpHeaders().getHeaderString(HttpHeaders.ACCEPT));
            if (negotiated.getValue() != null) {
                return negotiated.getValue();
            }
        }
        return defaultMediaType;
    }
}
