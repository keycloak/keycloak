package org.keycloak.admin.client.jackson3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

class StringOrArraySerializer3 extends ValueSerializer<Object> {
    @Override
    public void serialize(Object o, JsonGenerator gen, SerializationContext ctxt) {
        String[] array = (String[]) o;
        if (array == null) {
            gen.writeNull();
        } else if (array.length == 1) {
            gen.writeString(array[0]);
        } else {
            gen.writeStartArray();
            for (String s : array) {
                gen.writeString(s);
            }
            gen.writeEndArray();
        }
    }
}
