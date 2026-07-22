package org.keycloak.services;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ServiceExceptionTest {

    @Test
    void parametersAreDefensivelyCopied() {
        Object[] parameters = {"original"};
        ServiceException exception = new ServiceException("message", parameters, Response.Status.BAD_REQUEST);

        parameters[0] = "changed";
        assertArrayEquals(new Object[] {"original"}, exception.getParameters().orElseThrow());

        Object[] returnedParameters = exception.getParameters().orElseThrow();
        returnedParameters[0] = "changed again";
        assertArrayEquals(new Object[] {"original"}, exception.getParameters().orElseThrow());
    }
}
