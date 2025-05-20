package org.keycloak.services.logging;

import org.keycloak.Config;
import org.keycloak.logging.MdcDefinitionProvider;
import org.keycloak.logging.MdcDefinitionProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.Objects;

import static org.keycloak.services.logging.DefaultMdcDefinitionProvider.MDC_KEY_CLIENT_ID;
import static org.keycloak.services.logging.DefaultMdcDefinitionProvider.MDC_KEY_REALM;

/**
 * The default provider factory can be configured via --spi-mdc-definition-default-mdc-keys to define mdc
 * keys to add as a comma-separated list. By default, "realm" and "clientId" are used if this parameter is not specified.
 * In addition to that, you can add "userId" and "ipAddress" which are supported by the default provider implementation.
 * If you need further keys, you need to extend the provider.
 *
 * @author <a href="mailto:b.eicki@gmx.net">Björn Eickvonder</a>
 */
public class DefaultMdcDefinitionProviderFactory implements MdcDefinitionProviderFactory {

    private String[] mdcKeys;

    @Override
    public MdcDefinitionProvider create(KeycloakSession session) {
        return new DefaultMdcDefinitionProvider(session, mdcKeys);
    }

    @Override
    public void init(Config.Scope config) {
        this.mdcKeys = Objects.requireNonNullElse(config.getArray("mdcKeys"),
                new String[]{MDC_KEY_REALM, MDC_KEY_CLIENT_ID});
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return "default";
    }
}
