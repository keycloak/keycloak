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

import org.keycloak.provider.ProviderEvent;

import org.keycloak.storage.SearchableModelField;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface GroupModel extends RoleMapperModel {

    public static class SearchableFields {
        public static final SearchableModelField<GroupModel> ID             = new SearchableModelField<>("id", String.class);
        public static final SearchableModelField<GroupModel> REALM_ID       = new SearchableModelField<>("realmId", String.class);
        /** Parent group ID */
        public static final SearchableModelField<GroupModel> PARENT_ID      = new SearchableModelField<>("parentGroupId", String.class);
        public static final SearchableModelField<GroupModel> NAME           = new SearchableModelField<>("name", String.class);
        /**
         * Field for comparison with roles granted to this group.
         * A role can be checked for belonging only via EQ operator. Role is referred by their ID
         */
        public static final SearchableModelField<GroupModel> ASSIGNED_ROLE  = new SearchableModelField<>("assignedRole", String.class);
    }

    interface GroupRemovedEvent extends ProviderEvent {
        RealmModel getRealm();
        GroupModel getGroup();
        KeycloakSession getKeycloakSession();
    }
    
    Comparator<GroupModel> COMPARE_BY_NAME = Comparator.comparing(GroupModel::getName);

    String getId();

    String getName();

    void setName(String name);

    /**
     * Set single value of specified attribute. Remove all other existing values
     *
     * @param name
     * @param value
     */
    void setSingleAttribute(String name, String value);

    void setAttribute(String name, List<String> values);

    void removeAttribute(String name);

    /**
     * @param name
     * @return null if there is not any value of specified attribute or first value otherwise. Don't throw exception if there are more values of the attribute
     */
    String getFirstAttribute(String name);

    /**
     * @param name
     * @return list of all attribute values or empty list if there are not any values. Never return null
     * @deprecated Use {@link #getAttributeStream(String) getAttributeStream} instead.
     */
    @Deprecated
    List<String> getAttribute(String name);

    /**
     * Returns group attributes that match the given name as a stream.
     * @param name {@code String} Name of the attribute to be used as a filter.
     * @return Stream of all attribute values or empty stream if there are not any values. Never return {@code null}.
     */
    default Stream<String> getAttributeStream(String name) {
        List<String> value = this.getAttribute(name);
        return value != null ? value.stream() : Stream.empty();
    }

    Map<String, List<String>> getAttributes();

    GroupModel getParent();
    String getParentId();

    /**
     * @deprecated Use {@link #getSubGroupsStream() getSubGroupsStream} instead.
     */
    @Deprecated
    Set<GroupModel> getSubGroups();

    /**
     * Returns all sub groups for the parent group as a stream.
     * @return Stream of {@link GroupModel}. Never returns {@code null}.
     */
    default Stream<GroupModel> getSubGroupsStream() {
        Set<GroupModel> value = this.getSubGroups();
        return value != null ? value.stream() : Stream.empty();
    }

    /**
     * You must also call addChild on the parent group, addChild on RealmModel if there is no parent group
     *
     * @param group
     */
    void setParent(GroupModel group);

    /**
     * Automatically calls setParent() on the subGroup
     *
     * @param subGroup
     */
    void addChild(GroupModel subGroup);

    /**
     * Automatically calls setParent() on the subGroup
     *
     * @param subGroup
     */
    void removeChild(GroupModel subGroup);

    /**
     * The {@link GroupModel.Streams} interface makes all collection-based methods in {@link GroupModel} default by providing
     * implementations that delegate to the {@link Stream}-based variants instead of the other way around.
     * <p/>
     * It allows for implementations to focus on the {@link Stream}-based approach for processing sets of data and benefit
     * from the potential memory and performance optimizations of that approach.
     */
    interface Streams extends GroupModel, RoleMapperModel.Streams {
        @Override
        default List<String> getAttribute(String name) {
            return this.getAttributeStream(name).collect(Collectors.toList());
        }

        @Override
        Stream<String> getAttributeStream(String name);

        @Override
        default Set<GroupModel> getSubGroups() {
            return this.getSubGroupsStream().collect(Collectors.toSet());
        }

        @Override
        Stream<GroupModel> getSubGroupsStream();
    }
}
