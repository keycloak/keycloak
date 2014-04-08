package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.keycloak.audit.Audit;
import org.keycloak.audit.AuditListener;
import org.keycloak.audit.AuditProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ClientConnection;
import org.keycloak.provider.ProviderSession;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuditManager {

    private Logger log = Logger.getLogger(AuditManager.class);

    private RealmModel realm;
    private ProviderSession providers;
    private ClientConnection clientConnection;

    public AuditManager(RealmModel realm, ProviderSession providers, ClientConnection clientConnection) {
        this.realm = realm;
        this.providers = providers;
        this.clientConnection = clientConnection;
    }

    public Audit createAudit() {
        List<AuditListener> listeners = new LinkedList<AuditListener>();

        if (realm.isAuditEnabled()) {
            AuditProvider auditProvider = providers.getProvider(AuditProvider.class);
            if (auditProvider != null) {
                listeners.add(auditProvider);
            } else {
                log.error("Audit enabled, but no audit provider configured");
            }
        }

        if (realm.getAuditListeners() != null) {
            for (String id : realm.getAuditListeners()) {
                AuditListener listener = providers.getProvider(AuditListener.class, id);
                if (listener != null) {
                    listeners.add(listener);
                } else {
                    log.error("Audit listener '" + id + "' registered, but not found");
                }
            }
        }

        return new Audit(listeners, realm, clientConnection.getRemoteAddr());
    }

}
