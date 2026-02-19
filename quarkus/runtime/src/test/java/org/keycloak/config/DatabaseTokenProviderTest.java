package org.keycloak.config;

import java.time.Instant;

import org.keycloak.quarkus.runtime.configuration.ManagedIdentity.DatabaseTokenProvider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseTokenProviderTest {

  @Test
  public void should_return_token() {
    DatabaseTokenProvider mockProvider = mock(DatabaseTokenProvider.class);

    DatabaseTokenProvider.TokenResponse fakeResponse =
      new DatabaseTokenProvider.TokenResponse(
        "fake-token",
        Instant.now().plusSeconds(3600)
      );

    when(mockProvider.getToken("postgres"))
      .thenReturn(fakeResponse);

    DatabaseTokenProvider.TokenResponse result =
      mockProvider.getToken("postgres");

    assertEquals("fake-token", result.token());
  }

}
