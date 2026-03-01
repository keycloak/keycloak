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
import java.util.function.Function;

import org.keycloak.validate.ValidationError;

import static java.util.Optional.ofNullable;

/**
 * <p>This interface wraps the attributes associated with a user profile. Different operations are provided to access and
 * manage these attributes.
 *
 * <p>Attributes are classified as:</p>
 * <ul>
 * <li>Managed
 * <li>Unmanaged
 * </ul>
 *
 * <p>A <i>managed</i> attribute is any attribute defined in the user profile configuration. Therefore, they are known by
 * the server and can be managed accordingly.
 *
 * <p>A <i>unmanaged</i> attributes is any attribute <b>not</b> defined in the user profile configuration. Therefore, the server
 * does not know about them and they cannot use capabilities provided by the server. However, they can still be managed by
 * administrators by setting any of the {@link org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy}.
 *
 * <p>Any attribute available from this interface has a corresponding {@link AttributeMetadata}</p>. The metadata describes
 * the settings for a given attribute so that the server can communicate to a caller the constraints
 * (see {@link org.keycloak.representations.userprofile.config.UPConfig} and the availability of the attribute in
 * a given {@link UserProfileContext}.
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
    default String getFirst(String name) {
        List<String> values = ofNullable(get(name)).orElse(List.of());

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
    List<String> get(String name);

    /**
     * Checks whether an attribute is read-only.
     *
     * @param name the attribute name
     *
     * @return {@code true} if the attribute is read-only. Otherwise, {@code false}
     */
    boolean isReadOnly(String name);

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
     * Returns all the attributes with read-write permissions in a particular {@link UserProfileContext}.
     *
     * @return the attributes
     */
    Map<String, List<String>> getWritable();

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
     * Returns only the attributes that have read permissions in a particular {@link UserProfileContext}.
     *
     * @return the attributes with read permission.
     */
    Map<String, List<String>> getReadable();

    /**
     * Returns the attributes as a {@link Map} that are accessible to a particular {@link UserProfileContext}.
     *
     * @return a map with all the attributes
     */
    Map<String, List<String>> toMap();

    /**
     * Returns a {@link Map} holding any unmanaged attribute.
     *
     * @return a map with any unmanaged attribute
     */
    Map<String, List<String>> getUnmanagedAttributes();

    /**
     * <p>Returns the annotations for an attribute with the given {@code name}.
     *
     * <p>The annotations returned by this method might differ from those returned directly from
     * the {@link AttributeMetadata#getAnnotations()} if the implementation supports annotations
     * being resolved dynamically based on contextual data. See {@link AttributeMetadata#setAnnotationDecorator(Function)}.
     *
     * @param name the name of the attribute
     * @return the annotations
     */
    default Map<String, Object> getAnnotations(String name) {
        AttributeMetadata metadata = getMetadata(name);

        if (metadata == null) {
            return Collections.emptyMap();
        }

        return metadata.getAnnotations();
    }
}
