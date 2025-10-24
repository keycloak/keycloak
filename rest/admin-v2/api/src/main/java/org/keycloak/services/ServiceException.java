package org.keycloak.services;

import jakarta.ws.rs.core.Response;

import java.util.Optional;

public class ServiceException extends RuntimeException {
    private Response.Status suggestedHttpResponseStatus;

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

    public Optional<Response.Status> getSuggestedResponseStatus() {
        return Optional.ofNullable(suggestedHttpResponseStatus);
    }

}
