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

import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.function.Supplier;

/**
 *
 * @author hmlnarik
 */
public class LazilyInitialized<T> {

    private final Supplier<T> supplier;

    private final AtomicMarkableReference<T> supplierRef = new AtomicMarkableReference<>(null, false);

    public LazilyInitialized(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (! isInitialized()) {
            supplierRef.compareAndSet(null, supplier == null ? null : supplier.get(), false, true);
        }
        return supplierRef.getReference();
    }

    /**
     * Returns {@code true} if the reference to the object has been initialized
     * @return
     */
    public boolean isInitialized() {
        return supplierRef.isMarked();
    }

}
