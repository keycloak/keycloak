package org.keycloak.models.entities;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PersistentUserSessionEntity {

    private String id;
    private String realmId;
    private String userId;
    private int lastSessionRefresh;
    private String data;
    private List<PersistentClientSessionEntity> clientSessions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getLastSessionRefresh() {
        return lastSessionRefresh;
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        this.lastSessionRefresh = lastSessionRefresh;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public List<PersistentClientSessionEntity> getClientSessions() {
        return clientSessions;
    }

    public void setClientSessions(List<PersistentClientSessionEntity> clientSessions) {
        this.clientSessions = clientSessions;
    }
}
