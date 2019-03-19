package org.keycloak.testsuite.dballocator.client.exceptions;

import javax.ws.rs.core.Response;

public class DBAllocatorException extends Exception {

    private Response errorResponse;

    public DBAllocatorException(Response errorResponse) {
        this.errorResponse = errorResponse;
    }

    public DBAllocatorException(Response errorResponse, Throwable throwable) {
        super(throwable);
        this.errorResponse = errorResponse;
    }

    public DBAllocatorException(Throwable throwable) {
        super(throwable);
    }

    public Response getErrorResponse() {
        return errorResponse;
    }
}
