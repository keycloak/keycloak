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
import org.keycloak.models.cache.infinispan.DefaultLazyLoader;
import org.keycloak.models.cache.infinispan.LazyLoader;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedGroup extends AbstractRevisioned implements InRealm {

    private final String realm;
    private final String name;
    private final String parentId;
    private final LazyLoader<GroupModel, MultivaluedHashMap<String, String>> attributes;
    private final LazyLoader<GroupModel, Set<String>> roleMappings;
    private final LazyLoader<GroupModel, Set<String>> subGroups;

    public CachedGroup(Long revision, RealmModel realm, GroupModel group) {
        super(revision, group.getId());
        this.realm = realm.getId();
        this.name = group.getName();
        this.parentId = group.getParentId();
        this.attributes = new DefaultLazyLoader<>(source -> new MultivaluedHashMap<>(source.getAttributes()), MultivaluedHashMap::new);
        this.roleMappings = new DefaultLazyLoader<>(source -> source.getRoleMappingsStream().map(RoleModel::getId).collect(Collectors.toSet()), Collections::emptySet);
        this.subGroups = new DefaultLazyLoader<>(source -> source.getSubGroupsStream().map(GroupModel::getId).collect(Collectors.toSet()), Collections::emptySet);
    }

    public String getRealm() {
        return realm;
    }

    public MultivaluedHashMap<String, String> getAttributes(Supplier<GroupModel> group) {
        return attributes.get(group);
    }

    public Set<String> getRoleMappings(Supplier<GroupModel> group) {
        // it may happen that groups were not loaded before so we don't actually need to invalidate entries in the cache
        if (group == null) {
            return Collections.emptySet();
        }
        return roleMappings.get(group);
    }

    public String getName() {
        return name;
    }

    public String getParentId() {
        return parentId;
    }

    public Set<String> getSubGroups(Supplier<GroupModel> group) {
        return subGroups.get(group);
    }
}
