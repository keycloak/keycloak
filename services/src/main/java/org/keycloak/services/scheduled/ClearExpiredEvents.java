package org.keycloak.services.scheduled;

import org.keycloak.events.EventStoreProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClearExpiredEvents implements ScheduledTask {

    @Override
    public void run(KeycloakSession session) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        if (eventStore != null) {
            for (RealmModel realm : session.realms().getRealms()) {
                if (realm.isEventsEnabled() && realm.getEventsExpiration() > 0) {
                    long olderThan = System.currentTimeMillis() - realm.getEventsExpiration() * 1000;
                    eventStore.clear(realm.getId(), olderThan);
                }
            }
        }
    }

}
