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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.transaction.TransactionalSessionHandler;

import jakarta.annotation.Priority;

@Provider
@ConstrainedTo(RuntimeType.SERVER)
@Priority(10000)
public class TransactionalResponseInterceptor implements WriterInterceptor, TransactionalSessionHandler {

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) {
        KeycloakSession session = Resteasy.getContextData(KeycloakSession.class);
        OutputStream outputStream = context.getOutputStream();

        // see https://github.com/keycloak/keycloak/issues/20451
        try {
            context.setOutputStream(new BufferedOutputStream(outputStream, 8192));
            context.proceed();
        } catch (Exception cause) {
            throw new RuntimeException(cause);
        } finally {
            // make sure response is closed after writing to the response output stream
            // this is needed in order to support streams from endpoints as they need access to underlying resources like database
            close(session);
        }
    }
}
