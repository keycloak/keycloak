package org.keycloak.connections.mongo.impl.types;

import com.mongodb.BasicDBObject;
import org.keycloak.connections.mongo.api.types.Mapper;
import org.keycloak.connections.mongo.api.types.MapperContext;

import java.util.HashMap;
import java.util.Map;

/**
 * For now, there is support just for convert to Map<String, simpleType>
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BasicDBObjectToMapMapper implements Mapper<BasicDBObject, Map> {

    @Override
    public Map convertObject(MapperContext<BasicDBObject, Map> context) {
        BasicDBObject dbObjectToConvert = context.getObjectToConvert();

        HashMap<String, Object> result = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : dbObjectToConvert.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Workaround as manually inserted numbers into mongo may be treated as "Double"
            if (value instanceof Double && context.getGenericTypes().get(1) == Integer.class) {
                value = ((Double)value).intValue();
            }

            if (key.contains(MapMapper.DOT_PLACEHOLDER)) {
                key = key.replaceAll(MapMapper.DOT_PLACEHOLDER, ".");
            }

            result.put(key, value);
        }
        return result;
    }

    @Override
    public Class<? extends BasicDBObject> getTypeOfObjectToConvert() {
        return BasicDBObject.class;
    }

    @Override
    public Class<Map> getExpectedReturnType() {
        return Map.class;
    }
}
