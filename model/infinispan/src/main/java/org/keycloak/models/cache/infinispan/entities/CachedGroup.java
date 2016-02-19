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

package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedGroup extends AbstractRevisioned implements InRealm {
    private String realm;
    private String name;
    private String parentId;
    private MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    private Set<String> roleMappings = new HashSet<>();
    private Set<String> subGroups = new HashSet<>();

    public CachedGroup(Long revision, RealmModel realm, GroupModel group) {
        super(revision, group.getId());
        this.realm = realm.getId();
        this.name = group.getName();
        this.parentId = group.getParentId();

        this.attributes.putAll(group.getAttributes());
        for (RoleModel role : group.getRoleMappings()) {
            roleMappings.add(role.getId());
        }
        Set<GroupModel> subGroups1 = group.getSubGroups();
        if (subGroups1 != null) {
            for (GroupModel subGroup : subGroups1) {
                subGroups.add(subGroup.getId());
            }
        }
    }

    public String getRealm() {
        return realm;
    }

    public MultivaluedHashMap<String, String> getAttributes() {
        return attributes;
    }

    public Set<String> getRoleMappings() {
        return roleMappings;
    }

    public String getName() {
        return name;
    }

    public String getParentId() {
        return parentId;
    }

    public Set<String> getSubGroups() {
        return subGroups;
    }
}
