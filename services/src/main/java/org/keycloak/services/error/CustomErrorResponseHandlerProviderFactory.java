package org.keycloak.services.error;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class CustomErrorResponseHandlerProviderFactory implements ErrorResponseHandlerProviderFactory {

  @Override
  public ErrorResponseHandlerProvider create(KeycloakSession keycloakSession) {
    return new CustomErrorResponseHandlerProvider(keycloakSession);
  }

  @Override
  public void init(Config.Scope scope) {

  }

  @Override
  public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

  }

  @Override
  public void close() {

  }

  @Override
  public String getId() {
    return "custom-error-response-handler";
  }
}
