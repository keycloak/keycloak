package org.keycloak.models.sessions.mongo.entities;

import org.keycloak.connections.mongo.api.MongoCollection;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.entities.AbstractIdentifiableEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@MongoCollection(collectionName = "clientSessions")
public class MongoClientSessionEntity extends AbstractIdentifiableEntity implements MongoIdentifiableEntity {

    private String id;
    private String clientId;
    private String realmId;
    private String sessionId;

    private String redirectUri;
    private String authMethod;

    private int timestamp;
    private String action;
    private List<String> roles;
    private List<String> protocolMappers;
    private Map<String, String> notes = new HashMap<String, String>();
    private Map<String, String> userSessionNotes = new HashMap<String, String>();
    private Map<String, ClientSessionModel.ExecutionStatus> authenticatorStatus = new HashMap<>();
    private String authUserId;

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

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
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

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getProtocolMappers() {
        return protocolMappers;
    }

    public void setProtocolMappers(List<String> protocolMappers) {
        this.protocolMappers = protocolMappers;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    public Map<String, String> getUserSessionNotes() {
        return userSessionNotes;
    }

    public void setUserSessionNotes(Map<String, String> userSessionNotes) {
        this.userSessionNotes = userSessionNotes;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    @Override
    public void afterRemove(MongoStoreInvocationContext context) {
    }

}
