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

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.UpdatableEntity;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.function.Supplier;

/**
 *
 * @author hmlnarik
 */
public class LazyDelegateProvider<T extends AbstractEntity> implements DelegateProvider<T> {

    protected final LazilyInitialized<T> delegateSupplier;

    public LazyDelegateProvider(Supplier<T> delegateSupplier) {
        this.delegateSupplier = new LazilyInitialized<>(delegateSupplier);
    }

    @Override
    public T getDelegate(boolean isRead, Enum<? extends EntityField<T>> field, Object... parameters) {
        T ref = delegateSupplier.get();
        if (ref == null) {
            throw new IllegalStateException("Invalid delegate obtained");
        }
        return ref;
    }

    @Override
    public boolean isUpdated() {
        if (delegateSupplier.isInitialized()) {
            T d = delegateSupplier.get();
            return d instanceof UpdatableEntity ? ((UpdatableEntity) d).isUpdated() : false;
        }
        return false;
    }
}
