package org.keycloak.services.scheduled;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClearExpiredUserSessions implements ScheduledTask {

    @Override
    public void run(KeycloakSession keycloakSession, ProviderSession providerSession) {
        for (RealmModel realm : keycloakSession.getRealms()) {
            realm.removeExpiredUserSessions();
        }
    }

}
