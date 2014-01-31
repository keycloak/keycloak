package org.keycloak.models.mongo.impl.types;

import java.util.Map;
import java.util.Set;

import com.mongodb.BasicDBObject;
import org.keycloak.models.mongo.api.types.Converter;
import org.keycloak.models.mongo.api.types.ConverterContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MapConverter<T extends Map> implements Converter<T, BasicDBObject> {

    // Just some dummy way of encoding . character as it's not allowed by mongo in key fields
    static final String DOT_PLACEHOLDER = "###";

    private final Class<T> mapType;

    public MapConverter(Class<T> mapType) {
        this.mapType = mapType;
    }

    @Override
    public BasicDBObject convertObject(ConverterContext<T> context) {
        T objectToConvert = context.getObjectToConvert();

        BasicDBObject dbObject = new BasicDBObject();
        Set<Map.Entry> entries = objectToConvert.entrySet();
        for (Map.Entry entry : entries) {
            String key = (String)entry.getKey();
            String value = (String)entry.getValue();

            if (key.contains(".")) {
                key = key.replaceAll("\\.", DOT_PLACEHOLDER);
            }

            dbObject.put(key, value);
        }
        return dbObject;
    }

    @Override
    public Class<? extends T> getConverterObjectType() {
        return mapType;
    }

    @Override
    public Class<BasicDBObject> getExpectedReturnType() {
        return BasicDBObject.class;
    }
}
