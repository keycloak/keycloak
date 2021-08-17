/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import java.util.List;
import java.util.stream.Stream;

public class HotRodEntityDescriptor<EntityType> {
    private final Class<?> modelTypeClass;
    private final Class<EntityType> entityTypeClass;
    private final List<Class<?>> hotRodClasses;
    private final String cacheName;

    public HotRodEntityDescriptor(Class<?> modelTypeClass, Class<EntityType> entityTypeClass, List<Class<?>> hotRodClasses, String cacheName) {
        this.modelTypeClass = modelTypeClass;
        this.entityTypeClass = entityTypeClass;
        this.hotRodClasses = hotRodClasses;
        this.cacheName = cacheName;
    }

    public Class<?> getModelTypeClass() {
        return modelTypeClass;
    }

    public Class<EntityType> getEntityTypeClass() {
        return entityTypeClass;
    }

    public Stream<Class<?>> getHotRodClasses() {
        return hotRodClasses.stream();
    }

    public String getCacheName() {
        return cacheName;
    }
}
