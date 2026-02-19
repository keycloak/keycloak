package org.keycloak.quarkus.runtime.configuration.ManagedIdentity;

import java.time.Instant;

import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.IdentityOptions;
import org.keycloak.config.Option;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsUtilities;

public class AwsDatabaseTokenProvider implements DatabaseTokenProvider {

  @Override
  public TokenResponse getToken(String vendor) {

    String region = requireConfig(IdentityOptions.AWS_REGION);

    String hostName = requireConfig(IdentityOptions.AWS_HOSTNAME);
    String portStr = requireConfig(IdentityOptions.AWS_PORT);
    String username = requireConfig(DatabaseOptions.DB_USERNAME);

    Region awsRegion = Region.of(region);

    int port;
    try {
      port = Integer.parseInt(portStr);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid AWS port: " + portStr, e);
    }

    RdsUtilities utilities = RdsUtilities.builder()
      .region(awsRegion)
      .credentialsProvider(DefaultCredentialsProvider.builder().build())
      .build();

    String token= utilities.generateAuthenticationToken(builder -> builder
      .hostname(hostName)
      .port(port)
      .username(username));
    if (token == null) {
      throw new IllegalStateException("Failed to acquire Aws token");
    }
    //max time for aws token are 15 min
    // 900 sec -300 (found in token manager) is the time the token will refresh
    Instant expires = Instant.now().plusSeconds(900);
    return new TokenResponse(token, expires);
  }

  private String requireConfig( Option<String> key) {
    String value = Configuration.getConfigValue(key).getValue();

    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
        "Required configuration property is missing or empty: " + key
      );
    }

    return value.trim();
  }
}
