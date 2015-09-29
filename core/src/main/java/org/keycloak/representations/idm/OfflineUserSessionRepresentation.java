package org.keycloak.representations.idm;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineUserSessionRepresentation {

    private String userSessionId;
    private String data;
    private List<OfflineClientSessionRepresentation> offlineClientSessions;

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

    public List<OfflineClientSessionRepresentation> getOfflineClientSessions() {
        return offlineClientSessions;
    }

    public void setOfflineClientSessions(List<OfflineClientSessionRepresentation> offlineClientSessions) {
        this.offlineClientSessions = offlineClientSessions;
    }
}
