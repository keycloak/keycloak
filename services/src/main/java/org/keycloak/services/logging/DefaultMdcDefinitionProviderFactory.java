package org.keycloak.services.logging;

import org.keycloak.Config;
import org.keycloak.logging.MdcDefinitionProvider;
import org.keycloak.logging.MdcDefinitionProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.keycloak.services.logging.DefaultMdcDefinitionProvider.MDC_KEY_CLIENT_ID;
import static org.keycloak.services.logging.DefaultMdcDefinitionProvider.MDC_KEY_IP_ADDRESS;
import static org.keycloak.services.logging.DefaultMdcDefinitionProvider.MDC_KEY_REALM;
import static org.keycloak.services.logging.DefaultMdcDefinitionProvider.MDC_KEY_USER_ID;

/**
 * The default provider factory can be configured via --spi-mdc-definition-default-mdc-keys to define mdc
 * keys to add as a comma-separated list. By default, "realm" and "clientId" are used if this parameter is not specified.
 * In addition to that, you can add "userId" and "ipAddress" which are supported by the default provider implementation.
 * If you need further keys, you need to extend the provider.
 *
 * @author <a href="mailto:b.eicki@gmx.net">Björn Eickvonder</a>
 */
public class DefaultMdcDefinitionProviderFactory implements MdcDefinitionProviderFactory {

    public static final String MDC_KEYS = "mdcKeys";
    public static final String[] MDC_KEYS_DEFAULT = {MDC_KEY_REALM, MDC_KEY_CLIENT_ID};
    private Set<String> mdcKeys;

    @Override
    public MdcDefinitionProvider create(KeycloakSession session) {
        return new DefaultMdcDefinitionProvider(session, mdcKeys);
    }

    @Override
    public void init(Config.Scope config) {
        this.mdcKeys = Set.of(Objects.requireNonNullElse(config.getArray(MDC_KEYS),
                MDC_KEYS_DEFAULT));
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

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();

        builder.property()
                .name(MDC_KEYS)
                .type("string")
                .helpText("Version")
                .options(MDC_KEY_REALM, MDC_KEY_CLIENT_ID, MDC_KEY_USER_ID, MDC_KEY_IP_ADDRESS)
                .defaultValue(MDC_KEYS_DEFAULT)
                .add();

        return builder.build();
    }
}
