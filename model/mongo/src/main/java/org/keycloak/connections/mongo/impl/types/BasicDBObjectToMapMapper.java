/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.mongo.impl.types;

import com.mongodb.BasicDBObject;
import org.keycloak.connections.mongo.api.types.Mapper;
import org.keycloak.connections.mongo.api.types.MapperContext;
import org.keycloak.connections.mongo.api.types.MapperRegistry;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BasicDBObjectToMapMapper implements Mapper<BasicDBObject, Map> {

    private final MapperRegistry mapperRegistry;

    public BasicDBObjectToMapMapper(MapperRegistry mapperRegistry) {
        this.mapperRegistry = mapperRegistry;
    }

    @Override
    public Map convertObject(MapperContext<BasicDBObject, Map> context) {
        BasicDBObject dbObjectToConvert = context.getObjectToConvert();
        Type expectedElementValueType = context.getGenericTypes().get(1);

        HashMap<String, Object> result = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : dbObjectToConvert.entrySet()) {
            String key = entry.getKey();
            Object dbValue = entry.getValue();

            // Workaround as manually inserted numbers into mongo may be treated as "Double"
            if (dbValue instanceof Double && expectedElementValueType == Integer.class) {
                dbValue = ((Double)dbValue).intValue();
            }

            MapperContext<Object, Object> newContext = getMapperContext(dbValue, expectedElementValueType);
            Object value = mapperRegistry.convertDBObjectToApplicationObject(newContext);

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

    private MapperContext<Object, Object> getMapperContext(Object dbValue, Type expectedElementValueType) {
        if (expectedElementValueType instanceof Class) {
            Class<?> clazz = (Class<?>) expectedElementValueType;
            return new MapperContext<>(dbValue, clazz, null);
        } else if (expectedElementValueType instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) expectedElementValueType;
            Class<?> expectedClazz = (Class<?>) parameterized.getRawType();
            Type[] generics = parameterized.getActualTypeArguments();

            return new MapperContext<>(dbValue, expectedClazz, Arrays.asList(generics));
        } else {
            throw new IllegalArgumentException("Unexpected type: '" + expectedElementValueType + "' for converting " + dbValue);
        }
    }
}
