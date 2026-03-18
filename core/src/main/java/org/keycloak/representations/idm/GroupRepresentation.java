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

package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class GroupRepresentation {
    // For an individual group these are the sufficient minimum fields
    // to identify a group and operate on it in a basic way
    protected String id;
    protected String name;
    protected String description;
    protected String path;
    protected String parentId;
    protected Long subGroupCount;
    // For navigating a hierarchy of groups, we can also include a minimum representation of subGroups
    // These aren't populated by default and are only included as-needed
    protected List<GroupRepresentation> subGroups;
    protected Map<String, List<String>>  attributes;
    protected List<String> realmRoles;
    protected Map<String, List<String>> clientRoles;

    private Map<String, Boolean> access;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Long getSubGroupCount() {
        return subGroupCount;
    }

    public void setSubGroupCount(Long subGroupCount) {
        this.subGroupCount = subGroupCount;
    }

    public List<String> getRealmRoles() {
        return realmRoles;
    }

    public void setRealmRoles(List<String> realmRoles) {
        this.realmRoles = realmRoles;
    }

    public Map<String, List<String>> getClientRoles() {
        return clientRoles;
    }

    public void setClientRoles(Map<String, List<String>> clientRoles) {
        this.clientRoles = clientRoles;
    }


    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>>  attributes) {
        this.attributes = attributes;
    }

    public GroupRepresentation singleAttribute(String name, String value) {
        if (this.attributes == null) attributes = new HashMap<>();
        attributes.put(name, Arrays.asList(value));
        return this;
    }

    public List<GroupRepresentation> getSubGroups() {
        if(subGroups == null) {
            subGroups = new ArrayList<>();
        }
        return subGroups;
    }

    public void setSubGroups(List<GroupRepresentation> subGroups) {
        this.subGroups = subGroups;
    }

    public Map<String, Boolean> getAccess() {
        return access;
    }

    public void setAccess(Map<String, Boolean> access) {
        this.access = access;
    }

    public void merge(GroupRepresentation g) {
        merge(this, g);
    }

    private void merge(GroupRepresentation g1, GroupRepresentation g2) {
        if(g1.equals(g2)) {
            Map<String, GroupRepresentation> g1Children = g1.getSubGroups().stream().collect(Collectors.toMap(GroupRepresentation::getId, g -> g));
            Map<String, GroupRepresentation> g2Children = g2.getSubGroups().stream().collect(Collectors.toMap(GroupRepresentation::getId, g -> g));

            g2Children.forEach((key, value) -> {
                if (g1Children.containsKey(key)) {
                    merge(g1Children.get(key), value);
                } else {
                    g1Children.put(key, value);
                }
            });
            g1.setSubGroups(new ArrayList<>(g1Children.values()));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GroupRepresentation that = (GroupRepresentation) o;
        boolean isEqual = Objects.equals(id, that.id) && Objects.equals(parentId, that.parentId);
        if(isEqual) {
            return true;
        } else {
            return Objects.equals(name, that.name) && Objects.equals(path, that.path);
        }
    }

    @Override
    public int hashCode() {
        if(id == null) {
            return Objects.hash(name, path);
        }
        return Objects.hash(id, parentId);
    }
}
