package org.keycloak.representations.workflows;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.util.MultivaluedHashMap;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class MultivaluedHashMapValueSerializer extends JsonSerializer<MultivaluedHashMap<String, String>> {

    @Override
    public void serialize(MultivaluedHashMap<String, String> map, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        Set<String> ignoredProperties = getIgnoredProperties(gen);

        gen.writeStartObject();

        for (Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();

            if (ignoredProperties.contains(key)) {
                continue;
            }

            List<String> values = entry.getValue();

            if (values.size() == 1) {
                String value = values.get(0);

                if (Boolean.TRUE.toString().equalsIgnoreCase(value) || Boolean.FALSE.toString().equalsIgnoreCase(value)) {
                    gen.writeBooleanField(key, Boolean.parseBoolean(value));
                } else {
                    gen.writeObjectField(key, value);
                }
            } else {
                gen.writeArrayFieldStart(key);
                for (String v : values) {
                    gen.writeString(v);
                }
                gen.writeEndArray();
            }
        }

        gen.writeEndObject();
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, MultivaluedHashMap<String, String> value) {
        // if all properties are ignored, consider the map as empty
        return getIgnoredProperties(provider.getGenerator()).containsAll(value.keySet());
    }

    private static Set<String> getIgnoredProperties(JsonGenerator gen) {
        Class<?> parentClazz = gen.currentValue().getClass();
        return Arrays.stream(parentClazz.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("get"))
                .map(name -> name.substring(3).toLowerCase()).collect(Collectors.toSet());
    }
}
