/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.validate.validators;

import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.validate.ValidatorConfig;

/**
 * 
 * Validate input being integer number {@link Integer} or {@link Long}. Accepts String also if convertible to
 * {@link Long} by common {@link Long#parseLong(String)} operation.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 */
public class IntegerValidator extends AbstractNumberValidator implements ConfiguredProvider {

    public static final String ID = "integer";
    public static final IntegerValidator INSTANCE = new IntegerValidator();

    public IntegerValidator() {
        super();
    }

    public IntegerValidator(ValidatorConfig config) {
        super(config);
    }

    @Override
    protected Number convert(Object value, ValidatorConfig config) {
        if (value instanceof Integer || value instanceof Long) {
            return (Number) value;
        }
        return new Long(value.toString());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected Number getMinMaxConfig(ValidatorConfig config, String key) {
        return config != null ? config.getLong(key) : null;
    }

    @Override
    protected boolean isFirstGreaterThanToSecond(Number n1, Number n2) {
        return n1.longValue() > n2.longValue();
    }
    
    @Override
    public String getHelpText() {
        return "Validator to check Integer number format and optionally min and max values";
    }

}
