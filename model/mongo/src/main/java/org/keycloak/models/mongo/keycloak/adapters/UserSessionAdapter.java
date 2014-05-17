package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.MongoClientUserSessionAssociationEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserSessionEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter extends AbstractMongoAdapter<MongoUserSessionEntity> implements UserSessionModel {

    private MongoUserSessionEntity entity;
    private RealmAdapter realm;

    public UserSessionAdapter(MongoUserSessionEntity entity, RealmAdapter realm, MongoStoreInvocationContext invContext)
    {
        super(invContext);
        this.entity = entity;
        this.realm = realm;
    }

    @Override
    protected MongoUserSessionEntity getMongoEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public void setId(String id) {
        entity.setId(id);
    }

    @Override
    public UserModel getUser() {
        return realm.getUserById(entity.getUser());
    }

    @Override
    public void setUser(UserModel user) {
        entity.setUser(user.getId());
    }

    @Override
    public String getIpAddress() {
        return entity.getIpAddress();
    }

    @Override
    public void setIpAddress(String ipAddress) {
        entity.setIpAddress(ipAddress);
    }

    @Override
    public int getStarted() {
        return entity.getStarted();
    }

    @Override
    public void setStarted(int started) {
        entity.setStarted(started);
    }

    @Override
    public int getLastSessionRefresh() {
        return entity.getLastSessionRefresh();
    }

    @Override
    public void setLastSessionRefresh(int seconds) {
        entity.setLastSessionRefresh(seconds);
    }

    @Override
    public void associateClient(ClientModel client) {
        List<ClientModel> clients = getClientAssociations();
        for (ClientModel ass : clients) {
            if (ass.getId().equals(client.getId())) return;
        }

        MongoClientUserSessionAssociationEntity association = new MongoClientUserSessionAssociationEntity();
        association.setClientId(client.getId());
        association.setSessionId(getId());

        getMongoStore().insertEntity(association, invocationContext);
    }

    @Override
    public List<ClientModel> getClientAssociations() {
        DBObject query = new QueryBuilder()
                .and("sessionId").is(getId())
                .get();
        List<MongoClientUserSessionAssociationEntity> associations = getMongoStore().loadEntities(MongoClientUserSessionAssociationEntity.class, query, invocationContext);

        List<ClientModel> result = new ArrayList<ClientModel>();
        for (MongoClientUserSessionAssociationEntity association : associations) {
            ClientModel client = realm.findClientById(association.getClientId());
            result.add(client);
        }
        return result;
    }

    @Override
    public void removeAssociatedClient(ClientModel client) {
        DBObject query = new QueryBuilder()
                .and("sessionId").is(getId())
                .and("clientId").is(client.getId())
                .get();
        getMongoStore().removeEntities(MongoClientUserSessionAssociationEntity.class, query, invocationContext);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        UserSessionAdapter that = (UserSessionAdapter) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
