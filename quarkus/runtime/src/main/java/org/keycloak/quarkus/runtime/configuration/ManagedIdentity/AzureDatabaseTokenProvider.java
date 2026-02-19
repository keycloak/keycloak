package org.keycloak.quarkus.runtime.configuration.ManagedIdentity;

import java.time.Instant;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;

public class AzureDatabaseTokenProvider implements DatabaseTokenProvider {
  private final DefaultAzureCredential credential;
  public AzureDatabaseTokenProvider() {
    this.credential = new DefaultAzureCredentialBuilder().build();
  }
  @Override
  public TokenResponse getToken(String vendor) {

    TokenRequestContext context = switch (vendor) {
      case "mssql" -> new TokenRequestContext()
        .addScopes("https://database.windows.net/.default");

      case "postgresql", "postgres", "mariadb", "mysql" -> new TokenRequestContext()
        .addScopes("https://ossrdbms-aad.database.windows.net/.default");

      default -> throw new IllegalArgumentException("Unsupported vendor type");
    };

    AccessToken accesstoken = credential.getToken(context).block();
    if (accesstoken == null) {
      throw new IllegalStateException("Failed to acquire Azure token");
    }
    String token = accesstoken.getToken();
    //max time for aws token are 60 min
    // 3600 sec -300 (found in token manager) is the time the token will refresh
    Instant expires = Instant.now().plusSeconds(3600);

    return new TokenResponse(token, expires);
  }
}
