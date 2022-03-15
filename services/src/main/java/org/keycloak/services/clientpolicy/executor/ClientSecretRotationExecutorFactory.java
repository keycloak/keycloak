package org.keycloak.services.clientpolicy.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.keycloak.Config.Scope;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:masales@redhat.com">Marcelo Sales</a>
 */
public class ClientSecretRotationExecutorFactory implements ClientPolicyExecutorProviderFactory,
    EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "secret-rotation";

    public static final String SECRET_EXPIRATION_PERIOD = "expiration-period";
    public static final Integer DEFAULT_SECRET_EXPIRATION_PERIOD = Long.valueOf(
        TimeUnit.DAYS.toSeconds(29)).intValue();

    public static final String SECRET_REMAINING_ROTATION_PERIOD = "remaining-rotation-period";
    public static final Integer DEFAULT_SECRET_REMAINING_ROTATION_PERIOD = Long.valueOf(
        TimeUnit.DAYS.toSeconds(10)).intValue();

    public static final String SECRET_ROTATED_EXPIRATION_PERIOD = "rotated-expiration-period";
    public static final Integer DEFAULT_SECRET_ROTATED_EXPIRATION_PERIOD = Long.valueOf(
        TimeUnit.DAYS.toSeconds(2)).intValue();
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty secretExpirationPeriod = new ProviderConfigProperty(
            SECRET_EXPIRATION_PERIOD, "Secret expiration",
            "When the secret is rotated. The time frequency for generating a new secret. (In seconds)",
            ProviderConfigProperty.STRING_TYPE, DEFAULT_SECRET_EXPIRATION_PERIOD);
        configProperties.add(secretExpirationPeriod);

        ProviderConfigProperty secretRotatedPeriod = new ProviderConfigProperty(
            SECRET_ROTATED_EXPIRATION_PERIOD, "Rotated Secret expiration",
            "When secret is rotated, this is the remaining expiration time for the old secret. This value should be always smaller than Secret expiration. When this is set to 0, the old secret will be immediately removed during client rotation (In seconds)",
            ProviderConfigProperty.STRING_TYPE, DEFAULT_SECRET_ROTATED_EXPIRATION_PERIOD);
        configProperties.add(secretRotatedPeriod);

        ProviderConfigProperty secretRemainingExpirationPeriod = new ProviderConfigProperty(
            SECRET_REMAINING_ROTATION_PERIOD, "Remain Expiration Time",
            "During dynamic client registration client-update request, the client secret will be automatically rotated if the remaining expiration time of the current secret is smaller than the value specified by this option. This configuration option is relevant only for dynamic client update requests (In seconds)",
            ProviderConfigProperty.STRING_TYPE, DEFAULT_SECRET_REMAINING_ROTATION_PERIOD);
        configProperties.add(secretRemainingExpirationPeriod);

    }

    @Override
    public String getHelpText() {
        return "The executor verifies that secret rotation is enabled for the client. If rotation is enabled, it provides validation of secrets and performs rotation if necessary.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new ClientSecretRotationExecutor(session);
    }

    @Override
    public void init(Scope config) {

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

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Feature.CLIENT_SECRET_ROTATION);
    }
}
