package org.keycloak.models.sessions.mongo;

import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractMongoAdapter<T extends MongoIdentifiableEntity> {

    protected final MongoStoreInvocationContext invocationContext;

    public AbstractMongoAdapter(MongoStoreInvocationContext invocationContext) {
        this.invocationContext = invocationContext;
    }

    protected abstract T getMongoEntity();

    protected void updateMongoEntity() {
        getMongoStore().updateEntity(getMongoEntity(), invocationContext);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (o == null || getClass() != o.getClass()) return false;

        AbstractMongoAdapter that = (AbstractMongoAdapter) o;

        if (getMongoEntity() == null && that.getMongoEntity() == null) return true;
        return getMongoEntity().equals(that.getMongoEntity());
    }

    @Override
    public int hashCode() {
        return getMongoEntity()!=null ? getMongoEntity().hashCode() : super.hashCode();
    }

    protected MongoStore getMongoStore() {
        return invocationContext.getMongoStore();
    }
}
