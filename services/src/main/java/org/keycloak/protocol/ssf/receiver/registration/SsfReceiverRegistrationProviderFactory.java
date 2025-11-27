package org.keycloak.protocol.ssf.receiver.registration;

import java.util.Map;

import org.keycloak.Config;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.receiver.DefaultSsfReceiver;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class SsfReceiverRegistrationProviderFactory extends AbstractIdentityProviderFactory<SsfReceiverRegistrationProvider> implements IdentityProviderFactory<SsfReceiverRegistrationProvider>, EnvironmentDependentProviderFactory {

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
    public SsfReceiverRegistrationProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new SsfReceiverRegistrationProvider(session, adaptConfig(model));
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new SsfReceiverRegistrationProviderConfig();
    }

    protected SsfReceiverRegistrationProviderConfig adaptConfig(IdentityProviderModel model) {
        if (model instanceof SsfReceiverRegistrationProviderConfig ssfModel) {
            return ssfModel;
        }
        return new SsfReceiverRegistrationProviderConfig(model);
    }

    public static SsfReceiver getSsfReceiver(KeycloakSession session, RealmModel realm, String alias) {
        IdentityProviderModel maybeSsfReceiverProvider = session.identityProviders().getByAlias(alias);
        SsfReceiverRegistrationProviderConfig receiverProviderConfig = null;
        if (maybeSsfReceiverProvider != null && SsfReceiverRegistrationProviderFactory.PROVIDER_ID.equals(maybeSsfReceiverProvider.getProviderId())) {
            receiverProviderConfig = new SsfReceiverRegistrationProviderConfig(maybeSsfReceiverProvider);
        }
        return new DefaultSsfReceiver(session, receiverProviderConfig);
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
