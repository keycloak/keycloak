package org.keycloak.models.mongo.impl.types;

import java.util.HashMap;
import java.util.Map;

import com.mongodb.BasicDBObject;
import org.keycloak.models.mongo.api.types.Mapper;
import org.keycloak.models.mongo.api.types.MapperContext;

/**
 * For now, there is support just for convert to Map<String, String>
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BasicDBObjectToMapMapper implements Mapper<BasicDBObject, Map> {

    @Override
    public Map convertObject(MapperContext<BasicDBObject, Map> context) {
        BasicDBObject dbObjectToConvert = context.getObjectToConvert();

        HashMap<String, String> result = new HashMap<String, String>();
        for (Map.Entry<String, Object> entry : dbObjectToConvert.entrySet()) {
            String key = entry.getKey();
            String value = (String)entry.getValue();

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
