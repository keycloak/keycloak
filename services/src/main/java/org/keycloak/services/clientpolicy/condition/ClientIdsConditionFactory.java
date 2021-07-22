package org.keycloak.services.clientpolicy.condition;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class ClientIdsConditionFactory implements ClientPolicyConditionProviderFactory {

  public static final String PROVIDER_ID = "client-ids";
  public static final String CLIENT_IDS = "clientIds";

  private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

  static {
    ProviderConfigProperty property;
    property = new ProviderConfigProperty(CLIENT_IDS, PROVIDER_ID + ".label", PROVIDER_ID + "-condition.tooltip",
        ProviderConfigProperty.MULTIVALUED_STRING_TYPE, null);
    configProperties.add(property);
  }

  @Override public ClientPolicyConditionProvider create(KeycloakSession session) {
    return new ClientIdsCondition(session);
  }

  @Override public void init(Config.Scope config) {
  }

  @Override public void postInit(KeycloakSessionFactory factory) {
  }

  @Override public void close() {
  }

  @Override public String getId() {
    return PROVIDER_ID;
  }

  @Override public String getHelpText() {
    return "The condition checks whether one of the specified client ids matches the client-id of the client to determine whether the policy is applied.";
  }

  @Override public List<ProviderConfigProperty> getConfigProperties() {
    return configProperties;
  }

}


