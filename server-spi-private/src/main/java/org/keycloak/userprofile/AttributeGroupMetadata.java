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

package org.keycloak.userprofile;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration of the attribute group.
 *
 * @author <a href="joerg.matysiak@bosch.io">Jörg Matysiak</a>
 */
public class AttributeGroupMetadata {

    private String name;
    private String displayHeader;
    private String displayDescription;
    private Map<String, Object> annotations;

    public AttributeGroupMetadata(String name, String displayHeader, String displayDescription, Map<String, Object> annotations) {
        this.name = name;
        this.displayHeader = displayHeader;
        this.displayDescription = displayDescription;
        if (annotations != null) {
            addAnnotations(annotations);
        }
    }

    public String getName() {
        return name;
    }

    public AttributeGroupMetadata setName(String name) {
        this.name = name != null ? name.trim() : null;
        return this;
    }

    public String getDisplayHeader() {
        return displayHeader;
    }

    public AttributeGroupMetadata setDisplayHeader(String displayHeader) {
        this.displayHeader = displayHeader;
        return this;
    }

    public String getDisplayDescription() {
        return displayDescription;
    }

    public AttributeGroupMetadata setDisplayDescription(String displayDescription) {
        this.displayDescription = displayDescription;
        return this;
    }

    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    public AttributeGroupMetadata addAnnotations(Map<String, Object> annotations) {
        if(annotations != null) {
            if(this.annotations == null) {
                this.annotations = new HashMap<>();
            }

            this.annotations.putAll(annotations);
        }
        return this;
    }

    public AttributeGroupMetadata clone() {
        return new AttributeGroupMetadata(name, displayHeader, displayDescription, annotations);
    }
}
