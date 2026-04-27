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

import java.util.List;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.ErrorRepresentation;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ErrorResponse {

    public static ErrorResponseException exists(String message) {
        return ErrorResponse.error(message, Response.Status.CONFLICT);
    }

    public static ErrorResponseException error(String message, Response.Status status) {
        return ErrorResponse.error(message, null, status);
    }
    
    public static ErrorResponseException error(String message, Object[] params, Response.Status status) {
        ErrorRepresentation error = new ErrorRepresentation();
        error.setErrorMessage(message);
        error.setParams(params);
        return new ErrorResponseException(Response.status(status).entity(error).type(MediaType.APPLICATION_JSON).build());
    }

    public static ErrorResponseException errors(List<ErrorRepresentation> s, Response.Status status) {
        return errors(s, status, true);
    }
    
    public static ErrorResponseException errors(List<ErrorRepresentation> s, Response.Status status, boolean shrinkSingleError) {
        if (shrinkSingleError && s.size() == 1) {
            return new ErrorResponseException(Response.status(status).entity(s.get(0)).type(MediaType.APPLICATION_JSON).build());
        }
        ErrorRepresentation error = new ErrorRepresentation();
        error.setErrors(s);
        if(!shrinkSingleError && s.size() == 1) {
            error.setErrorMessage(s.get(0).getErrorMessage());
            error.setParams(s.get(0).getParams());
            error.setField(s.get(0).getField());
        }
        return new ErrorResponseException(Response.status(status).entity(error).type(MediaType.APPLICATION_JSON).build());
    }
}
