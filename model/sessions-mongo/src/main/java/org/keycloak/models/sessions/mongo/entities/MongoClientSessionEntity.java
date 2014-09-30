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
    private ClientSessionModel.Action action;
    private List<String> roles;
    private Map<String, String> notes = new HashMap<String, String>();

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

    public ClientSessionModel.Action getAction() {
        return action;
    }

    public void setAction(ClientSessionModel.Action action) {
        this.action = action;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void afterRemove(MongoStoreInvocationContext context) {
    }

}
