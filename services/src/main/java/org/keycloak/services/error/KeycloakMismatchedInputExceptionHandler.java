/*
 * Copyright 2023
 *  Red Hat, Inc. and/or its affiliates
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

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.models.KeycloakSession;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;

/**
 * Override explicitly added ExceptionMapper for handling {@link MismatchedInputException} in RestEasy Jackson
 */
@Provider
public class KeycloakMismatchedInputExceptionHandler implements ExceptionMapper<MismatchedInputException> {

    @Context
    KeycloakSession session;

    /**
     * Return escaped original message
     */
    @Override
    public Response toResponse(MismatchedInputException exception) {
        return KeycloakErrorHandler.getResponse(session, exception);
    }
}