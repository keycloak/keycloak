/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.resources;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.VerificationException;

/**
 *
 * @author hmlnarik
 */
public class LoginActionsServiceException extends VerificationException {

    private final Response response;

    public LoginActionsServiceException(Response response) {
        this.response = response;
    }

    public LoginActionsServiceException(Response response, String message) {
        super(message);
        this.response = response;
    }

    public LoginActionsServiceException(Response response, String message, Throwable cause) {
        super(message, cause);
        this.response = response;
    }

    public LoginActionsServiceException(Response response, Throwable cause) {
        super(cause);
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

}
