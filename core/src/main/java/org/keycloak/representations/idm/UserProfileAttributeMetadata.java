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
package org.keycloak.representations.idm;

import java.util.Map;

/**
 * @author Vlastimil Elias <velias@redhat.com>
 */
public class UserProfileAttributeMetadata {

    private String name;
    private String displayName;
    private boolean required;
    private boolean readOnly;
    private Map<String, Object> annotations;
    private Map<String, Map<String, Object>> validators;
    private String group;
    private boolean multivalued;
    private String defaultValue;

    public UserProfileAttributeMetadata() {

    }

    public UserProfileAttributeMetadata(String name, String displayName, boolean required, boolean readOnly, String group, Map<String, Object> annotations,
            Map<String, Map<String, Object>> validators, boolean multivalued, String defaultValue) {
        this.name = name;
        this.displayName = displayName;
        this.required = required;
        this.readOnly = readOnly;
        this.annotations = annotations;
        this.validators = validators;
        this.group = group;
        this.multivalued = multivalued;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return display name, either direct string to display, or construct for i18n like <code>${i18nkey}</code>
     */
    public String getDisplayName() {
        return displayName;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public String getGroup() {
        return group;
    }

    /**
     * Get info about attribute annotations loaded from UserProfile configuration.
     */
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    /**
     * Get info about validators applied to attribute.
     * 
     * @return map where key is validatorId and value is map with configuration for given validator (loaded from UserProfile configuration)
     */
    public Map<String, Map<String, Object>> getValidators() {
        return validators;
    }

    public void setMultivalued(boolean multivalued) {
        this.multivalued = multivalued;
    }

    public boolean isMultivalued() {
        return multivalued;
    }
}
