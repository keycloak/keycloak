package org.keycloak.admin.client.jackson3;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.util.MultivaluedHashMap;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

class MultivaluedHashMapValueSerializer3 extends ValueSerializer<MultivaluedHashMap<String, String>> {

    @Override
    public void serialize(MultivaluedHashMap<String, String> map, JsonGenerator gen, SerializationContext ctxt) {
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
                    gen.writeBooleanProperty(key, Boolean.parseBoolean(value));
                } else {
                    gen.writeName(key);
                    gen.writeString(value);
                }
            } else {
                gen.writeArrayPropertyStart(key);
                for (String v : values) {
                    gen.writeString(v);
                }
                gen.writeEndArray();
            }
        }

        gen.writeEndObject();
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, MultivaluedHashMap<String, String> value) {
        return getIgnoredProperties(ctxt.getGenerator()).containsAll(value.keySet());
    }

    private static Set<String> getIgnoredProperties(JsonGenerator gen) {
        Class<?> parentClazz = gen.currentValue().getClass();
        return Arrays.stream(parentClazz.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("get"))
                .map(name -> name.substring(3).toLowerCase()).collect(Collectors.toSet());
    }
}
