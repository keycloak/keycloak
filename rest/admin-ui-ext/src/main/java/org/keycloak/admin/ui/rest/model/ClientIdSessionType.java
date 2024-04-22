
package org.keycloak.admin.ui.rest.model;

import java.util.Objects;

/**
 * A tuple containing the clientId and the session type (online/offline).
 *
 */
public class ClientIdSessionType {


    public enum SessionType {
        ALL, REGULAR, OFFLINE
    }

    private final String clientId;
    private final SessionType type;

    public ClientIdSessionType(String clientId, SessionType type) {
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
        ClientIdSessionType clientIdSessionType = (ClientIdSessionType) o;
        return Objects.equals(clientId, clientIdSessionType.clientId) && type == clientIdSessionType.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, type);
    }
}
