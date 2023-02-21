
package org.keycloak.admin.ui.rest.model;

import java.util.Objects;

public class SessionId {


    public enum SessionType {
        ALL, REGULAR, OFFLINE
    }

    private final String clientId;
    private final SessionType type;

    public SessionId(String clientId, SessionType type) {
        this.clientId = clientId;
        this.type = type;
    }

    public String getClientId() {
        return clientId;
    }

    public SessionType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionId sessionId = (SessionId) o;
        return Objects.equals(clientId, sessionId.clientId) && type == sessionId.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, type);
    }
}
