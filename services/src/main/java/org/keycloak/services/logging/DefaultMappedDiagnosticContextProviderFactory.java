package org.keycloak.services.logging;

import org.keycloak.Config;
import org.keycloak.logging.MappedDiagnosticContextProvider;
import org.keycloak.logging.MappedDiagnosticContextProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.keycloak.services.logging.DefaultMappedDiagnosticContextProvider.MDC_KEY_CLIENT_ID;
import static org.keycloak.services.logging.DefaultMappedDiagnosticContextProvider.MDC_KEY_IP_ADDRESS;
import static org.keycloak.services.logging.DefaultMappedDiagnosticContextProvider.MDC_KEY_ORGANIZATION;
import static org.keycloak.services.logging.DefaultMappedDiagnosticContextProvider.MDC_KEY_REALM;
import static org.keycloak.services.logging.DefaultMappedDiagnosticContextProvider.MDC_KEY_USER_ID;

/**
 * The default provider factory can be configured via --spi-mapped-diagnostic-context-default-mdc-keys to define mdc
 * keys to add as a comma-separated list. By default, "realm" and "clientId" are used if this parameter is not specified.
 * In addition to that, you can add "userId", "ipAddress" and "org" which are supported by the default provider implementation.
 * If you need further keys, you need to extend the provider.
 *
 * @author <a href="mailto:b.eicki@gmx.net">Björn Eickvonder</a>
 */
public class DefaultMappedDiagnosticContextProviderFactory implements MappedDiagnosticContextProviderFactory {

    public static final String MDC_KEYS = "mdcKeys";
    protected static final String[] MDC_KEYS_DEFAULT = {MDC_KEY_REALM, MDC_KEY_CLIENT_ID};
    private Set<String> mdcKeys;

    @Override
    public MappedDiagnosticContextProvider create(KeycloakSession session) {
        return new DefaultMappedDiagnosticContextProvider(session, mdcKeys);
    }

    @Override
    public void init(Config.Scope config) {
        this.mdcKeys = Set.of(Objects.requireNonNullElse(config.getArray(MDC_KEYS), MDC_KEYS_DEFAULT));
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
                .helpText("Comma-separated list of MDC keys to add to the Mapped Diagnostic Context. By default, 'realm' and 'clientId' are used. You can also add 'userId', 'ipAddress' and 'org'.")
                .options(MDC_KEY_REALM, MDC_KEY_CLIENT_ID, MDC_KEY_USER_ID, MDC_KEY_IP_ADDRESS, MDC_KEY_ORGANIZATION)
                .defaultValue(MDC_KEYS_DEFAULT)
                .add();

        return builder.build();
    }
}
