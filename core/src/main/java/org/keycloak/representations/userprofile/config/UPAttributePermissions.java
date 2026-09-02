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

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Configuration of permissions for the attribute
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class UPAttributePermissions implements Cloneable {

    private Set<String> view = Collections.emptySet();
    private Set<String> edit = Collections.emptySet();

    public UPAttributePermissions() {
        // for reflection
    }

    public UPAttributePermissions(Set<String> view, Set<String> edit) {
        this.view = view;
        this.edit = edit;
    }

    public Set<String> getView() {
        return view;
    }

    public void setView(Set<String> view) {
        this.view = view;
    }

    public Set<String> getEdit() {
        return edit;
    }

    public void setEdit(Set<String> edit) {
        this.edit = edit;
    }

    @Override
    public String toString() {
        return "UPAttributePermissions [view=" + view + ", edit=" + edit + "]";
    }

    @JsonIgnore
    public boolean isEmpty() {
        return getEdit().isEmpty() && getView().isEmpty();
    }

    @Override
    protected UPAttributePermissions clone() {
        Set<String> view = this.view == null ? null : new HashSet<>(this.view);
        Set<String> edit = this.edit == null ? null : new HashSet<>(this.edit);
        return new UPAttributePermissions(view, edit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(view, edit);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final UPAttributePermissions other = (UPAttributePermissions) obj;
        return Objects.equals(this.view, other.view)
                && Objects.equals(this.edit, other.edit);
    }
}
