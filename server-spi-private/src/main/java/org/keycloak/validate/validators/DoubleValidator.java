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
 * Validate input being any kind of {@link Number}. Accepts String also if convertible to {@link Double} by common
 * {@link Double#parseDouble(String)}. Min and Max validation is based on {@link Double} precision also.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 */
public class DoubleValidator extends AbstractNumberValidator implements ConfiguredProvider {

    public static final String ID = "double";

    public static final DoubleValidator INSTANCE = new DoubleValidator();

    public DoubleValidator() {
        super();
    }

    public DoubleValidator(ValidatorConfig config) {
        super(config);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected Number convert(Object value, ValidatorConfig config) {
        if (value instanceof Number) {
            return (Number) value;
        }
        return Double.valueOf(value.toString());
    }

    @Override
    protected Number getMinMaxConfig(ValidatorConfig config, String key) {
        return config != null ? config.getDouble(key) : null;
    }

    @Override
    protected boolean isFirstGreaterThanToSecond(Number n1, Number n2) {
        return n1.doubleValue() > n2.doubleValue();
    }
    
    @Override
    public String getHelpText() {
        return "Validator to check Double number format and optionally min and max values";
    }

}
