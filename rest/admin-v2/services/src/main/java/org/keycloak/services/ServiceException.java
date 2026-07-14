package org.keycloak.services;

import java.util.Optional;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class ServiceException extends RuntimeException {
    private Response.Status suggestedHttpResponseStatus;
    private Object[] parameters;

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public ServiceException(String message, Response.Status suggestedStatus) {
        this(message);
        this.suggestedHttpResponseStatus = suggestedStatus;
    }

    public ServiceException(String message, Object[] parameters, Response.Status suggestedStatus) {
        this(message, suggestedStatus);
        this.parameters = parameters;
    }

    public ServiceException(Response.Status suggestedStatus) {
        super();
        this.suggestedHttpResponseStatus = suggestedStatus;
    }

    public Optional<Response.Status> getSuggestedResponseStatus() {
        return Optional.ofNullable(suggestedHttpResponseStatus);
    }

    public Optional<Object[]> getParameters() {
        return Optional.ofNullable(parameters);
    }

    public WebApplicationException toWebApplicationException() {
        return toWebApplicationException(Response.Status.BAD_REQUEST);
    }

    public WebApplicationException toWebApplicationException(Response.Status orReturnStatus) {
        if (getMessage() != null) {
            return new WebApplicationException(getMessage(), getSuggestedResponseStatus().orElse(orReturnStatus));
        } else {
            return new WebApplicationException(getSuggestedResponseStatus().orElse(orReturnStatus));
        }
    }

}
