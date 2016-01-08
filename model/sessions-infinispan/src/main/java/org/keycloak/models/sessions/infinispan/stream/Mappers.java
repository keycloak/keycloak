package org.keycloak.models.sessions.infinispan.stream;

import org.keycloak.models.sessions.infinispan.UserSessionTimestamp;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Mappers {

    public static Function<Map.Entry<String, SessionEntity>, UserSessionTimestamp> clientSessionToUserSessionTimestamp() {
        return new ClientSessionToUserSessionTimestampMapper();
    }

    public static Function<Map.Entry<String, Optional<UserSessionTimestamp>>, UserSessionTimestamp> userSessionTimestamp() {
        return new UserSessionTimestampMapper();
    }

    public static Function<Map.Entry<String, SessionEntity>, String> sessionId() {
        return new SessionIdMapper();
    }

    public static Function<Map.Entry<LoginFailureKey, LoginFailureEntity>, LoginFailureKey> loginFailureId() {
        return new LoginFailureIdMapper();
    }

    public static Function<Map.Entry<String, SessionEntity>, String> clientSessionToUserSessionId() {
        return new ClientSessionToUserSessionIdMapper();
    }

    private static class ClientSessionToUserSessionTimestampMapper implements Function<Map.Entry<String, SessionEntity>, UserSessionTimestamp>, Serializable {
        @Override
        public UserSessionTimestamp apply(Map.Entry<String, SessionEntity> entry) {
            SessionEntity e = entry.getValue();
            ClientSessionEntity entity = (ClientSessionEntity) e;
            return new UserSessionTimestamp(entity.getUserSession(), entity.getTimestamp());
        }
    }

    private static class UserSessionTimestampMapper implements Function<Map.Entry<String, Optional<org.keycloak.models.sessions.infinispan.UserSessionTimestamp>>, org.keycloak.models.sessions.infinispan.UserSessionTimestamp>, Serializable {
        @Override
        public org.keycloak.models.sessions.infinispan.UserSessionTimestamp apply(Map.Entry<String, Optional<org.keycloak.models.sessions.infinispan.UserSessionTimestamp>> e) {
            return e.getValue().get();
        }
    }

    private static class SessionIdMapper implements Function<Map.Entry<String, SessionEntity>, String>, Serializable {
        @Override
        public String apply(Map.Entry<String, SessionEntity> entry) {
            return entry.getKey();
        }
    }

    private static class LoginFailureIdMapper implements Function<Map.Entry<LoginFailureKey, LoginFailureEntity>, LoginFailureKey>, Serializable {
        @Override
        public LoginFailureKey apply(Map.Entry<LoginFailureKey, LoginFailureEntity> entry) {
            return entry.getKey();
        }
    }

    private static class ClientSessionToUserSessionIdMapper implements Function<Map.Entry<String, SessionEntity>, String>, Serializable {
        @Override
        public String apply(Map.Entry<String, SessionEntity> entry) {
            SessionEntity e = entry.getValue();
            ClientSessionEntity entity = (ClientSessionEntity) e;
            return entity.getUserSession();
        }
    }
}
