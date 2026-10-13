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

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.services.resources.admin.AdminRoot;

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

    /**
     * Resolves the exception's message key using the admin theme messages and returns an error response
     * with the localized message.
     */
    public static ErrorResponseException error(KeycloakSession session, String locale, ModelException me, Response.Status status) {
        return error(resolveMessage(session, locale, me.getMessage(), me.getParameters()), status);
    }

    /**
     * Resolves a message key from the admin theme messages, formatting it with the supplied parameters.
     * If no translation is found, the key itself is returned as-is.
     */
    public static String resolveMessage(KeycloakSession session, String locale, String messageKey, Object[] params) {
        if (messageKey == null) {
            return null;
        }
        Properties messages = AdminRoot.getMessages(session, session.getContext().getRealm(), locale);
        String message = messages.getProperty(messageKey);
        if (message == null) {
            if (params == null || params.length == 0) {
                return messageKey;
            }
            message = messageKey;
        }
        try {
            Locale resolvedLocale = locale != null ? Locale.forLanguageTag(locale) : Locale.ENGLISH;
            return new MessageFormat(message, resolvedLocale).format(params);
        } catch (IllegalArgumentException e) {
            return message;
        }
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
