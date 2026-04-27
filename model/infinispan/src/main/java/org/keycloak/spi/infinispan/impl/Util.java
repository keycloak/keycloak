/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.spi.infinispan.impl;

import org.keycloak.config.Option;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * Utility method for this package and subpackages
 */
public final class Util {

    private Util() {
    }

    /**
     * Copies the {@link Option} information into the {@link ProviderConfigurationBuilder}.
     *
     * @param builder  The property to set/configure.
     * @param name     The desired property name.
     * @param label    The label of the property's argument.
     * @param type     The type of the property's value.
     * @param option   The source {@link Option} to gather the information.
     * @param isSecret {@code true} if the property is a secret.
     */
    public static void copyFromOption(ProviderConfigurationBuilder builder, String name, String label, String type, Option<?> option, boolean isSecret) {
        var property = builder.property()
                .name(name)
                .helpText(option.getDescription())
                .label(label)
                .type(type)
                .secret(isSecret);
        option.getDefaultValue().ifPresent(property::defaultValue);
        property.options(option.getExpectedValues());
        property.add();
    }

}
