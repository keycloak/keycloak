package org.keycloak.quarkus.runtime.configuration.ManagedIdentity;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class DatabaseTokenManager {
  private final DatabaseTokenProvider provider;
  private final AtomicReference<CachedToken> cache = new AtomicReference<>();

  public DatabaseTokenManager(DatabaseTokenProvider provider) {
    this.provider = provider;
  }

  public synchronized String getToken(String vendor) {

    CachedToken current = cache.get();

    if (current != null && !current.isExpiringSoon()) {
      return current.token();
    }

    DatabaseTokenProvider.TokenResponse response =
      provider.getToken(vendor);

    CachedToken updated = new CachedToken(
      response.token(),
      response.expiresAt()
    );

    cache.set(updated);

    return updated.token();
  }

  private record CachedToken(String token, Instant expiresAt) {
    //moment to refresh existing password sent to quarkus
    boolean isExpiringSoon() {
      return Instant.now().isAfter(expiresAt.minusSeconds(300));
    }
  }
}
