package org.keycloak.protocol.ssf.receiver;

import java.util.Map;

import org.keycloak.Config;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

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

    public static SsfReceiver getSsfReceiver(KeycloakSession session, RealmModel realm, String alias) {
        IdentityProviderModel maybeSsfReceiverProvider = session.identityProviders().getByAlias(alias);
        SsfReceiverProviderConfig receiverProviderConfig = null;
        if (maybeSsfReceiverProvider != null && SsfReceiverProviderFactory.PROVIDER_ID.equals(maybeSsfReceiverProvider.getProviderId())) {
            receiverProviderConfig = new SsfReceiverProviderConfig(maybeSsfReceiverProvider);
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
