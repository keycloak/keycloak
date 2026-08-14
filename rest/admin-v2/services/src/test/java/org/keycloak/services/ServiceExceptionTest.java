package org.keycloak.services;

import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.OAuth2ErrorRepresentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void parameterizedErrorPreservesOAuthFields() {
        OAuth2ErrorRepresentation original = new OAuth2ErrorRepresentation("invalid_request", "details");
        Object[] parameters = {"field"};

        Object enriched = ServiceExceptionMapper.addParameters(original, "message", parameters);
        parameters[0] = "changed";

        Map<?, ?> error = (Map<?, ?>) enriched;
        assertEquals("invalid_request", error.get("error"));
        assertEquals("details", error.get("error_description"));
        assertEquals("message", error.get("errorMessage"));
        assertArrayEquals(new Object[] {"field"}, (Object[]) error.get("params"));
    }
}
