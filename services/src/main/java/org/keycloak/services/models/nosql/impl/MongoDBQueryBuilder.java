package org.keycloak.services.models.nosql.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.keycloak.services.models.nosql.api.query.NoSQLQueryBuilder;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoDBQueryBuilder extends NoSQLQueryBuilder {

    protected MongoDBQueryBuilder() {};

    @Override
    public NoSQLQueryBuilder inCondition(String name, List<?> values) {
        if (values == null) {
            values = new LinkedList<Object>();
        }

        if ("_id".equals(name)) {
            // we need to convert Strings to ObjectID
            List<ObjectId> objIds = new ArrayList<ObjectId>();
            for (Object object : values) {
                ObjectId objectId = new ObjectId(object.toString());
                objIds.add(objectId);
            }
            values = objIds;
        }

        BasicDBObject inObject = new BasicDBObject("$in", values);
        put(name, inObject);
        return this;
    }
}
