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

import org.keycloak.models.map.common.UpdatableEntity;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.function.Supplier;

/**
 *
 * @author hmlnarik
 */
public class LazyDelegateProvider<T extends UpdatableEntity> implements DelegateProvider {

    private final Supplier<T> delegateSupplier;

    private final AtomicMarkableReference<T> delegate = new AtomicMarkableReference<>(null, false);

    public LazyDelegateProvider(Supplier<T> delegateSupplier) {
        this.delegateSupplier = delegateSupplier;
    }

    @Override
    public T getDelegate(boolean isRead, Object field, Object... parameters) {
        if (! isDelegateInitialized()) {
            delegate.compareAndSet(null, delegateSupplier == null ? null : delegateSupplier.get(), false, true);
        }
        T ref = delegate.getReference();
        if (ref == null) {
            throw new IllegalStateException("Invalid delegate obtained");
        }
        return ref;
    }

    protected boolean isDelegateInitialized() {
        return delegate.isMarked();
    }

    @Override
    public boolean isUpdated() {
        if (isDelegateInitialized()) {
            T d = getDelegate(true, this);
            return d.isUpdated();
        }
        return false;
    }
}
