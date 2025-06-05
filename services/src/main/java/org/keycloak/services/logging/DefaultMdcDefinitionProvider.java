package org.keycloak.services.logging;

import org.keycloak.logging.MdcDefinitionProvider;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The default implementation adds to MDC the realm name, clientId, userId, ipAddress depending
 * on how the provider is initialized. If you want to add further keys you should overwrite
 * {@link #getAdditionalMdcValues(KeycloakContext)} and add the keys to constructor array.
 *
 * @author <a href="mailto:b.eicki@gmx.net">Björn Eickvonder</a>
 */
public class DefaultMdcDefinitionProvider implements MdcDefinitionProvider {

    public static final String MDC_PREFIX = "kc.";
    public static final String MDC_KEY_REALM = MDC_PREFIX + "realm";
    public static final String MDC_KEY_CLIENT_ID = MDC_PREFIX + "clientId";
    public static final String MDC_KEY_USER_ID = MDC_PREFIX + "userId";
    public static final String MDC_KEY_IP_ADDRESS = MDC_PREFIX + "ipAddress";

    protected final KeycloakSession session;
    protected final Set<String> mdcKeys;

    public DefaultMdcDefinitionProvider(KeycloakSession session,
                                        Set<String> mdcKeys) {
        this.session = session;
        this.mdcKeys = mdcKeys;
    }

    @Override
    public final Set<String> getMdcKeys() {
        return mdcKeys;
    }

    @Override
    public final Map<String, String> getMdcValues(KeycloakContext keycloakContext) {
        Map<String, String> mdc = new HashMap<>();

        RealmModel realm = keycloakContext.getRealm();
        if (realm != null && mdcKeys.contains(MDC_KEY_REALM)) {
            mdc.put(MDC_KEY_REALM, realm.getName());
        }
        if (keycloakContext.getClient() != null && mdcKeys.contains(MDC_KEY_CLIENT_ID)) {
            mdc.put(MDC_KEY_CLIENT_ID, keycloakContext.getClient().getClientId());
        }
        if (keycloakContext.getUser() != null && mdcKeys.contains(MDC_KEY_USER_ID)) {
            mdc.put(MDC_KEY_USER_ID, keycloakContext.getUser().getId());
        }
        if (keycloakContext.getUserSession() != null && mdcKeys.contains(MDC_KEY_IP_ADDRESS)) {
            mdc.put(MDC_KEY_IP_ADDRESS, keycloakContext.getUserSession().getIpAddress());
        }
        mdc.putAll(getAdditionalMdcValues(keycloakContext));
        return mdc;
    }

    protected Map<String, String> getAdditionalMdcValues(KeycloakContext keycloakContext) {
        return Collections.emptyMap();
    }

    @Override
    public void close() {
        // no-op
    }
}
