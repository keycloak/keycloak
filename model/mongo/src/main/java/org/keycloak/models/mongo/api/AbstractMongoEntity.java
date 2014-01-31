package org.keycloak.models.mongo.api;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractMongoEntity implements MongoEntity {

    @Override
    public void afterRemove(MongoStore mongoStore) {
        // Empty by default
    }
}
