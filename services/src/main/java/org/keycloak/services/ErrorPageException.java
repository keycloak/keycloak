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
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;

import static org.keycloak.OAuthErrorException.INVALID_REQUEST;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ErrorPageException extends WebApplicationException {

    private String error;
    private String errorMessage;
    private Object[] parameters;

    public ErrorPageException(KeycloakSession session, Response.Status status, String errorMessage, Object... parameters) {
        this(session, INVALID_REQUEST, status, errorMessage, parameters);
    }

    public ErrorPageException(KeycloakSession session, String error, Response.Status status, String errorMessage, Object... parameters) {
        this(session, null, error, status, errorMessage, parameters);
    }

    public ErrorPageException(KeycloakSession session, AuthenticationSessionModel authSession, Response.Status status, String errorMessage, Object... parameters) {
        this(session, authSession, INVALID_REQUEST, status, errorMessage, parameters);
    }

    public ErrorPageException(KeycloakSession session, AuthenticationSessionModel authSession, String error, Response.Status status, String errorMessage, Object... parameters) {
        super(errorMessage, ErrorPage.error(session, authSession, status, errorMessage, parameters));
        this.error = error;
        this.errorMessage = errorMessage;
        this.parameters = parameters;
    }

    public ErrorPageException(Response response) {
        super((Throwable) null, response);
    }

    public Response.Status getStatus() {
        return Response.Status.fromStatusCode(getResponse().getStatus());
    }

    public String getError() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Object[] getParameters() {
        return parameters;
    }
}
