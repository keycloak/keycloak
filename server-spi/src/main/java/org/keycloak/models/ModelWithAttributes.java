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

package org.keycloak.models;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Represents model with attributes
 */
public interface ModelWithAttributes {

    /**
     * Set single value of specified attribute. Remove all other existing values.
     *
     * @param name String name of the attribute
     * @param value String value of the attribute
     */
    default void setSingleAttribute(String name, String value) {
        setAttribute(name, Collections.singletonList(value));
    }

    /**
     * Set list of values as attribute. 
     *
     * @param name String name of the attribute
     * @param values List of attribute values
     */
    void setAttribute(String name, List<String> values);

    /**
     * Removes attribute with the given name.
     * 
     * @param name String name of the attribute
     */
    void removeAttribute(String name);

    /**
     * Returns first found value of the attribute with the given name.
     * 
     * @param name String name of the attribute
     * @return first value of specified attribute or {@code null} if there is not any value. Don't throw exception if there are more values of the attribute.
     */
    default String getFirstAttribute(String name) {
        return getAttributeStream(name).findFirst().orElse(null);
    }

    /**
     * Returns all model's attributes that match the given name as a stream.
     * @param name {@code String} Name of an attribute to be used as a filter.
     * @return Stream of {@code String}. Never returns {@code null}.
     */
    Stream<String> getAttributeStream(String name);

    /**
     * Returns all model's attributes as a map, where key is attribute name and value is list of attribute values.
     * @return map of attributes
     */
    Map<String, List<String>> getAttributes();

}
