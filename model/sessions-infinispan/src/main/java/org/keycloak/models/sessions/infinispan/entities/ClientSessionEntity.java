package org.keycloak.models.sessions.infinispan.entities;

import org.keycloak.models.ClientSessionModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientSessionEntity extends SessionEntity {

    private String client;

    private String userSession;

    private String authMethod;

    private String redirectUri;

    private String state;

    private int timestamp;

    private String action;

    private Set<String> roles;
    private Set<String> protocolMappers;
    private Map<String, String> notes;
    private Map<String, String> userSessionNotes;
    private Map<String, ClientSessionModel.ExecutionStatus> authenticatorStatus = new HashMap<>();
    private String authUserId;
    private Set<String> requiredActions = new HashSet<>();


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

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getProtocolMappers() {
        return protocolMappers;
    }

    public void setProtocolMappers(Set<String> protocolMappers) {
        this.protocolMappers = protocolMappers;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    public Map<String, ClientSessionModel.ExecutionStatus> getAuthenticatorStatus() {
        return authenticatorStatus;
    }

    public void setAuthenticatorStatus(Map<String, ClientSessionModel.ExecutionStatus> authenticatorStatus) {
        this.authenticatorStatus = authenticatorStatus;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public Map<String, String> getUserSessionNotes() {
        return userSessionNotes;
    }

    public void setUserSessionNotes(Map<String, String> userSessionNotes) {
        this.userSessionNotes = userSessionNotes;
    }

    public Set<String> getRequiredActions() {
        return requiredActions;
    }
}
