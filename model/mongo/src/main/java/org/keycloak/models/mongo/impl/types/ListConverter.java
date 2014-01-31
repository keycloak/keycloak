package org.keycloak.models.mongo.impl.types;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.keycloak.models.mongo.api.types.Converter;
import org.keycloak.models.mongo.api.types.ConverterContext;
import org.keycloak.models.mongo.api.types.TypeConverter;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ListConverter<T extends List> implements Converter<T, BasicDBList> {

    private final TypeConverter typeConverter;
    private final Class<T> listType;

    public ListConverter(TypeConverter typeConverter, Class<T> listType) {
        this.typeConverter = typeConverter;
        this.listType = listType;
    }

    @Override
    public BasicDBList convertObject(ConverterContext<T> context) {
        T appObjectsList = context.getObjectToConvert();

        BasicDBList dbObjects = new BasicDBList();
        for (Object appObject : appObjectsList) {
            Object dbObject = typeConverter.convertApplicationObjectToDBObject(appObject, Object.class);

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
