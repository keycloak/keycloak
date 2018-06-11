package org.keycloak.jose.jws;

import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

@SuppressWarnings("rawtypes")
public class HmacTokenSignatureProviderFactory implements TokenSignatureProviderFactory  {

    public static final String ID = "hmac-signature";

    private static final String HELP_TEXT = "Generates token signature provider using HMAC key";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create().build();

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
        return ID;
    }

    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public TokenSignatureProvider create(KeycloakSession session, ComponentModel model) {
        return new HmacTokenSignatureProvider(session, model);
    }


}
