package org.keycloak.quarkus.runtime.configuration.ManagedIdentity;

import java.time.Instant;

public interface DatabaseTokenProvider  {
   TokenResponse  getToken(String vendor);
   record TokenResponse (String token, Instant expiresAt) {}
}
