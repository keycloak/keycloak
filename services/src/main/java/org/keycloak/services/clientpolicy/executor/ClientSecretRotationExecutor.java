package org.keycloak.services.clientpolicy.executor;

import static org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutorFactory.DEFAULT_SECRET_EXPIRATION_PERIOD;
import static org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutorFactory.DEFAULT_SECRET_REMAINING_ROTATION_PERIOD;
import static org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutorFactory.DEFAULT_SECRET_ROTATED_EXPIRATION_PERIOD;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCClientConfigWrapper;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.ClientSecretRotationContext;


/**
 * @author <a href="mailto:masales@redhat.com">Marcelo Sales</a>
 */
public class ClientSecretRotationExecutor implements
    ClientPolicyExecutorProvider<ClientSecretRotationExecutor.Configuration> {

  private static final Logger logger = Logger.getLogger(ClientSecretRotationExecutor.class);
  private final KeycloakSession session;
  private Configuration configuration;

  public ClientSecretRotationExecutor(KeycloakSession session) {
    this.session = session;
  }

  @Override
  public String getProviderId() {
    return ClientSecretRotationExecutorFactory.PROVIDER_ID;
  }

  @Override
  public Class<Configuration> getExecutorConfigurationClass() {
    return Configuration.class;
  }

  @Override
  public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
    if (!session.getContext().getClient().isPublicClient() && !session.getContext().getClient()
        .isBearerOnly()) {

      switch (context.getEvent()) {
        case UPDATE:
          executeOnClientUpdate((AdminClientUpdateContext) context);
          break;

        case AUTHORIZATION_REQUEST:
        case TOKEN_REQUEST:
          executeOnAuthRequest();
          return;

        default:
          return;
      }
    }
  }

  @Override
  public void setupConfiguration(ClientSecretRotationExecutor.Configuration config) {

    if (config == null) {
      configuration = new Configuration().parseWithDefaultValues();
    } else {
      configuration = config.parseWithDefaultValues();
    }

  }

  private void executeOnAuthRequest() {
    ClientModel client = session.getContext().getClient();
    OIDCClientConfigWrapper wrapper = OIDCClientConfigWrapper.fromClientModel(client);

    if (!wrapper.hasClientSecretExpirationTime()) {
      //first login with policy
      updatedSecretExpiration(wrapper);
    }

  }

  private void executeOnClientUpdate(AdminClientUpdateContext adminContext) {
    OIDCClientConfigWrapper clientConfigWrapper = OIDCClientConfigWrapper.fromClientModel(
        adminContext.getTargetClient());

    if (adminContext instanceof ClientSecretRotationContext
        || clientConfigWrapper.isClientSecretExpired() || !clientConfigWrapper.hasClientSecretExpirationTime()) {
      rotateSecret(adminContext, clientConfigWrapper);
    } else {
      //TODO validation for client dynamic registration

      int secondsRemaining = clientConfigWrapper.getClientSecretExpirationTime()
          - configuration.remainExpirationPeriod;
      if (secondsRemaining <= configuration.remainExpirationPeriod) {
//        rotateSecret(adminContext);
      }
    }
  }

  private void rotateSecret(AdminClientUpdateContext adminContext,
      OIDCClientConfigWrapper clientConfigWrapper) {

    if (adminContext instanceof ClientSecretRotationContext) {
      ClientSecretRotationContext secretRotationContext = ((ClientSecretRotationContext) adminContext);
      if (secretRotationContext.isForceRotation()) {
        updateRotateSecret(clientConfigWrapper, secretRotationContext.getCurrentSecret());
        updateClientConfigProperties(clientConfigWrapper);
      }
    } else if (!clientConfigWrapper.hasClientSecretExpirationTime()) {
      updatedSecretExpiration(clientConfigWrapper);
    } else {
      updatedSecretExpiration(clientConfigWrapper);
      updateRotateSecret(clientConfigWrapper, clientConfigWrapper.getSecret());
      KeycloakModelUtils.generateSecret(adminContext.getTargetClient());
      updateClientConfigProperties(clientConfigWrapper);
    }

    clientConfigWrapper.updateClientRepresentationAttributes(adminContext.getProposedClientRepresentation());
  }

  private void updatedSecretExpiration(OIDCClientConfigWrapper clientConfigWrapper) {
    clientConfigWrapper.setClientSecretExpirationTime(
        Time.currentTime() + configuration.getExpirationPeriod());
  }

  private void updateClientConfigProperties(OIDCClientConfigWrapper clientConfigWrapper) {
    clientConfigWrapper.setClientSecretCreationTime(Time.currentTime());
    updatedSecretExpiration(clientConfigWrapper);
  }

  private void updateRotateSecret(OIDCClientConfigWrapper clientConfigWrapper, String secret) {
    if (configuration.rotatedExpirationPeriod > 0) {
      clientConfigWrapper.setClientRotatedSecret(secret);
      clientConfigWrapper.setClientRotatedSecretCreationTime();
      clientConfigWrapper.setClientRotatedSecretExpirationTime( Time.currentTime() + configuration.getRotatedExpirationPeriod());
    } else {
      clientConfigWrapper.setClientRotatedSecret(null);
      clientConfigWrapper.setClientRotatedSecretCreationTime(null);
      clientConfigWrapper.setClientRotatedSecretExpirationTime(null);
    }
  }

  public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

    @JsonProperty(ClientSecretRotationExecutorFactory.SECRET_EXPIRATION_PERIOD)
    protected Integer expirationPeriod;
    @JsonProperty(ClientSecretRotationExecutorFactory.SECRET_REMAINING_ROTATION_PERIOD)
    protected Integer remainExpirationPeriod;
    @JsonProperty(ClientSecretRotationExecutorFactory.SECRET_ROTATED_EXPIRATION_PERIOD)
    private Integer rotatedExpirationPeriod;

    @Override
    public boolean validateConfig() {
      // expiration must be a positive value greater than 0 (seconds)
      if (expirationPeriod <= 0) {
        return false;
      }

      // rotated secret duration could not be bigger than the main secret
      if (rotatedExpirationPeriod > expirationPeriod) {
        return false;
      }

      // remaining secret expiration period could not be bigger than main secret
      if (remainExpirationPeriod > expirationPeriod) {
        return false;
      }

      return true;
    }

    public Integer getExpirationPeriod() {
      return expirationPeriod;
    }

    public void setExpirationPeriod(Integer expirationPeriod) {
      this.expirationPeriod = expirationPeriod;
    }

    public Integer getRemainExpirationPeriod() {
      return remainExpirationPeriod;
    }

    public void setRemainExpirationPeriod(Integer remainExpirationPeriod) {
      this.remainExpirationPeriod = remainExpirationPeriod;
    }

    public Integer getRotatedExpirationPeriod() {
      return rotatedExpirationPeriod;
    }

    public void setRotatedExpirationPeriod(Integer rotatedExpirationPeriod) {
      this.rotatedExpirationPeriod = rotatedExpirationPeriod;
    }

    public Configuration parseWithDefaultValues() {
      if (getExpirationPeriod() == null) {
        setExpirationPeriod(DEFAULT_SECRET_EXPIRATION_PERIOD);
      }

      if (getRemainExpirationPeriod() == null) {
        setRemainExpirationPeriod(DEFAULT_SECRET_REMAINING_ROTATION_PERIOD);
      }

      if (getRotatedExpirationPeriod() == null) {
        setRotatedExpirationPeriod(DEFAULT_SECRET_ROTATED_EXPIRATION_PERIOD);
      }

      return this;
    }
  }

}
