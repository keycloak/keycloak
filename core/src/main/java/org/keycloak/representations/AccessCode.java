package org.keycloak.representations;

import java.util.Set;

/**
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AccessCode {
    protected String id;
    protected String clientId;
    protected String userId;
    protected String state;
    protected String sessionState;
    protected String redirectUri;
    protected int timestamp;
    protected Action action;
    protected Set<String> requestedRoles;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Set<String> getRequestedRoles() {
        return requestedRoles;
    }

    public void setRequestedRoles(Set<String> requestedRoles) {
        this.requestedRoles = requestedRoles;
    }

    public static enum Action {
        OAUTH_GRANT,
        VERIFY_EMAIL,
        UPDATE_PROFILE,
        CONFIGURE_TOTP,
        UPDATE_PASSWORD
    }

}
