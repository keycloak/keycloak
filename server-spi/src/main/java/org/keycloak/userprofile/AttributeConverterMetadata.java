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

import org.keycloak.convert.Converter;
import org.keycloak.convert.ConverterConfig;
import org.keycloak.convert.ConverterContext;
import org.keycloak.convert.Converters;
import org.keycloak.models.KeycloakSession;

/**
 * Binds a {@link Converter} to its per-attribute configuration and applies it to attribute values before validation.
 */
public final class AttributeConverterMetadata {

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
     * @return the converted value
     */
    public Object convert(KeycloakSession session, Object input) {
        Converter converter = Converters.converter(session, converterId);
        if (converter == null) {
            throw new RuntimeException("No converter with id " + converterId + " found to convert UserProfile attribute in realm "
                    + (session != null && session.getContext().getRealm() != null ? session.getContext().getRealm().getName() : "unknown"));
        }
        return converter.convert(input, new ConverterContext(session), converterConfig);
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
