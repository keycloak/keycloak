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
package org.keycloak.models.map.common.delegate;

import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.UpdatableEntity;

/**
 *
 * @author hmlnarik
 */
public class SimpleDelegateProvider<T extends UpdatableEntity> implements DelegateProvider<T> {

    private final T delegate;

    public SimpleDelegateProvider(T delegate) {
        this.delegate = delegate;
    }

    @Override
    public T getDelegate(boolean isRead, Enum<? extends EntityField<T>> field, Object... parameters) {
        return this.delegate;
    }

    @Override
    public boolean isUpdated() {
        return this.delegate.isUpdated();
    }
}
