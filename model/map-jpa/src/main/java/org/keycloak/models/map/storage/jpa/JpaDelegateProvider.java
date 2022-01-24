/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa;

import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.map.common.AbstractEntity;

/**
 * Base class for all delegate providers for the JPA storage.
 *
 * Wraps the delegate so that it can be safely updated during lazy loading.
 */
public abstract class JpaDelegateProvider<T extends JpaRootEntity & AbstractEntity> {
    private T delegate;

    protected JpaDelegateProvider(T delegate) {
        this.delegate = delegate;
    }

    protected T getDelegate() {
        return delegate;
    }

    /**
     * Validates the new entity.
     *
     * Will throw {@link ModelIllegalStateException} if the entity has been deleted or changed in the meantime.
     */
    protected void setDelegate(T newDelegate) {
        if (newDelegate == null) {
            throw new ModelIllegalStateException("Unable to retrieve entity: " + delegate.getClass().getName() + "#" + delegate.getId());
        }
        if (newDelegate.getVersion() != delegate.getVersion()) {
            throw new ModelIllegalStateException("Version of entity changed between two loads: " + delegate.getClass().getName() + "#" + delegate.getId());
        }
        this.delegate = newDelegate;
    }
}
