package org.keycloak.broker.saml;

import org.keycloak.Config;
import org.keycloak.encryption.EncryptionProvider;
import org.keycloak.encryption.EncryptionProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;

public class JksProviderFactory implements EncryptionProviderFactory {
    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    @Override
    public EncryptionProvider create(KeycloakSession session) {
        return new JksProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        logger.info("Jks Provider Factory init()");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        logger.info("JKS Provider Factory post init()");
    }

    @Override
    public void close() {}

    @Override
    public String getId() {
        return "jks-encryption-provider";
    }
}
