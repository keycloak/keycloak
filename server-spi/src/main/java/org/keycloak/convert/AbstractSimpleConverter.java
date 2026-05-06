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
package org.keycloak.convert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base class for arbitrary value type converters. Functionality covered in this base class:
 * <ul>
 * <li>accepts a single value or a collection of values - collection elements are converted individually
 * <li>{@code null} values are passed through unchanged by default
 * </ul>
 */
public abstract class AbstractSimpleConverter implements SimpleConverter {

    @Override
    public Object convert(Object input, ConverterContext context, ConverterConfig config) {
        if (input instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<Object> values = (Collection<Object>) input;
            List<Object> converted = new ArrayList<>(values.size());
            for (Object value : values) {
                converted.add(convert(value, context, config));
            }
            return converted;
        }

        if (input == null) {
            return null;
        }

        return doConvert(input, context, config);
    }

    /**
     * Perform the actual conversion on a single non-null value.
     *
     * @param value   the value to convert, never {@code null}
     * @param context the converter context
     * @param config  the converter configuration
     * @return the converted value (may be {@code null} to remove the value)
     */
    protected abstract Object doConvert(Object value, ConverterContext context, ConverterConfig config);
}
