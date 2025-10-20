package org.keycloak.protocol.ssf.keys;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.KeyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

// @AutoService(KeyProviderFactory.class)
public class TransmitterKeyProviderFactory implements KeyProviderFactory<TransmitterKeyProvider> {

    public static final String PROVIDER_ID = "ssf-transmitter-key";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Shared Signals Transmitter Key Provider";
    }

    @Override
    public TransmitterKeyProvider create(KeycloakSession session, ComponentModel model) {
        return new TransmitterKeyProvider(session, model);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {

        var configPropertyList = ProviderConfigurationBuilder.create() //
                .property(Attributes.PRIORITY_PROPERTY)//
                .property(Attributes.ENABLED_PROPERTY) //
                .property(Attributes.ACTIVE_PROPERTY) //
                .build();

        return configPropertyList;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        ConfigurationValidationHelper.check(model) //
                .checkLong(Attributes.PRIORITY_PROPERTY, false) //
                .checkBoolean(Attributes.ENABLED_PROPERTY, false) //
                .checkBoolean(Attributes.ACTIVE_PROPERTY, false);
    }
}
