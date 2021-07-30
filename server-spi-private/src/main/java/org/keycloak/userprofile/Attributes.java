/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.userprofile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.keycloak.models.UserModel;
import org.keycloak.validate.ValidationError;

/**
 * <p>This interface wraps the attributes associated with a user profile. Different operations are provided to access and
 * manage these attributes.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface Attributes {

    /**
     * Default value for attributes with no value set.
     */
    List<String> EMPTY_VALUE = Collections.emptyList();

    /**
     * Returns the first value associated with the attribute with the given {@name}.
     *
     * @param name the name of the attribute
     *
     * @return the first value
     */
    default String getFirstValue(String name) {
        List<String> values = getValues(name);

        if (values.isEmpty()) {
            return null;
        }

        return values.get(0);
    }

    /**
     * Returns all values for an attribute with the given {@code name}.
     *
     * @param name the name of the attribute
     *
     * @return the attribute values
     */
    List<String> getValues(String name);

    /**
     * Checks whether an attribute is read-only.
     *
     * @param key
     *
     * @return
     */
    boolean isReadOnly(String key);

    /**
     * Validates the attribute with the given {@code name}.
     *
     * @param name the name of the attribute
     * @param listeners the listeners for listening for errors. <code>ValidationError.inputHint</code> contains name of the attribute in error. 
     *
     * @return {@code true} if validation is successful. Otherwise, {@code false}. In case there is no attribute with the given {@code name},
     * {@code false} is also returned but without triggering listeners
     */
    boolean validate(String name, Consumer<ValidationError>... listeners);

    /**
     * Checks whether an attribute with the given {@code name} is defined.
     *
     * @param name the name of the attribute
     *
     * @return {@code true} if the attribute is defined. Otherwise, {@code false}
     */
    boolean contains(String name);

    /**
     * Returns the names of all defined attributes.
     *
     * @return the set of attribute names
     */
    Set<String> nameSet();

    /**
     * Returns all attributes defined.
     *
     * @return the attributes
     */
    Set<Map.Entry<String, List<String>>> attributeSet();

    /**
     * <p>Returns the metadata associated with the attribute with the given {@code name}.
     *
     * <p>The {@link AttributeMetadata} is a copy of the original metadata. The original metadata
     * keeps immutable.
     *
     * @param name the attribute name
     * @return the metadata
     */
    AttributeMetadata getMetadata(String name);

    /**
     * Returns whether the attribute with the given {@code name} is required.
     *
     * @param name the attribute name
     * @return {@code true} if the attribute is required. Otherwise, {@code false}.
     */
    boolean isRequired(String name);

    /**
     * Similar to {{@link #getReadable(boolean)}} but with the possibility to add or remove
     * the root attributes.
     *
     * @param includeBuiltin if the root attributes should be included.
     * @return the attributes with read/write permission.
     */
    default Map<String, List<String>> getReadable(boolean includeBuiltin) {
        return getReadable().entrySet().stream().filter(entry -> {
            if (includeBuiltin) {
                return true;
            }
            return !isRootAttribute(entry.getKey());
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Returns only the attributes that have read/write permissions.
     *
     * @return the attributes with read/write permission.
     */
    Map<String, List<String>> getReadable();

    /**
     * Returns whether the attribute with the given {@code name} is a root attribute.
     *
     * @param name the attribute name
     * @return
     */
    default boolean isRootAttribute(String name) {
        return UserModel.USERNAME.equals(name)
                || UserModel.EMAIL.equals(name)
                || UserModel.FIRST_NAME.equals(name)
                || UserModel.LAST_NAME.equals(name);
    }

    Map<String, List<String>> toMap();
}
