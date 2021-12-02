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

import java.util.Collections;
import java.util.Set;

/**
 * Configuration of permissions for the attribute
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class UPAttributePermissions {

    private Set<String> view = Collections.emptySet();
    private Set<String> edit = Collections.emptySet();

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

}
