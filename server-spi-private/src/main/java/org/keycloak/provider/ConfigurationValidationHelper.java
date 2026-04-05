/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.provider;

import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.utils.StringUtil;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ConfigurationValidationHelper {

    private ComponentModel model;

    private ConfigurationValidationHelper(ComponentModel model) {
        this.model = model;
    }

    public static ConfigurationValidationHelper check(ComponentModel model) {
        return new ConfigurationValidationHelper(model);
    }

    public ConfigurationValidationHelper checkInt(ProviderConfigProperty property, boolean required) throws ComponentValidationException {
        return checkInt(property.getName(), property.getLabel(), required);
    }

    public ConfigurationValidationHelper checkList(ProviderConfigProperty property, boolean required) throws ComponentValidationException {
        checkSingle(property.getName(), property.getLabel(), required);

        String value = model.getConfig().getFirst(property.getName());
        if (value != null && !property.getOptions().contains(value)) {
            String options = StringUtil.joinValuesWithLogicalCondition("or", property.getOptions());
            throw new ComponentValidationException("''{0}'' should be {1}", property.getLabel(), options);
        }

        return this;
    }

    public ConfigurationValidationHelper checkInt(String key, String label, boolean required) throws ComponentValidationException {
        checkSingle(key, label, required);

        String val = model.getConfig().getFirst(key);
        if (val != null) {
            try {
                Integer.parseInt(val);
            } catch (NumberFormatException e) {
                throw new ComponentValidationException("''{0}'' should be a number", label);
            }
        }

        return this;
    }

    public ConfigurationValidationHelper checkLong(ProviderConfigProperty property, boolean required) throws ComponentValidationException {
        return checkLong(property.getName(), property.getLabel(), required);
    }

    public ConfigurationValidationHelper checkLong(String key, String label, boolean required) throws ComponentValidationException {
        checkSingle(key, label, required);

        String val = model.getConfig().getFirst(key);
        if (val != null) {
            try {
                Long.parseLong(val);
            } catch (NumberFormatException e) {
                throw new ComponentValidationException("''{0}'' should be a number", label);
            }
        }

        return this;
    }

    public ConfigurationValidationHelper checkSingle(ProviderConfigProperty property, boolean required) throws ComponentValidationException {
        return checkSingle(property.getName(), property.getLabel(), required);
    }

    public ConfigurationValidationHelper checkSingle(String key, String label, boolean required) throws ComponentValidationException {
        if (model.getConfig().containsKey(key) && model.getConfig().get(key).size() > 1) {
            throw new ComponentValidationException("''{0}'' should be a single entry", label);
        }

        if (required) {
            checkRequired(key, label);
        }

        return this;
    }

    public ConfigurationValidationHelper checkRequired(ProviderConfigProperty property) throws ComponentValidationException {
        return checkRequired(property.getName(), property.getLabel());
    }

    public ConfigurationValidationHelper checkRequired(String key, String label) throws ComponentValidationException {
        List<String> values = model.getConfig().get(key);
        if (values == null) {
            throw new ComponentValidationException("''{0}'' is required", label);
        }

        return this;
    }

    public ConfigurationValidationHelper checkBoolean(ProviderConfigProperty property, boolean required) throws ComponentValidationException {
        return checkBoolean(property.getName(), property.getLabel(), required);
    }

    public ConfigurationValidationHelper checkBoolean(String key, String label, boolean required) {
        checkSingle(key, label, required);

        String val = model.getConfig().getFirst(key);
        if (val != null && !(val.equals("true") || val.equals("false"))) {
            throw new ComponentValidationException("''{0}'' should be ''true'' or ''false''", label);
        }

        return this;
    }
}
