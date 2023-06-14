package org.keycloak.authentication.otp;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

public interface OTPApplicationProviderFactory extends ProviderFactory<OTPApplicationProvider> {

    @Override
    default void init(Config.Scope config) {
    }

    @Override
    default void postInit(KeycloakSessionFactory factory) {
    }

}
