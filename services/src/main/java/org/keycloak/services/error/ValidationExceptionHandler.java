package org.keycloak.services.error;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Set;
import java.util.stream.Collectors;

@Provider
public class ValidationExceptionHandler implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        return Response.status(400)
                .entity(new ViolationExceptionResponse("Provided data is invalid",
                        exception.getConstraintViolations()
                                .stream()
                                .map(f -> "%s: %s".formatted(f.getPropertyPath(), f.getMessage()))
                                .collect(Collectors.toSet())))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    public record ViolationExceptionResponse(String error, Set<String> violations) {
    }
}
