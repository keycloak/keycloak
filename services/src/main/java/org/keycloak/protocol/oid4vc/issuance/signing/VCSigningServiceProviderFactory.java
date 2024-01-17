package org.keycloak.protocol.oid4vc.issuance.signing;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.time.Clock;

/**
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public interface VCSigningServiceProviderFactory extends ComponentFactory<VerifiableCredentialsSigningService, VerifiableCredentialsSigningService>, EnvironmentDependentProviderFactory {
    Clock CLOCK = Clock.systemUTC();

    public static ProviderConfigurationBuilder configurationBuilder() {
        return ProviderConfigurationBuilder.create()
                .property(SigningProperties.KEY_ID.asConfigProperty());
    }

    @Override
    default void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        ConfigurationValidationHelper.check(model)
                .checkRequired(SigningProperties.KEY_ID.asConfigProperty());
        validateSpecificConfiguration(session, realm, model);
    }


    @Override
    default void close() {
        // no-op
    }

    @Override
    default void init(Config.Scope config) {
        // no-op
    }

    @Override
    default void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    default boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI);
    }

    @Override
    default boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI);
    }

    /**
     * Should validate potential implementation specific configuration of the factory.
     */
    void validateSpecificConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException;

    /**
     * Should return the credentials format supported by the signing service.
     *
     * @return the format
     */
    Format supportedFormat();
}

