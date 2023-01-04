package org.keycloak.testsuite.dballocator.client;

import org.keycloak.testsuite.dballocator.client.exceptions.DBAllocatorUnavailableException;

import jakarta.ws.rs.core.Response;
import java.util.concurrent.Callable;

@FunctionalInterface
public interface BackoffRetryPolicy {
    Response retryTillHttpOk(Callable<Response> callableSupplier) throws DBAllocatorUnavailableException;
}
