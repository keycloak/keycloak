package org.keycloak.services.scheduled;

import org.keycloak.audit.AuditProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClearExpiredAuditEvents implements ScheduledTask {

    @Override
    public void run(KeycloakSession session) {
        AuditProvider audit = session.getProvider(AuditProvider.class);
        if (audit != null) {
            for (RealmModel realm : session.getRealms()) {
                if (realm.isAuditEnabled() && realm.getAuditExpiration() > 0) {
                    long olderThan = System.currentTimeMillis() - realm.getAuditExpiration() * 1000;
                    audit.clear(realm.getId(), olderThan);
                }
            }
        }
    }

}
