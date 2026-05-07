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
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import com.webauthn4j.converter.util.CborConverter;
import com.webauthn4j.converter.util.ObjectConverter;

final class MdocCbor {

    private static final ObjectConverter OBJECT_CONVERTER = new ObjectConverter();
    private static final CborConverter CBOR_CONVERTER = OBJECT_CONVERTER.getCborConverter();
    private static final DateTimeFormatter TDATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private MdocCbor() {
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

    static Tagged tdate(Instant instant) {
        return new Tagged(0, TDATE_FORMATTER.format(instant.atOffset(ZoneOffset.UTC)));
    }

    static Tagged encodedCbor(Object value) {
        return new Tagged(24, encode(value));
    }

    record Tagged(int tag, Object value) implements JsonSerializable {

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

    record IntegerMap(Map<Integer, Integer> value) implements JsonSerializable {

        @Override
        public void serialize(JsonGenerator generator, SerializerProvider provider) throws IOException {
            CBORGenerator cborGenerator = (CBORGenerator) generator;
            cborGenerator.writeStartObject(value.size());
            for (Map.Entry<Integer, Integer> entry : value.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
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
