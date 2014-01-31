package org.keycloak.models.mongo.api;

/**
 * Base interface for object, which is persisted in Mongo
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface MongoEntity {

    /**
     * Lifecycle callback, which is called after removal of this object from Mongo.
     * It may be useful for triggering removal of wired objects.
     */
    void afterRemove(MongoStore mongoStore);

}
