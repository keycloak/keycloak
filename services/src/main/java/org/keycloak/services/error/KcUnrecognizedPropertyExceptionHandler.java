/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.services.error;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Override explicitly added ExceptionMapper for handling <code>UnrecognizedPropertyException</code> in RestEasy Jackson
 *
 * <code>org.jboss.resteasy.plugins.providers.jackson.UnrecognizedPropertyExceptionHandler</code>
 */
public class KcUnrecognizedPropertyExceptionHandler implements ExceptionMapper<UnrecognizedPropertyException> {

    @Context
    private HttpHeaders headers;

    /**
     * Return escaped original message
     */
    @Override
    public Response toResponse(UnrecognizedPropertyException exception) {
        return KeycloakErrorHandler.getResponse(headers, new BadRequestException(exception.getMessage()));
    }
}