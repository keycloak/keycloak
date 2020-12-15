/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author hmlnarik
 */
public class Serialization {

    public static final ObjectMapper MAPPER = new ObjectMapper();
    public static final ConcurrentHashMap<Class<?>, ObjectReader> READERS = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Class<?>, ObjectWriter> WRITERS = new ConcurrentHashMap<>();

    abstract class IgnoreUpdatedMixIn { @JsonIgnore public abstract boolean isUpdated(); }

    static {
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        MAPPER.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        MAPPER.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

        MAPPER.addMixIn(AbstractEntity.class, IgnoreUpdatedMixIn.class);
    }


    public static <T extends AbstractEntity> T from(T orig) {
        if (orig == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        final Class<T> origClass = (Class<T>) orig.getClass();

        // Naive solution but will do.
        try {
            ObjectReader reader = READERS.computeIfAbsent(origClass, MAPPER::readerFor);
            ObjectWriter writer = WRITERS.computeIfAbsent(origClass, MAPPER::writerFor);
            final T res = reader.readValue(writer.writeValueAsBytes(orig));
            return res;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
