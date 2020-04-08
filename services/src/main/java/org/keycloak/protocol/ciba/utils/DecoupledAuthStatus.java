package org.keycloak.protocol.ciba.utils;

public interface DecoupledAuthStatus {
    String SUCCEEDED = "succeeded";
    String UNAUTHORIZED = "unauthorized";
    String CANCELLED = "cancelled";
    String FAILED = "failed";
    String DIFFERENT = "different";
    String EXPIRED = "expired";
    String UNKNOWN = "unknown";
}
