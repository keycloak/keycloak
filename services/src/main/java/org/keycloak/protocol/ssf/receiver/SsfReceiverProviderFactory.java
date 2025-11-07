package org.keycloak.protocol.ssf.receiver;

import org.keycloak.Config;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import java.util.Map;

public class SsfReceiverProviderFactory extends AbstractIdentityProviderFactory<SsfReceiverProvider> implements IdentityProviderFactory<SsfReceiverProvider>, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "ssf-receiver";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getName() {
        return "SSF Receiver";
    }

    @Override
    public SsfReceiverProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new SsfReceiverProvider(session, adaptConfig(model));
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new SsfReceiverProviderConfig();
    }

    protected SsfReceiverProviderConfig adaptConfig(IdentityProviderModel model) {
        if (model instanceof SsfReceiverProviderConfig ssfModel) {
            return ssfModel;
        }
        return new SsfReceiverProviderConfig(model);
    }

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, String config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SSF);
    }
}
