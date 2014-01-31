package org.keycloak.models.mongo.impl.types;

import com.mongodb.BasicDBList;
import org.keycloak.models.mongo.api.types.Converter;
import org.keycloak.models.mongo.api.types.ConverterContext;
import org.keycloak.models.mongo.api.types.TypeConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BasicDBListConverter implements Converter<BasicDBList, List> {

    private final TypeConverter typeConverter;

    public BasicDBListConverter(TypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    @Override
    public List convertObject(ConverterContext<BasicDBList> context) {
        BasicDBList dbList = context.getObjectToConvert();
        ArrayList<Object> appObjects = new ArrayList<Object>();
        Class<?> expectedListElementType = context.getGenericTypes().get(0);

        for (Object dbObject : dbList) {
            ConverterContext<Object> newContext = new ConverterContext<Object>(dbObject, expectedListElementType, null);
            appObjects.add(typeConverter.convertDBObjectToApplicationObject(newContext));
        }
        return appObjects;
    }

    @Override
    public Class<? extends BasicDBList> getConverterObjectType() {
        return BasicDBList.class;
    }

    @Override
    public Class<List> getExpectedReturnType() {
        return List.class;
    }
}
