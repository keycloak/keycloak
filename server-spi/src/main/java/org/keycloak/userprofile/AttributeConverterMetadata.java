/*
 *
 *  * Copyright 2025  Red Hat, Inc. and/or its affiliates
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

import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

import org.keycloak.convert.Converter;
import org.keycloak.convert.ConverterConfig;
import org.keycloak.convert.ConverterContext;
import org.keycloak.convert.Converters;
import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

/**
 * Binds a {@link Converter} to its per-attribute configuration and applies it to attribute values before validation.
 */
public final class AttributeConverterMetadata {

    private static final Logger logger = Logger.getLogger(AttributeConverterMetadata.class);

    private final String converterId;
    private final ConverterConfig converterConfig;

    public AttributeConverterMetadata(String converterId) {
        this.converterId = converterId;
        this.converterConfig = ConverterConfig.configFromMap(null);
    }

    public AttributeConverterMetadata(String converterId, ConverterConfig converterConfig) {
        this.converterId = converterId;
        this.converterConfig = converterConfig;
    }

    /**
     * @return the converter id
     */
    public String getConverterId() {
        return converterId;
    }

    /**
     * Get converter configuration as map.
     *
     * @return never null
     */
    public Map<String, Object> getConverterConfig() {
        return converterConfig.asMap();
    }

    /**
     * Converts the given value using the referenced {@link Converter} within the given {@link KeycloakSession}.
     *
     * @param session the session in which the conversion is performed
     * @param input   the value to convert
     * @return the converted value, or the unchanged {@code input} if the converter is not available
     */
    public Object convert(KeycloakSession session, Object input) {
        return bind(new ConverterContext(session)).apply(input);
    }

    /**
     * Resolves the referenced {@link Converter} once and returns a function that applies it with this converter's
     * configuration within the given {@code context}. Use it to convert many values without looking up the
     * converter for each one.
     * <p>
     * If the converter is not available (for example, a custom provider was removed after it was configured),
     * a warning is logged and the returned function passes values through unchanged. Conversion runs whenever
     * attributes are read, so failing here would make the user profile of the whole realm unusable.
     *
     * @param context the context in which the conversion is performed
     * @return a function converting a single value
     */
    public UnaryOperator<Object> bind(ConverterContext context) {
        KeycloakSession session = context.getSession();
        Converter converter = Converters.converter(session, converterId);
        if (converter == null) {
            logger.warnf("No converter with id '%s' found to convert user profile attributes in realm '%s'. Values are not converted.",
                    converterId, session.getContext().getRealm() != null ? session.getContext().getRealm().getName() : "unknown");
            return UnaryOperator.identity();
        }
        return input -> converter.convert(input, context, converterConfig);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof AttributeConverterMetadata)) return false;
        AttributeConverterMetadata other = (AttributeConverterMetadata) o;
        return Objects.equals(getConverterId(), other.getConverterId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(converterId);
    }
}
