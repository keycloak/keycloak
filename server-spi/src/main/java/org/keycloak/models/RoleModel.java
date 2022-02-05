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

import org.keycloak.storage.SearchableModelField;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RoleModel {

    public static class SearchableFields {
        public static final SearchableModelField<RoleModel> ID                  = new SearchableModelField<>("id", String.class);
        public static final SearchableModelField<RoleModel> REALM_ID            = new SearchableModelField<>("realmId", String.class);
        /** If client role, ID of the client (not the clientId) */
        public static final SearchableModelField<RoleModel> CLIENT_ID           = new SearchableModelField<>("clientId", String.class);
        public static final SearchableModelField<RoleModel> NAME                = new SearchableModelField<>("name", String.class);
        public static final SearchableModelField<RoleModel> DESCRIPTION         = new SearchableModelField<>("description", String.class);
        public static final SearchableModelField<RoleModel> IS_CLIENT_ROLE      = new SearchableModelField<>("isClientRole", Boolean.class);
    }

    String getName();

    String getDescription();

    void setDescription(String description);

    String getId();

    void setName(String name);

    boolean isComposite();

    void addCompositeRole(RoleModel role);

    void removeCompositeRole(RoleModel role);

    /**
     * @deprecated Use {@link #getCompositesStream() getCompositesStream} instead.
     */
    @Deprecated
    default Set<RoleModel> getComposites() {
        return getCompositesStream().collect(Collectors.toSet());
    }

    /**
     * Returns all composite roles as a stream.
     * @return Stream of {@link RoleModel}. Never returns {@code null}.
     */
    default Stream<RoleModel> getCompositesStream() {
        return getCompositesStream(null, null, null);
    }

    /**
     * Returns a paginated stream of composite roles of {@code this} role that contain given string in its name.
     *
     * @param search Case-insensitive search string
     * @param first Index of the first result to return. Ignored if negative or {@code null}.
     * @param max Maximum number of results to return. Ignored if negative or {@code null}.
     * @return A stream of requested roles ordered by the role name
     */
    Stream<RoleModel> getCompositesStream(String search, Integer first, Integer max);

    boolean isClientRole();

    String getContainerId();

    RoleContainerModel getContainer();

    boolean hasRole(RoleModel role);

    void setSingleAttribute(String name, String value);

    void setAttribute(String name, List<String> values);

    void removeAttribute(String name);

    default String getFirstAttribute(String name) {
        return getAttributeStream(name).findFirst().orElse(null);
    }

    /**
     * @deprecated Use {@link #getAttributeStream(String) getAttributeStream} instead.
     */
    @Deprecated
    default List<String> getAttribute(String name) {
        return getAttributeStream(name).collect(Collectors.toList());
    }

    /**
     * Returns all role's attributes that match the given name as a stream.
     * @param name {@code String} Name of an attribute to be used as a filter.
     * @return Stream of {@code String}. Never returns {@code null}.
     */
    Stream<String> getAttributeStream(String name);

    Map<String, List<String>> getAttributes();
}
