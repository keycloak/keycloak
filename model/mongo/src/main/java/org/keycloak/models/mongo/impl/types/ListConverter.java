package org.keycloak.models.mongo.impl.types;

import java.util.List;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.keycloak.models.mongo.api.types.Converter;
import org.keycloak.models.mongo.api.types.TypeConverter;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ListConverter<T extends List> implements Converter<T, BasicDBList> {

    // Key for ObjectType field, which points to actual Java type of element objects inside list
    static final String OBJECT_TYPE = "OBJECT_TYPE";

    private final TypeConverter typeConverter;
    private final Class<T> listType;

    public ListConverter(TypeConverter typeConverter, Class<T> listType) {
        this.typeConverter = typeConverter;
        this.listType = listType;
    }

    @Override
    public BasicDBList convertObject(T appObjectsList) {
        BasicDBList dbObjects = new BasicDBList();
        for (Object appObject : appObjectsList) {
            Object dbObject = typeConverter.convertApplicationObjectToDBObject(appObject, Object.class);

            // We need to add OBJECT_TYPE key to object, so we can retrieve correct Java type of object during load of this list
            if (dbObject instanceof BasicDBObject) {
                BasicDBObject basicDBObject = (BasicDBObject)dbObject;
                basicDBObject.put(OBJECT_TYPE, appObject.getClass().getName());
            }

            dbObjects.add(dbObject);
        }
        return dbObjects;
    }

    @Override
    public Class<? extends T> getConverterObjectType() {
        return listType;
    }

    @Override
    public Class<BasicDBList> getExpectedReturnType() {
        return BasicDBList.class;
    }
}
