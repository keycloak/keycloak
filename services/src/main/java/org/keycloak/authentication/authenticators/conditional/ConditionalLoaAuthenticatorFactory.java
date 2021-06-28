package org.keycloak.authentication.authenticators.conditional;

import java.util.List;
import org.keycloak.Config;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class ConditionalLoaAuthenticatorFactory implements ConditionalAuthenticatorFactory {

  public static final String PROVIDER_ID = "conditional-level-of-authentication";
  private static final ConditionalLoaAuthenticator SINGLETON = new ConditionalLoaAuthenticator();
  private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = new AuthenticationExecutionModel.Requirement[]{
      AuthenticationExecutionModel.Requirement.REQUIRED,
      AuthenticationExecutionModel.Requirement.DISABLED
  };

  private static final List<ProviderConfigProperty> CONFIG = ProviderConfigurationBuilder.create()
      .property()
      .name(ConditionalLoaAuthenticator.LEVEL)
      .label(ConditionalLoaAuthenticator.LEVEL)
      .helpText(ConditionalLoaAuthenticator.LEVEL + ".tooltip")
      .type(ProviderConfigProperty.STRING_TYPE)
      .add()
      .build();

  @Override
  public void init(Config.Scope config) { }

  @Override
  public void postInit(KeycloakSessionFactory factory) { }

  @Override
  public void close() { }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public String getDisplayType() {
    return "Condition - Level of Authentication";
  }

  @Override
  public String getReferenceCategory() {
    return "condition";
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
    return "Flow is executed only if the configured LOA or a higher one has been requested but not yet satisfied.";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return CONFIG;
  }

  @Override
  public ConditionalAuthenticator getSingleton() {
    return SINGLETON;
  }
}
