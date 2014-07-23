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
    protected String usernameUsed;
    protected String state;
    protected String sessionState;
    protected String redirectUri;
    protected boolean rememberMe;
    protected String authMethod;
    protected int timestamp;
    protected int expiration;
    protected Set<String> requiredActions;
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

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public int getExpiration() {
        return expiration;
    }

    public void setExpiration(int expiration) {
        this.expiration = expiration;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public Set<String> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(Set<String> requiredActions) {
        this.requiredActions = requiredActions;
    }

    public String getUsernameUsed() {
        return usernameUsed;
    }

    public void setUsernameUsed(String usernameUsed) {
        this.usernameUsed = usernameUsed;
    }

    public Set<String> getRequestedRoles() {
        return requestedRoles;
    }

    public void setRequestedRoles(Set<String> requestedRoles) {
        this.requestedRoles = requestedRoles;
    }
}
