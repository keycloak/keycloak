package org.keycloak.models.mongo.api;

import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AbstractMongoIdentifiableEntity implements MongoIdentifiableEntity {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void afterRemove(MongoStore mongoStore, MongoStoreInvocationContext invocationContext) {
        // Empty by default
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (this.id == null) return false;

        if (o == null || getClass() != o.getClass()) return false;

        AbstractMongoIdentifiableEntity that = (AbstractMongoIdentifiableEntity) o;

        if (!getId().equals(that.getId())) return false;

        return true;

    }

    @Override
    public int hashCode() {
        return id!=null ? id.hashCode() : super.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s [ id=%s ]", getClass().getSimpleName(), getId());
    }
}
