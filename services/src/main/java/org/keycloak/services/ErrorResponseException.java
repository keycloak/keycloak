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

package org.keycloak.services;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.utils.KeycloakSessionUtil;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ErrorResponseException extends WebApplicationException {

    private final Response response;
    private final String error;
    private final String errorDescription;
    private final Response.Status status;

    public ErrorResponseException(String error, String errorDescription, Response.Status status) {
        super(error, status);
        this.response = null;
        this.error = error;
        this.errorDescription = errorDescription;
        this.status = status;
    }

    public ErrorResponseException(Response response) {
        this.response = response;
        this.error = null;
        this.errorDescription = null;
        this.status = null;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    @Override
    public Response getResponse() {
        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        if (session != null) {
            // This has to happen, since calling getResponse() with non-null result leads to
            // directly returning the result instead of
            // propagating exception to KeycloakErrorHandler.toResponse(Throwable) which would ensure rollback on other exception types.
            //
            // See org.jboss.resteasy.core.ExceptionHandler.unwrapException(HttpRequest, Throwable, RESTEasyTracingLogger)

            session.getTransactionManager().setRollbackOnly();
        }
        if (response != null) {
            return response;
        } else {
            OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(error, errorDescription);
            return Response.status(status).entity(errorRep).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

}
