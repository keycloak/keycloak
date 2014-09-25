package org.keycloak.models.sessions.infinispan.entities;

import org.keycloak.models.ClientSessionModel;

import java.io.Serializable;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientSessionEntity extends SessionEntity {

    private String client;

    private String userSession;

    private String redirectUri;

    private String state;

    private int timestamp;

    private ClientSessionModel.Action action;

    private Set<String> roles;

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getUserSession() {
        return userSession;
    }

    public void setUserSession(String userSession) {
        this.userSession = userSession;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public ClientSessionModel.Action getAction() {
        return action;
    }

    public void setAction(ClientSessionModel.Action action) {
        this.action = action;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
