package org.keycloak.models.sessions.infinispan;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Consumers {

    private Consumers() {
    }

    public static UserSessionModelsConsumer userSessionModels(InfinispanUserSessionProvider provider, RealmModel realm, boolean offline) {
        return new UserSessionModelsConsumer(provider, realm, offline);
    }

    public static class UserSessionIdAndTimestampConsumer implements Consumer<Map.Entry<String, SessionEntity>> {

        private Map<String, Integer> sessions = new HashMap<>();

        @Override
        public void accept(Map.Entry<String, SessionEntity> entry) {
            SessionEntity e = entry.getValue();
            if (e instanceof ClientSessionEntity) {
                ClientSessionEntity ce = (ClientSessionEntity) e;
                sessions.put(ce.getUserSession(), ce.getTimestamp());
            }
        }

    }

    public static class UserSessionModelsConsumer implements Consumer<Map.Entry<String, SessionEntity>> {

        private InfinispanUserSessionProvider provider;
        private RealmModel realm;
        private boolean offline;
        private List<UserSessionModel> sessions = new LinkedList<>();

        private UserSessionModelsConsumer(InfinispanUserSessionProvider provider, RealmModel realm, boolean offline) {
            this.provider = provider;
            this.realm = realm;
            this.offline = offline;
        }

        @Override
        public void accept(Map.Entry<String, SessionEntity> entry) {
            SessionEntity e = entry.getValue();
            sessions.add(provider.wrap(realm, (UserSessionEntity) e, offline));
        }

        public List<UserSessionModel> getSessions() {
            return sessions;
        }

    }
}
