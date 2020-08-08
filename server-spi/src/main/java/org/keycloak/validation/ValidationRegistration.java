/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.validation;

import java.util.Set;

/**
 * Denotes a registration of a {@link Validation} with additional meta-data.
 */
public class ValidationRegistration implements Comparable<ValidationRegistration> {

    /**
     * Denotes the key of the attribute or entity that the referenced {@link Validation} can validate.
     */
    private final ValidationKey key;

    /**
     * A logical name describing the validation.
     */
    private final String name;

    /**
     * The actual {@link Validation}
     */
    private final Validation validation;

    /**
     * Denotes the contexts in which the referenced validation can be applied.
     *
     * @see ValidationContextKey
     */
    private final Set<ValidationContextKey> contextKeys;

    /**
     * The order in case multiple validation are registered for the same attribute.
     */
    private final double order;

    /**
     * Creates a new {@link ValidationRegistration}.
     *
     * @param name
     * @param key
     * @param validation
     * @param order
     * @param contextKeys
     */
    public ValidationRegistration(String name, ValidationKey key, Validation validation, double order, Set<ValidationContextKey> contextKeys) {
        this.name = name;
        this.key = key;
        this.validation = validation;
        this.order = order;
        this.contextKeys = contextKeys;
    }

    public String getName() {
        return name;
    }

    public ValidationKey getKey() {
        return key;
    }

    public Validation getValidation() {
        return validation;
    }

    public Set<ValidationContextKey> getContextKeys() {
        return contextKeys;
    }

    public double getOrder() {
        return order;
    }

    @Override
    public int compareTo(ValidationRegistration that) {
        return Double.compare(this.getOrder(), that.getOrder());
    }

    public boolean isEligibleForContextKey(ValidationContextKey key) {

        if (key == null) {
            return false;
        }

        // check for direct validation context key match
        Set<ValidationContextKey> keys = getContextKeys();
        if (keys.contains(key)) {
            return true;
        }

        // check if parent validation context key which includes all children
        ValidationContextKey current = key.getParent();
        if (current == null) {
            return false;
        }

        int i = 0;
        do {
            i++;
            if (keys.contains(current)) {
                return true;
            }
            current = current.getParent();
            if (i > 100) { // simple guard against infinite loops in malformed configurations
                throw new IllegalStateException("loop detected in ValidationContextKey hierarchy: contextKey:" + current.getName() + " registration name: " + this.getName());
            }
        } while (current != null);

        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "key=" + key +
                ", name='" + name + '\'' +
                ", validation=" + validation +
                ", contextKeys=" + contextKeys +
                ", order=" + order +
                '}';
    }
}
