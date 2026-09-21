/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.mdoc;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import com.webauthn4j.converter.util.CborConverter;
import com.webauthn4j.converter.util.ObjectConverter;

final class CborUtil {

    static final int TAG_TDATE = 0;
    static final int TAG_ENCODED_CBOR = 24;

    private static final ObjectConverter OBJECT_CONVERTER = createObjectConverter();
    private static final CborConverter CBOR_CONVERTER = OBJECT_CONVERTER.getCborConverter();
    private static final DateTimeFormatter TDATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    // ISO mdoc requires definite length CBOR encoding, while Jackson's default Map serializer emits indefinite
    // length maps. Register a serializer that writes the map header with its size for every encoded map.
    private static ObjectConverter createObjectConverter() {
        SimpleModule definiteLengthMaps = new SimpleModule();
        definiteLengthMaps.addSerializer(new DefiniteLengthMapSerializer());
        ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
        cborMapper.registerModule(definiteLengthMaps);
        return new ObjectConverter(new ObjectMapper(), cborMapper);
    }

    private CborUtil() {
    }

    static byte[] encode(Object value) {
        return CBOR_CONVERTER.writeValueAsBytes(value);
    }

    // COSE signatures cover the protected-header byte string exactly. Jackson's default Map serializer emits an
    // indefinite-length map, so integer-only COSE headers use a sized CBOR object for deterministic minimal bytes.
    static byte[] encodeIntegerMap(Map<Integer, Integer> value) {
        return encode(new IntegerMap(value));
    }

    static Object decode(byte[] encoded) {
        return CBOR_CONVERTER.readValue(encoded, Object.class);
    }

    @SuppressWarnings("unchecked")
    static Map<Object, Object> asMap(Object object, String name) {
        if (object instanceof Map<?, ?>) {
            return (Map<Object, Object>) object;
        }
        throw new MdocException("Unexpected map structure for " + name);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asStringKeyMap(Object object, String name) {
        if (object instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) object;
            for (Object key : map.keySet()) {
                if (!(key instanceof String)) {
                    throw new MdocException("Unexpected non-string map key for " + name);
                }
            }
            return (Map<String, Object>) object;
        }
        throw new MdocException("Unexpected map structure for " + name);
    }

    @SuppressWarnings("unchecked")
    static List<Object> asList(Object object, String name) {
        if (object instanceof List<?>) {
            return (List<Object>) object;
        }
        throw new MdocException("Unexpected array structure for " + name);
    }

    static String asString(Object object, String name) {
        if (object instanceof String) {
            return (String) object;
        }
        throw new MdocException("Unexpected string structure for " + name);
    }

    static byte[] asByteArray(Object object, String name) {
        if (object instanceof byte[]) {
            return (byte[]) object;
        }
        throw new MdocException("Unexpected byte string structure for " + name);
    }

    static Object unwrapEncodedCbor(Object item) {
        // CBOR tag 24 means the byte string contains an encoded CBOR data item. ISO mdoc wraps
        // IssuerSignedItemBytes and MSO bytes this way, so parser callers need to decode the nested item.
        if (item instanceof Tagged) {
            Tagged taggedItem = (Tagged) item;
            if (taggedItem.tag() == TAG_ENCODED_CBOR && taggedItem.value() instanceof byte[]) {
                return decode((byte[]) taggedItem.value());
            }
        }
        if (item instanceof byte[]) {
            return decode((byte[]) item);
        }
        return item;
    }

    static Tagged tdate(Instant instant) {
        // ISO mdoc restricts tdate values to RFC 3339 timestamps without fractional seconds
        Instant truncated = instant.truncatedTo(ChronoUnit.SECONDS);
        return new Tagged(TAG_TDATE, TDATE_FORMATTER.format(truncated.atOffset(ZoneOffset.UTC)));
    }

    static Tagged encodedCbor(Object value) {
        return new Tagged(TAG_ENCODED_CBOR, encode(value));
    }

    static final class Tagged implements JsonSerializable {

        private final int tag;
        private final Object value;

        Tagged(int tag, Object value) {
            this.tag = tag;
            this.value = value;
        }

        int tag() {
            return tag;
        }

        Object value() {
            return value;
        }

        @Override
        public void serialize(JsonGenerator generator, SerializerProvider provider) throws IOException {
            ((CBORGenerator) generator).writeTag(tag);
            generator.writeObject(value);
        }

        @Override
        public void serializeWithType(JsonGenerator generator, SerializerProvider provider, TypeSerializer typeSerializer)
                throws IOException {
            serialize(generator, provider);
        }
    }

    static final class DefiniteLengthMapSerializer extends StdSerializer<Map<?, ?>> {

        DefiniteLengthMapSerializer() {
            super(Map.class, false);
        }

        @Override
        public void serialize(Map<?, ?> value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            CBORGenerator cborGenerator = (CBORGenerator) generator;
            cborGenerator.writeStartObject(value.size());
            for (Map.Entry<?, ?> entry : value.entrySet()) {
                Object key = entry.getKey();
                if (key instanceof Number) {
                    cborGenerator.writeFieldId(((Number) key).longValue());
                } else {
                    generator.writeFieldName(String.valueOf(key));
                }
                generator.writeObject(entry.getValue());
            }
            generator.writeEndObject();
        }
    }

    static final class IntegerMap implements JsonSerializable {

        private final Map<Integer, Integer> value;

        IntegerMap(Map<Integer, Integer> value) {
            this.value = value;
        }

        @Override
        public void serialize(JsonGenerator generator, SerializerProvider provider) throws IOException {
            CBORGenerator cborGenerator = (CBORGenerator) generator;
            cborGenerator.writeStartObject(value.size());
            for (Map.Entry<Integer, Integer> entry : value.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
                cborGenerator.writeFieldId(entry.getKey());
                generator.writeNumber(entry.getValue());
            }
            generator.writeEndObject();
        }

        @Override
        public void serializeWithType(JsonGenerator generator, SerializerProvider provider, TypeSerializer typeSerializer)
                throws IOException {
            serialize(generator, provider);
        }
    }
}
