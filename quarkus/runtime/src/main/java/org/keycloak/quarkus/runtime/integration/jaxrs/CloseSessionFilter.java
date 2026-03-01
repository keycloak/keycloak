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
import java.io.OutputStream;
import java.util.stream.Stream;

import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.utils.KeycloakSessionUtil;

@Provider
@PreMatching
@Priority(1)
public class CloseSessionFilter implements ContainerResponseFilter, org.keycloak.quarkus.runtime.transaction.TransactionalSessionHandler {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        Object entity = responseContext.getEntity();

        if (entity instanceof Stream) {
            Stream entityStream = (Stream) entity;
            entityStream.onClose(this::closeSession);
            return;
        }

        if (entity instanceof StreamingOutput) {
            responseContext.setEntity(new StreamingOutput() {
                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException {
                    try {
                        ((StreamingOutput) entity).write(output);
                    } finally {
                        closeSession();
                    }
                }
            });
            return;
        }

        closeSession();
    }

    private void closeSession() {
        close(KeycloakSessionUtil.getKeycloakSession());
    }
}
