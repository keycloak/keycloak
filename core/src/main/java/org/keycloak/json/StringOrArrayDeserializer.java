package org.keycloak.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class StringOrArrayDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        if (jsonNode.isArray()) {
            ArrayList<String> a = new ArrayList<>(1);
            Iterator<JsonNode> itr = jsonNode.iterator();
            while (itr.hasNext()) {
                a.add(itr.next().getTextValue());
            }
            return a.toArray(new String[a.size()]);
        } else {
            return new String[] { jsonNode.getTextValue() };
        }
    }

}
