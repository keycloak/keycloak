/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.cache.infinispan.organization;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;
import org.keycloak.models.cache.infinispan.entities.InRealm;

/**
 * Cached entry that stores organization group IDs resulting from queries.
 * This is a lightweight cache entry that only stores group IDs, not the full group objects.
 * Full groups are retrieved from the realm's group cache when needed.
 */
public class CachedOrgGroupIds extends AbstractRevisioned implements InRealm {

    private final String realmId;
    private final List<String> groupIds;

    /**
     * Constructor for caching a stream of group models.
     *
     * @param revision the cache revision number
     * @param id the cache key
     * @param realm the realm
     * @param groups stream of groups to cache (will be consumed and converted to IDs)
     */
    public CachedOrgGroupIds(Long revision, String id, RealmModel realm, Stream<GroupModel> groups) {
        super(revision, id);
        this.realmId = realm.getId();
        Set<String> ids = groups.map(GroupModel::getId).collect(Collectors.toSet());
        groupIds = ids.isEmpty() ? List.of() : List.copyOf(ids);
    }

    /**
     * Returns the cached group IDs.
     *
     * @return immutable collection of group IDs
     */
    public Collection<String> getGroupIds() {
        return groupIds;
    }

    @Override
    public String getRealm() {
        return realmId;
    }
}
