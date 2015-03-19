package org.keycloak.connections.mongo.impl.types;

import com.mongodb.BasicDBObject;
import org.keycloak.connections.mongo.api.types.Mapper;
import org.keycloak.connections.mongo.api.types.MapperContext;

import java.util.Map;
import java.util.Set;

/**
 * For now, we support just convert from Map<String, simpleType>
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MapMapper<T extends Map> implements Mapper<T, BasicDBObject> {

    // Just some dummy way of encoding . character as it's not allowed by mongo in key fields
    static final String DOT_PLACEHOLDER = "###";

    private final Class<T> mapType;

    public MapMapper(Class<T> mapType) {
        this.mapType = mapType;
    }

    @Override
    public BasicDBObject convertObject(MapperContext<T, BasicDBObject> context) {
        T mapToConvert = context.getObjectToConvert();
        return convertMap(mapToConvert);
    }

    public static BasicDBObject convertMap(Map mapToConvert) {
        BasicDBObject dbObject = new BasicDBObject();
        Set<Map.Entry> entries = mapToConvert.entrySet();
        for (Map.Entry entry : entries) {
            String key = (String)entry.getKey();
            Object value = entry.getValue();

            if (key.contains(".")) {
                key = key.replaceAll("\\.", DOT_PLACEHOLDER);
            }

            dbObject.put(key, value);
        }
        return dbObject;
    }

    @Override
    public Class<? extends T> getTypeOfObjectToConvert() {
        return mapType;
    }

    @Override
    public Class<BasicDBObject> getExpectedReturnType() {
        return BasicDBObject.class;
    }
}
