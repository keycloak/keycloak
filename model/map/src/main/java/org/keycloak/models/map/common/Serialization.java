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
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jdk8.StreamSerializer;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public class Serialization {

    public static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
      .enable(SerializationFeature.INDENT_OUTPUT)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)
      .setVisibility(PropertyAccessor.ALL, Visibility.NONE)
      .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
      .activateDefaultTyping(new LaissezFaireSubTypeValidator() /* TODO - see javadoc */, ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS, JsonTypeInfo.As.PROPERTY)
      .addMixIn(UpdatableEntity.class, IgnoreUpdatedMixIn.class)
      .addMixIn(DeepCloner.class, IgnoredTypeMixIn.class)
    ;

    public static final ConcurrentHashMap<Class<?>, ObjectReader> READERS = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Class<?>, ObjectWriter> WRITERS = new ConcurrentHashMap<>();

    @JsonIgnoreType
    public class IgnoredTypeMixIn {}

    public abstract class IgnoreUpdatedMixIn {
        @JsonIgnore public abstract boolean isUpdated();
    }

    static {
        JavaType type = TypeFactory.unknownType();
        JavaType streamType = MAPPER.getTypeFactory().constructParametricType(Stream.class, type);
        SimpleModule module = new SimpleModule().addSerializer(new StreamSerializer(streamType, type));
        MAPPER.registerModule(module);
    }


    public static <T> T from(T orig) {
        if (orig == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        final Class<T> origClass = (Class<T>) orig.getClass();

        // Naive solution but will do.
        try {
            ObjectReader reader = READERS.computeIfAbsent(origClass, MAPPER::readerFor);
            ObjectWriter writer = WRITERS.computeIfAbsent(origClass, MAPPER::writerFor);
            final T res;
            res = reader.readValue(writer.writeValueAsBytes(orig));

            return res;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static <T> T from(T orig, T target) {
        if (orig == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        final Class<T> origClass = (Class<T>) orig.getClass();

        // Naive solution but will do.
        try {
            ObjectReader reader = MAPPER.readerForUpdating(target);
            ObjectWriter writer = WRITERS.computeIfAbsent(origClass, MAPPER::writerFor);
            final T res;
            res = reader.readValue(writer.writeValueAsBytes(orig));

            if (res != target) {
                throw new IllegalStateException("Should clone into desired target");
            }

            return res;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
