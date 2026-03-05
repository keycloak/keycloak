package org.keycloak.protocol.oid4vc.clientpolicy;

import java.util.Collections;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProviderFactory;

public class CredentialClientPolicyExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "oid4vci-policy-executor";

    @Override
    public CredentialClientPolicyExecutor create(KeycloakSession session) {
        return new CredentialClientPolicyExecutor(session);
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
        return "This executor checks client policies related to the credential offer process";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

}
