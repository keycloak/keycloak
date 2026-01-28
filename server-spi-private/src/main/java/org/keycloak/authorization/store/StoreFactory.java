/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.store;

import org.keycloak.provider.Provider;

/**
 * <p>A factory for the different types of storages that manage the persistence of the domain model types.
 *
 * <p>Implementations of this interface are usually related with the creation of those storage types accordingly with a
 * specific persistence mechanism such as relational and NoSQL databases, filesystem, etc.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface StoreFactory extends Provider {

    /**
     * Returns a {@link ResourceStore}.
     *
     * @return the resource store
     */
    ResourceStore getResourceStore();

    /**
     * Returns a {@link ResourceServerStore}.
     *
     * @return the resource server store
     */
    ResourceServerStore getResourceServerStore();

    /**
     * Returns a {@link ScopeStore}.
     *
     * @return the scope store
     */
    ScopeStore getScopeStore();

    /**
     * Returns a {@link PolicyStore}.
     *
     * @return the policy store
     */
    PolicyStore getPolicyStore();

    /**
     * Returns a {@link PermissionTicketStore}.
     *
     * @return the permission ticket store
     */
    PermissionTicketStore getPermissionTicketStore();

    /**
     * Sets whether or not changes to instances returned from this factory are supported. Once marked as read-only, any attempt to
     * change state will throw an {@link IllegalStateException}.
     *
     * @param readOnly if true, changes are not supported
     */
    void setReadOnly(boolean readOnly);

    /**
     * Indicates if instances returned from storage are read-only.
     *
     * @return if true, instances only support reads.
     */
    boolean isReadOnly();
}
