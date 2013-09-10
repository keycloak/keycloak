package org.keycloak.services.models.nosql.api;

import java.util.List;

import org.keycloak.services.models.nosql.api.query.NoSQLQuery;
import org.keycloak.services.models.nosql.api.query.NoSQLQueryBuilder;
import org.picketlink.common.properties.Property;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface NoSQL {

    /**
     * Insert object if it's oid is null. Otherwise update
     */
    void saveObject(NoSQLObject object);

    <T extends NoSQLObject> T loadObject(Class<T> type, String oid);

    <T extends NoSQLObject> T loadSingleObject(Class<T> type, NoSQLQuery query);

    <T extends NoSQLObject> List<T> loadObjects(Class<T> type, NoSQLQuery query);

    // Object must have filled oid
    void removeObject(NoSQLObject object);

    void removeObject(Class<? extends NoSQLObject> type, String oid);

    void removeObjects(Class<? extends NoSQLObject> type, NoSQLQuery query);

    NoSQLQueryBuilder createQueryBuilder();

    <S> void pushItemToList(NoSQLObject object, String listPropertyName, S itemToPush);

    <S> void pullItemFromList(NoSQLObject object, String listPropertyName, S itemToPull);
}
