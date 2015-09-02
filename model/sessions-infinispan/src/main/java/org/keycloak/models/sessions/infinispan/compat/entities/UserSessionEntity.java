package org.keycloak.models.sessions.infinispan.compat.entities;

import org.keycloak.models.UserSessionModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionEntity {

    private String id;
    private String brokerSessionId;
    private String brokerUserId;
    private String realm;
    private String user;
    private String loginUsername;
    private String ipAddress;
    private String authMethod;
    private boolean rememberMe;
    private int started;
    private int lastSessionRefresh;
    private UserSessionModel.State state;
    private Map<String, String> notes = new HashMap<String, String>();
    private List<ClientSessionEntity> clientSessions = Collections.synchronizedList(new LinkedList<ClientSessionEntity>());

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getLoginUsername() {
        return loginUsername;
    }

    public void setLoginUsername(String loginUsername) {
        this.loginUsername = loginUsername;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public int getStarted() {
        return started;
    }

    public void setStarted(int started) {
        this.started = started;
    }

    public int getLastSessionRefresh() {
        return lastSessionRefresh;
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        this.lastSessionRefresh = lastSessionRefresh;
    }

    public void addClientSession(ClientSessionEntity clientSession) {
        if (clientSessions == null) {
            clientSessions = new LinkedList<ClientSessionEntity>();
        }
        clientSessions.add(clientSession);
    }

    public void removeClientSession(ClientSessionEntity clientSession) {
        if (clientSessions != null) {
            clientSessions.remove(clientSession);
        }
    }

    public List<ClientSessionEntity> getClientSessions() {
        return clientSessions;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public UserSessionModel.State getState() {
        return state;
    }

    public void setState(UserSessionModel.State state) {
        this.state = state;
    }

    public String getBrokerSessionId() {
        return brokerSessionId;
    }

    public void setBrokerSessionId(String brokerSessionId) {
        this.brokerSessionId = brokerSessionId;
    }

    public String getBrokerUserId() {
        return brokerUserId;
    }

    public void setBrokerUserId(String brokerUserId) {
        this.brokerUserId = brokerUserId;
    }
}
