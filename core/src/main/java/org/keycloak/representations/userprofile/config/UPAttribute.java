/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.userprofile.config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration of the Attribute.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class UPAttribute implements Cloneable {

    private String name;
    private String displayName;
    /** key in the Map is name of the validator, value is its configuration */
    private Map<String, Map<String, Object>> validations;
    private Map<String, Object> annotations;
    /** null means it is not required */
    private UPAttributeRequired required;
    /** null means everyone can view and edit the attribute */
    private UPAttributePermissions permissions;
    /** null means it is always selected */
    private UPAttributeSelector selector;
    private String group;
    private boolean multivalued;
    private String defaultValue;

    public UPAttribute() {
    }

    public UPAttribute(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public UPAttribute(String name, UPGroup group) {
        this(name);
        this.group = group.getName();
    }

    public UPAttribute(String name, UPAttributePermissions permissions, UPAttributeRequired required, UPAttributeSelector selector) {
        this(name);
        this.permissions = permissions;
        this.required = required;
        this.selector = selector;
    }

    public UPAttribute(String name, UPAttributePermissions permissions, UPAttributeRequired required) {
        this(name, permissions, required, null);
    }

    public UPAttribute(String name, UPAttributePermissions permissions) {
        this(name, permissions, null);
    }

    public UPAttribute(String name, boolean multivalued, UPAttributePermissions permissions) {
        this(name, permissions, null);
        setMultivalued(multivalued);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public Map<String, Map<String, Object>> getValidations() {
        return validations;
    }

    public void setValidations(Map<String, Map<String, Object>> validations) {
        this.validations = validations;
    }

    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, Object> annotations) {
        this.annotations = annotations;
    }

    public UPAttributeRequired getRequired() {
        return required;
    }

    public void setRequired(UPAttributeRequired required) {
        this.required = required;
    }

    public UPAttributePermissions getPermissions() {
        return permissions;
    }

    public void setPermissions(UPAttributePermissions permissions) {
        this.permissions = permissions;
    }

    public void addValidation(String validator, Map<String, Object> config) {
        if (validations == null) {
            validations = new HashMap<>();
        }
        validations.put(validator, config);
    }

    public UPAttributeSelector getSelector() {
        return selector;
    }

    public void setSelector(UPAttributeSelector selector) {
        this.selector = selector;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group != null ? group.trim() : null;
    }

    public void setMultivalued(boolean multivalued) {
        this.multivalued = multivalued;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isMultivalued() {
        return multivalued;
    }

    @Override
    public String toString() {
        return "UPAttribute [name=" + name + ", displayName=" + displayName + ", permissions=" + permissions + ", selector=" + selector + ", required=" + required + ", validations=" + validations + ", annotations=" + annotations + ", group=" + group + ", multivalued=" + multivalued + ", defaultValue=" + defaultValue + "]";
    }

    @Override
    protected UPAttribute clone() {
        UPAttribute attr = new UPAttribute(this.name);
        attr.setDisplayName(this.displayName);

        Map<String, Map<String, Object>> validations;
        if (this.validations == null) {
            validations = null;
        } else {
            validations = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> entry : this.validations.entrySet()) {
                Map<String, Object> newVal = entry.getValue() == null ? null : new LinkedHashMap<>(entry.getValue());
                validations.put(entry.getKey(), newVal);
            }
        }
        attr.setValidations(validations);

        attr.setAnnotations(this.annotations == null ? null : new HashMap<>(this.annotations));
        attr.setRequired(this.required == null ? null : this.required.clone());
        attr.setPermissions(this.permissions == null ? null : this.permissions.clone());
        attr.setSelector(this.selector == null ? null : this.selector.clone());
        attr.setGroup(this.group);
        attr.setMultivalued(this.multivalued);
        attr.setDefaultValue(this.defaultValue);
        return attr;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final UPAttribute other = (UPAttribute) obj;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.displayName, other.displayName)
                && Objects.equals(this.group, other.group)
                && Objects.equals(this.validations, other.validations)
                && Objects.equals(this.annotations, other.annotations)
                && Objects.equals(this.required, other.required)
                && Objects.equals(this.permissions, other.permissions)
                && Objects.equals(this.selector, other.selector)
                && Objects.equals(this.multivalued, other.multivalued)
                && Objects.equals(this.defaultValue, other.defaultValue);
    }
}
