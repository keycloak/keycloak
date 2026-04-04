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

package org.keycloak.models.utils.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Type;

/**
 * A representation of a JavaBean style property
 *
 * @param <V> the type of the properties value
 *
 * @see Properties
 */
public interface Property<V> {

    /**
     * Returns the name of the property. If the property is a field, then the field name is returned. Otherwise, if the
     * property is a method, then the name that is returned is the getter method name without the "get" or "is" prefix,
     * and a lower case first letter.
     *
     * @return The name of the property
     */
    String getName();

    /**
     * Returns the property type
     *
     * @return The property type
     */
    Type getBaseType();

    /**
     * Returns the property type
     *
     * @return The property type
     */
    Class<V> getJavaClass();

    /**
     * Get the element responsible for retrieving the property value
     *
     * @return
     */
    AnnotatedElement getAnnotatedElement();

    /**
     * Get the member responsible for retrieving the property value
     *
     * @return
     */
    Member getMember();

    /**
     * Returns the property value for the specified bean. The property to be returned is either a field or getter
     * method.
     *
     * @param bean The bean to read the property from
     *
     * @return The property value
     *
     * @throws ClassCastException if the value is not of the type V
     */
    V getValue(Object instance);

    /**
     * This method sets the property value for a specified bean to the specified value. The property to be set is either
     * a field or setter method.
     *
     * @param bean The bean containing the property to set
     * @param value The new property value
     */
    void setValue(Object instance, V value);

    /**
     * Returns the class that declares the property
     *
     * @return
     */
    Class<?> getDeclaringClass();

    /**
     * Indicates whether this is a read-only property
     *
     * @return
     */
    boolean isReadOnly();

    /**
     * Calls the setAccessible method on the underlying member(s).
     * <p/>
     * The operation should be performed within a {@link PrivilegedAction}
     */
    void setAccessible();

    /**
     * Indicates whether the given <code>annotation</code> is defined for this property. This method will consider
     * the annotations present in both field and accessor method.
     *
     * @param annotation The Annotation to check.
     *
     * @return True if the annotation is defined. Otherwise is false.
     */
    boolean isAnnotationPresent(Class<? extends Annotation> annotation);
}
