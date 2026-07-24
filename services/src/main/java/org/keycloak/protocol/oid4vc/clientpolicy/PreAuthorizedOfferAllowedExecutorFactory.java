package org.keycloak.protocol.oid4vc.clientpolicy;

import java.util.Collections;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProviderFactory;

public class PreAuthorizedOfferAllowedExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "oid4vci-preauth-allowed";

    @Override
    public PreAuthorizedOfferAllowedExecutor create(KeycloakSession session) {
        return new PreAuthorizedOfferAllowedExecutor(session);
    }

    @Override
    public void init(Config.Scope config) {
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
    public String getHelpText() {
        return "Governs whether pre-authorized offer grants are allowed";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_POLICIES)
                && Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI);
    }
}
