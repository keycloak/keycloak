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

import java.util.HashMap;
import java.util.List;

import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedUserConsents extends AbstractRevisioned implements InRealm {
    private HashMap<String, CachedUserConsent> consents = new HashMap<>();
    private final String realmId;
    private boolean allConsents;

    public CachedUserConsents(Long revision, String id, RealmModel realm,
                              List<CachedUserConsent> consents) {
        this(revision, id, realm, consents, true);
    }

    public CachedUserConsents(Long revision, String id, RealmModel realm,
            List<CachedUserConsent> consents, boolean allConsents) {
        super(revision, id);
        this.realmId = realm.getId();
        this.allConsents = allConsents;
        if (consents != null) {
            for (CachedUserConsent consent : consents) {
                this.consents.put(consent.getClientDbId(), consent);
            }
        }
    }

    @Override
    public String getRealm() {
        return realmId;
    }


    public HashMap<String, CachedUserConsent> getConsents() {
        return consents;
    }

    public boolean isAllConsents() {
        return allConsents;
    }
}
