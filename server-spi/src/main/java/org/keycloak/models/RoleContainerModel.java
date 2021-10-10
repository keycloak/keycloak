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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.keycloak.provider.ProviderEvent;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RoleContainerModel {

    interface RoleRemovedEvent extends ProviderEvent {
        RoleModel getRole();
        KeycloakSession getKeycloakSession();
    }

    String getId();

    RoleModel getRole(String name);

    RoleModel addRole(String name);

    RoleModel addRole(String id, String name);

    boolean removeRole(RoleModel role);

    /**
     * @deprecated Use {@link #getRolesStream() getRolesStream} instead.
     */
    @Deprecated
    default Set<RoleModel> getRoles() {
        return getRolesStream().collect(Collectors.toSet());
    }

    /**
     * Returns available roles as a stream.
     * @return Stream of {@link RoleModel}. Never returns {@code null}.
     */
    Stream<RoleModel> getRolesStream();

    /**
     * @deprecated Use {@link #getRolesStream(Integer, Integer) getRolesStream} instead.
     */
    @Deprecated
    default Set<RoleModel> getRoles(Integer firstResult, Integer maxResults) {
        return getRolesStream(firstResult, maxResults).collect(Collectors.toSet());
    }

    /**
     * Returns available roles as a stream.
     * @param firstResult {@code Integer} Index of the first desired role. Ignored if negative or {@code null}.
     * @param maxResults {@code Integer} Maximum number of returned roles. Ignored if negative or {@code null}.
     * @return Stream of {@link RoleModel}. Never returns {@code null}.
     */
    Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults);

    /**
     * @deprecated Use {@link #searchForRolesStream(String, Integer, Integer) searchForRolesStream} instead.
     */
    @Deprecated
    default Set<RoleModel> searchForRoles(String search, Integer first, Integer max) {
        return searchForRolesStream(search, first, max).collect(Collectors.toSet());
    }

    /**
     * Searches roles by the given name. Returns all roles that match the given filter.
     * @param search {@code String} Name of the role to be used as a filter.
     * @param first {@code Integer} Index of the first desired role. Ignored if negative or {@code null}.
     * @param max {@code Integer} Maximum number of returned roles. Ignored if negative or {@code null}.
     * @return Stream of {@link RoleModel}. Never returns {@code null}.
     */
    Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max);

    /**
     * @deprecated Default roles are now managed by {@link org.keycloak.models.RealmModel#getDefaultRole()}. This method will be removed.
     * @return List of default roles names or empty list if there are none. Never returns {@code null}.
     */
    @Deprecated
    default List<String> getDefaultRoles() {
        Stream<String> defaultRolesStream = getDefaultRolesStream();
        if (defaultRolesStream != null) {
            return defaultRolesStream.collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * @deprecated Default roles are now managed by {@link org.keycloak.models.RealmModel#getDefaultRole()}. This method will be removed.
     * @return Stream of default roles names or empty stream if there are none. Never returns {@code null}.
     */
    @Deprecated
    Stream<String> getDefaultRolesStream();

    /**
     * @deprecated Default roles are now managed by {@link org.keycloak.models.RealmModel#getDefaultRole()}. This method will be removed.
     */
    @Deprecated
    void addDefaultRole(String name);

    /**
     * @deprecated Default roles are now managed by {@link org.keycloak.models.RealmModel#getDefaultRole()}. This method will be removed.
     */
    @Deprecated
    default void updateDefaultRoles(String... defaultRoles) {
        List<String> defaultRolesArray = Arrays.asList(defaultRoles);
        Collection<String> entities = getDefaultRolesStream().collect(Collectors.toList());
        Set<String> already = new HashSet<>();
        ArrayList<String> remove = new ArrayList<>();
        for (String rel : entities) {
            if (! defaultRolesArray.contains(rel)) {
                remove.add(rel);
            } else {
                already.add(rel);
            }
        }
        removeDefaultRoles(remove.toArray(new String[] {}));

        for (String roleName : defaultRoles) {
            if (!already.contains(roleName)) {
                addDefaultRole(roleName);
            }
        }
    }

    /**
     * @deprecated Default roles are now managed by {@link org.keycloak.models.RealmModel#getDefaultRole()}. This method will be removed.
     */
    @Deprecated
    void removeDefaultRoles(String... defaultRoles);

}
