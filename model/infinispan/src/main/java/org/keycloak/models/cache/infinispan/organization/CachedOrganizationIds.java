/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;
import org.keycloak.models.cache.infinispan.entities.InRealm;

public class CachedOrganizationIds extends AbstractRevisioned implements InRealm {

    private final String realmId;
    private final List<String> orgIds;

    public CachedOrganizationIds(Long revision, String id, RealmModel realm, OrganizationModel model) {
        super(revision, id);
        this.realmId = realm.getId();
        orgIds = List.of(model.getId());
    }

    public CachedOrganizationIds(Long revision, String id, RealmModel realm, Stream<OrganizationModel> models) {
        super(revision, id);
        this.realmId = realm.getId();
        var ids = models.map(OrganizationModel::getId).collect(Collectors.toSet());
        orgIds = ids.isEmpty() ? List.of() : List.of(ids.toArray(new String[0]));
    }

    public Collection<String> getOrgIds() {
        return orgIds;
    }

    @Override
    public String getRealm() {
        return realmId;
    }
}
