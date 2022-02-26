package org.keycloak.services.clientpolicy.executor;

import static org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutorFactory.DEFAULT_SECRET_EXPIRATION_PERIOD;
import static org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutorFactory.DEFAULT_SECRET_REMAINING_ROTATION_PERIOD;
import static org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutorFactory.DEFAULT_SECRET_ROTATED_EXPIRATION_PERIOD;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCClientConfigWrapper;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.ClientSecretRotationContext;
import org.keycloak.utils.ClockUtil;

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
          AdminClientUpdateContext adminContext = (AdminClientUpdateContext) context;
          OIDCClientConfigWrapper clientConfigWrapper = OIDCClientConfigWrapper.fromClientRepresentation(
              adminContext.getProposedClientRepresentation());

          LocalDateTime creationTime = LocalDateTime.ofInstant(
              Instant.ofEpochSecond(clientConfigWrapper.getClientSecretCreationTime()),
              ZoneId.systemDefault());

          LocalDateTime expiration = creationTime.plus(configuration.getExpirationPeriod(),
              ChronoUnit.SECONDS);

          LocalDateTime now = LocalDateTime.now(ClockUtil.getClock());
          if ((adminContext instanceof ClientSecretRotationContext
              && ((ClientSecretRotationContext) adminContext).isForceRotation())
              || expiration.isBefore(now)) {
            rotateSecret(adminContext, expiration);
          } else {
            //TODO validation for client dynamic registration
            LocalDateTime remainPeriod = expiration.minusSeconds(
                configuration.remainExpirationPeriod);
            long secondsRemaining = ChronoUnit.SECONDS.between(now, expiration);
            if (secondsRemaining <= configuration.remainExpirationPeriod) {
              rotateSecret(adminContext, expiration);
            }
          }

          break;

        default:
          return;
      }
    }
  }

  private void rotateSecret(AdminClientUpdateContext adminContext, LocalDateTime expiration) {
    ClientModel client = adminContext.getTargetClient();
    OIDCClientConfigWrapper clientConfigWrapper = OIDCClientConfigWrapper.fromClientModel(client);

    if (adminContext instanceof ClientSecretRotationContext
        && !((ClientSecretRotationContext) adminContext).isForceRotation()) {
      ClientSecretRotationContext secretRotationContext = (ClientSecretRotationContext) adminContext;
      clientConfigWrapper.setClientSecretRotated(secretRotationContext.getCurrentSecret());
    } else {
      clientConfigWrapper.setClientSecretRotated(client.getSecret());
      KeycloakModelUtils.generateSecret(client);
    }

    clientConfigWrapper.setClientSecretCreationTime(ClockUtil.currentTimeInSeconds());
    clientConfigWrapper.setClientSecretRotatedCreationTime();

    ZoneOffset offset = OffsetDateTime.now(ClockUtil.getClock()).getOffset();

    clientConfigWrapper.setClientSecretExpirationTime(
        Long.valueOf(expiration.toEpochSecond(offset)).intValue());
    clientConfigWrapper.setClientSecretRotatedExpirationTime(Long.valueOf(
            LocalDateTime.now(ClockUtil.getClock())
                .plusSeconds(configuration.getRotatedExpirationPeriod()).toEpochSecond(offset))
        .intValue());

    clientConfigWrapper.updateClientRepresentationAttributes(adminContext.getProposedClientRepresentation());
  }

  @Override
  public void setupConfiguration(ClientSecretRotationExecutor.Configuration config) {

    if (config == null) {
      configuration = new Configuration().parseWithDefaultValues();
    } else {
      configuration = config.parseWithDefaultValues();
    }

  }

  public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

    @JsonProperty(ClientSecretRotationExecutorFactory.SECRET_EXPIRATION_PERIOD)
    protected Integer expirationPeriod;

    @JsonProperty(ClientSecretRotationExecutorFactory.SECRET_REMAINING_ROTATION_PERIOD)
    protected Integer remainExpirationPeriod;

    @JsonProperty(ClientSecretRotationExecutorFactory.SECRET_ROTATED_EXPIRATION_PERIOD)
    private Integer rotatedExpirationPeriod;

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
