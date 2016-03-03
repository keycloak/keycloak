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

import java.util.HashSet;
import java.util.Set;

import com.mongodb.BasicDBList;
import org.keycloak.connections.mongo.api.types.Mapper;
import org.keycloak.connections.mongo.api.types.MapperContext;
import org.keycloak.connections.mongo.api.types.MapperRegistry;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BasicDBListToSetMapper implements Mapper<BasicDBList, Set> {

    private final MapperRegistry mapperRegistry;

    public BasicDBListToSetMapper(MapperRegistry mapperRegistry) {
        this.mapperRegistry = mapperRegistry;
    }

    @Override
    public Set convertObject(MapperContext<BasicDBList, Set> context) {
        BasicDBList dbList = context.getObjectToConvert();
        Set<Object> appObjects = new HashSet<Object>();
        Class<?> expectedListElementType = (Class<?>) context.getGenericTypes().get(0);

        for (Object dbObject : dbList) {
            MapperContext<Object, Object> newContext = new MapperContext<Object, Object>(dbObject, expectedListElementType, null);
            appObjects.add(mapperRegistry.convertDBObjectToApplicationObject(newContext));
        }
        return appObjects;
    }

    @Override
    public Class<? extends BasicDBList> getTypeOfObjectToConvert() {
        return BasicDBList.class;
    }

    @Override
    public Class<Set> getExpectedReturnType() {
        return Set.class;
    }
}
