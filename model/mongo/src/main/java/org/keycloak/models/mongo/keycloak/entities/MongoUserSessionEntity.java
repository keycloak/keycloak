package org.keycloak.models.mongo.keycloak.entities;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
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

    @Override
    public void afterRemove(MongoStoreInvocationContext context) {
        // Remove all roles, which belongs to this application
        DBObject query = new QueryBuilder()
                .and("sessionId").is(getId())
                .get();
        context.getMongoStore().removeEntities(MongoClientUserSessionAssociationEntity.class, query, context);
    }

}
