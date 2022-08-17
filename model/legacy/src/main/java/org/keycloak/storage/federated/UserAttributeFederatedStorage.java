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
package org.keycloak.storage.federated;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.RealmModel;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserAttributeFederatedStorage {
    void setSingleAttribute(RealmModel realm, String userId, String name, String value);
    void setAttribute(RealmModel realm, String userId, String name, List<String> values);
    void removeAttribute(RealmModel realm, String userId, String name);
    MultivaluedHashMap<String, String> getAttributes(RealmModel realm, String userId);

    /**
     * @deprecated Use {@link #getUsersByUserAttributeStream(RealmModel, String, String) getUsersByUserAttributeStream} instead.
     */
    @Deprecated
    List<String> getUsersByUserAttribute(RealmModel realm, String name, String value);

    /**
     * Searches for federated users that have an attribute with the specified {@code name} and {@code value}.
     *
     * @param realm a reference to the realm.
     * @param name the attribute name.
     * @param value the attribute value.
     * @return a non-null {@link Stream} of users that match the search criteria.
     */
    default Stream<String> getUsersByUserAttributeStream(RealmModel realm, String name, String value) {
        List<String> users = this.getUsersByUserAttribute(realm, name, value);
        return users != null ? users.stream() : Stream.empty();
    }

    /**
     * The {@link Streams} interface makes all collection-based methods in {@link UserAttributeFederatedStorage}
     * default by providing implementations that delegate to the {@link Stream}-based variants instead of the other way
     * around.
     * <p/>
     * It allows for implementations to focus on the {@link Stream}-based approach for processing sets of data and benefit
     * from the potential memory and performance optimizations of that approach.
     */
    interface Streams extends UserAttributeFederatedStorage {

        @Override
        default List<String> getUsersByUserAttribute(RealmModel realm, String name, String value) {
            return this.getUsersByUserAttributeStream(realm, name, value).collect(Collectors.toList());
        }

        @Override
        Stream<String> getUsersByUserAttributeStream(RealmModel realm, String name, String value);
    }
}
