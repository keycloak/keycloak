package org.keycloak.testsuite.dballocator.client.exceptions;


import javax.ws.rs.core.Response;

public class DBAllocatorUnavailableException extends DBAllocatorException {

    public DBAllocatorUnavailableException(Response errorResponse) {
        super(errorResponse);
    }
}
