package org.keycloak.models.sessions.mem.entities;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionEntity {

    private String id;
    private String realm;
    private String user;
    private String loginUsername;
    private String ipAddress;
    private String authMethod;
    private boolean rememberMe;
    private int started;
    private int lastSessionRefresh;
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

}
