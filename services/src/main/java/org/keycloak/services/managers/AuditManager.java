package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.keycloak.ClientConnection;
import org.keycloak.audit.Audit;
import org.keycloak.audit.AuditListener;
import org.keycloak.audit.AuditProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuditManager {

    private Logger log = Logger.getLogger(AuditManager.class);

    private RealmModel realm;
    private KeycloakSession session;
    private ClientConnection clientConnection;

    public AuditManager(RealmModel realm, KeycloakSession session, ClientConnection clientConnection) {
        this.realm = realm;
        this.session = session;
        this.clientConnection = clientConnection;
    }

    public Audit createAudit() {
        List<AuditListener> listeners = new LinkedList<AuditListener>();

        if (realm.isAuditEnabled()) {
            AuditProvider auditProvider = session.getProvider(AuditProvider.class);
            if (auditProvider != null) {
                listeners.add(auditProvider);
            } else {
                log.error("Audit enabled, but no audit provider configured");
            }
        }

        if (realm.getAuditListeners() != null) {
            for (String id : realm.getAuditListeners()) {
                AuditListener listener = session.getProvider(AuditListener.class, id);
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
