package org.keycloak.models.mongo.impl.types;

import java.util.HashMap;
import java.util.Map;

import com.mongodb.BasicDBObject;
import org.keycloak.models.mongo.api.types.Converter;
import org.keycloak.models.mongo.api.types.ConverterContext;

/**
 * For now, we support just convert to Map<String, String>
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BasicDBObjectToMapConverter implements Converter<BasicDBObject, Map> {

    @Override
    public Map convertObject(ConverterContext<BasicDBObject> context) {
        BasicDBObject objectToConvert = context.getObjectToConvert();

        HashMap<String, String> result = new HashMap<String, String>();
        for (Map.Entry<String, Object> entry : objectToConvert.entrySet()) {
            String key = entry.getKey();
            String value = (String)entry.getValue();

            if (key.contains(MapConverter.DOT_PLACEHOLDER)) {
                key = key.replaceAll(MapConverter.DOT_PLACEHOLDER, ".");
            }

            result.put(key, value);
        }
        return result;
    }

    @Override
    public Class<? extends BasicDBObject> getConverterObjectType() {
        return BasicDBObject.class;
    }

    @Override
    public Class<Map> getExpectedReturnType() {
        return Map.class;
    }
}
