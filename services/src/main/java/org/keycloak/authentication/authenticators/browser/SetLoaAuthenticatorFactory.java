package org.keycloak.authentication.authenticators.browser;

import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class SetLoaAuthenticatorFactory implements AuthenticatorFactory {

  public static final String PROVIDER_ID = "set-level-of-authentication";
  private static final SetLoaAuthenticator SINGLETON = new SetLoaAuthenticator();
  private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = new AuthenticationExecutionModel.Requirement[]{
      AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED
  };

  private static final List<ProviderConfigProperty> CONFIG = ProviderConfigurationBuilder.create()
      .property()
      .name(SetLoaAuthenticator.LEVEL)
      .label(SetLoaAuthenticator.LEVEL)
      .helpText(SetLoaAuthenticator.LEVEL + ".tooltip")
      .type(ProviderConfigProperty.STRING_TYPE)
      .add()
      .property()
      .name(SetLoaAuthenticator.STORE_IN_USER_SESSION)
      .label(SetLoaAuthenticator.STORE_IN_USER_SESSION)
      .helpText(SetLoaAuthenticator.STORE_IN_USER_SESSION + ".tooltip")
      .type(ProviderConfigProperty.BOOLEAN_TYPE)
      .defaultValue("true")
      .add()
      .build();

  @Override
  public String getDisplayType() {
    return "Set Level of Authentication";
  }

  @Override
  public String getReferenceCategory() {
    return "loa";
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Override
  public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
    return REQUIREMENT_CHOICES;
  }

  @Override
  public boolean isUserSetupAllowed() {
    return false;
  }

  @Override
  public String getHelpText() {
    return "Set the Level of Authentication (LOA).";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return CONFIG;
  }

  @Override
  public Authenticator create(KeycloakSession session) {
    return SINGLETON;
  }

  @Override
  public void init(Config.Scope config) {
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
  }

  @Override
  public void close() {
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }
}
