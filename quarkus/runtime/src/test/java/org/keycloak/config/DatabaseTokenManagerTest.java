package org.keycloak.config;

import java.time.Instant;

import org.keycloak.quarkus.runtime.configuration.ManagedIdentity.DatabaseTokenManager;
import org.keycloak.quarkus.runtime.configuration.ManagedIdentity.DatabaseTokenProvider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatabaseTokenManagerTest {
  @Test
  public void should_return_TokenResponse() {
    DatabaseTokenProvider mockProvider = mock(DatabaseTokenProvider.class);
    DatabaseTokenManager manager = new DatabaseTokenManager(mockProvider);
    Instant future = Instant.now().plusSeconds(3600);

    when(mockProvider.getToken("postgres"))
      .thenReturn(new DatabaseTokenProvider.TokenResponse(
        "fake-token",
        future
      ));
    String token = manager.getToken("postgres");

    assertEquals("fake-token", token);
  }

  @Test
  public void should_cache_tokens() {
    DatabaseTokenProvider mockProvider = mock(DatabaseTokenProvider.class);
    DatabaseTokenManager manager = new DatabaseTokenManager(mockProvider);
    Instant future = Instant.now().plusSeconds(3600);

    when(mockProvider.getToken("postgres"))
      .thenReturn(new DatabaseTokenProvider.TokenResponse(
        "fake-token",
        future
      ));
    manager.getToken("postgres");
    manager.getToken("postgres");
    verify(mockProvider, times(1)).getToken("postgres");
  }

  @Test
  public void should_refresh_token_that_will_Expire() {
    DatabaseTokenProvider mockProvider = mock(DatabaseTokenProvider.class);
    DatabaseTokenManager manager = new DatabaseTokenManager(mockProvider);

    when(mockProvider.getToken("postgres"))
      .thenReturn(new DatabaseTokenProvider.TokenResponse(
        "token1",
        Instant.now().plusSeconds(100)  // < 300 seconds
      ))
      .thenReturn(new DatabaseTokenProvider.TokenResponse(
        "token2",
        Instant.now().plusSeconds(3600)
      ));

    String first = manager.getToken("postgres");
    // manager should return the second token obtained from the provider the second time is called since first token will expire in 100 sec
    String second = manager.getToken("postgres");

    assertEquals("token1", first);
    assertEquals("token2", second);
  }
}
