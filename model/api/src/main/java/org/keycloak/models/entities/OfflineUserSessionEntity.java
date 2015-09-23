package org.keycloak.models.entities;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineUserSessionEntity {

    private String userSessionId;
    private String data;
    private List<OfflineClientSessionEntity> offlineClientSessions;

    public String getUserSessionId() {
        return userSessionId;
    }

    public void setUserSessionId(String userSessionId) {
        this.userSessionId = userSessionId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public List<OfflineClientSessionEntity> getOfflineClientSessions() {
        return offlineClientSessions;
    }

    public void setOfflineClientSessions(List<OfflineClientSessionEntity> offlineClientSessions) {
        this.offlineClientSessions = offlineClientSessions;
    }
}
