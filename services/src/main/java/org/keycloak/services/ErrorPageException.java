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

import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ErrorPageException extends WebApplicationException {

    private final KeycloakSession session;
    private final Response.Status status;
    private final String errorMessage;
    private final Object[] parameters;
    private final AuthenticationSessionModel authSession;
    private final Response response;

    
    public ErrorPageException(KeycloakSession session, Response.Status status, String errorMessage, Object... parameters) {
        super(errorMessage, status);
        this.session = session;
        this.status = status;
        this.errorMessage = errorMessage;
        this.parameters = parameters;
        this.authSession = null;
        this.response = null;
    }
    
    public ErrorPageException(KeycloakSession session, AuthenticationSessionModel authSession, Response.Status status, String errorMessage, Object... parameters) {
        this.session = session;
        this.status = status;
        this.errorMessage = errorMessage;
        this.parameters = parameters;
        this.authSession = authSession;
        this.response = null;
    }

    public ErrorPageException(Response response) {
        this.session = null;
        this.status = null;
        this.errorMessage = null;
        this.parameters = null;
        this.authSession = null;
        this.response = response;
    }

    @Override
    public Response getResponse() {
        return response != null ? response : ErrorPage.error(session, authSession, status, errorMessage, parameters);
    }
}
