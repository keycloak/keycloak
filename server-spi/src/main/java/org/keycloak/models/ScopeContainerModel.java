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

package org.keycloak.models;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ScopeContainerModel {

    /**
     * @deprecated Use {@link #getScopeMappingsStream() getScopeMappingsStream} instead.
     */
    @Deprecated
    default Set<RoleModel> getScopeMappings() {
        return getScopeMappingsStream().collect(Collectors.toSet());
    }

    /**
     * Returns scope mappings for this scope container as a stream.
     * @return Stream of {@link RoleModel}. Never returns {@code null}.
     */
    Stream<RoleModel> getScopeMappingsStream();

    /**
     * From the scope mappings returned by {@link #getScopeMappings()} returns only those
     * that belong to the realm that owns this scope container.
     * @return set of {@link RealmModel}
     * @deprecated Use {@link #getRealmScopeMappingsStream() getRealmScopeMappingsStream} instead.
     */
    @Deprecated
    default Set<RoleModel> getRealmScopeMappings() {
        return getRealmScopeMappingsStream().collect(Collectors.toSet());
    }

    /**
     * From the scope mappings returned by {@link #getScopeMappingsStream()} returns only those
     * that belong to the realm that owns this scope container.
     * @return stream of {@link RoleModel}. Never returns {@code null}.
     */
    Stream<RoleModel> getRealmScopeMappingsStream();

    void addScopeMapping(RoleModel role);

    void deleteScopeMapping(RoleModel role);

    /**
     * Returns {@code true}, if this object has the given role directly in its scope.
     *
     * @param role the role
     * @return see description
     * @see #hasScope(RoleModel) if you want to check whether this object has the given role directly or indirectly in
     *      its scope
     */
    default boolean hasDirectScope(RoleModel role) {
        return getScopeMappingsStream().anyMatch(r -> Objects.equals(r, role));
    }

    /**
     * Returns {@code true}, if this object has the given role directly or indirectly in its scope, {@code false}
     * otherwise.
     *
     * @param role the role
     * @return see description
     * @see #hasDirectScope(RoleModel) if you want to check if this object has the given role directly in its scope
     */
    boolean hasScope(RoleModel role);

}
