package org.keycloak.models.sessions.infinispan;

import java.io.Serializable;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionTimestamp implements Serializable {
    private String userSessionId;
    private int clientSessionTimestamp;

    public UserSessionTimestamp(String userSessionId, int clientSessionTimestamp) {
        this.userSessionId = userSessionId;
        this.clientSessionTimestamp = clientSessionTimestamp;
    }

    public String getUserSessionId() {
        return userSessionId;
    }

    public int getClientSessionTimestamp() {
        return clientSessionTimestamp;
    }
}
