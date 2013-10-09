package org.keycloak.models.mongo.api;

/**
 * Base interface for object, which is persisted in NoSQL database
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface NoSQLObject {

    /**
     * Lifecycle callback, which is called after removal of this object from NoSQL database.
     * It may be useful for triggering removal of wired objects.
     */
    void afterRemove(NoSQL noSQL);

}
