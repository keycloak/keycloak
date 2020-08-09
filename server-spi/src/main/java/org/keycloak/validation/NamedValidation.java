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

/**
 * Decorates a {@link Validation} with a name.
 */
public class NamedValidation extends DelegatingValidation {

    private final String name;

    public NamedValidation(ValidationRegistration registration) {
        this(registration.getName(), registration.getValidation(), registration.getValidation()::isSupported);
    }

    public NamedValidation(String name, Validation delegate, ValidationSupported supported) {
        super(delegate, supported);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "name='" + name + '\'' +
                '}';
    }
}
