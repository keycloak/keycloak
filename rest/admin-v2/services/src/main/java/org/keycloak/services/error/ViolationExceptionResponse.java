package org.keycloak.services.error;

import java.util.Set;

public record ViolationExceptionResponse(String error, Set<String> violations) {
}
