package org.keycloak.services.clientpolicy.condition;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;

import java.util.List;

public class ClientIdsCondition extends AbstractClientPolicyConditionProvider<ClientIdsCondition.Configuration> {

  private static final Logger logger = Logger.getLogger(ClientIdsCondition.class);

  public ClientIdsCondition(KeycloakSession session) {
    super(session);
  }

  @Override public Class<ClientIdsCondition.Configuration> getConditionConfigurationClass() {
    return ClientIdsCondition.Configuration.class;
  }

  @Override public String getProviderId() {
    return ClientIdsConditionFactory.PROVIDER_ID;
  }

  @Override public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
    switch (context.getEvent()) {
    case AUTHORIZATION_REQUEST:
    case TOKEN_REQUEST:
    case TOKEN_REFRESH:
    case TOKEN_REVOKE:
    case TOKEN_INTROSPECT:
    case USERINFO_REQUEST:
    case LOGOUT_REQUEST:
      if (clientIdMatched(session.getContext().getClient()))
        return ClientPolicyVote.YES;
      return ClientPolicyVote.NO;
    default:
      return ClientPolicyVote.ABSTAIN;
    }
  }

  private boolean clientIdMatched(ClientModel client) {
    if (client == null || client.getClientId() == null)
      return false;

    List<String> configuredClientIds = configuration.getClientIds();

    if (configuredClientIds == null)
      return false;

    String clientId = client.getClientId();

    for (String configuredClientId : configuredClientIds) {
      if (clientId.equals(configuredClientId)) {
        return true;
      }
    }

    return false;

  }

  public static class Configuration extends ClientPolicyConditionConfigurationRepresentation {

    protected List<String> clientIds;

    public List<String> getClientIds() {
      return clientIds;
    }

    public void setClientIds(List<String> clientIds) {
      this.clientIds = clientIds;
    }
  }

}
