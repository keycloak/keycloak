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
package org.keycloak.models.cache.infinispan.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.models.IssuedVerifiableCredentialModel;
import org.keycloak.models.RealmModel;

public class CachedUserIssuedVerifiableCredentials extends AbstractRevisioned implements InRealm {

    private final List<CachedUserIssuedVerifiableCredential> credentials;
    private final String realmId;

    public CachedUserIssuedVerifiableCredentials(long revision, String id, RealmModel realm, List<IssuedVerifiableCredentialModel> credentials) {
        super(revision, id);
        this.realmId = realm.getId();
        this.credentials = credentials != null
                ? credentials.stream()
                        .map(CachedUserIssuedVerifiableCredential::new)
                        .collect(Collectors.toCollection(ArrayList::new))
                : new ArrayList<>();
    }

    public List<CachedUserIssuedVerifiableCredential> getCredentials() {
        return credentials;
    }

    @Override
    public String getRealm() {
        return realmId;
    }
}
