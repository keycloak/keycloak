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

package org.keycloak.models.cache.infinispan.idp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;
import org.keycloak.models.cache.infinispan.entities.InRealm;

public class IdentityProviderListQuery extends AbstractRevisioned implements InRealm {
    private final String realmId;
    private final Map<String, Set<String>> searchKeys;

    public IdentityProviderListQuery(Long revision, String id, RealmModel realm, String searchKey, Set<String> result) {
        super(revision, id);
        this.realmId = realm.getId();
        this.searchKeys = new HashMap<>();
        this.searchKeys.put(searchKey, result);
    }

    public IdentityProviderListQuery(Long revision, String id, RealmModel realm, String searchKey, Set<String> result, IdentityProviderListQuery previous) {
        super(revision, id);
        this.realmId = realm.getId();
        this.searchKeys = new HashMap<>();
        this.searchKeys.putAll(previous.searchKeys);
        this.searchKeys.put(searchKey, result);
    }

    @Override
    public String getRealm() {
        return realmId;
    }

    public Set<String> getIDPs(String searchKey) {
        return searchKeys.get(searchKey);
    }
}
