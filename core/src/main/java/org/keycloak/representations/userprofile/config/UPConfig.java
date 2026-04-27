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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Configuration of the User Profile for one realm.
 *
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class UPConfig implements Cloneable {

    public enum UnmanagedAttributePolicy {

        /**
         * Unmanaged attributes are enabled and available from any context.
         */
        ENABLED,

        /**
         * Unmanaged attributes are only available as read-only and only through the management interfaces.
         */
        ADMIN_VIEW,

        /**
         * Unmanaged attributes are only available as read-write and only through the management interfaces.
         */
        ADMIN_EDIT
    }

    private List<UPAttribute> attributes;
    private List<UPGroup> groups;

    private UnmanagedAttributePolicy unmanagedAttributePolicy;

    public List<UPAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<UPAttribute> attributes) {
        this.attributes = attributes;
    }

    public UPConfig addOrReplaceAttribute(UPAttribute attribute) {
        if (attributes == null) {
            attributes = new ArrayList<>();
        }

        removeAttribute(attribute.getName());
        attributes.add(attribute);

        return this;
    }

    public boolean removeAttribute(String name) {
        return attributes != null && attributes.removeIf(attribute -> attribute.getName().equals(name));
    }

    public List<UPGroup> getGroups() {
        if (groups == null) {
            return Collections.emptyList();
        }
        return groups;
    }

    public void setGroups(List<UPGroup> groups) {
        this.groups = groups;
    }

    public UPConfig addGroup(UPGroup group) {
        if (groups == null) {
            groups = new ArrayList<>();
        }

        groups.add(group);

        return this;
    }

    @JsonIgnore
    public UPAttribute getAttribute(String name) {
        for (UPAttribute attribute : getAttributes()) {
            if (attribute.getName().equals(name)) {
                return attribute;
            }
        }
        return null;
    }

    public UnmanagedAttributePolicy getUnmanagedAttributePolicy() {
        return unmanagedAttributePolicy;
    }

    public void setUnmanagedAttributePolicy(UnmanagedAttributePolicy unmanagedAttributePolicy) {
        this.unmanagedAttributePolicy = unmanagedAttributePolicy;
    }

    @Override
    public String toString() {
        return "UPConfig [attributes=" + attributes + ", groups=" + groups + "]";
    }

    @Override
    public UPConfig clone() {
        UPConfig cfg = new UPConfig();

        cfg.setUnmanagedAttributePolicy(this.unmanagedAttributePolicy);
        if (attributes != null) {
            cfg.setAttributes(attributes.stream().map(UPAttribute::clone).collect(Collectors.toList()));
        }
        if (groups != null) {
            cfg.setGroups(groups.stream().map(UPGroup::clone).collect(Collectors.toList()));
        }

        return cfg;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, groups, unmanagedAttributePolicy);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final UPConfig other = (UPConfig) obj;
        return Objects.equals(this.attributes, other.attributes)
                && Objects.equals(this.groups, other.groups)
                && this.unmanagedAttributePolicy == other.unmanagedAttributePolicy;
    }
}
