package org.keycloak.models.sessions.infinispan.entities;

import org.keycloak.models.UserSessionModel;
import org.keycloak.util.MultivaluedHashMap;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionEntity extends SessionEntity {

    private String user;

    private String loginUsername;

    private String ipAddress;

    private String authMethod;

    private MultivaluedHashMap<String, String> claims;

    private boolean rememberMe;

    private int started;

    private int lastSessionRefresh;

    private Set<String> clientSessions;

    private UserSessionModel.State state;

    private Map<String, String> notes;

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

    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    public UserSessionModel.State getState() {
        return state;
    }

    public void setState(UserSessionModel.State state) {
        this.state = state;
    }

    public MultivaluedHashMap<String, String> getClaims() {
        return claims;
    }

    public void setClaims(MultivaluedHashMap<String, String> claims) {
        this.claims = claims;
    }
}
