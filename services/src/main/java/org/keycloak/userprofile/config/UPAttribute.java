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
package org.keycloak.userprofile.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration of the Attribute.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class UPAttribute {

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

    @Override
    public String toString() {
        return "UPAttribute [name=" + name + ", displayName=" + displayName + ", permissions=" + permissions + ", selector=" + selector + ", required=" + required + ", validations=" + validations + ", annotations=" + annotations + "]";
    }
}
