package org.keycloak.models.mongo.keycloak.entities;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.models.entities.AbstractIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoIdentifiableEntity;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@MongoCollection(collectionName = "sessions")
public class MongoUserSessionEntity extends AbstractIdentifiableEntity implements MongoIdentifiableEntity {

    private String realmId;

    private String user;

    private String ipAddress;

    private int started;

    private int lastSessionRefresh;

    private List<String> associatedClientIds = new ArrayList<String>();

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
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

    public List<String> getAssociatedClientIds() {
        return associatedClientIds;
    }

    public void setAssociatedClientIds(List<String> associatedClientIds) {
        this.associatedClientIds = associatedClientIds;
    }

    @Override
    public void afterRemove(MongoStoreInvocationContext context) {
    }

}
