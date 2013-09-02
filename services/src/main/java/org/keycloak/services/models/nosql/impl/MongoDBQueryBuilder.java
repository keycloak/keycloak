package org.keycloak.services.models.nosql.impl;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.keycloak.services.models.nosql.api.query.NoSQLQueryBuilder;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoDBQueryBuilder extends NoSQLQueryBuilder {

    @Override
    public NoSQLQueryBuilder inCondition(String name, Object[] values) {
        if (values == null) {
            values = new Object[0];
        }

        if ("_id".equals(name)) {
            // we need to convert Strings to ObjectID
            ObjectId[] objIds = new ObjectId[values.length];
            for (int i=0 ; i<values.length ; i++) {
                String id = values[i].toString();
                ObjectId objectId = new ObjectId(id);
                objIds[i] = objectId;
            }
            values = objIds;
        }

        BasicDBObject inObject = new BasicDBObject("$in", values);
        put(name, inObject);
        return this;
    }
}
