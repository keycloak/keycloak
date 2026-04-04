package org.keycloak.services.logging;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.logging.MappedDiagnosticContextProvider;
import org.keycloak.logging.MappedDiagnosticContextProviderFactory;
import org.keycloak.logging.MappedDiagnosticContextUtil;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.jboss.logging.MDC;

/**
 * The default provider factory can be configured via --spi-mapped-diagnostic-context-default-mdc-keys to define mdc
 * keys to add as a comma-separated list. By default, "realm", "clientId", "userId", "ipAddress" and "org" are supported by the default provider implementation.
 * If you need further keys, you need to extend the provider.
 *
 * @author <a href="mailto:b.eicki@gmx.net">Björn Eickvonder</a>
 */
public class DefaultMappedDiagnosticContextProviderFactory implements MappedDiagnosticContextProviderFactory, MappedDiagnosticContextProvider, EnvironmentDependentProviderFactory {

    public static final String MDC_KEY_REALM_NAME = MDC_PREFIX + "realmName";
    public static final String MDC_KEY_CLIENT_ID = MDC_PREFIX + "clientId";
    public static final String MDC_KEY_USER_ID = MDC_PREFIX + "userId";
    public static final String MDC_KEY_IP_ADDRESS = MDC_PREFIX + "ipAddress";
    public static final String MDC_KEY_ORGANIZATION = MDC_PREFIX + "org";
    public static final String MDC_KEY_SESSION_ID = MDC_PREFIX + "sessionId";
    public static final String MDC_KEY_AUTHENTICATION_SESSION_ID = MDC_PREFIX + "authenticationSessionId";
    public static final String MDC_KEY_AUTHENTICATION_TAB_ID = MDC_PREFIX + "authenticationTabId";

    public static final String MDC_KEYS = "mdcKeys";
    private Set<String> mdcKeys;

    @Override
    public MappedDiagnosticContextProvider create(KeycloakSession session) {
        // not using session, thus implementing MappedDiagnosticContextProvider here and handling it as singleton is fine
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        this.mdcKeys = Arrays.stream(Objects.requireNonNullElse(config.getArray(MDC_KEYS), new String[] {})).map(s -> MDC_PREFIX + s).collect(Collectors.toSet());
        MappedDiagnosticContextUtil.setKeysToClear(mdcKeys);
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
                .helpText("Comma-separated list of MDC keys to add to the Mapped Diagnostic Context.")
                .options(Stream.of(MDC_KEY_REALM_NAME, MDC_KEY_CLIENT_ID, MDC_KEY_USER_ID, MDC_KEY_IP_ADDRESS, MDC_KEY_ORGANIZATION, MDC_KEY_SESSION_ID, MDC_KEY_AUTHENTICATION_SESSION_ID, MDC_KEY_AUTHENTICATION_TAB_ID)
                        .map(s -> s.substring(MDC_PREFIX.length())).collect(Collectors.toList()))
                .add();

        return builder.build();
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.LOG_MDC);
    }

    @Override
    public void update(KeycloakContext keycloakContext, AuthenticationSessionModel session) {
        if (mdcKeys.contains(MDC_KEY_AUTHENTICATION_SESSION_ID)) {
            putMdc(MDC_KEY_AUTHENTICATION_SESSION_ID, session != null ? (session.getParentSession() != null ? session.getParentSession().getId() : null) : null);
        }
        if (mdcKeys.contains(MDC_KEY_AUTHENTICATION_TAB_ID)) {
            putMdc(MDC_KEY_AUTHENTICATION_TAB_ID, session != null ? session.getTabId() : null);
        }
    }

    @Override
    public void update(KeycloakContext keycloakContext, RealmModel realm) {
        if (mdcKeys.contains(MDC_KEY_REALM_NAME)) {
            putMdc(MDC_KEY_REALM_NAME, realm != null ? realm.getName() : null);
        }
    }

    @Override
    public void update(KeycloakContext keycloakContext, ClientModel client) {
        if (mdcKeys.contains(MDC_KEY_CLIENT_ID)) {
            putMdc(MDC_KEY_CLIENT_ID, client != null ? client.getClientId() : null);
        }
    }

    @Override
    public void update(KeycloakContext keycloakContext, OrganizationModel organization) {
        if (mdcKeys.contains(MDC_KEY_ORGANIZATION)) {
            putMdc(MDC_KEY_ORGANIZATION, organization != null ? organization.getAlias() : null);
        }
    }

    @Override
    public void update(KeycloakContext keycloakContext, UserSessionModel userSession) {
        if (mdcKeys.contains(MDC_KEY_USER_ID)) {
            putMdc(MDC_KEY_USER_ID, userSession != null && userSession.getUser() != null ? userSession.getUser().getId() : null);
        }
        if (mdcKeys.contains(MDC_KEY_SESSION_ID)) {
            putMdc(MDC_KEY_SESSION_ID, userSession != null ? userSession.getId() : null);
        }
        if (mdcKeys.contains(MDC_KEY_IP_ADDRESS)) {
            putMdc(MDC_KEY_IP_ADDRESS, userSession != null ? userSession.getIpAddress() : null);
        }
    }

    protected void putMdc(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        } else {
            MDC.remove(key);
        }
    }
}
