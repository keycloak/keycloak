package org.keycloak.scim.services;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.filter.ScimFilterException;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.protocol.response.ErrorResponse;
import org.keycloak.theme.Theme;

class Error {

    static Response toResponse(KeycloakSession session, Exception e) {
        if (e instanceof ModelValidationException mve) {
            String language = session.getContext().getRequestHeaders().getHeaderString(HttpHeaders.ACCEPT_LANGUAGE);
            Properties messages = getMessageBundle(session, language);
            String format = messages.getProperty(mve.getMessage(), mve.getMessage())
                    .replace("{{", "{").replace("}}", "}")
                    .replace("'", "");
            String message = MessageFormat.format(format, mve.getParameters());
            session.getTransactionManager().setRollbackOnly();
            return invalidSyntax(message);
        } else if (e instanceof ModelDuplicateException) {
            return errorResponse(Status.CONFLICT, "uniqueness", "A resource with the same unique attribute already exists");
        } else if (e instanceof ScimFilterException) {
            return badRequest("invalidFilter", e.getMessage());
        } else if (e instanceof ForbiddenException) {
            return forbidden();
        } else if (e instanceof jakarta.ws.rs.ForbiddenException fe) {
            throw fe;
        }

        return errorResponse(Status.INTERNAL_SERVER_ERROR, "An unexpected error occurred when processing the request");
    }

    static Response resourceNotFound(String id) {
        return errorResponse(Status.NOT_FOUND, "Resource not found with id " + id);
    }

    static Response badRequest(String type, String detail) {
        return errorResponse(Status.BAD_REQUEST, type, detail);
    }

    static Response badRequest(String detail) {
        return badRequest(null, detail);
    }

    static Response invalidSyntax(String detail) {
        return badRequest("invalidSyntax", detail);
    }

    static Response forbidden() {
        return errorResponse(Status.FORBIDDEN, null);
    }

    private static Response errorResponse(Status status, String type, String detail) {
        ErrorResponse error = new ErrorResponse(detail, status.getStatusCode());
        error.setScimType(type);
        return Response.status(error.getStatusInt()).type(MediaType.APPLICATION_JSON).entity(error).build();
    }

    private static Response errorResponse(Status status, String detail) {
        return errorResponse(status, null, detail);
    }

    private static Properties getMessageBundle(KeycloakSession session, String lang) {
        try {
            Theme theme = session.theme().getTheme(Theme.Type.ADMIN);
            Locale locale = lang != null ? Locale.forLanguageTag(lang) : Locale.ENGLISH;
            return theme.getMessages(locale);
        } catch (IOException e) {
            return new Properties();
        }
    }
}
