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

import com.mongodb.BasicDBList;
import org.keycloak.connections.mongo.api.types.Mapper;
import org.keycloak.connections.mongo.api.types.MapperContext;
import org.keycloak.connections.mongo.api.types.MapperRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BasicDBListMapper implements Mapper<BasicDBList, List> {

    private final MapperRegistry mapperRegistry;

    public BasicDBListMapper(MapperRegistry mapperRegistry) {
        this.mapperRegistry = mapperRegistry;
    }

    @Override
    public List convertObject(MapperContext<BasicDBList, List> context) {
        BasicDBList dbList = context.getObjectToConvert();
        ArrayList<Object> appObjects = new ArrayList<Object>();
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
    public Class<List> getExpectedReturnType() {
        return List.class;
    }
}
