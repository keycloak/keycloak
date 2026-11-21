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
package org.keycloak.convert.converters;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.convert.AbstractStringConverter;
import org.keycloak.convert.ConverterConfig;
import org.keycloak.convert.ConverterContext;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Trims leading and/or trailing whitespace from a String value. The same characters recognized by
 * {@link String#strip()} (Unicode whitespace) are removed. Non-string values are returned unchanged.
 * <p>
 * Configuration options (all optional, default to {@literal true}):
 * <ul>
 * <li>{@link #KEY_TRIM_LEADING} - remove leading whitespace</li>
 * <li>{@link #KEY_TRIM_TRAILING} - remove trailing whitespace</li>
 * </ul>
 * <p>
 * This converter addresses, among others, the case where a user registers with a username or email that differs from
 * an existing account only by surrounding whitespace, which would otherwise create a distinct account.
 */
public class TrimWhitespaceConverter extends AbstractStringConverter implements ConfiguredProvider {

    public static final TrimWhitespaceConverter INSTANCE = new TrimWhitespaceConverter();

    public static final String ID = "trim-whitespace";

    public static final String KEY_TRIM_LEADING = "trim-leading";
    public static final String KEY_TRIM_TRAILING = "trim-trailing";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(KEY_TRIM_LEADING);
        property.setLabel("Trim leading whitespace");
        property.setHelpText("Remove whitespace at the beginning of the value. Defaults to true.");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(Boolean.TRUE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(KEY_TRIM_TRAILING);
        property.setLabel("Trim trailing whitespace");
        property.setHelpText("Remove whitespace at the end of the value. Defaults to true.");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(Boolean.TRUE);
        configProperties.add(property);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected String doConvert(String value, ConverterContext context, ConverterConfig config) {
        boolean trimLeading = config.getBooleanOrDefault(KEY_TRIM_LEADING, Boolean.TRUE);
        boolean trimTrailing = config.getBooleanOrDefault(KEY_TRIM_TRAILING, Boolean.TRUE);

        if (trimLeading && trimTrailing) {
            return value.strip();
        }
        if (trimLeading) {
            return value.stripLeading();
        }
        if (trimTrailing) {
            return value.stripTrailing();
        }
        return value;
    }

    @Override
    public String getHelpText() {
        return "Removes leading and/or trailing whitespace from the attribute value.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
}
