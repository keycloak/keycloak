/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.convert;

import org.keycloak.provider.Provider;

/**
 * Converts (transforms) a single input value into a new value. Converters are intended to run
 * <strong>before</strong> any {@link org.keycloak.validate.Validator} so that validation operates on the prepared
 * value.
 * <p>
 * Examples of typical conversions:
 * <ul>
 *     <li>Trimming leading/trailing whitespace from a string</li>
 *     <li>Converting a date from one locale format to another</li>
 *     <li>Normalizing a value to a canonical form</li>
 * </ul>
 * <p>
 * Converters can be configured with an optional {@link ConverterConfig}.
 */
public interface Converter extends Provider {

    /**
     * Converts the given {@code input}.
     *
     * @param input the value to convert
     * @return the converted value
     */
    default Object convert(Object input) {
        return convert(input, new ConverterContext(), ConverterConfig.EMPTY);
    }

    /**
     * Converts the given {@code input} with the given {@code config}.
     *
     * @param input  the value to convert
     * @param config parameterization for the current conversion
     * @return the converted value
     */
    default Object convert(Object input, ConverterConfig config) {
        return convert(input, new ConverterContext(), config);
    }

    /**
     * Converts the given {@code input} within the given {@code context}.
     *
     * @param input   the value to convert
     * @param context the converter context
     * @return the converted value
     */
    default Object convert(Object input, ConverterContext context) {
        return convert(input, context, ConverterConfig.EMPTY);
    }

    /**
     * Converts the given {@code input} within the given {@code context} and with the given {@code config}.
     *
     * @param input   the value to convert
     * @param context the converter context
     * @param config  parameterization for the current conversion
     * @return the converted value
     */
    Object convert(Object input, ConverterContext context, ConverterConfig config);

    @Override
    default void close() {
        // NOOP
    }
}
