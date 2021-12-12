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

package org.keycloak.models.map.storage.hotRod.common;

import org.keycloak.models.map.storage.ModelEntityUtil;

import java.util.function.Function;

public class HotRodEntityDescriptor<E, D extends HotRodEntityDelegate<E>> {
    private final Class<?> modelTypeClass;
    private final Class<E> entityTypeClass;
    private final Function<E, D> hotRodDelegateProvider;

    public HotRodEntityDescriptor(Class<?> modelTypeClass, Class<E> entityTypeClass, Function<E, D> hotRodDelegateProvider) {
        this.modelTypeClass = modelTypeClass;
        this.entityTypeClass = entityTypeClass;
        this.hotRodDelegateProvider = hotRodDelegateProvider;
    }

    public Class<?> getModelTypeClass() {
        return modelTypeClass;
    }

    public Class<E> getEntityTypeClass() {
        return entityTypeClass;
    }

    public String getCacheName() {
        return ModelEntityUtil.getModelName(modelTypeClass);
    }

    public Function<E, D> getHotRodDelegateProvider() {
        return hotRodDelegateProvider;
    }
}
