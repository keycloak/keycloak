package org.keycloak.services.models.nosql.impl.types;

import com.mongodb.BasicDBList;
import org.keycloak.services.models.nosql.api.types.Converter;

/**
 * Convert BasicDBList to String[] and viceversa (T needs to be declared as Object as Array is not possible here :/ )
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BasicDBListToStringArrayConverter implements Converter<Object, BasicDBList> {

    private static final String[] PLACEHOLDER = new String[] {};

    @Override
    public Object convertDBObjectToApplicationObject(BasicDBList dbObject) {
        return dbObject.toArray(PLACEHOLDER);
    }

    @Override
    public BasicDBList convertApplicationObjectToDBObject(Object applicationObject) {
        BasicDBList list = new BasicDBList();

        String[] array = (String[])applicationObject;
        for (String key : array) {
            list.add(key);
        }

        return list;
    }

    @Override
    public Class<?> getApplicationObjectType() {
        return PLACEHOLDER.getClass();
    }

    @Override
    public Class<BasicDBList> getDBObjectType() {
        return BasicDBList.class;
    }
}
