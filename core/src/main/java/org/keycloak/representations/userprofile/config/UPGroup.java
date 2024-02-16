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
import java.util.Map;
import java.util.Objects;

/**
 * Configuration of the attribute group.
 *
 * @author <a href="joerg.matysiak@bosch.io">Jörg Matysiak</a>
 */
public class UPGroup implements Cloneable {

    private String name;
    private String displayHeader;
    private String displayDescription;
    private Map<String, Object> annotations;

    public UPGroup() {
        // for reflection
    }

    public UPGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public String getDisplayHeader() {
        return displayHeader;
    }

    public void setDisplayHeader(String displayHeader) {
        this.displayHeader = displayHeader;
    }

    public String getDisplayDescription() {
        return displayDescription;
    }

    public void setDisplayDescription(String displayDescription) {
        this.displayDescription = displayDescription;
    }

    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, Object> annotations) {
        this.annotations = annotations;
    }

    @Override
    protected UPGroup clone() {
        UPGroup group = new UPGroup(this.name);
        group.setDisplayHeader(displayHeader);
        group.setDisplayDescription(displayDescription);
        group.setAnnotations(this.annotations == null ? null : new HashMap<>(this.annotations));
        return group;
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
        final UPGroup other = (UPGroup) obj;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.displayHeader, other.displayHeader)
                && Objects.equals(this.displayDescription, other.displayDescription)
                && Objects.equals(this.annotations, other.annotations);
    }
}
