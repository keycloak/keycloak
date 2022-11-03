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
import java.util.stream.Stream;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.Provider;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.transaction.TransactionalSessionHandler;

@Provider
@PreMatching
@Priority(1)
public class TransactionalResponseFilter implements ContainerResponseFilter, TransactionalSessionHandler {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        Object entity = responseContext.getEntity();

        if (shouldDelaySessionClose(entity)) {
            return;
        }

        close(Resteasy.getContextData(KeycloakSession.class));
    }

    private static boolean shouldDelaySessionClose(Object entity) {
        // do not close the session if the response entity is a stream
        // that is because we need the session open until the stream is transformed as it might require access to the database
        return entity instanceof Stream || entity instanceof StreamingOutput;
    }
}
