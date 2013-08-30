package org.keycloak.services.models.nosql.api;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface NoSQL {

    /**
     * Insert object if it's oid is null. Otherwise update
     */
    void saveObject(NoSQLObject object);

    <T extends NoSQLObject> T loadObject(Class<T> type, String oid);

    <T extends NoSQLObject> List<T> loadObjects(Class<T> type, Map<String, Object> queryAttributes);

    // Object must have filled oid
    void removeObject(NoSQLObject object);

    void removeObject(Class<? extends NoSQLObject> type, String oid);

    void removeObjects(Class<? extends NoSQLObject> type, Map<String, Object> queryAttributes);
}
