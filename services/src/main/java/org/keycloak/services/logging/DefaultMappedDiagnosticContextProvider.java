package org.keycloak.services.logging;

import org.jboss.logging.MDC;
import org.keycloak.logging.MappedDiagnosticContextProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Set;

/**
 * The default implementation adds to MDC the realm name, clientId, userId, ipAddress and org depending
 * on how the provider is initialized. If you want to add further keys you should overwrite one of the update methods.
 * and add the keys to constructor array.
 *
 * @author <a href="mailto:b.eicki@gmx.net">Björn Eickvonder</a>
 */
public class DefaultMappedDiagnosticContextProvider implements MappedDiagnosticContextProvider {

    public static final String MDC_KEY_REALM = MDC_PREFIX + "realm";
    public static final String MDC_KEY_CLIENT_ID = MDC_PREFIX + "clientId";
    public static final String MDC_KEY_USER_ID = MDC_PREFIX + "userId";
    public static final String MDC_KEY_IP_ADDRESS = MDC_PREFIX + "ipAddress";
    public static final String MDC_KEY_ORGANIZATION = MDC_PREFIX + "org";

    protected final KeycloakSession session;
    protected final Set<String> mdcKeys;

    public DefaultMappedDiagnosticContextProvider(KeycloakSession session,
                                                  Set<String> mdcKeys) {
        this.session = session;
        this.mdcKeys = mdcKeys;
    }

    @Override
    public void update(KeycloakContext keycloakContext, AuthenticationSessionModel session) {
        // nothing of interest here
    }

    @Override
    public void update(KeycloakContext keycloakContext, RealmModel realm) {
        if (mdcKeys.contains(MDC_KEY_REALM)) {
            putMdc(MDC_KEY_REALM, realm != null ? realm.getName() : null);
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

    @Override
    public void close() {
        // no-op
    }
}
