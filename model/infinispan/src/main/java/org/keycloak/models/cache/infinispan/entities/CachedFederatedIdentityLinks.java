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

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.RealmModel;

import java.util.HashSet;
import java.util.Set;

/**
 * The cache entry, which contains list of all identityProvider links for particular user. It needs to be updated every time when any
 * federation link is added, removed or updated for the user
 * 
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CachedFederatedIdentityLinks extends AbstractRevisioned implements InRealm {

    private final String realmId;
    private final Set<FederatedIdentityModel> federatedIdentities = new HashSet<>();

    public CachedFederatedIdentityLinks(Long revision, String id, RealmModel realm, Set<FederatedIdentityModel> federatedIdentities) {
        super(revision, id);
        this.realmId = realm.getId();
        this.federatedIdentities.addAll(federatedIdentities);
    }

    @Override
    public String getRealm() {
        return realmId;
    }

    public Set<FederatedIdentityModel> getFederatedIdentities() {
        return federatedIdentities;
    }
}
