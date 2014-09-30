package org.keycloak.models.sessions.infinispan.entities;

import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionEntity extends SessionEntity {

    private String user;

    private String loginUsername;

    private String ipAddress;

    private String authMethod;

    private boolean rememberMe;

    private int started;

    private int lastSessionRefresh;

    private Set<String> clientSessions;

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

    public Set<String> getClientSessions() {
        return clientSessions;
    }

    public void setClientSessions(Set<String> clientSessions) {
        this.clientSessions = clientSessions;
    }
}
