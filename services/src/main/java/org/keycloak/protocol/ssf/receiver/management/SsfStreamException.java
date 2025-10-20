package org.keycloak.protocol.ssf.receiver.management;

import jakarta.ws.rs.core.Response;
import org.keycloak.protocol.ssf.SsfException;

public class SsfStreamException extends SsfException {

    private final Response.Status status;

    public SsfStreamException(Response.Status statusCode) {
        this.status = statusCode;
    }

    public SsfStreamException(String message, Response.Status status) {
        super(message);
        this.status = status;
    }

    public SsfStreamException(String message, Throwable cause, Response.Status status) {
        super(message, cause);
        this.status = status;
    }

    public Response.Status getStatus() {
        return status;
    }
}
